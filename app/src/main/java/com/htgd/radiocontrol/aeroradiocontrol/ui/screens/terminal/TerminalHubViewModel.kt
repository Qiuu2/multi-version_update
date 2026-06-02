package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.AppForegroundState
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.PollingCadence
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.PollingRefreshScheduler
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.PollingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [TerminalHubScreen] (TASK-AR-102; polling wired in PA-03).
 *
 * Consumes [TerminalRepository] (data, AR-101 → V3 impl, PA-01) and projects its
 * domain models into [TerminalHubUiState] — the screen never sees data DTOs/domain
 * types. The repository seam is identical to AR-102 (observeZones + refresh), so
 * the Retrofit→V3 swap stays transparent here (VM zero-change, PA-01 AC#7).
 *
 * Plan A "realtime" (D-13): there is no WebSocket; freshness comes from polling the
 * v3 data path on the Handoff cadence (终端 10s). A [PollingRefreshScheduler] runs
 * the periodic refresh on [viewModelScope] — it refreshes immediately on [start]
 * (fresh data on screen open), then every cadence, pausing while backgrounded and
 * backing off on failure. The scheduler dies with the scope (no explicit stop
 * needed; onCleared cancels viewModelScope → the loop ends).
 *
 * State derivation (combine of the zones SSOT Flow + the latest refresh result):
 *   - first frame, before any refresh resolves          → [TerminalHubUiState.Loading]
 *   - refresh failed AND no zones to show               → [TerminalHubUiState.Error]
 *   - refresh succeeded (or cache present) but no zones  → [TerminalHubUiState.Empty]
 *   - zones present                                      → [TerminalHubUiState.Success]
 *   - [TerminalHubUiState.Partial] is reserved for the realtime-disconnected /
 *     cache-degraded case; the repository exposes no cache-vs-fresh signal
 *     (PM Q3), so it is not produced here yet.
 *
 * No Retrofit/OkHttp here — all network goes through the repository (soul
 * anti-pattern: Direct Retrofit Call).
 */
@HiltViewModel
class TerminalHubViewModel @Inject constructor(
    private val repository: TerminalRepository,
    foregroundState: AppForegroundState,
) : ViewModel() {

    /** null = refresh not yet resolved (Loading); success/failure thereafter. */
    private val refreshResult = MutableStateFlow<Result<Unit>?>(null)

    /**
     * Polling drives the periodic refresh; each tick's outcome feeds [refreshResult]
     * so the 5-state derivation resolves exactly as in AR-102. Cadence = 终端 10s.
     */
    private val poller = PollingRefreshScheduler(
        scope = viewModelScope,
        refresh = { repository.refresh().also { refreshResult.value = it } },
        cadence = PollingCadence.Terminal,
        foregroundState = foregroundState,
    )

    /** Light banner state (刷新中 / 刷新失败) — NOT a connection state (no WS). */
    val pollingState: StateFlow<PollingState> = poller.state

    val uiState: StateFlow<TerminalHubUiState> =
        combine(repository.observeZones(), refreshResult) { zones, refresh ->
            deriveState(zones, refresh)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TerminalHubUiState.Loading,
        )

    init {
        // Begins polling: an immediate first refresh, then every 10s while foreground.
        poller.start()
    }

    /**
     * Manual retry (error/empty CTA): an out-of-band refresh independent of the
     * polling cadence. Drives Error→Success/Empty. Safe to call repeatedly.
     */
    fun refresh() {
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
            // ★ Task2: fixed copy — never expose raw exception message (may contain
            // HTML 500 body from the backend). The screen renders this string directly.
            refresh.isFailure -> TerminalHubUiState.Error("加载失败，请重试")
            else -> TerminalHubUiState.Empty
        }
    }
}
