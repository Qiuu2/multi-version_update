package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [ExecutionLogScreen] — TASK-PA-03c.
 *
 * The execution log is a one-shot fetch ([TaskRepository.getExecutionLog]): v3 has
 * no push / no dedicated log-stream endpoint, so there is no Flow/SSOT here — the
 * screen calls [load] on open (and the retry CTA re-calls it). No polling: a log is
 * an on-demand snapshot, not a live state. Maps domain [TaskLog] → [LogEntry] at
 * this boundary.
 *
 * Note: with the stub the repo returns emptyList → state resolves to Empty. Expected.
 */
@HiltViewModel
class ExecutionLogViewModel @Inject constructor(
    private val repository: TaskRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExecutionLogUiState>(ExecutionLogUiState.Loading)
    val uiState: StateFlow<ExecutionLogUiState> = _uiState.asStateFlow()

    /** Fetch the log one-shot. Safe to call repeatedly (retry CTA / reopen). */
    fun load() {
        _uiState.value = ExecutionLogUiState.Loading
        viewModelScope.launch {
            repository.getExecutionLog()
                .onSuccess { logs ->
                    _uiState.value =
                        if (logs.isEmpty()) ExecutionLogUiState.Empty
                        else ExecutionLogUiState.Success(logs.map { it.toLogEntry() })
                }
                .onFailure {
                    _uiState.value = ExecutionLogUiState.Error(it.message ?: "加载执行日志失败")
                }
        }
    }
}
