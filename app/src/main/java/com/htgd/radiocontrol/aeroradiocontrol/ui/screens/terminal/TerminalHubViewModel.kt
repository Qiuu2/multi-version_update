package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [TerminalHubScreen] (TASK-AR-102).
 *
 * Consumes [TerminalRepository] (data, AR-101) and projects its domain models
 * into [TerminalHubUiState] — the screen never sees data DTOs/domain types.
 *
 * State derivation (combine of the zones SSOT Flow + the latest refresh result):
 *   - first frame, before any refresh resolves          → [TerminalHubUiState.Loading]
 *   - refresh failed AND no zones to show               → [TerminalHubUiState.Error]
 *   - refresh succeeded (or cache present) but no zones  → [TerminalHubUiState.Empty]
 *   - zones present                                      → [TerminalHubUiState.Success]
 *   - [TerminalHubUiState.Partial] is reserved for the realtime-disconnected /
 *     cache-degraded case; the repository exposes no cache-vs-fresh signal
 *     (PM Q3), so it is wired to fe-platform's connectionState in a later task.
 *
 * No Retrofit/OkHttp here — all network goes through the repository (soul
 * anti-pattern: Direct Retrofit Call).
 */
@HiltViewModel
class TerminalHubViewModel @Inject constructor(
    private val repository: TerminalRepository,
) : ViewModel() {

    /** null = refresh not yet resolved (Loading); success/failure thereafter. */
    private val refreshResult = MutableStateFlow<Result<Unit>?>(null)

    val uiState: StateFlow<TerminalHubUiState> =
        combine(repository.observeZones(), refreshResult) { zones, refresh ->
            deriveState(zones, refresh)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TerminalHubUiState.Loading,
        )

    init {
        refresh()
    }

    /** Triggers a network refresh; drives Error/retry. Safe to call repeatedly. */
    fun refresh() {
        // Reset to Loading only when we have nothing resolved yet; a retry after
        // an error keeps any stale zones visible underneath until it resolves.
        viewModelScope.launch {
            refreshResult.value = repository.refresh()
        }
    }

    private fun deriveState(
        zones: List<Zone>,
        refresh: Result<Unit>?,
    ): TerminalHubUiState {
        val zoneUis = zones.map { it.toZoneUi() }
        return when {
            zoneUis.isNotEmpty() -> TerminalHubUiState.Success(zoneUis)
            refresh == null -> TerminalHubUiState.Loading
            refresh.isFailure -> TerminalHubUiState.Error(
                refresh.exceptionOrNull()?.message ?: "加载终端失败",
            )
            else -> TerminalHubUiState.Empty
        }
    }
}
