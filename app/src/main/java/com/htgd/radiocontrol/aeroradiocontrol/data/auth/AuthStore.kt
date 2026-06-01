package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for the session: server address, JWT, refresh token,
 * account name, token expiry, and remember-me flag.
 *
 * ★ NEXT-2 (2026-06-01) — ICD-AuthState bumped v2.1 → v2.2 (additive).
 *
 * Two layers (D-16 / 方案 C):
 *   - **L1 凭据** (rememberMe-controlled, plain): account / serverHost /
 *     serverPort / rememberMe. Survives logout iff rememberMe=true. Used for
 *     LoginScreen prefill UX.
 *   - **L2 鉴权** (encrypted): jwt / refreshToken? / tokenExpiry. Plus
 *     serverAddress (plain, but participates in the L2 atomic invariant
 *     because the URL must be present whenever a session is active).
 *
 * **L2 four-tuple atomic invariant**: {jwt, serverAddress, account,
 * tokenExpiry-not-past} are all-or-none for a "valid session". Any field
 * missing/expired → [clearL2Atomically] + route to LoginScreen. The atomic
 * check is performed by [StartupAuthDecider.resumeSessionIfValid] at app
 * start, BEFORE the Compose tree mounts.
 *
 * Consumers (post-NEXT-2):
 *   - [StartupAuthDecider] reads the four-tuple synchronously at app start
 *     (via .value — initialized in the impl's constructor from commit()-backed
 *     prefs, so no race) and rehydrates `Constant.serveraddress` accordingly.
 *   - [com.htgd.radiocontrol.aeroradiocontrol.data.network.DynamicBaseUrlInterceptor]
 *     reads [serverAddress] to rebuild URLs (DORMANT under Plan A — Retrofit
 *     bypassed by V3CallbackAdapter; kept for Plan B).
 *   - [com.htgd.radiocontrol.aeroradiocontrol.data.network.AuthInterceptor]
 *     reads [jwt] for Bearer; calls [refresh] on 401; on refresh failure
 *     additionally clears the static [com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig]
 *     to keep `Constant.serveraddress` in lockstep with the cleared session.
 *   - LoginViewModel observes [serverAddress] / [account] / [rememberMe] for
 *     prefill; calls [saveLogin] / [clearLogin] / [clearL1Account] /
 *     [setRememberMe] per the 4-path logout matrix (see KDoc on each method).
 *
 * Threading contract (RISK-AR-001):
 *   Interceptors run on OkHttp background threads and read state with a
 *   *blocking* `.value` / `runBlocking { … first() }`. Every mutation publishes
 *   to the StateFlow only AFTER the underlying SharedPreferences `commit()`
 *   returns, so a concurrent reader always observes a fully-consistent
 *   snapshot — never a torn write. Token refresh is serialized (see [refresh])
 *   so a burst of 401s triggers exactly one network refresh.
 *
 * Security contract (soul: Security First):
 *   [jwt], [refreshToken], and [tokenExpiry] live in EncryptedSharedPreferences
 *   ("auth_secure.xml"). Password is NEVER persisted. L1 fields (account,
 *   server host/port, rememberMe) live in plain prefs ("auth_plain.xml") for
 *   convenience prefill. The `auth_secure.xml` file is excluded from Auto
 *   Backup via `res/xml/backup_rules.xml` + `res/xml/data_extraction_rules.xml`
 *   so it does not restore across uninstall (Path A fix).
 */
interface AuthStore {

    // ─── L1 (rememberMe-controlled, plain) ────────────────────────────────

    /** Last logged-in account name (kept across logout to pre-fill login;
     *  cleared by [clearL1Account] when the user toggles rememberMe off). */
    val account: StateFlow<String?>

    /** Whether the user opted to remember the L1 prefill (account + host:port).
     *  Independent of [isLoggedIn] — survives logout. Default: false on a
     *  fresh install (no L1 fields written yet). ★ NEXT-2 addition. */
    val rememberMe: StateFlow<Boolean>

    // ─── L2 (encrypted) + serverAddress (plain, participates in L2 invariant) ─

    /** Current server endpoint, or null before first login / after a reset. */
    val serverAddress: StateFlow<ServerAddress?>

    /** Current JWT (no "Bearer " prefix), or null when logged out. */
    val jwt: StateFlow<String?>

    /** Current refresh token, or null when logged out / unsupported by backend. */
    val refreshToken: StateFlow<String?>

    /** Epoch-millis when [jwt] becomes invalid, or null when unknown. Used by
     *  [StartupAuthDecider] to short-circuit a near-expiry session before
     *  the first server call. ★ NEXT-2 addition. */
    val tokenExpiry: StateFlow<Long?>

    /** True iff a valid JWT is currently held. Convenience for navigation.
     *  Note: this checks JWT presence ONLY; the L2 atomic four-tuple check
     *  ({jwt, serverAddress, account, tokenExpiry-not-past}) is in
     *  [StartupAuthDecider]. Do NOT use [isLoggedIn] alone to gate routing to
     *  Main — it caused the 2026-06-01 kill-app BLOCKER (LoginRoute.kt:32
     *  bounced to Main with `Constant.serveraddress=null`). */
    val isLoggedIn: StateFlow<Boolean>

    // ─── Mutators ──────────────────────────────────────────────────────────

    /**
     * Persists a successful login and publishes it to all flows.
     *
     * @param tokenExpiry epoch-millis when the JWT expires. null = unknown
     *   (legacy 60h-from-issue default applies via [StartupAuthDecider]). The
     *   server now confirms ~60h TTL (PA-15 capture); callers may compute
     *   `System.currentTimeMillis() + 60 * 60 * 60 * 1000` if they have no
     *   server-provided expiry.
     * @param rememberMe whether to also persist L1 fields (account + host +
     *   port) so the next LoginScreen prefills. Default true (typical "logged
     *   in once" UX). Set false when the user explicitly opts out.
     * @param refreshToken the backend refresh token, or null if unsupported.
     *   See OPEN(INQ-O-1 D-1): the legacy stack re-logged-in with stored
     *   credentials; until confirmed, callers may pass null and [refresh] will
     *   report it cannot refresh.
     */
    suspend fun saveLogin(
        address: ServerAddress,
        account: String,
        jwt: String,
        refreshToken: String?,
        tokenExpiry: Long? = null,
        rememberMe: Boolean = true,
    )

    /**
     * Clears the session (L2: JWT + refresh token + tokenExpiry) and publishes
     * null to the token flows. Server address and account (L1) are intentionally
     * retained so the login screen can pre-fill them. This is the **active
     * logout / token expiry / 401** path of the 4-path logout matrix; the
     * caller MUST also clear [com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig]
     * (`setBaseUrl("")`) to keep `Constant.serveraddress` in lockstep.
     *
     * Equivalent to [clearL2Atomically] for the legacy "L2 secrets only" cases.
     * Distinguish from:
     *   - [clearL1Account] — rememberMe toggle off; L2 untouched.
     *   - [reset] — wipe everything (factory reset).
     */
    suspend fun clearLogin()

    /**
     * Atomically clears the L2 four-tuple {jwt, refreshToken, tokenExpiry,
     * serverAddress} so a partially-populated state cannot persist across
     * processes. Used by [StartupAuthDecider] when the invariant fails (any
     * field missing/expired). Single-transaction `commit()` on the secure
     * store; serverAddress lives in plain prefs but is wiped in the same
     * logical step. Idempotent. ★ NEXT-2 addition.
     */
    suspend fun clearL2Atomically()

    /**
     * Clears the L1 prefill fields {account, serverHost, serverPort} and
     * resets [rememberMe] to false in a single transaction. L2 is untouched.
     * Triggered by the LoginScreen rememberMe toggle going OFF. ★ NEXT-2
     * addition.
     */
    suspend fun clearL1Account()

    /** Sets the [rememberMe] flag without touching any credential. Used by
     *  the LoginScreen toggle when the user has not yet submitted a login;
     *  the flag is consulted on the NEXT [saveLogin] to decide whether to
     *  persist L1. ★ NEXT-2 addition. */
    suspend fun setRememberMe(enabled: Boolean)

    /** Wipes everything including server address, account, and rememberMe. */
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
