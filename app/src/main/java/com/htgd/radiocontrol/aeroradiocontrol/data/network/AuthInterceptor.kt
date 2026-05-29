package com.htgd.radiocontrol.aeroradiocontrol.data.network

import com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStore
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
 *   cleared the session and we return the original 401 for the UI to handle.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val authStore: AuthStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Login must not carry a (stale/absent) token — it's how you GET one.
        if (isAuthExempt(request)) {
            return chain.proceed(request)
        }

        val jwt = runBlocking { authStore.jwt.first() }
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
            // refresh() already cleared the session on failure; surface a fresh
            // 401 by re-issuing without a token (the server will 401 again,
            // which the UI maps to "session expired → login").
            onFailure = { chain.proceed(request.withBearer(null)) },
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
