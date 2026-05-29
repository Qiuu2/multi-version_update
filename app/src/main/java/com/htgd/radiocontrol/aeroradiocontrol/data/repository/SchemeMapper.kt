package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.dto.SchemeRowDto
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Scheme
import com.htgd.radiocontrol.aeroradiocontrol.data.model.SchemeTask
import com.htgd.radiocontrol.aeroradiocontrol.data.model.SchemeTaskStatus

/**
 * DTO -> domain mapper for the 作息 (schedule) domain.
 *
 * Kept separate from the Repository (like [TerminalMapper] / [ServerStateMapper] /
 * [MediaMapper]) so the wire->domain translation — especially the status
 * derivation — is unit-testable in isolation.
 *
 * The v3 /task/sechinfo wire is FLAT: each [SchemeRowDto] is one task that carries
 * its parent scheme's name (`schemeName` = sechename) and the scheme's run state
 * (`projectState`). [groupRowsIntoSchemes] folds the rows into nested domain
 * Schemes by scheme name (mirrors v3 TaskZuoxiActivity grouping by sechename).
 */

/**
 * Groups flat scheme-task rows into nested [Scheme]s by scheme name.
 *
 * - The domain Scheme [id] IS the scheme name (`sechename`) — that is v3's only
 *   scheme identity, and it is the key the enable/disable POST is keyed on, so
 *   setSchemeActive(schemeId=...) can pass it straight through.
 * - A row with no scheme name is dropped (it can't belong to an addressable scheme).
 * - [Scheme.active] is derived from the scheme's `projectState` (see [isSchemeActive]).
 *   Rows of one scheme should agree on projectState; the first non-null wins.
 * - Insertion order of schemes follows first appearance (stable for the UI).
 */
fun List<SchemeRowDto>.groupRowsIntoSchemes(): List<Scheme> {
    val byScheme = LinkedHashMap<String, MutableList<SchemeRowDto>>()
    for (row in this) {
        val name = row.schemeName?.takeIf { it.isNotBlank() } ?: continue
        byScheme.getOrPut(name) { mutableListOf() }.add(row)
    }
    return byScheme.map { (name, rows) ->
        val projectState = rows.firstNotNullOfOrNull { it.projectState }
        Scheme(
            id = name,
            name = name,
            active = isSchemeActive(projectState),
            tasks = rows.map { it.toSchemeTask(projectState) },
        )
    }
}

/**
 * Whether a scheme is currently running, from its `projectState`.
 *
 * PINNED against real v3 (PA-10, TaskZuoxiActivity:248-254 + startOrStopProject):
 * the v3 list UI renders `projectState == 0` as "on" (运行中) and anything else as
 * "off"; the enable/disable POST writes the new projectState back (enable POSTs
 * state=0). So **0 == active/running**, non-zero == stopped. (Counter-intuitive
 * sign, hence pinned from code, not assumed.) A null projectState (field absent)
 * is treated as not-active.
 */
fun isSchemeActive(projectState: Int?): Boolean = projectState == 0

/**
 * Maps one [SchemeRowDto] to a domain [SchemeTask].
 *
 * Status derivation (PINNED against real v3, PA-10) — Unknown-tolerant:
 *   - scheme not running (projectState != 0 / null)   -> Disabled
 *   - scheme running (projectState == 0) & taskstate>0 -> Running   (task active)
 *   - scheme running & taskstate == 0                  -> Idle      (scheduled, not firing now)
 *   - no usable state at all (both null)               -> Unknown("no-state")
 * The exact taskstate value semantics beyond "0 vs non-zero" are not separately
 * documented; if a richer set surfaces, extend here and unclassifiable -> Unknown
 * (R-003). taskId may be absent on synthetic/aggregate rows → falls back to the
 * task name as id.
 */
fun SchemeRowDto.toSchemeTask(schemeProjectState: Int?): SchemeTask = SchemeTask(
    id = taskId?.takeIf { it.isNotBlank() } ?: taskName.orEmpty(),
    name = taskName.orEmpty(),
    status = deriveTaskStatus(schemeProjectState, taskState),
    startTime = startTime,
    mediaName = mediaName,
    volume = volume,
)

/** Derives [SchemeTaskStatus] from the scheme run state + the per-task state. */
fun deriveTaskStatus(schemeProjectState: Int?, taskState: Int?): SchemeTaskStatus = when {
    schemeProjectState == null && taskState == null -> SchemeTaskStatus.Unknown("no-state")
    !isSchemeActive(schemeProjectState) -> SchemeTaskStatus.Disabled
    (taskState ?: 0) != 0 -> SchemeTaskStatus.Running
    else -> SchemeTaskStatus.Idle
}
