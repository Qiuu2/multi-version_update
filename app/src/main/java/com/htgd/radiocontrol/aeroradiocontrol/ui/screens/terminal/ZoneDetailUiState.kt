package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.compose.runtime.Immutable

/**
 * Presentation state for [ZoneDetailScreen] (TASK-AR-105).
 *
 * States:
 *   - [Loading]  first load (refresh not yet resolved, zone not yet in the SSOT).
 *   - [Error]    refresh failed and the zone isn't available → error + retry.
 *   - [NotFound] refresh succeeded but no zone with this id exists (deleted) →
 *                distinct from Empty: the zone itself is gone, not just terminal-less.
 *   - [Empty]    zone found but it has no terminals → add-terminals empty state.
 *   - [Success]  zone with its terminals to render.
 */
@Immutable
sealed interface ZoneDetailUiState {
    data object Loading : ZoneDetailUiState
    data class Error(val message: String) : ZoneDetailUiState
    data object NotFound : ZoneDetailUiState
    data class Empty(val zone: ZoneUi) : ZoneDetailUiState
    data class Success(val zone: ZoneUi) : ZoneDetailUiState
}
