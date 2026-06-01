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
 *   3. If valid → **rehydrate `Constant.serveraddress`** by calling
 *      [ServerConfig.setBaseUrl] with the AuthStore-tracked address. This
 *      is the missing rehydration step PA-10/PA-15 never had: the static
 *      field is process-scoped and was reset to null on every kill-app.
 *   4. If invalid → [AuthStore.clearL2Atomically] (single-transaction wipe
 *      of any partial residue) and return false → caller routes to Login.
 *
 * Synchronous + idempotent. Safe to call multiple times. Main-thread call
 * before any Composable mounts; no race against the AuthStoreImpl ctor.
 *
 * Why this exists (audit reference): under Plan A, `Constant.serveraddress`
 * is the URL truth for every V3*Repository. Only `V3LoginAuthenticator.
 * authenticate()` writes it. A restored session via
 * `LoginRoute.LaunchedEffect(loggedIn)` bounces past Login → Main WITHOUT
 * calling authenticate() → the static stays null on every process restart
 * → repos build `"null/terminal/terzone"` → "加载失败 serveraddress must
 * not be null". Full mechanism in
 * `.state/api-snapshots/auth-split-brain-rootcause.md` §3.
 *
 * Sister memory: `[[static-field-rehydration-trap]]` — the generalized
 * lesson this seam embodies.
 */
interface StartupAuthDecider {
    /**
     * @return true iff the persisted L2 four-tuple is complete and not
     *   expired AND `Constant.serveraddress` has been (re)hydrated via
     *   [ServerConfig.setBaseUrl]. False otherwise (caller routes to Login;
     *   any partial L2 residue has been atomically cleared).
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

        // ★ The rehydration that was missing under Plan A. URL shape mirrors
        // V3LoginAuthenticator.kt:63 — "http://${host}:${port}/api". `address`
        // is non-null at this point (the `valid` predicate above asserted it),
        // but Kotlin can't smart-cast across that intermediate boolean — use !!
        // which is safe because the !valid early-return covered the null case.
        serverConfig.setBaseUrl("http://${address!!.host}:${address.port}/api")
        return true
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
