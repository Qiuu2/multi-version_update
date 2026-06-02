package com.htgd.radiocontrol.aeroradiocontrol.data.network

import java.io.IOException

/**
 * Typed network-layer exception — the data-integration seam for fe-business-3
 * error rendering (NEXT-3, Task 2, 2026-06-01).
 *
 * All non-happy-path outcomes from the network layer (HTTP error, wrong
 * content-type, parse failure, or absent session) are converted to this type
 * BEFORE reaching the Repository or ViewModel. The UI layer MUST handle each
 * [Kind] independently and MUST NOT render [rawMessage] directly (it may
 * contain a raw HTML error page from the server).
 *
 * Two interception layers:
 *   1. **[AuthInterceptor]** (Retrofit/OkHttp path): fails fast with
 *      [Kind.NO_SESSION] when `jwt` is blank/null or `baseUrl` is empty —
 *      the request is never sent. HTTP non-2xx → [Kind.HTTP_ERROR].
 *   2. **[V3CallbackAdapter]** (v3/RequestManger path): [V3HttpException]
 *      is mapped to [Kind.HTTP_ERROR] when the HTTP code is non-200, or
 *      [Kind.NON_JSON] when the response body looks like HTML.
 *
 * ICD-data-seam (fe-business-3 contract):
 *   - Package: `com.htgd.radiocontrol.aeroradiocontrol.data.network`
 *   - Class: `ApiException`
 *   - Fields: `kind: Kind`, `httpCode: Int?` (null for non-HTTP failures),
 *     `rawMessage: String?` (sanitized/truncated; NEVER render to UI).
 *   - Thrown by: `AuthInterceptor.intercept()`, `V3CallbackAdapter.get()`,
 *     `V3CallbackAdapter.post()`.
 *   - Caught by: Repository `runCatching` → `Result.failure(ApiException)` →
 *     ViewModel maps `Kind` → user-facing copy string.
 *
 * fe-business mapping guidance (sent via SendMessage to fe-business-3):
 *   - [Kind.NO_SESSION]   → route to Login (session gone mid-session)
 *   - [Kind.HTTP_ERROR]   → show error with httpCode if instructive (e.g. 401 =
 *                           "登录已过期"; 500 = "服务器错误"; else "请求失败 (code)")
 *   - [Kind.NON_JSON]     → "服务器返回了非预期格式" (do NOT show rawMessage)
 *   - [Kind.PARSE_FAIL]   → "数据格式解析失败，请联系管理员"
 */
class ApiException(
    val kind: Kind,
    val httpCode: Int? = null,
    /** Sanitized error source (MAY contain raw server text — NEVER render directly). */
    val rawMessage: String? = null,
) : IOException(buildMessage(kind, httpCode, rawMessage)) {

    enum class Kind {
        /** No live session (jwt blank or baseUrl empty) — short-circuit, no request sent. */
        NO_SESSION,
        /** HTTP non-2xx response from the server. */
        HTTP_ERROR,
        /** Server responded 200 but with non-JSON content (e.g. an HTML error page). */
        NON_JSON,
        /** Server responded 200 with JSON, but the JSON could not be parsed. */
        PARSE_FAIL,
    }

    companion object {
        private fun buildMessage(kind: Kind, httpCode: Int?, rawMessage: String?): String {
            val prefix = when (kind) {
                Kind.NO_SESSION -> "no active session"
                Kind.HTTP_ERROR -> "HTTP ${httpCode ?: "?"}"
                Kind.NON_JSON   -> "non-JSON response"
                Kind.PARSE_FAIL -> "JSON parse failure"
            }
            // Truncate raw message to avoid enormous stack traces from HTML pages.
            val suffix = rawMessage
                ?.take(MAX_RAW_LEN)
                ?.let { if (rawMessage.length > MAX_RAW_LEN) "$it…" else it }
                ?.let { ": $it" }
                .orEmpty()
            return "$prefix$suffix"
        }

        /** Maximum characters of raw server response included in the exception message. */
        private const val MAX_RAW_LEN = 200

        /** Returns true iff the string looks like an HTML response (heuristic). */
        fun isHtmlBody(body: String): Boolean =
            body.trimStart().startsWith("<") ||
                body.contains("<html", ignoreCase = true) ||
                body.contains("<!DOCTYPE", ignoreCase = true)
    }
}
