package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStore
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.ServerAddress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [LoginScreen] (TASK-AR-005).
 *
 * Owns the submission lifecycle (idle → submitting → success/error) as a single
 * [LoginUiState] StateFlow. Field text stays in the composable; the ViewModel is
 * handed the values on submit so it never holds the password.
 *
 * Dependencies (both injected):
 *   - [AuthStore] — the session source of truth. Read [AuthStore.isLoggedIn] for
 *     navigation, [serverAddress]/[account] to pre-fill, and call [saveLogin] on
 *     success.
 *   - [LoginAuthenticator] — the `/authorizations` seam. The real implementation
 *     arrives from data-integration (AR-002); until then the default fails and
 *     the form shows "登录暂不可用".
 *
 * No Retrofit / OkHttp here — the network call lives behind the authenticator
 * seam (soul anti-pattern: Direct Retrofit Call).
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authStore: AuthStore,
    private val authenticator: LoginAuthenticator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Navigation gate — host routes to Main when this turns true. */
    val isLoggedIn: StateFlow<Boolean> = authStore.isLoggedIn

    /** Pre-fill values (last server / account survive logout). */
    val prefillAccount: StateFlow<String?> = authStore.account
    val prefillServer: StateFlow<ServerAddress?> = authStore.serverAddress

    /**
     * Whether to pre-fill the account + server fields on the next LoginScreen open.
     * Exposed so [LoginRoute] can initialise the rememberMe Switch from the persisted
     * flag rather than always defaulting to `true` — so a user who opted out on a
     * prior login sees the switch OFF (and blank fields) next time. ★ Task1.
     */
    val prefillRememberMe: StateFlow<Boolean> = authStore.rememberMe

    /**
     * Explicit logout — clears L2 (JWT + token) but keeps L1 (account + host) for
     * prefill on re-login. Call from the 5-tab scaffold's "退出登录" menu item.
     * ★ Task1 logout UI hook.
     */
    fun onLogout() {
        viewModelScope.launch { authStore.clearLogin() }
    }

    /**
     * User toggled rememberMe OFF — clears L1 account/host/port + resets the flag so
     * the next app open shows a blank form. L2 is untouched (the current session stays
     * live). ★ Task1 rememberMe-off UI hook.
     */
    fun onRememberMeOff() {
        viewModelScope.launch { authStore.clearL1Account() }
    }

    /** Clears the form-level error banner (user dismissed it). */
    fun dismissError() {
        _uiState.value = _uiState.value.copy(formError = null)
    }

    /**
     * Validates input, authenticates, and on success persists the session.
     *
     * Ordering matters: validate the address first (cheap, local) so a malformed
     * endpoint surfaces as a field error without a wasted network round-trip.
     * Re-entrancy is guarded by [LoginUiState.isSubmitting] — a second tap while
     * a request is in flight is ignored.
     */
    /**
     * @param remember the LoginScreen's rememberMe Switch value (PA-NEXT2-FE
     *   2026-06-01) — flows through to [AuthStore.saveLogin] so the L1 (account)
     *   prefill survives only when the user opts in. Pre-fix LoginRoute discarded
     *   it; the default of `true` at the saveLogin layer always won.
     */
    fun onSubmit(account: String, password: String, ip: String, port: String, remember: Boolean) {
        if (_uiState.value.isSubmitting) return

        val serverError = validateServerAddress(ip, port)
        if (serverError != null) {
            _uiState.value = LoginUiState(
                fieldErrors = LoginUiState.FieldErrors(server = serverError),
            )
            return
        }

        val combined = if (port.isBlank()) ip.trim() else "${ip.trim()}:${port.trim()}"
        val address = ServerAddress.parse(combined).getOrElse {
            // validateServerAddress already passed, so this should not happen;
            // guard defensively rather than force-unwrap.
            _uiState.value = LoginUiState(
                fieldErrors = LoginUiState.FieldErrors(server = "服务器地址格式不正确"),
            )
            return
        }

        _uiState.value = LoginUiState(phase = LoginUiState.Phase.Submitting)
        viewModelScope.launch {
            authenticator.authenticate(address, account, password).fold(
                onSuccess = { result ->
                    // PA-NEXT2-FE: pass tokenExpiry + rememberMe EXPLICITLY (no
                    // default-arg fall-through), so a future signature change to
                    // saveLogin surfaces as a compile error here rather than as
                    // silently-wrong UX.
                    authStore.saveLogin(
                        address = address,
                        account = result.account ?: account,
                        jwt = result.jwt,
                        refreshToken = result.refreshToken,
                        tokenExpiry = null,
                        rememberMe = remember,
                    )
                    // isLoggedIn flips via AuthStore; reflect success for the UI.
                    _uiState.value = LoginUiState(phase = LoginUiState.Phase.Success)
                },
                onFailure = { e ->
                    _uiState.value = LoginUiState(formError = messageFor(e))
                },
            )
        }
    }

    /** Maps an authentication failure to a user-facing banner message. */
    private fun messageFor(cause: Throwable): String = when (cause) {
        is UnsupportedOperationException -> "登录暂不可用，请稍后再试"
        else -> cause.message?.takeIf { it.isNotBlank() } ?: "登录失败，请检查账号或网络"
    }
}
