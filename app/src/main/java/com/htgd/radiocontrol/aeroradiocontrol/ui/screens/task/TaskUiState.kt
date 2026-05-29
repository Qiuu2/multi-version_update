package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

import androidx.compose.runtime.Immutable

/**
 * Presentation states for the task Tab screens (TASK-PA-03b, interface-independent).
 *
 * Mirrors the terminal Tab's 5-state pattern (soul: State Completeness). The
 * future ViewModels (over V3TaskRepository) emit these; the stateless `*Content`
 * composables render them. No data-layer types leak upward.
 */

/** Task home: the active 作息 scheme + its timeline. */
@Immutable
sealed interface TaskHomeUiState {
    data object Loading : TaskHomeUiState
    /** No active scheme / no tasks → empty illustration + "编辑方案" CTA. */
    data object Empty : TaskHomeUiState
    data class Error(val message: String) : TaskHomeUiState
    data class Success(val scheme: SchemeUi) : TaskHomeUiState
    /** Scheme shown but the last refresh degraded (e.g. polling stale). */
    data class Partial(val scheme: SchemeUi, val staleMessage: String) : TaskHomeUiState

    val schemeOrNull: SchemeUi?
        get() = when (this) {
            is Success -> scheme
            is Partial -> scheme
            else -> null
        }
}

/** Scheme detail / edit: one scheme by id. NotFound (deleted) ≠ Empty (no tasks). */
@Immutable
sealed interface SchemeDetailUiState {
    data object Loading : SchemeDetailUiState
    data class Error(val message: String) : SchemeDetailUiState
    data object NotFound : SchemeDetailUiState
    data class Empty(val scheme: SchemeUi) : SchemeDetailUiState
    data class Success(val scheme: SchemeUi) : SchemeDetailUiState
}

/** Execution log list. */
@Immutable
sealed interface ExecutionLogUiState {
    data object Loading : ExecutionLogUiState
    data object Empty : ExecutionLogUiState
    data class Error(val message: String) : ExecutionLogUiState
    data class Success(val entries: List<LogEntry>) : ExecutionLogUiState
}
