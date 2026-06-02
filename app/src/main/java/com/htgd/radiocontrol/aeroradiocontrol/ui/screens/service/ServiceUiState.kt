package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.service

import androidx.compose.runtime.Immutable

/**
 * Presentation state for the Service Tab (系统健康度) — TASK-PA Service.
 *
 * The states the screen covers (soul: State Completeness). There is no Empty here:
 * /server/serverstate is a single-object endpoint — a successful refresh always
 * yields one [ServiceUi], and an empty `data` array is reported by the repository
 * as Result.failure (→ Error), not as an empty success. The poll-failure-with-prior-
 * data case is surfaced via a non-blocking banner in the Success branch (same as the
 * terminal Tab), not as a separate Partial state.
 *
 *   - [Loading]  first fetch not yet resolved → skeleton.
 *   - [Error]    fetch failed with nothing to show → error + retry.
 *   - [Success]  the server-health card to render.
 */
@Immutable
sealed interface ServiceUiState {
    data object Loading : ServiceUiState
    data class Error(val message: String) : ServiceUiState
    data class Success(val server: ServiceUi) : ServiceUiState
}
