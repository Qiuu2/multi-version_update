package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Atomic startup auth check — the NEXT-2 D-16 root-cause-C fix.
 *
 * ★ Single load-bearing seam: call from `V4Activity.onCreate` BEFORE
 * `setContent { … }` to decide whether the Compose tree starts on Main
 * (restored session) or Login. The decision is:
 *
 *   1. Read the L2 four-tuple synchronously via `AuthStore.{jwt,
 *      serverAddress, account, tokenExpiry}.value`. AuthStoreImpl's
 *      constructor populated these from commit()-backed SharedPreferences
 *      reads, so `.value` is correct at app start without flow collection.
 *   2. Check the atomic invariant: all four fields valid (non-blank token,
 *      non-null address, non-blank account, expiry-not-past-clock-now).
 *   3. If valid → rehydrate **both** v3 static caches via [ServerConfig]:
 *        a. [ServerConfig.setBaseUrl] — `Constant.serveraddress` (URL);
 *        b. [ServerConfig.setAuthToken] — `ServerToken.serverToken` (Bearer).
 *      ★ NEXT-3 (2026-06-01): the token-cache rehydration was the missing step
 *      that caused "kill-app → 500". `RequestManger` reads
 *      `ServerToken.serverToken` for the Authorization header; the static resets
 *      to `""` on every process kill. Without rehydration all RequestManger
 *      calls after restart sent `Authorization: ` (empty) → server 500.
 *      Root cause: [[static-field-rehydration-trap]] applied to the token cache.
 *   4. If invalid → [AuthStore.clearL2Atomically] (single-transaction wipe
 *      of any partial residue) and return false → caller routes to Login.
 *
 * Synchronous + idempotent. Safe to call multiple times. Main-thread call
 * before any Composable mounts; no race against the AuthStoreImpl ctor.
 *
 * Why this exists (audit reference): under Plan A, `Constant.serveraddress`
 * is the URL truth for every V3*Repository. Only `V3LoginAuthenticator.
 * authenticate()` writes it (and `ServerToken.serverToken`). A restored
 * session via `LoginRoute.LaunchedEffect(loggedIn)` bounced past Login → Main
 * WITHOUT calling authenticate() → both statics stayed at their empty defaults
 * on every process restart. Full mechanism in
 * `.state/api-snapshots/auth-split-brain-rootcause.md` §3.
 *
 * Sister memory: `[[static-field-rehydration-trap]]` — the generalized
 * lesson this seam embodies.
 */
interface StartupAuthDecider {
    /**
     * @return true iff the persisted L2 four-tuple is complete and not
     *   expired AND both v3 static caches (`Constant.serveraddress` +
     *   `ServerToken.serverToken`) have been (re)hydrated. False otherwise
     *   (caller routes to Login; any partial L2 residue has been atomically
     *   cleared).
     */
    fun resumeSessionIfValid(): Boolean
}

/**
 * Production [StartupAuthDecider].
 *
 * [clock] is injected so tests can pin "now"; production uses
 * `System::currentTimeMillis`. The synchronous `runBlocking` around
 * `clearL2Atomically` is intentional and bounded: V4Activity.onCreate is
 * already a main-thread call, the clear is a single SharedPreferences
 * commit() (microsecond-scale), and matches the existing
 * AuthInterceptor / DynamicBaseUrlInterceptor runBlocking pattern
 * (RISK-AR-001).
 */
@Singleton
class DefaultStartupAuthDecider @Inject constructor(
    private val authStore: AuthStore,
    private val serverConfig: ServerConfig,
    private val clock: Clock = SystemClock,
) : StartupAuthDecider {

    override fun resumeSessionIfValid(): Boolean {
        val jwt = authStore.jwt.value
        val address = authStore.serverAddress.value
        val account = authStore.account.value
        val expiry = authStore.tokenExpiry.value
        val now = clock.now()

        val valid = !jwt.isNullOrBlank()
            && address != null
            && !account.isNullOrBlank()
            // null expiry = unknown → trust the persisted session (legacy
            // 60h-from-issue default applies; the next 401 will clear it).
            && (expiry == null || expiry > now)

        if (!valid) {
            // Wipe any partial residue so the next attempt isn't tricked into
            // the same half-state. Bounded runBlocking — see class KDoc.
            runBlocking { authStore.clearL2Atomically() }
            return false
        }

        // ★ URL rehydration (NEXT-2): `Constant.serveraddress` is process-scoped
        // and resets to null on every kill-app. URL shape mirrors
        // V3LoginAuthenticator.kt:63 — "http://${host}:${port}/api". `address`
        // is non-null here (the `valid` predicate asserted it), but Kotlin can't
        // smart-cast across that boolean — use !! which is safe because the
        // !valid early-return already covered the null case.
        serverConfig.setBaseUrl("http://${address!!.host}:${address.port}/api")

        // ★ Token-cache rehydration (NEXT-3 2026-06-01): `ServerToken.serverToken`
        // is also process-scoped (default ""). `RequestManger` reads it for the
        // Authorization header on every request. Without this line, kill-app →
        // restart sends `Authorization: ` (empty) → server returns 500.
        // Token format mirrors V3LoginAuthenticator:80 (`Constant.token_tag + rawJwt`).
        // `jwt` is non-null/non-blank here (the `valid` predicate asserted it).
        serverConfig.setAuthToken(TOKEN_TAG + jwt!!)
        return true
    }

    private companion object {
        // Mirrors Constant.token_tag — verbatim "Bearer " (with trailing space).
        // Cannot import Constant here (Android static-init trap); literal is
        // confirmed equal per Constant.java:10 `"Bearer "`. Tests verify the
        // format via RecordingServerConfig.authTokenWrites.
        const val TOKEN_TAG = "Bearer "
    }
}

/**
 * Injectable clock seam so [DefaultStartupAuthDecider] is unit-testable
 * without freezing real time. Same idiom as the [TokenRefresher] seam.
 */
fun interface Clock {
    fun now(): Long
}

/** Production [Clock] = `System.currentTimeMillis()`. */
object SystemClock : Clock {
    override fun now(): Long = System.currentTimeMillis()
}
