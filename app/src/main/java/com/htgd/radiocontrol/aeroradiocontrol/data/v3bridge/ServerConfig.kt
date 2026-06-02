package com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge

import com.htgd.radiocontrol.aeroradiocontrol.constant.Constant
import com.htgd.radiocontrol.aeroradiocontrol.constant.ServerToken

/**
 * Seam for reading/writing the v3 server base URL **and** the v3 bearer token
 * cache, so data-layer code does not touch `Constant` / `ServerToken` directly
 * (Plan A / TASK-PA-01).
 *
 * Why this exists:
 *   v3's `Constant` (and `ServerToken`) are Java classes whose static initializers
 *   transitively touch Android framework — loading them on a plain JVM unit test
 *   throws `ExceptionInInitializerError`. Hiding the accesses behind this
 *   interface lets the V3 Repository/Authenticator/StartupAuthDecider be
 *   unit-tested with in-memory fakes (the project idiom — no Robolectric — same as
 *   AuthStoreImplTest injects KeyValueStore fakes). Production reads/writes the
 *   real statics; **v3 Constant/ServerToken are NOT otherwise modified** (Plan A
 *   red line — only the new-code access points are made injectable).
 *
 * ★ NEXT-3 (2026-06-01) — [setAuthToken] added:
 *   `RequestManger` reads `ServerToken.serverToken` for the Authorization header
 *   on every request (Plan A D-14). Under a kill-app restart the static field
 *   resets to `""`. [DefaultStartupAuthDecider] must rehydrate it alongside
 *   [setBaseUrl] so the v3 stack is fully operational before any repo refresh
 *   fires. Root cause: [[static-field-rehydration-trap]] applied to the token
 *   cache, not just the URL.
 */
interface ServerConfig {
    /** The current v3 base URL, e.g. "http://host:port/api" (Constant.serveraddress). */
    fun baseUrl(): String

    /** Sets the v3 base URL (mirrors v3 LoginActivity setting Constant.serveraddress). */
    fun setBaseUrl(url: String)

    /**
     * Returns the current v3 bearer token (`ServerToken.serverToken`), including
     * the "Bearer " prefix. Empty string when no session is active (static default
     * on fresh process start, or after a [setBaseUrl]("") logout clear).
     *
     * Used by [V3CallbackAdapter] to fail-fast before dispatching to
     * [com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger]: if the token
     * is blank the request would silently send `Authorization: ` (empty) and get
     * a 500 from the server. Checking here keeps the guard in the seam layer
     * (testable without Android) rather than in the v3 Java primitive.
     */
    fun authToken(): String

    /**
     * Rehydrates the v3 bearer-token cache (`ServerToken.serverToken`) so
     * [com.htgd.radiocontrol.aeroradiocontrol.httptask.RequestManger] attaches
     * the correct Authorization header after a kill-app restart.
     *
     * [bearerToken] must be the FULL header value including the "Bearer " prefix
     * (e.g. `"Bearer eyJ..."`) — mirrors the format
     * [com.htgd.radiocontrol.aeroradiocontrol.data.repository.V3LoginAuthenticator]
     * writes at login time (`Constant.token_tag + rawJwt`).
     *
     * Idempotent: safe to call on every [DefaultStartupAuthDecider.resumeSessionIfValid]
     * hit (including the idempotency re-call case).
     */
    fun setAuthToken(bearerToken: String)
}

/** Production [ServerConfig] backed by v3 `Constant.serveraddress` + `ServerToken`. */
class ConstantServerConfig @javax.inject.Inject constructor() : ServerConfig {
    override fun baseUrl(): String = Constant.serveraddress
    override fun setBaseUrl(url: String) {
        Constant.serveraddress = url
    }
    override fun authToken(): String = ServerToken.serverToken
    override fun setAuthToken(bearerToken: String) {
        ServerToken.serverToken = bearerToken
    }
}
