package com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge

import com.htgd.radiocontrol.aeroradiocontrol.data.network.ApiException
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder
import com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger
import com.htgd.radiocontrol.aeroradiocontrol.httptask.onRequestLister
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Bridges the legacy v3 network primitive ([RequestManger]) — a one-shot
 * OkHttp callback returning the raw JSON string — into a `suspend Result<String>`
 * (Plan A / TASK-PA-01).
 *
 * Design (per CTO D-13 Plan A + PM dispatch):
 *  - We wrap [RequestManger] DIRECTLY (the pure network primitive), NOT the
 *    `httptask.*Method` wrappers — those are coupled to Activity/Context/UI
 *    (`runOnUiThread`, ButtonBox) and also pre-deserialize into v3 POJOs. By
 *    going through RequestManger we get the RAW JSON, which the V3*Repository
 *    then parses with the reverse-engineered Gson DTOs + the existing AR-101
 *    Mapper — so the interface/domain-model/Mapper stack is reused unchanged.
 *  - v3 supplies baseUrl + the Authorization header itself (Constant.serveraddress
 *    + ServerToken.serverToken). Plan A introduces NO interceptors. We only build
 *    the URL from Constant paths and hand it to RequestManger.
 *  - Success/failure: RequestManger already routes HTTP 200 → onSucess, else →
 *    onFailed (EorroCode.SUCESS == 200). We surface that as Result. The
 *    *business* sub-status inside the body (e.g. data[0].state == "15") is a
 *    SEPARATE concern handled by the caller (V3*Repository), not here — this
 *    adapter is pure transport translation.
 *
 * Sync/async safety (I-2): [RequestManger.get]/[postHashMap] use OkHttp
 * `enqueue` (async), but a future/sync callback that fires inline before
 * suspension would still be safe — [suspendCancellableCoroutine] + the
 * `isActive`/`resume`-once guard means an inline callback simply resumes the
 * already-suspended continuation, and a double-fire is ignored. Cancellation
 * just discards the result (v3 exposes no call-cancel handle; the OkHttp call
 * completes harmlessly).
 *
 * ★ NEXT-3 (2026-06-01) — NO_SESSION fail-fast on the v3 live path:
 *   Under Plan A ALL business requests travel V3CallbackAdapter → RequestManger
 *   (Retrofit is dormant). [AuthInterceptor]'s NO_SESSION belt lives on the
 *   dormant Retrofit path. This class is therefore the ACTUAL belt for the live
 *   path: [get] and [post] check [ServerConfig.authToken] and [ServerConfig.baseUrl]
 *   BEFORE dispatching to [RequestManger] — if either is blank the request is
 *   short-circuited as [ApiException.Kind.NO_SESSION] and the socket is never
 *   touched. This:
 *   (a) literally satisfies CTO "缺 token 绝不发任何业务请求" on the live path;
 *   (b) covers mid-session token-loss (e.g. 401 logout clears auth, a stale
 *       coroutine wakes up and tries a refresh) without relying solely on the
 *       Nav-graph断源;
 *   (c) complements Task 1's ServerToken rehydration (rehydrate guarantees the
 *       token is present when a valid session exists; this guard guarantees no
 *       request fires when it isn't).
 *   Exception: [post] with `needToken=false` (login call) is exempt — the login
 *   call is how you GET a token, it must not be blocked by the absence of one.
 */
@Singleton
class V3CallbackAdapter @Inject constructor(
    private val serverConfig: ServerConfig,
) {

    /**
     * GET [url] (absolute, built from Constant.serveraddress + path).
     *
     * Fails fast with [ApiException.Kind.NO_SESSION] if the v3 token or base URL
     * is blank — [RequestManger] is never called in that case.
     */
    suspend fun get(url: String): Result<String> {
        // ★ NEXT-3: NO_SESSION belt on the live v3 path. All GETs carry the
        // Authorization header (RequestManger.java:148 addHeader(token)), so a
        // blank token always means "no session". Short-circuit before the socket.
        val token = serverConfig.authToken()
        val base  = serverConfig.baseUrl()
        if (token.isBlank() || base.isBlank()) {
            return Result.failure(ApiException(
                kind = ApiException.Kind.NO_SESSION,
                rawMessage = "token=${if (token.isBlank()) "blank" else "present"} " +
                    "baseUrl=${if (base.isBlank()) "blank" else "present"}",
            ))
        }

        return suspendCancellableCoroutine { cont ->
            val lister = resumingListener(cont)
            // Guard the dispatch itself: RequestManger.get throws IOException and
            // could fail synchronously (e.g. bad URL) before any callback fires;
            // without this the continuation would never resume → hang.
            try {
                // getInstance() ensures the v3 singleton + OkHttpClient are
                // initialized; get()/postHashMap() are static (call on the class).
                RequestManger.getInstance()
                RequestManger.get(url, lister)
            } catch (t: Throwable) {
                if (cont.isActive) cont.resume(Result.failure(t))
            }
        }
    }

    /**
     * POST (form) via the v3 [MyRequestBuilder].
     *
     * When [MyRequestBuilder.isNeedToken] is true, fails fast with
     * [ApiException.Kind.NO_SESSION] if the v3 token or base URL is blank.
     * Login POSTs (`isNeedToken=false`) are exempt — they're how you GET a token.
     */
    suspend fun post(builder: MyRequestBuilder): Result<String> {
        // ★ NEXT-3: only token-bearing POSTs are guarded. The login call has
        // needToken=false (V3LoginAuthenticator.kt:70) and must bypass this check.
        if (builder.isNeedToken) {
            val token = serverConfig.authToken()
            val base  = serverConfig.baseUrl()
            if (token.isBlank() || base.isBlank()) {
                return Result.failure(ApiException(
                    kind = ApiException.Kind.NO_SESSION,
                    rawMessage = "token=${if (token.isBlank()) "blank" else "present"} " +
                        "baseUrl=${if (base.isBlank()) "blank" else "present"}",
                ))
            }
        }

        return suspendCancellableCoroutine { cont ->
            val lister = resumingListener(cont)
            try {
                RequestManger.getInstance()
                RequestManger.postHashMap(builder, lister)
            } catch (t: Throwable) {
                if (cont.isActive) cont.resume(Result.failure(t))
            }
        }
    }

    /**
     * A one-shot [onRequestLister] that resumes [cont] exactly once. The
     * `isActive` guard makes a double-fire (or a fire after cancellation) a
     * no-op, so an inline/synchronous callback is safe (I-2).
     *
     * ★ NEXT-3 (2026-06-01) — HTML-body sanitization:
     *   `RequestManger.onFailed` fires for any non-200 HTTP code and passes the
     *   raw response body as `message`. If the server returned an HTML error
     *   page (500, gateway error, etc.) that body reaches the ViewModel as the
     *   exception message and the UI renders a wall of HTML text. We intercept
     *   here and convert to a typed [ApiException] so:
     *   - [ApiException.Kind.NON_JSON] is produced for HTML bodies
     *   - [ApiException.Kind.HTTP_ERROR] is produced for non-HTML non-200 bodies
     *   The raw body is still attached as [ApiException.rawMessage] (truncated)
     *   for logging/debugging, but fe-business MUST NOT render it directly.
     */
    private fun resumingListener(
        cont: kotlinx.coroutines.CancellableContinuation<Result<String>>,
    ) = object : onRequestLister {
        override fun onSucess(code: Int, response: String) {
            if (cont.isActive) cont.resume(Result.success(response))
        }
        override fun onFailed(code: Int, message: String) {
            if (!cont.isActive) return
            val failure = if (ApiException.isHtmlBody(message)) {
                // Server returned an HTML error page — wrap as NON_JSON so the
                // UI renders a safe generic message instead of the raw HTML.
                Result.failure<String>(ApiException(
                    kind = ApiException.Kind.NON_JSON,
                    httpCode = code.takeIf { it > 0 },
                    rawMessage = message,
                ))
            } else {
                // Non-200 with a non-HTML body (e.g. JSON error envelope, plain
                // text) — wrap as HTTP_ERROR, preserving the raw message for
                // debug logging but keeping the type distinct.
                Result.failure<String>(ApiException(
                    kind = ApiException.Kind.HTTP_ERROR,
                    httpCode = code.takeIf { it > 0 },
                    rawMessage = message,
                ))
            }
            cont.resume(failure)
        }
    }
}

/**
 * Transport-level failure from the v3 stack: a non-200 HTTP code or an OkHttp
 * IO error, as reported by [onRequestLister.onFailed]. Distinct from a business
 * failure (HTTP 200 + body status code), which the Repository derives.
 */
class V3HttpException(val code: Int, override val message: String) :
    IOException("v3 request failed (code=$code): $message")
