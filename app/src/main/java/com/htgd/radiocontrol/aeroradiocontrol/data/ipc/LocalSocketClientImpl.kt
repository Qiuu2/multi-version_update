package com.htgd.radiocontrol.aeroradiocontrol.data.ipc

import com.htgd.radiocontrol.aeroradiocontrol.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [LocalSocketClient] over the on-device daemon at 127.0.0.1:4521.
 *
 * See [LocalSocketClient] for the contract. The transport is injected
 * ([SocketConnectionFactory]) and all blocking I/O runs on [ioDispatcher], so
 * the connect / reconnect / send logic is unit-testable on a plain JVM without
 * a real socket (mirrors AuthStoreImpl's KeyValueStore seam).
 *
 * Concurrency: a single [ioMutex] serializes connect / disconnect / send so the
 * mutable [connection] reference and each command's write+read pair are never
 * interleaved across coroutines.
 */
@Singleton
class LocalSocketClientImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val connectionFactory: SocketConnectionFactory,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher),
) : LocalSocketClient {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionStateFlow: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val ioMutex = Mutex()

    /** The live connection, or null when disconnected. Guarded by [ioMutex]. */
    private var connection: SocketConnection? = null

    /** Reconnect attempts since the last successful connect. Guarded by [ioMutex]. */
    private var reconnectAttempt = 0

    override suspend fun connect(): Result<Unit> = withContext(ioDispatcher) {
        ioMutex.withLock { connectLocked() }
    }

    /** Must be called holding [ioMutex]. */
    private fun connectLocked(): Result<Unit> {
        if (connection != null && _connectionState.value == ConnectionState.CONNECTED) {
            return Result.success(Unit) // idempotent
        }
        _connectionState.value = ConnectionState.CONNECTING
        return runCatching {
            connection = connectionFactory.open(
                HOST,
                PORT,
                CONNECT_TIMEOUT_MS,
                READ_TIMEOUT_MS,
            )
            _connectionState.value = ConnectionState.CONNECTED
            reconnectAttempt = 0
        }.onFailure {
            connection = null
            _connectionState.value = ConnectionState.ERROR
            scheduleReconnect()
        }
    }

    override suspend fun disconnect() {
        withContext(ioDispatcher) {
            ioMutex.withLock {
                // A user-requested disconnect cancels any pending reconnect.
                reconnectAttempt = MAX_RECONNECT_ATTEMPTS
                runCatching { connection?.close() }
                connection = null
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    override suspend fun sendCommand(command: ShellCommand): Result<String> =
        withContext(ioDispatcher) {
            ioMutex.withLock {
                val conn = connection
                if (conn == null || _connectionState.value != ConnectionState.CONNECTED) {
                    return@withLock Result.failure(NotConnectedException())
                }
                runCatching {
                    conn.writeLine(command.raw)
                    conn.readLine() ?: throw java.io.IOException("Connection closed by peer")
                }.onFailure {
                    // Transport broke mid-command: drop the dead socket and reflect it.
                    runCatching { conn.close() }
                    connection = null
                    _connectionState.value = ConnectionState.ERROR
                    scheduleReconnect()
                }
            }
        }

    /**
     * Schedules a bounded, backing-off reconnect. Must be called holding
     * [ioMutex] (it reads/writes [reconnectAttempt]). After
     * [MAX_RECONNECT_ATTEMPTS] it gives up until the next explicit [connect].
     */
    private fun scheduleReconnect() {
        if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) return
        reconnectAttempt++
        val backoffMs = RECONNECT_BASE_DELAY_MS * reconnectAttempt
        scope.launch {
            delay(backoffMs)
            ioMutex.withLock {
                // Skip if a concurrent connect()/disconnect() already resolved it.
                if (_connectionState.value == ConnectionState.ERROR) connectLocked()
            }
        }
    }

    class NotConnectedException : IllegalStateException("Local IPC socket is not connected")

    companion object {
        const val HOST = "127.0.0.1"
        const val PORT = 4521
        const val CONNECT_TIMEOUT_MS = 5_000
        const val READ_TIMEOUT_MS = 3_000 // matches legacy SocketClient SO_TIMEOUT
        const val MAX_RECONNECT_ATTEMPTS = 3
        const val RECONNECT_BASE_DELAY_MS = 2_000L
    }
}
