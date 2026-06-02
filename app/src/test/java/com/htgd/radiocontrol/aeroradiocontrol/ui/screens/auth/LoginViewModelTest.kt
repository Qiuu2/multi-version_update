package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStore
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.ServerAddress
import com.htgd.radiocontrol.aeroradiocontrol.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [LoginViewModel] (TASK-AR-005).
 *
 * Uses an in-memory [FakeAuthStore] (captures saveLogin args, lets tests flip
 * isLoggedIn) and a controllable [LoginAuthenticator] lambda, so the submit
 * lifecycle and the AuthResult→saveLogin mapping are verified without a device.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    /** Records the last [saveLogin] call so tests assert the 1:1 mapping.
     *  ★ NEXT-2 v2.2: tokenExpiry + rememberMe added.
     *  ★ PA-NEXT2-FE 2026-06-01: SaveLoginArgs extended to capture all 6 args
     *  so the rememberMe-propagation tests can assert on the recorded value. */
    private class FakeAuthStore : AuthStore {
        val _serverAddress = MutableStateFlow<ServerAddress?>(null)
        val _jwt = MutableStateFlow<String?>(null)
        val _refreshToken = MutableStateFlow<String?>(null)
        val _tokenExpiry = MutableStateFlow<Long?>(null)
        val _account = MutableStateFlow<String?>(null)
        val _rememberMe = MutableStateFlow(false)
        val _isLoggedIn = MutableStateFlow(false)

        var saveLoginArgs: SaveLoginArgs? = null

        override val serverAddress: StateFlow<ServerAddress?> = _serverAddress.asStateFlow()
        override val jwt: StateFlow<String?> = _jwt.asStateFlow()
        override val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()
        override val tokenExpiry: StateFlow<Long?> = _tokenExpiry.asStateFlow()
        override val account: StateFlow<String?> = _account.asStateFlow()
        override val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()
        override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

        override suspend fun saveLogin(
            address: ServerAddress,
            account: String,
            jwt: String,
            refreshToken: String?,
            tokenExpiry: Long?,
            rememberMe: Boolean,
        ) {
            saveLoginArgs = SaveLoginArgs(address, account, jwt, refreshToken, tokenExpiry, rememberMe)
            _serverAddress.value = address
            _account.value = account
            _jwt.value = jwt
            _refreshToken.value = refreshToken
            _tokenExpiry.value = tokenExpiry
            _rememberMe.value = rememberMe
            _isLoggedIn.value = true
        }

        override suspend fun clearLogin() { _jwt.value = null; _isLoggedIn.value = false }
        override suspend fun clearL2Atomically() { clearLogin() }
        override suspend fun clearL1Account() {
            _account.value = null; _serverAddress.value = null; _rememberMe.value = false
        }
        override suspend fun setRememberMe(enabled: Boolean) { _rememberMe.value = enabled }
        override suspend fun reset() { clearLogin(); _serverAddress.value = null; _account.value = null }
        override suspend fun refresh(knownStaleJwt: String?): Result<String> = Result.failure(IllegalStateException("unused"))
    }

    private data class SaveLoginArgs(
        val address: ServerAddress,
        val account: String,
        val jwt: String,
        val refreshToken: String?,
        val tokenExpiry: Long?,
        val rememberMe: Boolean,
    )

    private fun authenticatorReturning(result: Result<AuthResult>) =
        LoginAuthenticator { _, _, _ -> result }

    @Test
    fun `invalid port fails validation and never calls authenticator`() = runTest {
        var called = false
        val store = FakeAuthStore()
        val vm = LoginViewModel(store, LoginAuthenticator { _, _, _ -> called = true; Result.success(AuthResult("j")) })

        vm.onSubmit(account = "admin", password = "pw", ip = "192.168.1.1", port = "abc", remember = true)

        assertEquals("端口必须是数字", vm.uiState.value.fieldErrors.server)
        assertTrue("authenticator must not run on a validation failure", !called)
        assertNull(store.saveLoginArgs)
    }

    @Test
    fun `successful login maps AuthResult onto saveLogin and reaches Success`() = runTest {
        val store = FakeAuthStore()
        val vm = LoginViewModel(
            store,
            authenticatorReturning(Result.success(AuthResult(jwt = "JWT123", refreshToken = "R1", account = null))),
        )

        vm.onSubmit(account = "admin", password = "pw", ip = "192.168.1.10", port = "8080", remember = true)

        assertEquals(LoginUiState.Phase.Success, vm.uiState.value.phase)
        val args = store.saveLoginArgs!!
        assertEquals(ServerAddress("192.168.1.10", 8080), args.address)
        assertEquals("admin", args.account)        // null override → typed account
        assertEquals("JWT123", args.jwt)
        assertEquals("R1", args.refreshToken)
        assertTrue(store.isLoggedIn.value)
    }

    @Test
    fun `server-canonicalized account from AuthResult overrides the typed one`() = runTest {
        val store = FakeAuthStore()
        val vm = LoginViewModel(
            store,
            authenticatorReturning(Result.success(AuthResult(jwt = "J", account = "ADMIN_CANON"))),
        )

        vm.onSubmit(account = "admin", password = "pw", ip = "10.0.0.1", port = "80", remember = true)

        assertEquals("ADMIN_CANON", store.saveLoginArgs!!.account)
    }

    @Test
    fun `host-only address parses with default port`() = runTest {
        val store = FakeAuthStore()
        val vm = LoginViewModel(store, authenticatorReturning(Result.success(AuthResult("J"))))

        vm.onSubmit(account = "a", password = "p", ip = "10.0.0.5", port = "", remember = true)

        assertEquals(ServerAddress("10.0.0.5", ServerAddress.DEFAULT_PORT), store.saveLoginArgs!!.address)
    }

    @Test
    fun `auth failure surfaces a form error and does not persist`() = runTest {
        val store = FakeAuthStore()
        val vm = LoginViewModel(
            store,
            authenticatorReturning(Result.failure(IllegalArgumentException("账号或密码错误"))),
        )

        vm.onSubmit(account = "admin", password = "bad", ip = "192.168.1.10", port = "8080", remember = true)

        assertEquals("账号或密码错误", vm.uiState.value.formError)
        assertNull(store.saveLoginArgs)
        assertTrue(!store.isLoggedIn.value)
    }

    @Test
    fun `unconfigured authenticator degrades to a friendly message`() = runTest {
        val store = FakeAuthStore()
        val vm = LoginViewModel(store, UnconfiguredLoginAuthenticator())

        vm.onSubmit(account = "admin", password = "pw", ip = "192.168.1.10", port = "8080", remember = true)

        assertEquals("登录暂不可用，请稍后再试", vm.uiState.value.formError)
        assertNull(store.saveLoginArgs)
    }

    @Test
    fun `second submit while submitting is ignored`() = runTest {
        val store = FakeAuthStore()
        val gate = CompletableDeferred<Result<AuthResult>>()
        var calls = 0
        val vm = LoginViewModel(store, LoginAuthenticator { _, _, _ -> calls++; gate.await() })

        vm.onSubmit("a", "p", "192.168.1.10", "8080", true)   // enters Submitting, suspends on gate
        assertTrue(vm.uiState.value.isSubmitting)
        vm.onSubmit("a", "p", "192.168.1.10", "8080", true)   // must be ignored

        gate.complete(Result.success(AuthResult("J")))
        assertEquals(1, calls)
        assertEquals(LoginUiState.Phase.Success, vm.uiState.value.phase)
    }

    @Test
    fun `prefill flows are exposed from the AuthStore`() = runTest {
        val store = FakeAuthStore().apply {
            _account.value = "lastuser"
            _serverAddress.value = ServerAddress("192.168.9.9", 9000)
        }
        val vm = LoginViewModel(store, UnconfiguredLoginAuthenticator())

        assertEquals("lastuser", vm.prefillAccount.value)
        assertEquals(ServerAddress("192.168.9.9", 9000), vm.prefillServer.value)
    }

    // ── PA-NEXT2-FE rememberMe propagation (regression: Critic 2026-06-01 NEXT-2) ─
    //
    // Pre-fix LoginRoute discarded the rememberMe Switch value into `_`, so the UI
    // toggle never reached AuthStore.saveLogin — every login persisted with the
    // default rememberMe=true regardless of the user's choice. These two cases lock
    // the wiring so a future regression (re-introducing a default-arg fall-through
    // in onSubmit or saveLogin) trips a test.

    @Test
    fun `onSubmit rememberMe true propagates to saveLogin`() = runTest {
        val store = FakeAuthStore()
        val vm = LoginViewModel(store, authenticatorReturning(Result.success(AuthResult("J"))))

        vm.onSubmit(account = "a", password = "p", ip = "10.0.0.1", port = "80", remember = true)

        assertEquals(true, store.saveLoginArgs!!.rememberMe)
        assertEquals(true, store.rememberMe.value)
    }

    @Test
    fun `onSubmit rememberMe false propagates to saveLogin`() = runTest {
        val store = FakeAuthStore()
        val vm = LoginViewModel(store, authenticatorReturning(Result.success(AuthResult("J"))))

        vm.onSubmit(account = "a", password = "p", ip = "10.0.0.1", port = "80", remember = false)

        assertEquals(false, store.saveLoginArgs!!.rememberMe)
        assertEquals(false, store.rememberMe.value)
    }

    // ── Task1 additions (2026-06-01): prefillRememberMe flow + logout UI hooks ─────

    @Test
    fun `prefillRememberMe exposes AuthStore rememberMe`() = runTest {
        // When AuthStore.rememberMe is true, the VM exposes it for LoginRoute to
        // initialise the Switch correctly (vs. always defaulting to ON).
        val store = FakeAuthStore().apply { _rememberMe.value = true }
        val vm = LoginViewModel(store, UnconfiguredLoginAuthenticator())

        assertEquals(true, vm.prefillRememberMe.value)

        store._rememberMe.value = false
        assertEquals(false, vm.prefillRememberMe.value)
    }

    @Test
    fun `onLogout calls clearLogin clearing JWT without wiping L1`() = runTest {
        // Explicit logout: L2 cleared (isLoggedIn=false), L1 account/host retained.
        val store = FakeAuthStore().apply {
            _account.value = "admin"
            _serverAddress.value = ServerAddress("10.0.0.1", 80)
            _jwt.value = "JWT_XYZ"
            _isLoggedIn.value = true
        }
        val vm = LoginViewModel(store, UnconfiguredLoginAuthenticator())

        vm.onLogout()

        // After clearLogin: JWT gone, isLoggedIn=false; L1 account/host preserved.
        assertEquals(false, store.isLoggedIn.value)
        assertEquals(null, store.jwt.value)
        assertEquals("admin", store.account.value)          // L1 retained
        assertEquals(ServerAddress("10.0.0.1", 80), store.serverAddress.value) // L1 retained
    }

    @Test
    fun `onRememberMeOff calls clearL1Account clearing account and rememberMe`() = runTest {
        // User flips switch OFF: L1 fields cleared, rememberMe=false. L2 untouched.
        val store = FakeAuthStore().apply {
            _account.value = "admin"
            _serverAddress.value = ServerAddress("10.0.0.1", 80)
            _jwt.value = "JWT_STILL_LIVE"
            _rememberMe.value = true
            _isLoggedIn.value = true
        }
        val vm = LoginViewModel(store, UnconfiguredLoginAuthenticator())

        vm.onRememberMeOff()

        assertEquals(null, store.account.value)         // L1 account wiped
        assertEquals(null, store.serverAddress.value)   // L1 server wiped
        assertEquals(false, store.rememberMe.value)     // flag cleared
        // L2 must remain intact (active session unaffected by the toggle).
        assertEquals(true, store.isLoggedIn.value)
        assertEquals("JWT_STILL_LIVE", store.jwt.value)
    }
}
