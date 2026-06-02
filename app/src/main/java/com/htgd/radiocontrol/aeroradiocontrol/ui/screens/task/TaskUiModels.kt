package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

/**
 * Presentation view models for the task Tab (TASK-PA-03b/c).
 *
 * The task ViewModels map the data layer's domain models (Scheme/SchemeTask/
 * SchemeTaskStatus/TaskLog) into these UI types at the VM boundary (see
 * TaskUiMappers.kt), so screens never touch DTOs/domain types — mirrors the
 * terminal Tab's TerminalUiModels split (AR-102). Only the deferred CRUD
 * SchemeEditScreen still uses the mock instances in TaskMockData.
 */

/** Visual lifecycle state of a task card. Domain→UI mapping (with Unknown
 *  fallback) happens at the ViewModel boundary (TaskUiMappers); the real
 *  SchemeTaskStatus value set is OPEN pending v3 适配实测 (O-1 void under D-13). */
enum class TaskCardState { Normal, Running, Swapped, Migrated, Deleted, Cancelled }

data class TaskItem(
    val id: String,
    val time: String,
    val title: String,
    val zone: String,
    val state: TaskCardState,
)

data class SchemeUi(
    val id: String,
    val name: String,
    val active: Boolean,
    val tasks: List<TaskItem>,
)

data class LogEntry(
    val time: String,
    val title: String,
    val success: Boolean,
    val detail: String,
)
