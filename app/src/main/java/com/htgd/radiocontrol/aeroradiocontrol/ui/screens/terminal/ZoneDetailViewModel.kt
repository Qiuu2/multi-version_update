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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [ZoneDetailScreen] (TASK-AR-105; polling wired in PA-03).
 *
 * Reuses the AR-102 pattern: consumes [TerminalRepository] (the same SSOT as the
 * hub), projects the one zone matching [zoneId] into [ZoneDetailUiState]. The
 * zone id is supplied via [load] because the terminal Tab navigates by local
 * state (not a NavController arg), so it can't arrive through SavedStateHandle.
 *
 * Plan A polling (D-13, no WS): the detail page polls the v3 data path on the
 * Handoff 详情 cadence (5s) via a [PollingRefreshScheduler] on [viewModelScope].
 * Polling [start]s on the first [load] (we have no zone to refresh before then);
 * [start] is idempotent so a re-[load] of the same id is a no-op. The loop dies
 * with the scope (onCleared cancels viewModelScope).
 *
 * State derivation (combine of the matched zone Flow + the latest refresh result):
 *   - zone present, has terminals  → Success
 *   - zone present, no terminals   → Empty(zone)
 *   - no matching zone + refresh ok → NotFound (zone was deleted)
 *   - no matching zone + refresh failed → Error
 *   - nothing resolved yet          → Loading
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ZoneDetailViewModel @Inject constructor(
    private val repository: TerminalRepository,
    foregroundState: AppForegroundState,
) : ViewModel() {

    private val zoneId = MutableStateFlow<String?>(null)
    private val refreshResult = MutableStateFlow<Result<Unit>?>(null)

    /**
     * Polling drives the periodic refresh once a zone is bound; each tick's outcome
     * feeds [refreshResult] so the 5-state derivation resolves as in AR-105.
     * Cadence = 详情 5s.
     */
    private val poller = PollingRefreshScheduler(
        scope = viewModelScope,
        refresh = { repository.refresh().also { refreshResult.value = it } },
        cadence = PollingCadence.Detail,
        foregroundState = foregroundState,
    )

    /** Light banner state (刷新中 / 刷新失败) — NOT a connection state (no WS). */
    val pollingState: StateFlow<PollingState> = poller.state

    /** The zone whose id matches [zoneId], or null while unset / not found. */
    private val matchedZone: Flow<Zone?> =
        zoneId.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.observeZones().map { zones -> zones.firstOrNull { it.id == id } }
        }

    val uiState: StateFlow<ZoneDetailUiState> =
        combine(matchedZone, refreshResult) { zone, refresh ->
            deriveState(zone, refresh)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ZoneDetailUiState.Loading,
        )

    /** Binds the screen's zone id and begins 5s polling. Idempotent per id. */
    fun load(id: String) {
        if (zoneId.value == id) return
        zoneId.value = id
        poller.start() // immediate first refresh + 5s cadence; idempotent if running
    }

    /**
     * Manual retry (error CTA): an out-of-band refresh independent of the polling
     * cadence. Drives Error→Success/Empty/NotFound.
     */
    fun refresh() {
        viewModelScope.launch {
            refreshResult.value = repository.refresh()
        }
    }

    private fun deriveState(
        zone: Zone?,
        refresh: Result<Unit>?,
    ): ZoneDetailUiState = when {
        zone != null -> {
            val zoneUi = zone.toZoneUi()
            if (zoneUi.terminals.isEmpty()) ZoneDetailUiState.Empty(zoneUi)
            else ZoneDetailUiState.Success(zoneUi)
        }
        refresh == null -> ZoneDetailUiState.Loading
        refresh.isFailure -> ZoneDetailUiState.Error(
            refresh.exceptionOrNull()?.message ?: "加载分区失败",
        )
        else -> ZoneDetailUiState.NotFound
    }
}
