package com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge

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
 */
@Singleton
class V3CallbackAdapter @Inject constructor() {

    /** GET [url] (absolute, built from Constant.serveraddress + path). */
    suspend fun get(url: String): Result<String> =
        suspendCancellableCoroutine { cont ->
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

    /** POST (form) via the v3 [MyRequestBuilder]. */
    suspend fun post(builder: MyRequestBuilder): Result<String> =
        suspendCancellableCoroutine { cont ->
            val lister = resumingListener(cont)
            try {
                RequestManger.getInstance()
                RequestManger.postHashMap(builder, lister)
            } catch (t: Throwable) {
                if (cont.isActive) cont.resume(Result.failure(t))
            }
        }

    /**
     * A one-shot [onRequestLister] that resumes [cont] exactly once. The
     * `isActive` guard makes a double-fire (or a fire after cancellation) a
     * no-op, so an inline/synchronous callback is safe (I-2).
     */
    private fun resumingListener(
        cont: kotlinx.coroutines.CancellableContinuation<Result<String>>,
    ) = object : onRequestLister {
        override fun onSucess(code: Int, response: String) {
            if (cont.isActive) cont.resume(Result.success(response))
        }
        override fun onFailed(code: Int, message: String) {
            if (cont.isActive) cont.resume(Result.failure(V3HttpException(code, message)))
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
