package com.htgd.radiocontrol.aeroradiocontrol.data.ipc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [LocalSocketClientImpl] — the coroutine IPC-socket wrapper.
 *
 * Runs on a plain JVM (no device) by injecting an in-memory
 * [SocketConnectionFactory] fake instead of a real socket, the same seam pattern
 * AuthStoreImplTest uses with KeyValueStore. The TestDispatcher is injected as
 * both the IO dispatcher and the reconnect scope so backoff delays and
 * reconnect coroutines are virtual-time controllable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalSocketClientImplTest {

    /** In-memory [SocketConnection]: canned responses + scriptable failures. */
    private class FakeConnection(
        private val responses: ArrayDeque<String> = ArrayDeque(),
        var failOnWrite: Boolean = false,
    ) : SocketConnection {
        val written = mutableListOf<String>()
        var closed = false
        fun enqueue(vararg lines: String) { responses.addAll(lines) }
        override fun writeLine(line: String) {
            if (failOnWrite) throw IOException("write failed")
            written += line
        }
        override fun readLine(): String? = responses.removeFirstOrNull()
        override fun close() { closed = true }
    }

    /** Factory that hands out queued connections and counts open attempts. */
    private class FakeFactory : SocketConnectionFactory {
        val openCount = AtomicInteger(0)
        private val queue = ArrayDeque<Result<FakeConnection>>()
        var lastConnection: FakeConnection? = null
        fun enqueueSuccess(conn: FakeConnection) { queue.add(Result.success(conn)) }
        fun enqueueFailure() { queue.add(Result.failure(IOException("connect refused"))) }
        override fun open(host: String, port: Int, connectTimeoutMs: Int, readTimeoutMs: Int): SocketConnection {
            openCount.incrementAndGet()
            val next = queue.removeFirstOrNull() ?: Result.success(FakeConnection())
            val conn = next.getOrElse { throw it }
            lastConnection = conn
            return conn
        }
    }

    private fun newClient(factory: FakeFactory, dispatcher: kotlinx.coroutines.CoroutineDispatcher) =
        LocalSocketClientImpl(
            ioDispatcher = dispatcher,
            connectionFactory = factory,
            scope = CoroutineScope(dispatcher),
        )

    // ── connect ──────────────────────────────────────────────────────────────

    @Test
    fun connect_success_movesToConnected() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeFactory().apply { enqueueSuccess(FakeConnection()) }
        val client = newClient(factory, dispatcher)

        val result = client.connect()

        assertTrue(result.isSuccess)
        assertEquals(ConnectionState.CONNECTED, client.connectionStateFlow.value)
        assertEquals(1, factory.openCount.get())
    }

    @Test
    fun connect_isIdempotent_whenAlreadyConnected() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeFactory().apply { enqueueSuccess(FakeConnection()) }
        val client = newClient(factory, dispatcher)

        client.connect()
        val again = client.connect()

        assertTrue(again.isSuccess)
        // Second connect is a no-op: still only one socket opened.
        assertEquals(1, factory.openCount.get())
    }

    @Test
    fun connect_failure_movesToErrorAndSchedulesReconnect() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        // First open fails (-> ERROR + reconnect scheduled); the scheduled
        // reconnect then succeeds.
        val factory = FakeFactory().apply {
            enqueueFailure()
            enqueueSuccess(FakeConnection())
        }
        val client = newClient(factory, dispatcher)

        val result = client.connect()
        assertTrue(result.isFailure)
        assertEquals(ConnectionState.ERROR, client.connectionStateFlow.value)

        // Let the backoff delay elapse so the reconnect coroutine runs.
        advanceUntilIdle()

        assertEquals(ConnectionState.CONNECTED, client.connectionStateFlow.value)
        assertEquals(2, factory.openCount.get()) // initial + 1 reconnect
    }

    // ── sendCommand ──────────────────────────────────────────────────────────

    @Test
    fun sendCommand_whenNotConnected_failsWithoutThrowing() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val client = newClient(FakeFactory(), dispatcher)

        val result = client.sendCommand(ShellCommand.Raw("status"))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is LocalSocketClientImpl.NotConnectedException)
    }

    @Test
    fun sendCommand_writesRawAndReturnsResponseLine() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val conn = FakeConnection().apply { enqueue("OK: status idle") }
        val factory = FakeFactory().apply { enqueueSuccess(conn) }
        val client = newClient(factory, dispatcher)
        client.connect()

        val result = client.sendCommand(ShellCommand.Raw("status"))

        assertTrue(result.isSuccess)
        assertEquals("OK: status idle", result.getOrNull())
        assertEquals(listOf("status"), conn.written) // exact wire payload
    }

    @Test
    fun sendCommand_peerClosed_failsAndDropsConnection() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val conn = FakeConnection() // no queued response -> readLine() == null
        val factory = FakeFactory().apply { enqueueSuccess(conn) }
        val client = newClient(factory, dispatcher)
        client.connect()

        val result = client.sendCommand(ShellCommand.Raw("status"))

        assertTrue(result.isFailure)
        assertEquals(ConnectionState.ERROR, client.connectionStateFlow.value)
        assertTrue(conn.closed)
    }

    @Test
    fun sendCommand_writeFailure_movesToErrorAndCloses() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val conn = FakeConnection(failOnWrite = true)
        val factory = FakeFactory().apply { enqueueSuccess(conn) }
        val client = newClient(factory, dispatcher)
        client.connect()

        val result = client.sendCommand(ShellCommand.Raw("paging start"))

        assertTrue(result.isFailure)
        assertEquals(ConnectionState.ERROR, client.connectionStateFlow.value)
        assertTrue(conn.closed)
    }

    // ── disconnect ─────────────────────────────────────────────────────────────

    @Test
    fun disconnect_closesAndCancelsPendingReconnect() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val conn = FakeConnection()
        val factory = FakeFactory().apply { enqueueSuccess(conn) }
        val client = newClient(factory, dispatcher)
        client.connect()

        client.disconnect()

        assertEquals(ConnectionState.DISCONNECTED, client.connectionStateFlow.value)
        assertTrue(conn.closed)
        assertFalse(client.connectionStateFlow.value == ConnectionState.CONNECTED)
    }

    @Test
    fun disconnect_whenNeverConnected_isSafe() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val client = newClient(FakeFactory(), dispatcher)

        client.disconnect() // must not throw

        assertEquals(ConnectionState.DISCONNECTED, client.connectionStateFlow.value)
    }

    @Test
    fun reconnect_givesUpAfterMaxAttempts() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        // Every open fails: initial + MAX_RECONNECT_ATTEMPTS, then it stops.
        val factory = FakeFactory().apply { repeat(10) { enqueueFailure() } }
        val client = newClient(factory, dispatcher)

        client.connect()
        advanceUntilIdle()

        assertEquals(ConnectionState.ERROR, client.connectionStateFlow.value)
        // initial open + exactly MAX_RECONNECT_ATTEMPTS reconnect tries.
        assertEquals(
            1 + LocalSocketClientImpl.MAX_RECONNECT_ATTEMPTS,
            factory.openCount.get(),
        )
    }
}
