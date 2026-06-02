package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [AuthStoreImpl] — the data-layer's session source of truth.
 *
 * Runs on a plain JVM (no device / Robolectric) by injecting in-memory
 * [KeyValueStore] fakes and a controllable [TokenRefresher]. The headline test
 * is [concurrentRefresh_runsExactlyOnce] (RISK-AR-001): a burst of 401s must
 * trigger exactly one network refresh.
 */
class AuthStoreImplTest {

    /** In-memory [KeyValueStore] standing in for (Encrypted)SharedPreferences. */
    private class FakeKeyValueStore : KeyValueStore {
        val map = mutableMapOf<String, Any?>()
        override fun getString(key: String): String? = map[key] as? String
        override fun getInt(key: String, default: Int): Int = map[key] as? Int ?: default
        override fun getLong(key: String, default: Long): Long = map[key] as? Long ?: default
        override fun getBoolean(key: String, default: Boolean): Boolean =
            map[key] as? Boolean ?: default
        override fun put(vararg entries: Pair<String, Any?>) {
            for ((k, v) in entries) if (v == null) map.remove(k) else map[k] = v
        }
        override fun clear() = map.clear()
    }

    private val sampleAddress = ServerAddress("192.168.1.10", 8080)

    private fun newStore(
        secure: FakeKeyValueStore = FakeKeyValueStore(),
        plain: FakeKeyValueStore = FakeKeyValueStore(),
        refresher: TokenRefresher = UnsupportedTokenRefresher(),
    ) = AuthStoreImpl(secure, plain, refresher)

    // ── First launch (no credentials) ──────────────────────────────────────

    @Test
    fun firstLaunch_allFlowsNullAndLoggedOut() = runTest {
        val store = newStore()
        assertNull(store.serverAddress.value)
        assertNull(store.jwt.value)
        assertNull(store.refreshToken.value)
        assertNull(store.account.value)
        assertFalse(store.isLoggedIn.value)
    }

    @Test
    fun firstLaunch_rememberMe_defaultsToTrue() = runTest {
        // ★ NEXT-3 (2026-06-01) regression guard: spec D-16 says "rememberMe 默认开".
        // The prior NEXT-2 default was false (implementation gap). A fresh install
        // must start with rememberMe=true so the first login pre-fills on the next
        // open without the user having to explicitly toggle the switch on.
        val store = newStore()
        assertTrue(store.rememberMe.value)
    }

    // ── saveLogin / clearLogin / reset ──────────────────────────────────────

    @Test
    fun saveLogin_publishesAndPersistsAcrossRestart() = runTest {
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        val store = newStore(secure, plain)

        store.saveLogin(sampleAddress, "operator", jwt = "jwt-1", refreshToken = "refresh-1")

        assertEquals(sampleAddress, store.serverAddress.value)
        assertEquals("operator", store.account.value)
        assertEquals("jwt-1", store.jwt.value)
        assertEquals("refresh-1", store.refreshToken.value)
        assertTrue(store.isLoggedIn.value)

        // A fresh instance over the same persisted stores = process restart.
        val restarted = newStore(secure, plain)
        assertEquals(sampleAddress, restarted.serverAddress.value)
        assertEquals("operator", restarted.account.value)
        assertEquals("jwt-1", restarted.jwt.value)
        assertTrue(restarted.isLoggedIn.value)
    }

    @Test
    fun clearLogin_dropsSecretsKeepsAddressAndAccount() = runTest {
        val store = newStore()
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1")

        store.clearLogin()

        assertNull(store.jwt.value)
        assertNull(store.refreshToken.value)
        assertFalse(store.isLoggedIn.value)
        // Address + account kept to pre-fill the next login.
        assertEquals(sampleAddress, store.serverAddress.value)
        assertEquals("operator", store.account.value)
    }

    @Test
    fun reset_wipesEverything() = runTest {
        val store = newStore()
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1")

        store.reset()

        assertNull(store.jwt.value)
        assertNull(store.refreshToken.value)
        assertNull(store.serverAddress.value)
        assertNull(store.account.value)
        assertFalse(store.isLoggedIn.value)
    }

    @Test
    fun saveLogin_withNullRefreshToken_isAllowed() = runTest {
        // OPEN(INQ-O-1 D-1): backend may not issue a refresh token.
        val store = newStore()
        store.saveLogin(sampleAddress, "operator", "jwt-1", refreshToken = null)
        assertEquals("jwt-1", store.jwt.value)
        assertNull(store.refreshToken.value)
        assertTrue(store.isLoggedIn.value)
    }

    // ── ★ NEXT-2: tokenExpiry round-trip + default-args back-compat ───────

    @Test
    fun saveLogin_tokenExpiry_roundTripsThroughSecureStore() = runTest {
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        val store = newStore(secure, plain)
        val exp = 1_700_000_000_000L

        store.saveLogin(
            address = sampleAddress, account = "op",
            jwt = "j", refreshToken = "r",
            tokenExpiry = exp, rememberMe = true,
        )

        // Live read.
        assertEquals(exp, store.tokenExpiry.value)
        // Ctor-read on a fresh impl over the same stores (process restart).
        val restarted = newStore(secure, plain)
        assertEquals(exp, restarted.tokenExpiry.value)
    }

    @Test
    fun ctor_tokenExpiry_zeroSentinel_isExposedAsNull() = runTest {
        // Plain SharedPreferences stores 0L when the key is absent (the
        // KEY_TOKEN_EXPIRY default in AuthStoreImpl). The impl must expose
        // that as null on the StateFlow so consumers don't see a fake "epoch
        // 1970" expiry.
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        val store = newStore(secure, plain)
        assertNull(store.tokenExpiry.value)
    }

    @Test
    fun saveLogin_defaultArgs_preserveV21BackwardCompat() = runTest {
        // v2.1 callers passed 4 args; v2.2 default args (tokenExpiry=null,
        // rememberMe=true) must keep their semantics working.
        val store = newStore()
        store.saveLogin(sampleAddress, "op", "j", refreshToken = "r")

        assertEquals("j", store.jwt.value)
        assertEquals("r", store.refreshToken.value)
        assertEquals(sampleAddress, store.serverAddress.value)
        assertEquals("op", store.account.value)
        // Default rememberMe=true.
        assertTrue(store.rememberMe.value)
        // Default tokenExpiry=null.
        assertNull(store.tokenExpiry.value)
        assertTrue(store.isLoggedIn.value)
    }

    @Test
    fun clearLogin_dropsTokenExpiryAlongsideJwt() = runTest {
        val store = newStore()
        store.saveLogin(sampleAddress, "op", "j", "r", tokenExpiry = 999L, rememberMe = true)
        assertEquals(999L, store.tokenExpiry.value)

        store.clearLogin()

        assertNull(store.tokenExpiry.value)
    }

    // ── refresh: success / failure ──────────────────────────────────────────

    @Test
    fun refresh_success_updatesJwt() = runTest {
        val refresher = TokenRefresher { Result.success("jwt-2") }
        val store = newStore(refresher = refresher)
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1")

        val result = store.refresh(knownStaleJwt = "jwt-1")

        assertTrue(result.isSuccess)
        assertEquals("jwt-2", result.getOrNull())
        assertEquals("jwt-2", store.jwt.value)
        assertTrue(store.isLoggedIn.value)
    }

    @Test
    fun refresh_failure_clearsSession() = runTest {
        val refresher = TokenRefresher { Result.failure(IllegalStateException("rejected")) }
        val store = newStore(refresher = refresher)
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1")

        val result = store.refresh(knownStaleJwt = "jwt-1")

        assertTrue(result.isFailure)
        assertNull(store.jwt.value)
        assertFalse(store.isLoggedIn.value)
        // Address kept so the UI can route back to a pre-filled login.
        assertEquals(sampleAddress, store.serverAddress.value)
    }

    @Test
    fun refresh_unsupportedByDefault_failsAndClears() = runTest {
        // Default binding (UnsupportedTokenRefresher) until D-1 is answered.
        val store = newStore()
        store.saveLogin(sampleAddress, "operator", "jwt-1", refreshToken = null)

        val result = store.refresh(knownStaleJwt = "jwt-1")

        assertTrue(result.isFailure)
        assertNull(store.jwt.value)
    }

    @Test
    fun refresh_whenTokenAlreadyMovedOn_skipsNetwork() = runTest {
        // A caller whose stale token no longer matches the live one (a peer
        // already refreshed) must NOT trigger another network refresh.
        val callCount = AtomicInteger(0)
        val refresher = TokenRefresher {
            callCount.incrementAndGet()
            Result.success("jwt-new")
        }
        val store = newStore(refresher = refresher)
        store.saveLogin(sampleAddress, "operator", "jwt-current", "refresh-1")

        // Caller still holds the old "jwt-stale" it sent; live token is newer.
        val result = store.refresh(knownStaleJwt = "jwt-stale")

        assertTrue(result.isSuccess)
        assertEquals("jwt-current", result.getOrNull())
        assertEquals(0, callCount.get()) // no network call
    }

    @Test
    fun refresh_withNullStaleJwt_forcesNetworkRefresh() = runTest {
        // null = unconditional refresh, even if the live token looks valid.
        val callCount = AtomicInteger(0)
        val refresher = TokenRefresher {
            callCount.incrementAndGet()
            Result.success("jwt-forced")
        }
        val store = newStore(refresher = refresher)
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1")

        val result = store.refresh(knownStaleJwt = null)

        assertTrue(result.isSuccess)
        assertEquals("jwt-forced", store.jwt.value)
        assertEquals(1, callCount.get())
    }

    // ── RISK-AR-001: concurrent refresh runs exactly once ───────────────────

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    @Test
    fun concurrentRefresh_runsExactlyOnce() = runBlocking {
        val callCount = AtomicInteger(0)

        // Run on a real multi-threaded dispatcher (not runTest's single virtual
        // thread) so the Mutex genuinely serializes and suspension truly
        // yields — this mirrors how OkHttp would fire N parallel 401s from its
        // own thread pool, the actual RISK-AR-001 scenario.
        val dispatcher = newFixedThreadPoolContext(8, "refresh-race")

        // The winner of the lock blocks inside the refresher on `barrier` until
        // every caller is in flight, guaranteeing real contention. The latch is
        // released once all 10 callers have entered store.refresh(); whichever
        // holds the lock then completes and publishes jwt-2, after which the
        // queued callers hit the double-check (live jwt "jwt-2" != the stale
        // "jwt-1" they each passed) and return jwt-2 WITHOUT re-refreshing.
        // The dedup keys on the passed stale token, not a pre-lock snapshot, so
        // it is timing-independent — no flaky window regardless of interleave.
        val allInFlight = java.util.concurrent.CountDownLatch(10)
        val barrier = CompletableDeferred<Unit>()
        val refresher = TokenRefresher {
            callCount.incrementAndGet()
            barrier.await()
            Result.success("jwt-2")
        }
        val store = newStore(refresher = refresher)
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1")

        val deferreds = (1..10).map {
            async(dispatcher) {
                allInFlight.countDown()
                store.refresh(knownStaleJwt = "jwt-1")
            }
        }
        // Wait until all callers have reached refresh(), then let the winner
        // through. Any caller blocked on the mutex will re-evaluate after.
        allInFlight.await(2, java.util.concurrent.TimeUnit.SECONDS)
        barrier.complete(Unit)
        val results = deferreds.awaitAll()
        dispatcher.close()

        // Exactly one network refresh despite 10 concurrent callers.
        assertEquals(1, callCount.get())
        // All callers observe the refreshed token.
        assertTrue(results.all { it.isSuccess })
        assertTrue(results.all { it.getOrNull() == "jwt-2" })
        assertEquals("jwt-2", store.jwt.value)
    }
}
