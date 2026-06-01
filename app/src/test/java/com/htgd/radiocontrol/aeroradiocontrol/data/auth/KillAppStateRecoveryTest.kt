package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ★ NEXT-2 LOAD-BEARING regression guard: simulates the 2026-06-01 kill-app
 * BLOCKER reproducer.
 *
 * Scenario: a session was persisted (commit-backed SharedPreferences contain
 * {jwt, host, port, account, tokenExpiry-future}), then the process is killed.
 * On restart, a FRESH [AuthStoreImpl] is constructed over the SAME persisted
 * stores — this models exactly what Hilt does when the SingletonComponent is
 * rebuilt at app start. The [StartupAuthDecider] is then asked to make the
 * atomic Main-vs-Login decision.
 *
 * PASS criteria:
 *  - decider returns true (session restored) AND
 *  - [ServerConfig.setBaseUrl] was called with the expected URL (the
 *    rehydration step PA-10/PA-15 never had).
 *
 * Negative cases: each L2 invariant field individually missing/blank/expired
 * → decider returns false AND clears the L2 residue (jwt/refresh/tokenExpiry/
 * serverAddress) atomically so the next attempt isn't tricked into the same
 * half-state. The L1 prefill (account, rememberMe) is preserved.
 *
 * Pre-PA-15 mental model would have made this test PASS the "logged-in" check
 * (jwt-only) and FAIL the rehydration (no setBaseUrl) → repos build
 * `"null/terminal/terzone"` → "加载失败 serveraddress must not be null".
 */
class KillAppStateRecoveryTest {

    /** Plain-JVM stand-in for SharedPreferences (matches AuthStoreImplTest's). */
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

    /** Recording ServerConfig fake — captures every setBaseUrl call. */
    private class RecordingServerConfig : ServerConfig {
        var current: String = ""
        val writes = mutableListOf<String>()
        override fun baseUrl(): String = current
        override fun setBaseUrl(url: String) { current = url; writes.add(url) }
    }

    private val sampleAddress = ServerAddress("192.168.1.10", 8080)
    private val anyAccount = "operator"
    private val anyJwt = "jwt-resumed"
    private val anyRefresh = "refresh-resumed"
    private val tFixedNow = 1_700_000_000_000L
    private val tExpiryFuture = tFixedNow + 60 * 60 * 1000L  // +1h
    private val tExpiryPast = tFixedNow - 60 * 60 * 1000L    // -1h
    private val fixedClock: Clock = Clock { tFixedNow }

    /** Builds AuthStoreImpl over the supplied stores; no TokenRefresher exercise. */
    private fun authStore(secure: FakeKeyValueStore, plain: FakeKeyValueStore): AuthStoreImpl =
        AuthStoreImpl(secure, plain, TokenRefresher { Result.failure(IllegalStateException("unused")) })

    /** Seeds a valid prior session into the two stores (mimics commit-backed
     *  persistence surviving process kill). */
    private fun seedValidSession(
        secure: FakeKeyValueStore,
        plain: FakeKeyValueStore,
        expiry: Long? = tExpiryFuture,
    ) {
        plain.put(
            AuthStoreImpl.KEY_HOST to sampleAddress.host,
            AuthStoreImpl.KEY_PORT to sampleAddress.port,
            AuthStoreImpl.KEY_ACCOUNT to anyAccount,
            AuthStoreImpl.KEY_REMEMBER_ME to true,
        )
        secure.put(
            AuthStoreImpl.KEY_JWT to anyJwt,
            AuthStoreImpl.KEY_REFRESH to anyRefresh,
            AuthStoreImpl.KEY_TOKEN_EXPIRY to expiry,
        )
    }

    // ── Happy path: valid persisted session is resumed ─────────────────────

    @Test
    fun resumeSessionIfValid_persistedSession_returnsTrueAndRehydratesServerConfig() {
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        seedValidSession(secure, plain)
        // Process restart — fresh AuthStoreImpl over the same stores.
        val store = authStore(secure, plain)
        val cfg = RecordingServerConfig()
        val decider = DefaultStartupAuthDecider(store, cfg, fixedClock)

        val resumed = decider.resumeSessionIfValid()

        assertTrue(resumed)
        // ★ The rehydration that was missing pre-NEXT-2.
        assertEquals(
            listOf("http://${sampleAddress.host}:${sampleAddress.port}/api"),
            cfg.writes,
        )
        // Session still intact post-decider.
        assertEquals(anyJwt, store.jwt.value)
        assertEquals(sampleAddress, store.serverAddress.value)
        assertEquals(anyAccount, store.account.value)
        assertTrue(store.isLoggedIn.value)
    }

    @Test
    fun resumeSessionIfValid_nullExpiry_treatedAsLegacyDefault_returnsTrue() {
        // documented-assumption: null expiry = legacy 60h-from-issue default
        // applies; the next 401 will clear if stale. Decider trusts the
        // persisted session.
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        seedValidSession(secure, plain, expiry = null)
        val store = authStore(secure, plain)
        val cfg = RecordingServerConfig()
        val decider = DefaultStartupAuthDecider(store, cfg, fixedClock)

        assertTrue(decider.resumeSessionIfValid())
        assertEquals(1, cfg.writes.size)
    }

    // ── Negative path: each missing L2 field → false + atomic clear ────────

    @Test
    fun resumeSessionIfValid_missingJwt_returnsFalse_clearsResidueAtomically() {
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        seedValidSession(secure, plain)
        secure.put(AuthStoreImpl.KEY_JWT to null)  // residue: everything else still set
        val store = authStore(secure, plain)
        val cfg = RecordingServerConfig()
        val decider = DefaultStartupAuthDecider(store, cfg, fixedClock)

        assertFalse(decider.resumeSessionIfValid())
        // No rehydration on the false path.
        assertTrue(cfg.writes.isEmpty())
        // L2 residue wiped atomically — including serverAddress (clearL2Atomically
        // is stronger than clearLogin).
        assertNull(store.jwt.value)
        assertNull(store.refreshToken.value)
        assertNull(store.tokenExpiry.value)
        assertNull(store.serverAddress.value)
        // L1 prefill preserved (account + rememberMe).
        assertEquals(anyAccount, store.account.value)
        assertTrue(store.rememberMe.value)
    }

    @Test
    fun resumeSessionIfValid_missingServerAddress_returnsFalse_clearsResidue() {
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        seedValidSession(secure, plain)
        plain.put(AuthStoreImpl.KEY_HOST to null, AuthStoreImpl.KEY_PORT to null)
        val store = authStore(secure, plain)
        val cfg = RecordingServerConfig()
        val decider = DefaultStartupAuthDecider(store, cfg, fixedClock)

        assertFalse(decider.resumeSessionIfValid())
        assertTrue(cfg.writes.isEmpty())
        assertNull(store.jwt.value)        // residue cleared
    }

    @Test
    fun resumeSessionIfValid_missingAccount_returnsFalse_clearsResidue() {
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        seedValidSession(secure, plain)
        plain.put(AuthStoreImpl.KEY_ACCOUNT to null)
        val store = authStore(secure, plain)
        val cfg = RecordingServerConfig()
        val decider = DefaultStartupAuthDecider(store, cfg, fixedClock)

        assertFalse(decider.resumeSessionIfValid())
        assertNull(store.jwt.value)
    }

    @Test
    fun resumeSessionIfValid_expiredToken_returnsFalse_clearsResidue() {
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        seedValidSession(secure, plain, expiry = tExpiryPast)
        val store = authStore(secure, plain)
        val cfg = RecordingServerConfig()
        val decider = DefaultStartupAuthDecider(store, cfg, fixedClock)

        assertFalse(decider.resumeSessionIfValid())
        assertNull(store.jwt.value)
        assertNull(store.tokenExpiry.value)
    }

    @Test
    fun resumeSessionIfValid_blankJwt_returnsFalse() {
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        seedValidSession(secure, plain)
        secure.put(AuthStoreImpl.KEY_JWT to "   ")  // whitespace = blank
        val store = authStore(secure, plain)
        val cfg = RecordingServerConfig()
        val decider = DefaultStartupAuthDecider(store, cfg, fixedClock)

        assertFalse(decider.resumeSessionIfValid())
    }

    @Test
    fun resumeSessionIfValid_freshInstall_emptyStores_returnsFalse() {
        // No prior session at all — typical first launch.
        val store = authStore(FakeKeyValueStore(), FakeKeyValueStore())
        val cfg = RecordingServerConfig()
        val decider = DefaultStartupAuthDecider(store, cfg, fixedClock)

        assertFalse(decider.resumeSessionIfValid())
        assertTrue(cfg.writes.isEmpty())
    }

    // ── Idempotency ────────────────────────────────────────────────────────

    @Test
    fun resumeSessionIfValid_calledTwice_isIdempotentOnValidSession() {
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        seedValidSession(secure, plain)
        val store = authStore(secure, plain)
        val cfg = RecordingServerConfig()
        val decider = DefaultStartupAuthDecider(store, cfg, fixedClock)

        assertTrue(decider.resumeSessionIfValid())
        assertTrue(decider.resumeSessionIfValid())
        // Second call re-rehydrates (writes the same URL again) — observable
        // idempotency: state matches.
        assertEquals(2, cfg.writes.size)
        assertTrue(cfg.writes.all { it == "http://${sampleAddress.host}:${sampleAddress.port}/api" })
    }
}
