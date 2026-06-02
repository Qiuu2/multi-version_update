package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerState
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.ServerStateRepository
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
 * ViewModel for [ServiceScreen] (系统健康度) — TASK-PA Service.
 *
 * Consumes [ServerStateRepository] (ICD-ServerStateRepository-v1; impl =
 * V3ServerStateRepository, REAL — hits /server/serverstate) and projects the server
 * snapshot into [ServiceUiState]. Same seam idiom + null-until-first-success SSOT
 * contract as the terminal hub; domain→UI mapping (with Unknown health fallback) is
 * at this boundary via [toServiceUi], so the screen never sees data-layer types.
 *
 * Plan A polling (D-13, no WS): a [PollingRefreshScheduler] runs the periodic refresh
 * on [viewModelScope] (immediate on open, then every cadence, foreground-gated,
 * backing off on failure). The loop dies with the scope.
 *
 * Cadence choice — [PollingCadence.Custom] 20s: Handoff has no dedicated 服务-Tab
 * cadence; system-health is low-urgency (slower than the 终端 10s list, no need for
 * the 详情 5s), so 20s sits in the sensible 15–30s band — fresh enough for a health
 * dashboard without hammering /server/serverstate.
 */
@HiltViewModel
class ServiceViewModel @Inject constructor(
    private val repository: ServerStateRepository,
    foregroundState: AppForegroundState,
) : ViewModel() {

    /** null = refresh not yet resolved (Loading); success/failure thereafter. */
    private val refreshResult = MutableStateFlow<Result<Unit>?>(null)

    private val poller = PollingRefreshScheduler(
        scope = viewModelScope,
        refresh = { repository.refresh().also { refreshResult.value = it } },
        cadence = SERVICE_CADENCE,
        foregroundState = foregroundState,
    )

    /** Light banner state (刷新中 / 刷新失败) — NOT a connection state (no WS). */
    val pollingState: StateFlow<PollingState> = poller.state

    val uiState: StateFlow<ServiceUiState> =
        combine(repository.observeServerState(), refreshResult) { server, refresh ->
            deriveState(server, refresh)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ServiceUiState.Loading,
        )

    init {
        poller.start()
    }

    /** Manual retry (error CTA): out-of-band refresh independent of the cadence. */
    fun refresh() {
        viewModelScope.launch {
            refreshResult.value = repository.refresh()
        }
    }

    private fun deriveState(
        server: ServerState?,
        refresh: Result<Unit>?,
    ): ServiceUiState = when {
        server != null -> ServiceUiState.Success(server.toServiceUi())
        refresh == null -> ServiceUiState.Loading
        // ★ Task2: fixed copy — never expose raw exception message.
        refresh.isFailure -> ServiceUiState.Error("加载失败，请重试")
        // refresh succeeded but the SSOT is still null (empty data array → repo
        // reports failure, so this branch is effectively unreachable; kept exhaustive).
        else -> ServiceUiState.Loading
    }

    private companion object {
        /** 服务-Tab health poll cadence (see class KDoc for the 20s rationale). */
        val SERVICE_CADENCE = PollingCadence.Custom(20_000L)
    }
}
