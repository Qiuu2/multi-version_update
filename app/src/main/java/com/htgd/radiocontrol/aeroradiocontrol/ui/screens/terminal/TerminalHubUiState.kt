package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.compose.runtime.Immutable

/**
 * Presentation state for [TerminalHubScreen] (TASK-AR-102).
 *
 * The five states the terminal hub must cover (soul: State Completeness):
 *   - [Loading]  first load / refresh with no cached data → skeleton.
 *   - [Empty]    repository returned zero zones/terminals → empty illustration.
 *   - [Error]    fetch failed with nothing to show → error + retry.
 *   - [Success]  zones + terminals to render.
 *   - [Partial]  data shown, but the last refresh partially failed (e.g. some
 *                zones loaded from cache, realtime/refresh degraded) → render
 *                what we have plus a non-blocking "部分数据可能过时 / 重试" banner.
 *                This is the terminal domain's 5th state (design-system-spec §8.2
 *                "部分失败" layer), distinct from a hard [Error].
 *
 * The ViewModel maps the repository's domain models into the UI view types
 * ([ZoneUi]/[TerminalUi]) so this state never exposes data-layer DTOs upward.
 */
@Immutable
sealed interface TerminalHubUiState {

    data object Loading : TerminalHubUiState

    data object Empty : TerminalHubUiState

    data class Error(val message: String) : TerminalHubUiState

    data class Success(
        val zones: List<ZoneUi>,
    ) : TerminalHubUiState

    /**
     * Data is available but the last sync degraded. [staleMessage] explains it
     * (e.g. "实时已断开，显示缓存数据"); the UI shows [zones] plus a warning banner.
     */
    data class Partial(
        val zones: List<ZoneUi>,
        val staleMessage: String,
    ) : TerminalHubUiState

    /** Zones to render for the success-or-partial states, or empty otherwise. */
    val zonesOrEmpty: List<ZoneUi>
        get() = when (this) {
            is Success -> zones
            is Partial -> zones
            else -> emptyList()
        }
}
