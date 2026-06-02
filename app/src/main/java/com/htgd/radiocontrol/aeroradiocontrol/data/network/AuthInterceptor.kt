package com.htgd.radiocontrol.aeroradiocontrol.data.network

import com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStore
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the `Authorization: Bearer <jwt>` header to every request except
 * login, and on a 401 refreshes the token once and retries.
 *
 * Ordering (must run AFTER [DynamicBaseUrlInterceptor]):
 *   The base-URL interceptor fixes the host first; this one then decides — from
 *   the resolved path — whether to attach auth. Logging runs last so it sees
 *   both the real host and the header.
 *
 * 401 → refresh → retry (RISK-AR-001 dedup lives in AuthStore):
 *   On a 401 we ask AuthStore to refresh, passing the exact JWT this request
 *   carried (`knownStaleJwt`). AuthStore serializes refresh and dedups by that
 *   token, so a burst of parallel 401s costs one network refresh. If refresh
 *   succeeds we retry once with the new token; if it fails AuthStore has already
 *   cleared the session AND ★ NEXT-2: we additionally call
 *   `serverConfig.setBaseUrl("")` so the static `Constant.serveraddress` cache
 *   is cleared in lockstep with the session — without this, a stale URL would
 *   linger and any unauthenticated retry could still build URLs against it.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authStore: AuthStore,
    private val serverConfig: ServerConfig,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Login must not carry a (stale/absent) token — it's how you GET one.
        if (isAuthExempt(request)) {
            return chain.proceed(request)
        }

        // ★ NEXT-3 (2026-06-01) — fail-fast guard: if the session is absent,
        // short-circuit BEFORE making any network call. Sending a request with
        // an empty or null token is never correct; the server returns 500 or 401.
        // This is the Retrofit-path safety net (the v3/RequestManger path has its
        // own guard in V3CallbackAdapter). Throwing ApiException(NO_SESSION) lets
        // the ViewModel map it to a Login route, not a generic "request failed".
        val baseUrl = serverConfig.baseUrl()
        val jwt = runBlocking { authStore.jwt.first() }
        if (jwt.isNullOrBlank() || baseUrl.isBlank()) {
            throw ApiException(
                kind = ApiException.Kind.NO_SESSION,
                rawMessage = "jwt=${if (jwt.isNullOrBlank()) "blank" else "present"} " +
                    "baseUrl=${if (baseUrl.isBlank()) "blank" else "present"}",
            )
        }

        val response = chain.proceed(request.withBearer(jwt))

        if (response.code != HTTP_UNAUTHORIZED) {
            return response
        }

        // 401 → single refresh+retry. Close the first response body before
        // re-issuing (OkHttp requires the body be consumed/closed).
        response.close()
        val refreshed = runBlocking { authStore.refresh(knownStaleJwt = jwt) }
        return refreshed.fold(
            onSuccess = { newJwt -> chain.proceed(request.withBearer(newJwt)) },
            // refresh() already cleared the session on failure. ★ NEXT-2: also
            // clear the static Constant.serveraddress cache so the next repo
            // call after the inevitable UI route-to-login sees a consistent
            // "no session" state instead of an URL that points at a server
            // we can no longer auth against.
            onFailure = {
                serverConfig.setBaseUrl("")
                // Surface a fresh 401 by re-issuing without a token (the server
                // will 401 again, which the UI maps to "session expired → login").
                chain.proceed(request.withBearer(null))
            },
        )
    }

    /** Requests that must NOT carry the Authorization header. */
    private fun isAuthExempt(request: Request): Boolean =
        request.url.encodedPath.contains(LOGIN_PATH)

    private fun Request.withBearer(jwt: String?): Request {
        val builder = newBuilder().removeHeader(HEADER_AUTHORIZATION)
        if (!jwt.isNullOrBlank()) {
            builder.header(HEADER_AUTHORIZATION, BEARER_PREFIX + jwt)
        }
        return builder.build()
    }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer " // legacy Constant.token_tag, confirm OPEN(D-2)
        const val LOGIN_PATH = "/authorizations"
        const val HTTP_UNAUTHORIZED = 401
    }
}
