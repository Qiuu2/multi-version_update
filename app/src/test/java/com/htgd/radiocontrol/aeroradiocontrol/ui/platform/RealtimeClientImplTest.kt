package com.htgd.radiocontrol.aeroradiocontrol.ui.platform

import com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStore
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.ServerAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [RealtimeClientImpl] — the receipt-independent WS
 * connection-lifecycle skeleton (TASK-AR-103).
 *
 * Runs on a plain JVM by injecting fakes for all three seams (transport factory,
 * AuthStore, foreground state). The client's internal scope is a test-owned
 * [CoroutineScope] on the [StandardTestDispatcher], cancelled in [tearDown] so
 * the heartbeat / polling periodic loops do not outlive the test.
 *
 * ⚠ Virtual-time discipline: the client runs *unbounded periodic* loops
 * (heartbeat 15s, polling 10s) while connected/down. `advanceUntilIdle()` would
 * therefore never return — these tests step time with bounded [advanceTimeBy] +
 * [runCurrent] instead, which is the correct idiom for code with infinite timers.
 *
 * Focus (per Critic brief): state machine transitions, single-instance +
 * foreground-gated reconnect, exponential backoff wiring, polling fallback, and
 * that the message layer is provably a placeholder (every frame → Unknown).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeClientImplTest {

    private var clientScope: CoroutineScope? = null

    @After
    fun tearDown() {
        // Kill the client's periodic loops (heartbeat/polling/foreground collect).
        clientScope?.cancel()
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeConnection : RealtimeConnection {
        val sent = mutableListOf<String>()
        var closed = false
        override fun send(text: String): Boolean { sent += text; return true }
        override fun close() { closed = true }
    }

    /** Hands out connections and captures the callbacks so tests can drive events. */
    private class FakeFactory : RealtimeConnectionFactory {
        val openCount = AtomicInteger(0)
        var failNextOpen = false
        var lastCallbacks: RealtimeConnectionCallbacks? = null
        var lastUrl: String? = null
        val connections = mutableListOf<FakeConnection>()
        override fun open(url: String, callbacks: RealtimeConnectionCallbacks): RealtimeConnection {
            openCount.incrementAndGet()
            lastUrl = url
            lastCallbacks = callbacks
            if (failNextOpen) { failNextOpen = false; throw IOException("connect refused") }
            return FakeConnection().also { connections += it }
        }
    }

    private class FakeForeground(initial: Boolean = true) : AppForegroundState {
        val flow = MutableStateFlow(initial)
        override val isForeground: StateFlow<Boolean> = flow.asStateFlow()
    }

    /** Minimal AuthStore with a session preset; only the read flows matter here.
     *  ★ NEXT-2 v2.2: additive members default to neutral (null/false) — this
     *  test does not exercise the L1/L2 split. */
    private class FakeAuthStore(loggedIn: Boolean = true) : AuthStore {
        private val _serverAddress = MutableStateFlow(if (loggedIn) ServerAddress("10.0.0.1", 8080) else null)
        private val _jwt = MutableStateFlow(if (loggedIn) "jwt-abc" else null)
        override val serverAddress: StateFlow<ServerAddress?> = _serverAddress.asStateFlow()
        override val jwt: StateFlow<String?> = _jwt.asStateFlow()
        override val refreshToken: StateFlow<String?> = MutableStateFlow(null).asStateFlow()
        override val tokenExpiry: StateFlow<Long?> = MutableStateFlow<Long?>(null).asStateFlow()
        override val account: StateFlow<String?> = MutableStateFlow(null).asStateFlow()
        override val rememberMe: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
        override val isLoggedIn: StateFlow<Boolean> = MutableStateFlow(loggedIn).asStateFlow()
        override suspend fun saveLogin(
            address: ServerAddress,
            account: String,
            jwt: String,
            refreshToken: String?,
            tokenExpiry: Long?,
            rememberMe: Boolean,
        ) = Unit
        override suspend fun clearLogin() = Unit
        override suspend fun clearL2Atomically() = Unit
        override suspend fun clearL1Account() = Unit
        override suspend fun setRememberMe(enabled: Boolean) = Unit
        override suspend fun reset() = Unit
        override suspend fun refresh(knownStaleJwt: String?): Result<String> = Result.failure(IllegalStateException())
    }

    /** Builds a client whose internal scope is on [scope] (test-owned, cancelled in tearDown). */
    private fun newClient(
        factory: FakeFactory,
        foreground: FakeForeground,
        authStore: AuthStore,
        scope: TestScope,
    ): RealtimeClientImpl {
        // Client scope shares the test scheduler (virtual time) but has its OWN
        // Job — NOT a child of the TestScope — so runTest's end-of-body idle check
        // does not await the client's infinite heartbeat/polling loops (which would
        // hang). tearDown() cancels it explicitly.
        val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val s = CoroutineScope(SupervisorJob() + dispatcher)
        clientScope = s
        return RealtimeClientImpl(
            ioDispatcher = dispatcher,
            authStore = authStore,
            foregroundState = foreground,
            connectionFactory = factory,
            scope = s,
        )
    }

    // ── connect / open ──────────────────────────────────────────────────────────

    @Test
    fun connect_inForeground_opensAndOnOpenMovesToConnected() = runTest {
        val factory = FakeFactory()
        val client = newClient(factory, FakeForeground(true), FakeAuthStore(), this)

        client.connect()
        runCurrent()
        assertEquals(ConnectionState.CONNECTING, client.connectionState.value)
        assertEquals(1, factory.openCount.get())
        // DRAFT URL is composed from the session (O-2 boundary, but URL must form).
        assertEquals("ws://10.0.0.1:8080/ws?token=jwt-abc", factory.lastUrl)

        factory.lastCallbacks!!.onOpen()
        runCurrent()
        assertEquals(ConnectionState.CONNECTED, client.connectionState.value)
    }

    @Test
    fun connect_isIdempotent_singleInstance_onlyOpensOnce() = runTest {
        val factory = FakeFactory()
        val client = newClient(factory, FakeForeground(true), FakeAuthStore(), this)

        client.connect()
        client.connect()
        runCurrent()

        assertEquals(1, factory.openCount.get()) // RISK-AR-004: no duplicate connect
    }

    @Test
    fun connect_inBackground_armsButDoesNotOpen_untilForeground() = runTest {
        val factory = FakeFactory()
        val foreground = FakeForeground(initial = false)
        val client = newClient(factory, foreground, FakeAuthStore(), this)

        client.connect()
        runCurrent()
        assertEquals(0, factory.openCount.get()) // background: no socket

        foreground.flow.value = true // resume
        runCurrent()
        assertEquals(1, factory.openCount.get()) // opens on foreground
    }

    @Test
    fun connect_loggedOut_movesToErrorAndDoesNotOpen() = runTest {
        val factory = FakeFactory()
        val client = newClient(factory, FakeForeground(true), FakeAuthStore(loggedIn = false), this)

        client.connect()
        runCurrent()

        assertEquals(0, factory.openCount.get()) // no jwt/address -> no URL to open
        assertEquals(ConnectionState.ERROR, client.connectionState.value)
    }

    // ── reconnect / backoff ───────────────────────────────────────────────────

    @Test
    fun openFailure_movesToPollingFallback_andReconnectsAfterBackoff() = runTest {
        val factory = FakeFactory().apply { failNextOpen = true }
        val client = newClient(factory, FakeForeground(true), FakeAuthStore(), this)

        client.connect()
        runCurrent()
        // Open threw -> ERROR then polling fallback engaged (POLLING_FALLBACK wins
        // as the surfaced state since data still flows via polling).
        assertEquals(ConnectionState.POLLING_FALLBACK, client.connectionState.value)
        assertEquals(1, factory.openCount.get())

        // First backoff is 2s; advancing past it fires the reconnect, which now succeeds.
        advanceTimeBy(2_001)
        runCurrent()
        assertEquals(2, factory.openCount.get())
        factory.lastCallbacks!!.onOpen()
        runCurrent()
        assertEquals(ConnectionState.CONNECTED, client.connectionState.value)
    }

    @Test
    fun onFailureAfterConnected_schedulesReconnect_andPolls() = runTest {
        val factory = FakeFactory()
        val client = newClient(factory, FakeForeground(true), FakeAuthStore(), this)
        client.connect(); runCurrent()
        factory.lastCallbacks!!.onOpen(); runCurrent()
        assertEquals(ConnectionState.CONNECTED, client.connectionState.value)

        // Transport drops.
        factory.lastCallbacks!!.onFailure(IOException("reset by peer"))
        runCurrent()
        assertEquals(ConnectionState.POLLING_FALLBACK, client.connectionState.value)

        advanceTimeBy(2_001); runCurrent()
        assertEquals(2, factory.openCount.get()) // reconnect fired
    }

    @Test
    fun background_pausesReconnect_noStorm() = runTest {
        val factory = FakeFactory().apply { failNextOpen = true }
        val foreground = FakeForeground(true)
        val client = newClient(factory, foreground, FakeAuthStore(), this)
        client.connect(); runCurrent()
        assertEquals(1, factory.openCount.get())

        // Go to background before the backoff fires: reconnect must be cancelled.
        foreground.flow.value = false
        runCurrent()
        advanceTimeBy(120_000) // way past any backoff
        runCurrent()
        assertEquals(1, factory.openCount.get()) // no reconnect while backgrounded
    }

    // ── disconnect ──────────────────────────────────────────────────────────────

    @Test
    fun disconnect_closesAndStopsReconnect() = runTest {
        val factory = FakeFactory()
        val client = newClient(factory, FakeForeground(true), FakeAuthStore(), this)
        client.connect(); runCurrent()
        factory.lastCallbacks!!.onOpen(); runCurrent()

        client.disconnect(); runCurrent()
        assertEquals(ConnectionState.DISCONNECTED, client.connectionState.value)
        assertTrue(factory.connections.first().closed)

        // A late callback after disconnect must not resurrect the connection.
        factory.lastCallbacks!!.onFailure(IOException("late"))
        advanceTimeBy(120_000); runCurrent()
        assertEquals(ConnectionState.DISCONNECTED, client.connectionState.value)
        assertEquals(1, factory.openCount.get())
    }

    // ── heartbeat scheduling ──────────────────────────────────────────────────

    @Test
    fun connected_schedulesHeartbeatFrames() = runTest {
        val factory = FakeFactory()
        val client = newClient(factory, FakeForeground(true), FakeAuthStore(), this)
        client.connect(); runCurrent()
        factory.lastCallbacks!!.onOpen(); runCurrent()

        val conn = factory.connections.first()
        assertEquals(1, conn.sent.size) // first heartbeat fires immediately
        advanceTimeBy(15_001); runCurrent()
        assertEquals(2, conn.sent.size) // second after the interval
        assertTrue(conn.sent.first().contains("\"type\":\"ping\"")) // DRAFT frame
    }

    // ── message layer is a placeholder (NOT finalized) ────────────────────────

    @Test
    fun anyMessage_isSurfacedAsUnknown_pendingO2() = runTest {
        val factory = FakeFactory()
        val client = newClient(factory, FakeForeground(true), FakeAuthStore(), this)
        val received = mutableListOf<BroadcastWSMessage>()
        val collector: Job = backgroundScope.launchCollect(client, received)
        client.connect(); runCurrent()
        factory.lastCallbacks!!.onOpen(); runCurrent()

        // Even a frame shaped like the DRAFT schema is NOT parsed into a typed
        // message — the schema is unverified (O-2). It arrives as Unknown.
        factory.lastCallbacks!!.onMessage("""{"type":"terminal_state","terminalId":"t1"}""")
        runCurrent()

        assertEquals(1, received.size)
        val msg = received.first()
        assertTrue(msg is BroadcastWSMessage.Unknown)
        assertEquals(
            """{"type":"terminal_state","terminalId":"t1"}""",
            (msg as BroadcastWSMessage.Unknown).rawText,
        )
        collector.cancel()
    }

    /** Collects [client] messages into [sink] on the test's auto-cancelled backgroundScope. */
    private fun CoroutineScope.launchCollect(
        client: RealtimeClient,
        sink: MutableList<BroadcastWSMessage>,
    ): Job = launch { client.messages.collect { sink += it } }
}
