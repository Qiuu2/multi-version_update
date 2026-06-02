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
 * Consumes [TaskRepository] (ICD-TaskRepository-v1; impl = V3TaskRepository) and
 * projects ALL 作息 schemes into [TaskHomeUiState]. Same seam idiom + 5-state
 * derivation as the terminal hub (AR-102/PA-03); domain→UI mapping (with Unknown
 * fallback) is at this boundary via [toSchemeUi], so the screen never sees data-layer
 * types.
 *
 * ★ Task3 (2026-06-01) — multi-scheme viewing upgrade:
 *   The repo correctly publishes ALL schemes (e.g. 日本作息 + 海星作息). Previously
 *   [deriveState] picked only [activeScheme] (first-active ?: first), so the second
 *   scheme was invisible — the "documented-assumption" PA-15 Option A that needed an
 *   upgrade when CTO actually saw 2 schemes on a real device.
 *
 *   Now:
 *   - [TaskHomeUiState.Success.allSchemes] carries the full list.
 *   - [_selectedIndex] tracks which scheme the user is currently VIEWING (pure UI
 *     selection; does NOT call setSchemeActive — "切换查看" ≠ "激活方案").
 *   - Default selected = first-active-index ?: 0 (preserves existing single-scheme
 *     and active-scheme behaviour; no regression when the list is re-loaded).
 *   - [selectScheme] lets the screen switch the viewed scheme (Handoff line 929:
 *     "当前作息方案名 + 切换按钮").
 *
 * Plan A polling (D-13, no WS): a [PollingRefreshScheduler] at 任务 cadence (30s)
 * runs the periodic refresh on [viewModelScope] (immediate on open, then every 30s,
 * foreground-gated, backing off on failure). The loop dies with the scope.
 */
@HiltViewModel
class TaskHomeViewModel @Inject constructor(
    private val repository: TaskRepository,
    foregroundState: AppForegroundState,
) : ViewModel() {

    /** null = refresh not yet resolved (Loading); success/failure thereafter. */
    private val refreshResult = MutableStateFlow<Result<Unit>?>(null)

    /**
     * The scheme index the user is currently VIEWING.
     *   - Sentinel -1 = "not yet chosen by the user" → [deriveState] will auto-pick
     *     [defaultIndexFor] (first-active ?: 0) on the first non-empty data load and
     *     write the resolved value back so subsequent emissions use it.
     *   - ≥ 0 = explicit user selection (or the resolved default after first load).
     *     Clamped to the list size in [deriveState] so a stale index after a scheme is
     *     deleted never goes out of bounds.
     */
    private val _selectedIndex = MutableStateFlow(-1)

    private val poller = PollingRefreshScheduler(
        scope = viewModelScope,
        refresh = { repository.refresh().also { refreshResult.value = it } },
        cadence = PollingCadence.TaskEnded, // 任务진행 30s (Handoff 节奏)
        foregroundState = foregroundState,
    )

    /** Light banner state (刷新中 / 刷新失败) — NOT a connection state (no WS). */
    val pollingState: StateFlow<PollingState> = poller.state

    val uiState: StateFlow<TaskHomeUiState> =
        combine(repository.observeSchemes(), refreshResult, _selectedIndex) { schemes, refresh, idx ->
            deriveState(schemes, refresh, idx)
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
     * Switch the currently VIEWED scheme to [index] (0-based). This is a pure
     * UI/viewing selection — it does NOT call [TaskRepository.setSchemeActive].
     * Negative indices are ignored (defensive); out-of-range positive indices are
     * clamped by [deriveState] on the next emission.
     */
    fun selectScheme(index: Int) {
        if (index >= 0) _selectedIndex.value = index
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

    /**
     * Default viewed index for a new [schemes] list: the first scheme whose
     * [active] flag is true, or 0 if none is active. Matches the original
     * first-active-wins behaviour so single-scheme and active-scheme campuses see
     * no change.
     */
    private fun defaultIndexFor(schemes: List<*>): Int =
        (schemes.indexOfFirst { (it as? com.htgd.radiocontrol.aeroradiocontrol.data.model.Scheme)?.active == true })
            .takeIf { it >= 0 } ?: 0

    private fun deriveState(
        schemes: List<Scheme>,
        refresh: Result<Unit>?,
        selectedIdx: Int,
    ): TaskHomeUiState {
        if (schemes.isEmpty()) {
            return when {
                refresh == null -> TaskHomeUiState.Loading
                // ★ Task2: fixed copy — never expose raw exception message.
                refresh.isFailure -> TaskHomeUiState.Error("加载失败，请重试")
                else -> TaskHomeUiState.Empty
            }
        }
        val schemeUis = schemes.map { it.toSchemeUi() }
        // Resolve the index:
        //   - sentinel -1 (not yet chosen): pick the default (first-active ?: 0).
        //   - in-range: use as-is.
        //   - out-of-range (stale after a deletion): fall back to the default.
        val safeIdx = when {
            selectedIdx < 0 -> defaultIndexFor(schemes)
            selectedIdx in schemeUis.indices -> selectedIdx
            else -> defaultIndexFor(schemes)
        }
        // Write the resolved value back so future emissions and [selectScheme] calls
        // share the same reference. Fire-and-forget (no suspend needed here).
        if (safeIdx != selectedIdx) _selectedIndex.value = safeIdx

        return when {
            refresh?.isFailure == true ->
                // Data is available (stale cache) but latest refresh failed — Partial.
                TaskHomeUiState.Partial(
                    allSchemes = schemeUis,
                    selectedIndex = safeIdx,
                    staleMessage = "刷新失败，正在自动重试…",
                )
            else ->
                TaskHomeUiState.Success(
                    allSchemes = schemeUis,
                    selectedIndex = safeIdx,
                )
        }
    }
}
