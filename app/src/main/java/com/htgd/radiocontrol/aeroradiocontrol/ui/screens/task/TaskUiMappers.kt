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
 *  - [SchemeTask] has no zone field — confirmed at real-impl (PA-10): the v3 scheme/
 *    task wire (TaskZuoxiModel / TaskGuangboModel) carries NO zone column. So
 *    [TaskItem.zone] is left blank rather than borrowing the media name (which would
 *    show a song title in a zone slot — a visible mismatch on real data). The UI
 *    tolerates the blank secondary line.
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
    // 时间轴/详情 show the start time at minute precision (PA-14 Phase C — schools
    // ring bells on the minute; the v3 wire sends "HH:mm:ss" — trim the seconds so
    // we don't read "07:50:00" when "07:50" is the operational truth).
    time = truncateToHourMinute(startTime),
    title = name,
    // No domain zone on the v3 wire (PA-10 confirmed) → blank, not the media name.
    zone = "",
    state = status.toCardState(),
)

/** Truncate "HH:mm:ss" / "HH:mm" → "HH:mm"; null/blank → empty. Tolerant of any
 *  trailing fragment after the second colon. */
private fun truncateToHourMinute(t: String?): String {
    if (t.isNullOrBlank()) return ""
    // Keep only the first two ':' segments — "07:50:00" → "07:50", "07:50" → "07:50",
    // "07" → "07" (no parse failure on partial input).
    val parts = t.split(':')
    return when (parts.size) {
        0, 1 -> t
        else -> parts[0] + ":" + parts[1]
    }
}

fun Scheme.toSchemeUi(): SchemeUi = SchemeUi(
    id = id,
    name = name,
    active = active,
    tasks = tasks.map { it.toTaskItem() },
)

fun TaskLog.toLogEntry(): LogEntry = LogEntry(
    time = timestamp,
    title = taskName,
    // The domain log has no success flag. PA-10 confirmed v3 exposes NO execution-log
    // endpoint, so getExecutionLog() returns empty and this mapper is never exercised
    // on real data (success is moot / never rendered). Kept defaulting to true so the
    // shape stays valid if a log source is ever added.
    success = true,
    detail = message,
)
