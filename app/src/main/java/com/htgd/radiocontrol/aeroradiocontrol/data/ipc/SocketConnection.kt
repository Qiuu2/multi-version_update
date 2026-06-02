package com.htgd.radiocontrol.aeroradiocontrol.data.ipc

import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

/**
 * A single open IPC connection: write a line, read a line, close.
 *
 * This seam exists so [LocalSocketClientImpl] can be unit-tested on a plain JVM
 * with an in-memory fake instead of a real socket — the same approach
 * [com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStoreImpl] uses with
 * KeyValueStore. Production uses [TcpSocketConnection]; tests inject a fake.
 *
 * Not thread-safe by itself; [LocalSocketClientImpl] serializes access via a
 * Mutex. All methods are blocking and are called from Dispatchers.IO.
 */
interface SocketConnection : Closeable {
    /** Writes [line] followed by a line terminator and flushes. */
    fun writeLine(line: String)

    /** Reads one response line, or null if the peer closed the stream. */
    fun readLine(): String?
}

/**
 * Opens a [SocketConnection] to a host/port. Injected into
 * [LocalSocketClientImpl] so the connect step is faked in tests.
 */
interface SocketConnectionFactory {
    /**
     * Opens and connects a socket within [connectTimeoutMs], applying
     * [readTimeoutMs] as the per-read SO_TIMEOUT.
     *
     * @throws java.io.IOException on connect failure (the client maps this to
     *   [ConnectionState.ERROR] / [Result.failure]).
     */
    fun open(host: String, port: Int, connectTimeoutMs: Int, readTimeoutMs: Int): SocketConnection
}

/**
 * Production [SocketConnection] over a real [java.net.Socket]. UTF-8 framing,
 * line-oriented, matching what the legacy SocketClient did (PrintWriter +
 * BufferedReader), but with explicit timeouts and resource closing.
 */
class TcpSocketConnection private constructor(
    private val socket: Socket,
    private val reader: BufferedReader,
    private val writer: PrintWriter,
) : SocketConnection {

    override fun writeLine(line: String) {
        writer.println(line)
        writer.flush()
    }

    override fun readLine(): String? = reader.readLine()

    override fun close() {
        // Close best-effort; any of these may already be closed by the peer.
        runCatching { writer.close() }
        runCatching { reader.close() }
        runCatching { socket.close() }
    }

    companion object Factory : SocketConnectionFactory {
        override fun open(
            host: String,
            port: Int,
            connectTimeoutMs: Int,
            readTimeoutMs: Int,
        ): SocketConnection {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
            socket.soTimeout = readTimeoutMs
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), /* autoFlush = */ true)
            return TcpSocketConnection(socket, reader, writer)
        }
    }
}
