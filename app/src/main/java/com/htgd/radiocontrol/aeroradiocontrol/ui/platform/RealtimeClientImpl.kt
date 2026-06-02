package com.htgd.radiocontrol.aeroradiocontrol.ui.platform

import com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStore
import com.htgd.radiocontrol.aeroradiocontrol.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [RealtimeClient]: one WS connection, heartbeat scheduling,
 * exponential-backoff reconnect, foreground/background pause-resume, and a
 * polling fallback — all serialized so a single instance never races itself
 * (RISK-AR-004). Mirrors the structured-concurrency shape of
 * [com.htgd.radiocontrol.aeroradiocontrol.data.ipc.LocalSocketClientImpl]:
 * injected [IoDispatcher] + an injectable [scope] (test-overridable) + a Mutex
 * guarding all mutable lifecycle state + an injected transport factory seam.
 *
 * What is FINAL here (receipt-independent — TASK-AR-103):
 *   - the [ConnectionState] machine and every transition,
 *   - [ExponentialBackoff] reconnect (2/4/8/16/32 → cap 60s),
 *   - single-instance + foreground-gated reconnect (no storm in background),
 *   - heartbeat *scheduling* (a timer that fires every [HEARTBEAT_INTERVAL_MS]),
 *   - polling-fallback *scheduling* (10s while down).
 *
 * What is a DRAFT PLACEHOLDER (OPEN INQ-O-2 — marked `TODO(O-2)`):
 *   - the WS URL + auth scheme ([buildWsUrl]),
 *   - the heartbeat *frame* text ([heartbeatFrame]),
 *   - message *parsing* ([parseMessage]) and what polling actually fetches.
 * These intentionally do not pretend to be finalized; on receipt they change and
 * drive ICD-BroadcastWS DRAFT→LIVE + ICD_UPDATE (via PM, STD-ICD-WRITE).
 */
@Singleton
class RealtimeClientImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val authStore: AuthStore,
    private val foregroundState: AppForegroundState,
    private val connectionFactory: RealtimeConnectionFactory,
    private val backoff: ExponentialBackoff = ExponentialBackoff(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher),
) : RealtimeClient, RealtimeConnectionCallbacks {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // extraBufferCapacity so emits from the OkHttp callback thread never suspend.
    private val _messages = MutableSharedFlow<BroadcastWSMessage>(extraBufferCapacity = 64)
    override val messages: Flow<BroadcastWSMessage> = _messages.asSharedFlow()

    /** Guards [connection], [reconnectJob], [pollingJob], [heartbeatJob], [reconnectAttempt], [active]. */
    private val mutex = Mutex()

    private var connection: RealtimeConnection? = null
    private var reconnectJob: Job? = null
    private var pollingJob: Job? = null
    private var heartbeatJob: Job? = null

    /** Reconnect attempts since the last successful open. Guarded by [mutex]. */
    private var reconnectAttempt = 0

    /**
     * True between [connect] and [disconnect]: the user wants a connection. The
     * reconnect/foreground machinery only acts while this is set, so a stray
     * timer firing after disconnect() cannot resurrect the socket.
     */
    private var active = false

    /** Observes foreground transitions to resume (fg) / pause reconnect (bg). */
    private val foregroundJob: Job = scope.launch {
        foregroundState.isForeground.collect { foreground ->
            mutex.withLock {
                if (!active) return@withLock
                if (foreground) {
                    // Back to foreground: if we are not connected, try now.
                    if (_connectionState.value != ConnectionState.CONNECTED) openConnectionLocked()
                } else {
                    // Backgrounded: stop hammering reconnects (battery / storm).
                    reconnectJob?.cancel()
                    reconnectJob = null
                }
            }
        }
    }

    override fun connect() {
        scope.launch {
            mutex.withLock {
                if (active) return@withLock // idempotent
                active = true
                reconnectAttempt = 0
                // Only actually open while foreground; otherwise wait for resume.
                if (foregroundState.isForeground.value) openConnectionLocked()
            }
        }
    }

    override fun disconnect() {
        scope.launch {
            mutex.withLock {
                active = false
                reconnectJob?.cancel(); reconnectJob = null
                pollingJob?.cancel(); pollingJob = null
                heartbeatJob?.cancel(); heartbeatJob = null
                runCatching { connection?.close() }
                connection = null
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * Opens the WS using the current session. Must hold [mutex].
     *
     * Bails to [ConnectionState.ERROR]+reconnect if there is no session yet
     * (logged out): the client stays armed and the next foreground/credential
     * change retries — the screen should call connect() after login.
     */
    private fun openConnectionLocked() {
        val address = authStore.serverAddress.value
        val jwt = authStore.jwt.value
        if (address == null || jwt == null) {
            // No session: cannot build an authed URL. Reflect and back off.
            _connectionState.value = ConnectionState.ERROR
            scheduleReconnectLocked()
            return
        }
        _connectionState.value = ConnectionState.CONNECTING
        runCatching {
            // TODO(O-2): buildWsUrl path/scheme/auth are DRAFT assumptions.
            connection = connectionFactory.open(buildWsUrl(address, jwt), this)
        }.onFailure {
            connection = null
            _connectionState.value = ConnectionState.ERROR
            scheduleReconnectLocked()
            startPollingFallbackLocked()
        }
    }

    /** Schedules one backing-off reconnect. Must hold [mutex]; only acts while [active]. */
    private fun scheduleReconnectLocked() {
        if (!active) return
        reconnectJob?.cancel()
        reconnectAttempt++
        val delayMs = backoff.delayMillisForAttempt(reconnectAttempt)
        reconnectJob = scope.launch {
            delay(delayMs)
            mutex.withLock {
                if (!active) return@withLock
                // Only reconnect in foreground; background resume will retry.
                if (foregroundState.isForeground.value &&
                    _connectionState.value != ConnectionState.CONNECTED
                ) {
                    openConnectionLocked()
                }
            }
        }
    }

    /**
     * Starts the 10s polling fallback so data keeps flowing while the WS is down
     * (Handoff §实时性). Must hold [mutex]; no-op if already polling.
     *
     * ⚠ TODO(O-2): the actual fetch (what endpoint, how results map to
     * [BroadcastWSMessage]) waits on the receipt — for now this only drives the
     * POLLING_FALLBACK state on the cadence; the fetch body is a placeholder.
     */
    private fun startPollingFallbackLocked() {
        if (pollingJob?.isActive == true) return
        _connectionState.value = ConnectionState.POLLING_FALLBACK
        pollingJob = scope.launch {
            while (isActive) {
                // TODO(O-2): fetch GET /terminal/terminalinfo (10s) and emit
                // TerminalStateChange per terminal; 30s once a task session ends.
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Starts heartbeat scheduling once connected. Must hold [mutex].
     *
     * ⚠ TODO(O-2): [heartbeatFrame] content + interval + whether the peer even
     * expects an app-level ping (vs protocol ping/pong) are DRAFT (INQ-O-2 §A-3).
     * The *scheduling* (a periodic send while CONNECTED) is what is final here.
     */
    private fun startHeartbeatLocked() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                connection?.send(heartbeatFrame())
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    // ── RealtimeConnectionCallbacks (invoked on OkHttp threads) ───────────────

    override fun onOpen() {
        scope.launch {
            mutex.withLock {
                if (!active) { runCatching { connection?.close() }; return@withLock }
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempt = 0
                reconnectJob?.cancel(); reconnectJob = null
                pollingJob?.cancel(); pollingJob = null // WS back: stop polling
                startHeartbeatLocked()
            }
        }
    }

    override fun onMessage(text: String) {
        // Parse off the callback thread is unnecessary (cheap), but emit via the
        // buffered SharedFlow so we never block OkHttp's reader.
        _messages.tryEmit(parseMessage(text))
    }

    override fun onFailure(t: Throwable) {
        scope.launch {
            mutex.withLock {
                if (!active) return@withLock
                connection = null
                heartbeatJob?.cancel(); heartbeatJob = null
                _connectionState.value = ConnectionState.ERROR
                scheduleReconnectLocked()
                startPollingFallbackLocked()
            }
        }
    }

    override fun onClosed(code: Int, reason: String) {
        scope.launch {
            mutex.withLock {
                if (!active) return@withLock
                connection = null
                heartbeatJob?.cancel(); heartbeatJob = null
                _connectionState.value = ConnectionState.ERROR
                scheduleReconnectLocked()
                startPollingFallbackLocked()
            }
        }
    }

    /**
     * Maps a raw WS text frame to a [BroadcastWSMessage].
     *
     * ⚠ DRAFT PLACEHOLDER (OPEN INQ-O-2 §A-4): there is NO real deserialization
     * yet — field names, the `type` discriminator, and state literals are all
     * unverified. Until the receipt lands every frame is surfaced as
     * [BroadcastWSMessage.Unknown] (R-003 fallback: never drop, never throw), so
     * the message stream is wired end-to-end without faking a finalized schema.
     */
    private fun parseMessage(text: String): BroadcastWSMessage =
        BroadcastWSMessage.Unknown(text) // TODO(O-2): real parse once schema is known

    /** DRAFT heartbeat frame (INQ-O-2 §A-3). */
    private fun heartbeatFrame(): String =
        """{"type":"ping","timestamp":${System.currentTimeMillis()}}""" // TODO(O-2)

    companion object {
        /** DRAFT (INQ-O-2 §A-3): app-level heartbeat cadence while connected. */
        const val HEARTBEAT_INTERVAL_MS = 15_000L

        /** Handoff §实时性: 10s terminal-state polling while the WS is down. */
        const val POLL_INTERVAL_MS = 10_000L
    }
}
