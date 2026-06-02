package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Stateful entry point for the login destination (TASK-AR-005).
 *
 * Connects [LoginViewModel] to the stateless [LoginScreen]:
 *   - observes [LoginViewModel.uiState] to render the five states,
 *   - routes onward when [LoginViewModel.isLoggedIn] flips true (so a restored
 *     session also navigates without the user re-typing anything),
 *   - forwards submit / dismiss to the ViewModel.
 *
 * [LoginScreen] stays free of Hilt so it remains previewable in isolation.
 */
/**
 * Stateful entry point for the login destination (TASK-AR-005).
 *
 * ★ Task1 additions (2026-06-01):
 *   - Observes [LoginViewModel.prefillRememberMe] to initialise the Switch from the
 *     persisted flag, so a user who previously opted out sees the switch OFF (and blank
 *     fields) rather than the unconditional `true` default.
 *   - [onRememberMeOff] wires the Switch-off path to [LoginViewModel.onRememberMeOff]
 *     (→ AuthStore.clearL1Account): L1 fields cleared, rememberMe=false persisted.
 *   - [onLogout] parameter left open for the 5-tab scaffold's "退出登录" menu to call
 *     [LoginViewModel.onLogout] (→ AuthStore.clearLogin); not wired to a UI element
 *     inside LoginRoute itself (that lives in MainScaffold), but the ViewModel method
 *     is available here for composition convenience.
 *
 * Nav invariant: the LaunchedEffect(loggedIn) fires ONLY when the user just submitted
 * a fresh login (loggedIn flips true via AuthStore after saveLogin). A restored session
 * is routed directly to Main by V4Activity + StartupAuthDecider BEFORE Compose mounts,
 * so loggedIn is already false when LoginRoute composes and the effect stays dormant.
 */
@Composable
fun LoginRoute(
    onLoggedIn: () -> Unit,
    onScanClick: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val loggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val prefillAccount by viewModel.prefillAccount.collectAsStateWithLifecycle()
    val prefillServer by viewModel.prefillServer.collectAsStateWithLifecycle()
    // ★ Task1: read persisted rememberMe flag to initialise the Switch correctly.
    val prefillRememberMe by viewModel.prefillRememberMe.collectAsStateWithLifecycle()

    LaunchedEffect(loggedIn) {
        if (loggedIn) onLoggedIn()
    }

    LoginScreen(
        state = state,
        // PA-NEXT2-FE 2026-06-01 — the 5th arg is the rememberMe Switch value from
        // LoginScreen.kt:208; pre-fix it was discarded into `_`, so AuthStore.saveLogin
        // always got its `rememberMe=true` default — ignoring the UI toggle (Cycle 5
        // of D-16 verification surfaced this). Pass through.
        onLogin = { account, password, ip, port, remember ->
            viewModel.onSubmit(account, password, ip, port, remember)
        },
        // ★ Task1: when the user flips the switch off, clear L1 account from the store.
        onRememberMeOff = viewModel::onRememberMeOff,
        onScanClick = onScanClick,
        onDismissError = viewModel::dismissError,
        initialAccount = prefillAccount.orEmpty(),
        initialIp = prefillServer?.host.orEmpty(),
        initialPort = prefillServer?.port?.toString().orEmpty(),
        // ★ Task1: initialise Switch from AuthStore.rememberMe, not a hard-coded true.
        initialRememberMe = prefillRememberMe,
    )
}
