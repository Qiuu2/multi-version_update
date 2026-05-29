package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import androidx.compose.runtime.Immutable

/**
 * Presentation state for [LoginScreen] (TASK-AR-005, pre-research skeleton).
 *
 * This is the shape a future `LoginViewModel` will expose as a `StateFlow`; for
 * now the screen owns local field state and a host passes a [LoginUiState] in to
 * exercise the non-default states. NO data-layer wiring lives here — AuthStore /
 * the refresh closure (TASK-AR-003 / AR-002) plug in when AR-005 is unblocked.
 *
 * The login screen's "5 states" map onto an input form like this:
 *   - [Idle]        default editable form (a login form's resting/"empty" state).
 *   - [Submitting]  loading — CTA shows progress, inputs disabled.
 *   - [Success]     auth succeeded, navigation about to fire (transient).
 *   - field errors  per-field validation (account / password / server address),
 *                   carried in [fieldErrors] — this is where 地址校验失败 lives.
 *   - [formError]   form-level failure (network down / server unreachable / bad
 *                   credentials) shown as a banner — design-system-spec §8.2's
 *                   致命 + 请求失败 layers.
 */
@Immutable
data class LoginUiState(
    val phase: Phase = Phase.Idle,
    val fieldErrors: FieldErrors = FieldErrors(),
    /** Form-level error banner message, or null when there is none. */
    val formError: String? = null,
) {
    enum class Phase { Idle, Submitting, Success }

    /** Per-field validation messages; null means the field is valid. */
    @Immutable
    data class FieldErrors(
        val account: String? = null,
        val password: String? = null,
        /** 服务器地址校验失败 (e.g. bad port) — drives the server section error. */
        val server: String? = null,
    ) {
        val hasAny: Boolean get() = account != null || password != null || server != null
    }

    val isSubmitting: Boolean get() = phase == Phase.Submitting

    companion object {
        val Idle = LoginUiState()
    }
}
