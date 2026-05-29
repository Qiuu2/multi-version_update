package com.htgd.radiocontrol.aeroradiocontrol.data.network

import com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStore
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.ServerAddress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewrites the placeholder host that every Retrofit API service is built
 * against into the real per-customer server, and prepends the `/api` base path.
 *
 * Why placeholder + interceptor (not a fixed baseUrl):
 *   The server address is dynamic — each campus has its own LAN host:port, set
 *   at login time. Retrofit needs a non-null baseUrl at construction, so all
 *   API interfaces are built against [PLACEHOLDER_HOST] with relative paths
 *   (`@POST("/authorizations")`), and this interceptor swaps in the real
 *   address per request from [AuthStore.serverAddress].
 *
 * Why `/api` lives here (AR-001 finding):
 *   The legacy stack's effective base is `http://{host}:{port}/api` — the `/api`
 *   segment is part of the base, not of any endpoint constant (`Constant.java`
 *   paths start at `/authorizations`, `/terminal/...`). Keeping `/api` in the
 *   interceptor lets every ApiService use paths identical to the legacy
 *   constants, so the migration is a 1:1 path copy with no `/api` sprinkled in.
 *
 * Ordering (must run BEFORE AuthInterceptor):
 *   This interceptor only touches the URL. AuthInterceptor adds the Bearer
 *   header. Logging runs last. If logging ran before this, it would print the
 *   useless placeholder host; if auth ran before this, nothing breaks today but
 *   the URL the request is signed against would be the placeholder.
 */
@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val authStore: AuthStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Only rewrite requests still pointing at the placeholder. A call that
        // already carries an absolute URL (e.g. a future @Url login probe) is
        // left untouched.
        if (request.url.host != PLACEHOLDER_HOST) {
            return chain.proceed(request)
        }

        val address = currentServerAddress()
        val rewritten = request.url.newBuilder()
            .scheme("http") // plaintext HTTP — known LAN constraint
            .host(address.host)
            .port(address.port)
            // Prepend "/api" ahead of the endpoint's own path: a relative
            // "/authorizations" becomes "/api/authorizations". encodedPath keeps
            // the original path's query-string handling intact.
            .encodedPath("/$API_BASE_SEGMENT" + request.url.encodedPath)
            .build()

        return chain.proceed(request.newBuilder().url(rewritten).build())
    }

    private fun currentServerAddress(): ServerAddress =
        // Blocking read is intentional and safe: AuthStore publishes to its
        // StateFlow only after the encrypted write commits, so this never sees
        // a half-written address. OkHttp already runs us off the main thread.
        runBlocking { authStore.serverAddress.first() }
            ?: throw ServerAddressNotConfiguredException()

    companion object {
        const val PLACEHOLDER_HOST = "placeholder.invalid"
        /** Base path segment shared by all endpoints (legacy base = .../api). */
        const val API_BASE_SEGMENT = "api"
    }
}

/**
 * Thrown when a request reaches the network layer before a server address has
 * been configured (i.e. before login). The UI should route to the login screen.
 */
class ServerAddressNotConfiguredException :
    IOException("Server address not configured — log in first")
