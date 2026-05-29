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

    LaunchedEffect(loggedIn) {
        if (loggedIn) onLoggedIn()
    }

    LoginScreen(
        state = state,
        onLogin = { account, password, ip, port, _ ->
            viewModel.onSubmit(account, password, ip, port)
        },
        onScanClick = onScanClick,
        onDismissError = viewModel::dismissError,
        initialAccount = prefillAccount.orEmpty(),
        initialIp = prefillServer?.host.orEmpty(),
        initialPort = prefillServer?.port?.toString().orEmpty(),
    )
}
