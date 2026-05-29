package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
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
 * ViewModel for [ZoneDetailScreen] (TASK-AR-105).
 *
 * Reuses the AR-102 pattern: consumes [TerminalRepository] (the same SSOT as the
 * hub), projects the one zone matching [zoneId] into [ZoneDetailUiState]. The
 * zone id is supplied via [load] because the terminal Tab navigates by local
 * state (not a NavController arg), so it can't arrive through SavedStateHandle.
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
) : ViewModel() {

    private val zoneId = MutableStateFlow<String?>(null)
    private val refreshResult = MutableStateFlow<Result<Unit>?>(null)

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

    /** Binds the screen's zone id and triggers a refresh. Idempotent per id. */
    fun load(id: String) {
        if (zoneId.value == id) return
        zoneId.value = id
        refresh()
    }

    /** Re-fetches into the SSOT; drives Error/retry. */
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
