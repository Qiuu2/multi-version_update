package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

/**
 * Presentation view models for the task Tab (TASK-PA-03b, interface-independent part).
 *
 * The (future) ViewModels will map the data layer's V3TaskRepository domain models
 * into these UI types, so screens never touch DTOs. Extracted from TaskMockData so
 * the screens (and the de-mocked versions) depend on real UI models, not the mock
 * file — mirrors the terminal Tab's TerminalUiModels split (AR-102). The mock
 * instances stay in TaskMockData until the V3TaskRepository ViewModel lands.
 */

/** Visual lifecycle state of a task card. Domain→UI mapping (with Unknown
 *  fallback) happens at the ViewModel boundary once V3TaskRepository's sealed
 *  domain state arrives; the real value set is OPEN pending O-1. */
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
