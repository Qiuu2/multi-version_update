package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

import com.htgd.radiocontrol.aeroradiocontrol.data.model.Scheme
import com.htgd.radiocontrol.aeroradiocontrol.data.model.SchemeTask
import com.htgd.radiocontrol.aeroradiocontrol.data.model.SchemeTaskStatus
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TaskLog

/**
 * Domain → UI mapping for the task Tab, done at the ViewModel boundary (TASK-PA-03c).
 *
 * Mirrors the terminal Tab's TerminalStatusMapper (AR-102): screens consume the UI
 * view types only ([SchemeUi]/[TaskItem]/[TaskCardState]/[LogEntry]); the data layer's
 * domain types ([Scheme]/[SchemeTask]/[SchemeTaskStatus]/[TaskLog]) never reach a
 * Composable. Status maps with an Unknown-tolerant `when` (R-003): any status the
 * mapper can't classify falls back to a safe, non-crashing UI state.
 *
 * Model-shape notes (honest gaps, surfaced for ICD):
 *  - The UI [TaskCardState] enum carries design-showcase states (Swapped/Migrated/
 *    Deleted/Cancelled) that the v3 domain has NO source for — they were mock-only.
 *    Real data only ever produces Normal / Running / Cancelled, plus the Unknown→
 *    Normal fallback. The unused enum values stay valid but are simply never emitted
 *    (same as terminal statuses the v3 stack doesn't drive).
 *  - [SchemeTask] has no zone field (v3 `TaskZuoxiModel` has none); [TaskItem.zone]
 *    therefore maps to the media name when present, else "" (UI renders it as a
 *    secondary line and tolerates blank). Pinned at impl time if v3 exposes a zone.
 */

/** SchemeTaskStatus → the card's visual lifecycle state. Unknown-tolerant. */
fun SchemeTaskStatus.toCardState(): TaskCardState = when (this) {
    SchemeTaskStatus.Running -> TaskCardState.Running
    SchemeTaskStatus.Idle -> TaskCardState.Normal
    SchemeTaskStatus.Disabled -> TaskCardState.Cancelled
    is SchemeTaskStatus.Unknown -> TaskCardState.Normal // R-003: never crash on a new value
}

fun SchemeTask.toTaskItem(): TaskItem = TaskItem(
    id = id,
    // 时间轴/详情 show the start time; blank-tolerant ("--" handled downstream).
    time = startTime.orEmpty(),
    title = name,
    // No domain zone (see file header); fall back to media name, else blank.
    zone = mediaName.orEmpty(),
    state = status.toCardState(),
)

fun Scheme.toSchemeUi(): SchemeUi = SchemeUi(
    id = id,
    name = name,
    active = active,
    tasks = tasks.map { it.toTaskItem() },
)

fun TaskLog.toLogEntry(): LogEntry = LogEntry(
    time = timestamp,
    title = taskName,
    // The domain log has no success flag yet (v3 source TBD at impl); treat every
    // recorded entry as a neutral/success row until the real field is confirmed.
    success = true,
    detail = message,
)
