package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ★ NEXT-2: covers the D-16 4-path logout matrix — L2-only path (active logout
 * / 401 / expiry) and L1-only path (rememberMe toggle off) MUST be
 * independent, never mixed.
 *
 * | Trigger                          | Clears L2 | Clears L1 |
 * | -------------------------------- | --------- | --------- |
 * | active logout / 401 / expiry     |    ✓      |    ✗      |   ← clearLogin()
 * | rememberMe toggle = OFF          |    ✗      |    ✓      |   ← clearL1Account()
 * | clearL2Atomically (decider)      |    ✓++    |    ✗      |   ← also wipes serverAddress
 *
 * NB: clearLogin (active logout) preserves serverAddress so the next
 * LoginScreen can prefill host:port; clearL2Atomically is the stronger
 * residue-wipe used by [StartupAuthDecider] when the four-tuple invariant
 * fails on app start.
 */
class LogoutPartialClearTest {

    private class FakeKeyValueStore : KeyValueStore {
        val map = mutableMapOf<String, Any?>()
        var putCallCount: Int = 0
        override fun getString(key: String): String? = map[key] as? String
        override fun getInt(key: String, default: Int): Int = map[key] as? Int ?: default
        override fun getLong(key: String, default: Long): Long = map[key] as? Long ?: default
        override fun getBoolean(key: String, default: Boolean): Boolean =
            map[key] as? Boolean ?: default
        override fun put(vararg entries: Pair<String, Any?>) {
            putCallCount++
            for ((k, v) in entries) if (v == null) map.remove(k) else map[k] = v
        }
        override fun clear() = map.clear()
    }

    private val sampleAddress = ServerAddress("192.168.1.10", 8080)
    private val noopRefresher = TokenRefresher { Result.failure(IllegalStateException("unused")) }

    private fun newStore(): Triple<AuthStoreImpl, FakeKeyValueStore, FakeKeyValueStore> {
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        val store = AuthStoreImpl(secure, plain, noopRefresher)
        return Triple(store, secure, plain)
    }

    // ── L2-only path (clearLogin) ──────────────────────────────────────────

    @Test
    fun clearLogin_dropsL2OnlyKeepsL1Prefill() = runTest {
        val (store, _, _) = newStore()
        store.saveLogin(
            address = sampleAddress, account = "operator",
            jwt = "jwt-1", refreshToken = "refresh-1",
            tokenExpiry = 1_000L, rememberMe = true,
        )

        store.clearLogin()

        // L2 cleared.
        assertNull(store.jwt.value)
        assertNull(store.refreshToken.value)
        assertNull(store.tokenExpiry.value)
        assertFalse(store.isLoggedIn.value)
        // L1 + serverAddress preserved so LoginScreen pre-fills.
        assertEquals(sampleAddress, store.serverAddress.value)
        assertEquals("operator", store.account.value)
        assertTrue(store.rememberMe.value)
    }

    @Test
    fun clearLogin_secureSingleTransaction() = runTest {
        val (store, secure, plain) = newStore()
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1", 1_000L, true)
        val secureBefore = secure.putCallCount
        val plainBefore = plain.putCallCount

        store.clearLogin()

        // L2 secrets cleared in ONE secure-store put (single editor.commit()).
        assertEquals(1, secure.putCallCount - secureBefore)
        // clearLogin does not touch the plain store (L1 + serverAddress preserved).
        assertEquals(0, plain.putCallCount - plainBefore)
    }

    // ── L2++ path (clearL2Atomically — decider residue wipe) ──────────────

    @Test
    fun clearL2Atomically_dropsL2AndServerAddressKeepsL1Account() = runTest {
        val (store, _, _) = newStore()
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1", 1_000L, true)

        store.clearL2Atomically()

        // L2 + serverAddress cleared (residue wipe).
        assertNull(store.jwt.value)
        assertNull(store.refreshToken.value)
        assertNull(store.tokenExpiry.value)
        assertNull(store.serverAddress.value)
        assertFalse(store.isLoggedIn.value)
        // L1 account + rememberMe untouched.
        assertEquals("operator", store.account.value)
        assertTrue(store.rememberMe.value)
    }

    @Test
    fun clearL2Atomically_oneCommitPerStore() = runTest {
        val (store, secure, plain) = newStore()
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1", 1_000L, true)
        val secureBefore = secure.putCallCount
        val plainBefore = plain.putCallCount

        store.clearL2Atomically()

        // One commit on each store (atomicity within each file; cross-store
        // is enforced by the decider's re-check, not multi-file commit).
        assertEquals(1, secure.putCallCount - secureBefore)
        assertEquals(1, plain.putCallCount - plainBefore)
    }

    // ── L1-only path (clearL1Account — rememberMe toggle off) ─────────────

    @Test
    fun clearL1Account_dropsL1OnlyKeepsL2() = runTest {
        val (store, _, _) = newStore()
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1", 1_000L, true)

        store.clearL1Account()

        // L1 cleared.
        assertNull(store.account.value)
        assertNull(store.serverAddress.value)  // host/port live in plain prefs
        assertFalse(store.rememberMe.value)
        // L2 untouched.
        assertEquals("jwt-1", store.jwt.value)
        assertEquals("refresh-1", store.refreshToken.value)
        assertEquals(1_000L, store.tokenExpiry.value)
        assertTrue(store.isLoggedIn.value)
    }

    @Test
    fun clearL1Account_singleTransaction_onPlainStore() = runTest {
        val (store, secure, plain) = newStore()
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1", 1_000L, true)
        val secureBefore = secure.putCallCount
        val plainBefore = plain.putCallCount

        store.clearL1Account()

        // ONE put on plain (single transaction wiping account+host+port+flag).
        assertEquals(1, plain.putCallCount - plainBefore)
        // Secure untouched.
        assertEquals(0, secure.putCallCount - secureBefore)
    }

    // ── Independence ───────────────────────────────────────────────────────

    @Test
    fun clearLogin_thenClearL1Account_doesNotInterfere() = runTest {
        val (store, _, _) = newStore()
        store.saveLogin(sampleAddress, "operator", "jwt-1", "refresh-1", 1_000L, true)

        store.clearLogin()
        // L1 still prefillable here.
        assertEquals("operator", store.account.value)

        store.clearL1Account()
        // Both gone.
        assertNull(store.jwt.value)
        assertNull(store.account.value)
        assertFalse(store.rememberMe.value)
    }
}
