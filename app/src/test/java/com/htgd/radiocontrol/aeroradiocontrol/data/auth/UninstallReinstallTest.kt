package com.htgd.radiocontrol.aeroradiocontrol.data.auth

import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ★ NEXT-2 root-cause-A regression guard — simulates the
 * uninstall/reinstall path AFTER the backup_rules fix.
 *
 * Pre-fix mechanism (Path A BLOCKER, 2026-05-30): Auto Backup with default
 * `allowBackup="true"` and no exclude rules restored
 * `/data/data/<pkg>/shared_prefs/auth_secure.xml` (the EncryptedSharedPreferences
 * L2 file) across uninstall — the encrypted JWT was decryptable post-reinstall
 * because the Android Keystore MasterKey alias survives. Result: skip-Login
 * resume into a stale session.
 *
 * Post-fix (this test simulates): `data_extraction_rules.xml` +
 * `backup_rules.xml` exclude `auth_secure.xml` from Cloud Backup, Device
 * Transfer, and pre-31 Auto Backup. The plain `auth_plain.xml` (L1 prefill) is
 * intentionally NOT excluded → it CAN restore. Modeled here by:
 *   - secure store CLEARED (uninstall wiped its file; backup_rules said "don't
 *     restore me").
 *   - plain store RETAINED with L1 fields (Auto Backup restored auth_plain.xml).
 *
 * PASS criteria:
 *   - [StartupAuthDecider.resumeSessionIfValid] returns false → LoginScreen.
 *   - L1 prefill (account / serverAddress / rememberMe) is still observable so
 *     the LoginScreen can pre-fill the host:port and account.
 *   - The user MUST re-enter the password (L2 is gone, no resume).
 *
 * Cross-references the canonical backup-rules artifacts under `res/xml/`
 * (NEXT-2 §1 / [[constraint-vs-artifact-rule]]).
 */
class UninstallReinstallTest {

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

    private class RecordingServerConfig : ServerConfig {
        var current: String = ""
        val writes = mutableListOf<String>()
        override fun baseUrl(): String = current
        override fun setBaseUrl(url: String) { current = url; writes.add(url) }
        override fun authToken(): String = ""
        override fun setAuthToken(bearerToken: String) { /* no-op in this test */ }
    }

    private val sampleAddress = ServerAddress("192.168.1.10", 8080)
    private val anyAccount = "operator"
    private val noopRefresher = TokenRefresher { Result.failure(IllegalStateException("unused")) }
    private val fixedClock: Clock = Clock { 1_700_000_000_000L }

    @Test
    fun uninstallReinstall_l1Restored_l2Excluded_landsOnLoginWithPrefill() {
        // Simulate post-uninstall state under the FIXED backup_rules.xml:
        //  - plain store retains L1 (auth_plain.xml NOT excluded → Auto Backup restored)
        //  - secure store is empty (auth_secure.xml excluded → not restored)
        val plain = FakeKeyValueStore().apply {
            put(
                AuthStoreImpl.KEY_HOST to sampleAddress.host,
                AuthStoreImpl.KEY_PORT to sampleAddress.port,
                AuthStoreImpl.KEY_ACCOUNT to anyAccount,
                AuthStoreImpl.KEY_REMEMBER_ME to true,
            )
        }
        val secure = FakeKeyValueStore()  // empty post-uninstall
        val store = AuthStoreImpl(secure, plain, noopRefresher)
        val cfg = RecordingServerConfig()
        val decider = DefaultStartupAuthDecider(store, cfg, fixedClock)

        val resumed = decider.resumeSessionIfValid()

        // ★ Lands on Login (L2 gone) — the user MUST re-authenticate.
        assertFalse(resumed)
        // No URL rehydration on the false path.
        assertTrue(cfg.writes.isEmpty())
        // The decider's clearL2Atomically also wipes serverAddress — so
        // serverAddress goes null after the decider runs (residue wipe). The
        // L1 PREFILL fields (account + rememberMe) are still observable for
        // the LoginScreen to consult.
        assertNull(store.jwt.value)
        assertNull(store.serverAddress.value)
        assertEquals(anyAccount, store.account.value)
        assertTrue(store.rememberMe.value)
        // ★ NOTE: this models the "decider has run" state. Pre-decider, the
        // LoginScreen could read serverAddress for prefill — UX flow is:
        //   1. AuthStoreImpl ctor reads persisted state → serverAddress visible.
        //   2. LoginScreen captures prefill (host:port + account).
        //   3. Decider runs → clears the (incomplete) L2-adjacent serverAddress.
        //   4. User types password → onSubmit() rebuilds the session.
        // The intermediate visibility for the LoginScreen is asserted in the
        // pre-decider state below.
    }

    @Test
    fun uninstallReinstall_preDeciderState_loginScreenSeesPrefill() {
        // Pre-decider — LoginViewModel observes AuthStore.serverAddress /
        // .account before V4Activity runs the decider. This is the moment the
        // LoginScreen reads its prefill flows.
        val plain = FakeKeyValueStore().apply {
            put(
                AuthStoreImpl.KEY_HOST to sampleAddress.host,
                AuthStoreImpl.KEY_PORT to sampleAddress.port,
                AuthStoreImpl.KEY_ACCOUNT to anyAccount,
                AuthStoreImpl.KEY_REMEMBER_ME to true,
            )
        }
        val store = AuthStoreImpl(FakeKeyValueStore(), plain, noopRefresher)

        // L1 is fully observable before any decider runs.
        assertEquals(sampleAddress, store.serverAddress.value)
        assertEquals(anyAccount, store.account.value)
        assertTrue(store.rememberMe.value)
        // L2 absent → not "logged in".
        assertNull(store.jwt.value)
        assertFalse(store.isLoggedIn.value)
    }

    @Test
    fun coldInstall_bothStoresEmpty_landsOnLoginAllBlank() {
        // Cold install path (no Auto Backup at all — first-ever launch on a
        // fresh device, OR uninstall+install where both files were excluded).
        val store = AuthStoreImpl(FakeKeyValueStore(), FakeKeyValueStore(), noopRefresher)
        val cfg = RecordingServerConfig()
        val decider = DefaultStartupAuthDecider(store, cfg, fixedClock)

        assertFalse(decider.resumeSessionIfValid())
        assertTrue(cfg.writes.isEmpty())
        assertNull(store.serverAddress.value)
        assertNull(store.account.value)
        // ★ NEXT-3 (2026-06-01): default is now TRUE (spec D-16 "rememberMe 默认开").
        // A cold install starts with rememberMe=true so the FIRST login pre-fills
        // on the next open without the user needing to toggle the switch.
        assertTrue(store.rememberMe.value)
    }
}
