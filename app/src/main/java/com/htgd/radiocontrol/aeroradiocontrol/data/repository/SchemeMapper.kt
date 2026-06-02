package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.dto.SchemeRowDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.SchemeTaskRowDto
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Scheme
import com.htgd.radiocontrol.aeroradiocontrol.data.model.SchemeTask
import com.htgd.radiocontrol.aeroradiocontrol.data.model.SchemeTaskStatus

/**
 * DTO -> domain mappers for the 作息 (schedule) domain.
 *
 * ★ PA-15 (2026-05-30) — rewritten for the TWO-endpoint flow:
 *   1. [SchemeRowDto.toSchemeSummary] — one /sechinfo row → Scheme(name, active,
 *      tasks=empty); the SKELETON onto which per-task rows are attached.
 *   2. [SchemeTaskRowDto.toSchemeTask] — one /sechetaskinfo row → one SchemeTask
 *      with derived status (from the 4 wire status ints).
 * The Repository composes them: GET /sechinfo → for each scheme parallel POST
 * /sechetaskinfo body `{name: sechename}` → attach tasks to the scheme summary.
 *
 * Sister lessons: [[terminal-zone-field-is-not-membership]] (don't trust a wire
 * field's apparent semantics — applied here to the 4 status ints, the `info`
 * back-pointer, and the name/taskname distinction); [[scheme-dto-misspelled-field]]
 * (TaskZuoxiModel vs TaskGuangboModel — settled to TaskGuangboModel; still right).
 */

/**
 * Whether a scheme is currently running, from its `projectState`.
 *
 * PINNED against real v3 (PA-10, TaskZuoxiActivity:248-254 + startOrStopProject):
 * the v3 list UI renders `projectState == 0` as "on" (运行中) and anything else
 * as "off"; the enable/disable POST writes the new projectState back (enable
 * POSTs state=0). So **0 == active/running**, non-zero == stopped. A null
 * projectState (field absent) is treated as not-active.
 *
 * MULTI-ACTIVE NOTE (PA-15 CTO capture): on real v3 backends MULTIPLE schemes
 * can have projectState=0 simultaneously (the capture showed both 海王作息 AND
 * 日本作息 active). [isSchemeActive] is per-scheme and correctly returns true
 * for each; the ViewModel-level pick of WHICH active scheme to feature is
 * upstream (current TaskHomeViewModel.activeScheme = first-active wins, Option A
 * — documented-assumption: fe may upgrade to expose the full active list later;
 * the data layer SSOT already carries all schemes, no repo change needed for
 * the upgrade).
 */
fun isSchemeActive(projectState: Int?): Boolean = projectState == 0

/**
 * Maps a [SchemeRowDto] (scheme-SUMMARY row from /sechinfo) to a [Scheme]
 * SKELETON with empty tasks, or null if the row has no usable scheme name.
 *
 * The Repository attaches the real per-task list by POST /sechetaskinfo body
 * `{name: sechename}` and overwriting [Scheme.tasks].
 */
fun SchemeRowDto.toSchemeSummary(): Scheme? {
    val name = schemeName?.takeIf { it.isNotBlank() } ?: return null
    return Scheme(
        id = name,
        name = name,
        active = isSchemeActive(projectState),
        tasks = emptyList(),
    )
}

/**
 * Maps one [SchemeTaskRowDto] (per-task row from /sechetaskinfo) to a domain
 * [SchemeTask]. The [schemeIsActive] parameter is the parent scheme's run state
 * (needed because a per-task `state` int alone doesn't tell us whether the
 * SCHEME is running — a task under a stopped scheme is functionally Disabled
 * regardless of its own state ints).
 *
 * Field mapping (CTO 2026-05-30 capture, scheme 海王作息):
 *   - [SchemeTask.id]        = wire `taskid.toString()` (REAL per-task id, e.g.
 *                              73657); falls back to `name`, then "" (domain
 *                              SchemeTask.id is non-null String).
 *   - [SchemeTask.name]      = wire `name` (★★★ NOT `taskname`).
 *   - [SchemeTask.startTime] = wire `starttime` (HH:MM:SS, e.g. "07:50:00").
 *   - [SchemeTask.mediaName] = wire `medianame` (e.g. "上课铃.mp3").
 *   - [SchemeTask.volume]    = wire `volume`.
 *   - [SchemeTask.status]    = derived (see [deriveTaskStatus]).
 *
 * MVP-deferred wire fields (captured on the DTO, not on the domain SchemeTask
 * yet): execmode (weekday bitmask, e.g. 62=Mon-Fri), startdate/enddate
 * (task-level validity), length/lengthtype, prepower, priority, israndomplay,
 * datasendmodel, mediaid. fe can promote these later → ICD_UPDATE (the domain
 * shape stays Unknown-safe).
 */
fun SchemeTaskRowDto.toSchemeTask(schemeIsActive: Boolean): SchemeTask = SchemeTask(
    id = taskId?.toString()?.takeIf { it.isNotBlank() }
        ?: name?.takeIf { it.isNotBlank() }
        ?: "",
    name = name.orEmpty(),
    status = deriveTaskStatus(schemeIsActive, state, taskState, enableState, offlineState),
    startTime = startTime,
    mediaName = mediaName,
    volume = volume,
)

/**
 * Derives a single [SchemeTaskStatus] from the parent scheme's run state + the
 * FOUR per-task status ints on /sechetaskinfo (state / taskstate / enablestate /
 * offlinestate). Unknown-tolerant (R-003) — sister to TerminalStatus derivation.
 *
 * documented-assumption — pending O-? clarification:
 *   The CTO 2026-05-30 capture shows mostly-zero values (state=0, taskstate=0,
 *   offlinestate=0, enablestate=1 across all 14 海王作息 rows). A single capture
 *   cannot fully enumerate the value set; the rules below are conservative,
 *   bias to surface clear off-states explicitly, and degrade any unrecognised
 *   combination to [SchemeTaskStatus.Unknown] with the raw signature.
 *
 * Current rules (top→bottom, first match wins):
 *   1. scheme NOT active                                  → Disabled
 *      (a per-task row under a stopped scheme can't be running; matches v3
 *      TaskZuoxiActivity rendering of stopped schemes.)
 *   2. enableState == 0 (per-task disable flag)           → Disabled
 *      (analogue: /task/taskdoorno "0=enable,1=disable"; CTO capture shows
 *      enableState=1 for active tasks, so 0 = disabled is consistent.)
 *   3. taskState != 0                                     → Running
 *      (any per-task non-zero "executing/paused/firing" signal — collapse to
 *      Running and let fe show "executing"; refine when O-? returns.)
 *   4. all four ints are null                             → Unknown("no-state")
 *      (defensive — a sparse row with no signals; should not happen on the wire.)
 *   5. otherwise (scheme active, no per-task running signal) → Idle
 *      (scheduled but not firing now — CTO capture's normal case).
 *
 * Any future richer per-task state set → extend this function; the sealed type
 * + fe boundary stay (must keep an `Unknown` branch).
 */
fun deriveTaskStatus(
    schemeIsActive: Boolean,
    state: Int?,
    taskState: Int?,
    enableState: Int?,
    offlineState: Int?,
): SchemeTaskStatus {
    if (!schemeIsActive) return SchemeTaskStatus.Disabled
    if (enableState == 0) return SchemeTaskStatus.Disabled
    if ((taskState ?: 0) != 0) return SchemeTaskStatus.Running
    val allNull = state == null && taskState == null &&
        enableState == null && offlineState == null
    if (allNull) {
        return SchemeTaskStatus.Unknown(
            "no-state(state=null,taskstate=null,enablestate=null,offlinestate=null)",
        )
    }
    return SchemeTaskStatus.Idle
}
