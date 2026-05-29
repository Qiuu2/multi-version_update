package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Scheme
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TaskRepository
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
 * ViewModel for the task home ([TaskScreen]) — TASK-PA-03c.
 *
 * Consumes [TaskRepository] (ICD-TaskRepository-v1; impl = V3TaskRepository, a STUB
 * returning empty until the real v3 adapter lands) and projects the active 作息
 * scheme into [TaskHomeUiState]. Same seam idiom + 5-state derivation as the terminal
 * hub (AR-102/PA-03); domain→UI mapping (with Unknown fallback) is at this boundary
 * via [toSchemeUi], so the screen never sees data-layer types.
 *
 * Active-scheme pick: the scheme marked `active`, else the first scheme (so a
 * single-scheme campus with no explicit active flag still shows its timeline).
 *
 * Plan A polling (D-13, no WS): a [PollingRefreshScheduler] at 任务 cadence (30s)
 * runs the periodic refresh on [viewModelScope] (immediate on open, then every 30s,
 * foreground-gated, backing off on failure). The loop dies with the scope.
 *
 * Note: with the stub the repo emits emptyList → state correctly resolves to
 * [TaskHomeUiState.Empty]. That is expected; do not fabricate data.
 */
@HiltViewModel
class TaskHomeViewModel @Inject constructor(
    private val repository: TaskRepository,
    foregroundState: AppForegroundState,
) : ViewModel() {

    /** null = refresh not yet resolved (Loading); success/failure thereafter. */
    private val refreshResult = MutableStateFlow<Result<Unit>?>(null)

    private val poller = PollingRefreshScheduler(
        scope = viewModelScope,
        refresh = { repository.refresh().also { refreshResult.value = it } },
        cadence = PollingCadence.TaskEnded, // 任务进度 30s (Handoff 节奏)
        foregroundState = foregroundState,
    )

    /** Light banner state (刷新中 / 刷新失败) — NOT a connection state (no WS). */
    val pollingState: StateFlow<PollingState> = poller.state

    val uiState: StateFlow<TaskHomeUiState> =
        combine(repository.observeSchemes(), refreshResult) { schemes, refresh ->
            deriveState(schemes, refresh)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TaskHomeUiState.Loading,
        )

    init {
        poller.start()
    }

    /** Manual retry (error/empty CTA): out-of-band refresh. */
    fun refresh() {
        viewModelScope.launch {
            refreshResult.value = repository.refresh()
        }
    }

    /**
     * Enable/disable a scheme (v3 /task/sechenableordisable). On success the repo
     * re-emits the flipped scheme through [observeSchemes]; on failure the previous
     * snapshot stays (the screen can surface the error via [refresh]/state).
     */
    fun setSchemeActive(schemeId: String, active: Boolean) {
        viewModelScope.launch {
            repository.setSchemeActive(schemeId, active)
        }
    }

    private fun activeScheme(schemes: List<Scheme>): Scheme? =
        schemes.firstOrNull { it.active } ?: schemes.firstOrNull()

    private fun deriveState(
        schemes: List<Scheme>,
        refresh: Result<Unit>?,
    ): TaskHomeUiState {
        val scheme = activeScheme(schemes)
        return when {
            scheme != null -> TaskHomeUiState.Success(scheme.toSchemeUi())
            refresh == null -> TaskHomeUiState.Loading
            refresh.isFailure -> TaskHomeUiState.Error(
                refresh.exceptionOrNull()?.message ?: "加载作息方案失败",
            )
            else -> TaskHomeUiState.Empty
        }
    }
}
