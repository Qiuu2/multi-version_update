package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for the session: server address, JWT, refresh token,
 * and account name.
 *
 * This is the ICD-AuthState-v2 contract surface. Three consumers depend on it:
 *   - DynamicBaseUrlInterceptor (AR-002) reads [serverAddress] to rebuild URLs.
 *   - AuthInterceptor (AR-002) reads [jwt] to attach the Bearer header and
 *     calls [refresh] on a 401.
 *   - LoginScreen (AR-005) calls [saveLogin] / [clearLogin] and observes the
 *     flows to drive navigation between the login and main screens.
 *
 * Threading contract (the part that matters — RISK-AR-001):
 *   Interceptors run on OkHttp's background threads and read the current token
 *   with a *blocking* read (`jwt.value` / `runBlocking { jwt.first() }`). Every
 *   mutation here publishes to the StateFlow only AFTER the encrypted write has
 *   committed, so a concurrent reader always observes a fully-consistent
 *   snapshot — either the old token or the new one, never a half-written mix.
 *   Token refresh is additionally serialized (see [refresh]) so a burst of
 *   401s triggers exactly one network refresh.
 *
 * Security contract (soul: Security First):
 *   The JWT and refresh token live in EncryptedSharedPreferences. The password
 *   is NEVER persisted by this store. Server address and account name are
 *   non-secret and live in plain prefs so they survive logout (convenience:
 *   the login screen can pre-fill them).
 */
interface AuthStore {

    /** Current server endpoint, or null before first login / after a reset. */
    val serverAddress: StateFlow<ServerAddress?>

    /** Current JWT (no "Bearer " prefix), or null when logged out. */
    val jwt: StateFlow<String?>

    /** Current refresh token, or null when logged out / unsupported by backend. */
    val refreshToken: StateFlow<String?>

    /** Last logged-in account name (kept across logout to pre-fill login). */
    val account: StateFlow<String?>

    /** True iff a valid JWT is currently held. Convenience for navigation. */
    val isLoggedIn: StateFlow<Boolean>

    /**
     * Persists a successful login and publishes it to all flows.
     *
     * @param refreshToken the backend refresh token, or null if the backend
     *   does not issue one. See OPEN(INQ-O-1 D-1): the legacy stack had no
     *   refresh endpoint and re-logged-in with stored credentials instead;
     *   until the backend confirms, callers may pass null and [refresh] will
     *   report it cannot refresh.
     */
    suspend fun saveLogin(
        address: ServerAddress,
        account: String,
        jwt: String,
        refreshToken: String?,
    )

    /**
     * Clears the session (JWT + refresh token) and publishes null to the token
     * flows. Server address and account name are intentionally retained so the
     * login screen can pre-fill them. Use [reset] to wipe everything.
     */
    suspend fun clearLogin()

    /** Wipes everything including server address and account. */
    suspend fun reset()

    /**
     * Refreshes the JWT, serialized so concurrent callers that hit the SAME
     * expired token share one network refresh.
     *
     * @param knownStaleJwt the JWT the caller just saw rejected (the value it
     *   sent on the request that got a 401). The dedup is keyed on this:
     *   - If, once the mutex is acquired, the live JWT no longer equals
     *     [knownStaleJwt], another caller already refreshed past it — return the
     *     live token, no second network call. This is timing-independent: it
     *     does not matter whether the other refresh finished before or after
     *     this caller entered, only that the token moved on.
     *   - Otherwise perform exactly one network refresh under the lock.
     *   Pass null to force an unconditional refresh (e.g. proactive refresh
     *   before a token is known to be stale).
     *
     * On failure the session is cleared (caller should route to login).
     * Returns the (possibly already-refreshed) JWT on success. The network
     * mechanism is pluggable via [TokenRefresher] because its shape is still
     * OPEN(INQ-O-1 D-1): dedicated refresh endpoint vs. credential re-login.
     */
    suspend fun refresh(knownStaleJwt: String?): Result<String>
}
