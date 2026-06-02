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
 * ViewModel for [SchemeDetailScreen] (read + enable/disable) — TASK-PA-03c.
 *
 * Reuses the ZoneDetailViewModel pattern: consumes [TaskRepository] (same SSOT as
 * the task home), projects the one scheme matching [schemeId] into
 * [SchemeDetailUiState]. The id arrives via [load] (the Task Tab navigates by local
 * state, not a NavController arg). 5 states incl. NotFound (deleted) ≠ Empty
 * (scheme with no tasks).
 *
 * Plan A polling (no WS): 详情 cadence (5s) on [viewModelScope], started on first
 * [load]; idempotent on re-load of the same id. The enable/disable toggle calls
 * [TaskRepository.setSchemeActive]; on success the flipped scheme re-emits through
 * [observeSchemes].
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SchemeDetailViewModel @Inject constructor(
    private val repository: TaskRepository,
    foregroundState: AppForegroundState,
) : ViewModel() {

    private val schemeId = MutableStateFlow<String?>(null)
    private val refreshResult = MutableStateFlow<Result<Unit>?>(null)

    private val poller = PollingRefreshScheduler(
        scope = viewModelScope,
        refresh = { repository.refresh().also { refreshResult.value = it } },
        cadence = PollingCadence.Detail, // 详情页 5s
        foregroundState = foregroundState,
    )

    /** Light banner state (刷新中 / 刷新失败) — NOT a connection state (no WS). */
    val pollingState: StateFlow<PollingState> = poller.state

    /** The scheme whose id matches [schemeId], or null while unset / not found. */
    private val matchedScheme: Flow<Scheme?> =
        schemeId.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.observeSchemes().map { schemes -> schemes.firstOrNull { it.id == id } }
        }

    val uiState: StateFlow<SchemeDetailUiState> =
        combine(matchedScheme, refreshResult) { scheme, refresh ->
            deriveState(scheme, refresh)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SchemeDetailUiState.Loading,
        )

    /** Binds the screen's scheme id and begins 5s polling. Idempotent per id. */
    fun load(id: String) {
        if (schemeId.value == id) return
        schemeId.value = id
        poller.start()
    }

    /** Manual retry (error CTA): out-of-band refresh. */
    fun refresh() {
        viewModelScope.launch {
            refreshResult.value = repository.refresh()
        }
    }

    /** Enable/disable this scheme; success flips `active` via [observeSchemes]. */
    fun setActive(active: Boolean) {
        val id = schemeId.value ?: return
        viewModelScope.launch {
            repository.setSchemeActive(id, active)
        }
    }

    private fun deriveState(
        scheme: Scheme?,
        refresh: Result<Unit>?,
    ): SchemeDetailUiState = when {
        scheme != null -> {
            val schemeUi = scheme.toSchemeUi()
            if (schemeUi.tasks.isEmpty()) SchemeDetailUiState.Empty(schemeUi)
            else SchemeDetailUiState.Success(schemeUi)
        }
        refresh == null -> SchemeDetailUiState.Loading
        // ★ Task2: fixed copy — never expose raw exception message.
        refresh.isFailure -> SchemeDetailUiState.Error("加载失败，请重试")
        else -> SchemeDetailUiState.NotFound
    }
}
