package com.htgd.radiocontrol.aeroradiocontrol.data.network

import retrofit2.Response

/**
 * Decides whether a server response counts as a business success.
 *
 * ⚠ SWITCHABLE SKELETON — hard-blocked by OPEN(INQ-O-1 Q3).
 *   The authoritative success signal is not yet confirmed by the backend:
 *     - documented-assumption (legacy `RequestManger`): HTTP 200 == success;
 *       the response envelope is `{ "data": [...] }` with NO `code`/`message`,
 *       so there is no body-level business code to check (AR-001 / OPEN Q2/Q3).
 *     - possible alternative: a body business code (e.g. legacy `EorroCode`
 *       constants) that can say "failed" even on HTTP 200, or "ok" on non-2xx.
 *   We encode the decision behind this interface so that when O-1 Q3 is
 *   answered, only the bound implementation changes — every authenticator /
 *   Repository call site stays put. Until then [HttpStatusSuccessPolicy] is the
 *   default and matches legacy behaviour.
 *
 * Operates on Retrofit's [Response] (the type authenticators / Repositories
 * hold), NOT on the OkHttp interceptor-chain response. Success/failure is a
 * data-layer concern that may need the parsed body (if Q3 turns out to be a body
 * business code); the interceptors only handle transport (URL, auth, 401-retry)
 * and do not consult this policy.
 */
fun interface ResponseSuccessPolicy {
    /** True iff [response] should be treated as a business success. */
    fun isSuccess(response: Response<*>): Boolean
}

/**
 * Default policy (documented-assumption): success == HTTP 2xx, mirroring the
 * legacy stack's `response.code() == 200` check.
 *
 * TODO(INQ-O-1 Q3): if the backend confirms a body business code is
 * authoritative, replace the Hilt binding with a policy that parses the
 * envelope and checks that code. No call site should need to change.
 */
class HttpStatusSuccessPolicy @javax.inject.Inject constructor() : ResponseSuccessPolicy {
    override fun isSuccess(response: Response<*>): Boolean = response.isSuccessful
}
