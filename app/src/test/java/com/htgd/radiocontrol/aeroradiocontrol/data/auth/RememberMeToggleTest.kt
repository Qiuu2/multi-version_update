package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ★ NEXT-2: covers the rememberMe (L1 prefill) toggle semantics from D-16:
 *  - setRememberMe(true/false): pure flag write; no credential side-effects.
 *  - saveLogin(rememberMe=true): persists L1 (account+host+port + flag).
 *  - saveLogin(rememberMe=false): records the L1 row WITH the flag = false;
 *    a subsequent restart would see rememberMe=false (prefill suppressed by
 *    the LoginScreen) and the L1 fields are still on disk but UX-ignored.
 *  - Toggling rememberMe OFF after a saveLogin should be done via
 *    [AuthStore.clearL1Account] (single-transaction wipe of account+host+port+
 *    flag); [LogoutPartialClearTest] covers that.
 */
class RememberMeToggleTest {

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
    private val noopRefresher = TokenRefresher { Result.failure(IllegalStateException("unused")) }

    private fun newStore(secure: FakeKeyValueStore, plain: FakeKeyValueStore) =
        AuthStoreImpl(secure, plain, noopRefresher)

    @Test
    fun defaultRememberMe_onFreshInstall_isTrue() = runTest {
        // ★ NEXT-3 (2026-06-01): spec D-16 "rememberMe 默认开". Changed from
        // false (NEXT-2 implementation gap) to true so first-time users get
        // prefill without having to explicitly toggle the switch on.
        val store = newStore(FakeKeyValueStore(), FakeKeyValueStore())
        assertTrue(store.rememberMe.value)
    }

    @Test
    fun setRememberMe_true_persistsFlagOnly_noCredentialSideEffect() = runTest {
        val store = newStore(FakeKeyValueStore(), FakeKeyValueStore())

        store.setRememberMe(true)

        assertTrue(store.rememberMe.value)
        // No credential side-effects.
        assertNull(store.jwt.value)
        assertNull(store.serverAddress.value)
        assertNull(store.account.value)
        assertFalse(store.isLoggedIn.value)
    }

    @Test
    fun setRememberMe_false_persistsFlagOnly_noCredentialSideEffect() = runTest {
        val store = newStore(FakeKeyValueStore(), FakeKeyValueStore())
        store.setRememberMe(true)

        store.setRememberMe(false)

        assertFalse(store.rememberMe.value)
        assertNull(store.account.value)
    }

    @Test
    fun setRememberMe_isPersisted_acrossRestart() = runTest {
        // Toggle true → simulate restart by reconstructing AuthStoreImpl over
        // the same fake stores → assert the flag is preserved.
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        val store1 = newStore(secure, plain)
        store1.setRememberMe(true)

        val store2 = newStore(secure, plain)
        assertTrue(store2.rememberMe.value)
    }

    @Test
    fun saveLogin_rememberMeTrue_writesL1Prefill() = runTest {
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        val store = newStore(secure, plain)

        store.saveLogin(
            address = sampleAddress, account = "operator",
            jwt = "jwt-1", refreshToken = "refresh-1",
            tokenExpiry = 999L, rememberMe = true,
        )

        // L1 written + flag on.
        assertEquals(sampleAddress, store.serverAddress.value)
        assertEquals("operator", store.account.value)
        assertTrue(store.rememberMe.value)
        // Persisted (restart-survivable).
        val restarted = newStore(secure, plain)
        assertEquals(sampleAddress, restarted.serverAddress.value)
        assertEquals("operator", restarted.account.value)
        assertTrue(restarted.rememberMe.value)
    }

    @Test
    fun saveLogin_rememberMeFalse_writesL1RowButFlagOff() = runTest {
        // Decision per D-16: saveLogin records what the user authenticated as;
        // the toggle flag is independent. rememberMe=false on saveLogin lands
        // L1 with the flag off — LoginScreen consults the flag to decide
        // prefill. Toggling off via clearL1Account is the explicit wipe path
        // (covered by LogoutPartialClearTest).
        val secure = FakeKeyValueStore()
        val plain = FakeKeyValueStore()
        val store = newStore(secure, plain)

        store.saveLogin(
            address = sampleAddress, account = "operator",
            jwt = "jwt-1", refreshToken = null,
            tokenExpiry = null, rememberMe = false,
        )

        assertEquals(sampleAddress, store.serverAddress.value)
        assertEquals("operator", store.account.value)
        assertFalse(store.rememberMe.value)
    }

    @Test
    fun rememberMe_flowEmitsToggleTransitions() = runTest {
        // StateFlow .value transitions correctly under repeated toggle.
        // ★ NEXT-3: default is now true; first assertion updated accordingly.
        val store = newStore(FakeKeyValueStore(), FakeKeyValueStore())

        assertTrue(store.rememberMe.value) // default=true (NEXT-3)
        store.setRememberMe(false)
        assertFalse(store.rememberMe.value)
        store.setRememberMe(true)
        assertTrue(store.rememberMe.value)
        store.setRememberMe(false)
        assertFalse(store.rememberMe.value)
    }
}
