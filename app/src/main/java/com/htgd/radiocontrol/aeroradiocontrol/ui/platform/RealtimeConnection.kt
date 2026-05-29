package com.htgd.radiocontrol.aeroradiocontrol.ui.platform

import com.htgd.radiocontrol.aeroradiocontrol.data.auth.ServerAddress
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens a WS connection and routes its lifecycle callbacks to [callbacks].
 *
 * Seam over OkHttp's `newWebSocket`, mirroring the IPC layer's
 * [com.htgd.radiocontrol.aeroradiocontrol.data.ipc.SocketConnectionFactory]: the
 * production impl wraps a real socket, tests inject a fake that drives
 * onOpen/onMessage/onFailure/onClosed in virtual time without any network. This
 * is what makes the connection-lifecycle/backoff/state-machine logic in
 * [RealtimeClientImpl] unit-testable on a plain JVM.
 *
 * ⚠ Receipt boundary (TASK-AR-103 / OPEN INQ-O-2): the URL composition
 * (`buildWsUrl`) — path, scheme, and how the JWT is carried — is a DRAFT
 * assumption. The auth scheme question (query token vs header vs first-frame) is
 * INQ-O-2 §A-2; resolved on receipt, then ICD-BroadcastWS DRAFT→LIVE via PM.
 */
interface RealtimeConnection {
    /** Sends a raw text frame (used by heartbeat scheduling). Returns false if the socket is gone. */
    fun send(text: String): Boolean

    /** Closes the socket with a normal-closure code. */
    fun close()
}

/** Callbacks the client implements; the connection invokes them on OkHttp threads. */
interface RealtimeConnectionCallbacks {
    fun onOpen()
    fun onMessage(text: String)
    fun onFailure(t: Throwable)
    fun onClosed(code: Int, reason: String)
}

/** Opens a [RealtimeConnection]. Injected so tests fake the transport. */
interface RealtimeConnectionFactory {
    fun open(url: String, callbacks: RealtimeConnectionCallbacks): RealtimeConnection
}

/**
 * Production factory over the shared singleton [OkHttpClient] (the one
 * AppModule provides — RISK-AUDIT-05: one client, one connection pool).
 *
 * NOTE the WS request is built here directly from a fully-formed [url] and does
 * NOT pass through DynamicBaseUrlInterceptor's `placeholder.invalid` rewrite /
 * `/api` prefix — that interceptor is for REST. The WS URL is composed by
 * [buildWsUrl] from the live [ServerAddress].
 */
@Singleton
class OkHttpRealtimeConnectionFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : RealtimeConnectionFactory {

    override fun open(url: String, callbacks: RealtimeConnectionCallbacks): RealtimeConnection {
        val request = Request.Builder().url(url).build()
        val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) = callbacks.onOpen()
            override fun onMessage(webSocket: WebSocket, text: String) = callbacks.onMessage(text)
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) =
                callbacks.onFailure(t)
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                // Ack the peer's close handshake, then treat as closed.
                webSocket.close(NORMAL_CLOSURE, null)
                callbacks.onClosed(code, reason)
            }
        })
        return object : RealtimeConnection {
            override fun send(text: String): Boolean = ws.send(text)
            override fun close() { ws.close(NORMAL_CLOSURE, null) }
        }
    }

    companion object {
        const val NORMAL_CLOSURE = 1000
    }
}

/**
 * Builds the WS URL from a server address and JWT.
 *
 * ⚠ DRAFT (OPEN INQ-O-2 §A-1/§A-2): `ws://` (plaintext — consistent with the
 * LAN's plaintext HTTP and the REST interceptor hardcoding `http`), path `/ws`,
 * JWT as a `token` query param. All three are unverified vendor assumptions to
 * be confirmed/corrected on the O-2 receipt. Kept in one function so the receipt
 * fix is a single edit.
 */
fun buildWsUrl(address: ServerAddress, jwt: String): String =
    "ws://${address.host}:${address.port}/ws?token=$jwt" // TODO(O-2): path / scheme / auth scheme
