package com.htgd.radiocontrol.aeroradiocontrol.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Wire DTOs for the 作息 (schedule) endpoints (ICD-SchemeDto §12).
 *
 * Envelope: every list endpoint returns `{ "data": [ ... ] }` (AR-001 §10.3),
 * same shape as terminal/serverstate/media. Gson `setLenient()` tolerates
 * unknown/extra fields → no crash on a server-added field (R-003).
 *
 * ★ PA-15 (2026-05-30) — TWO endpoints, TWO DTO shapes:
 *   1. `GET /task/sechinfo`  → [SchemeEnvelopeDto] / [SchemeRowDto]
 *      — scheme SUMMARY (one row per scheme). Carries `taskcount: Int` (NOT a
 *        task array), `sechename`, `projectstate` (0=running), `startdate/enddate`
 *        (scheme validity), `taskid` (scheme's OWN taskid, namespace-shared with
 *        per-task taskids but semantically the scheme identity).
 *   2. `POST /task/sechetaskinfo` body `{name: <sechename>}` →
 *      [SchemeTaskEnvelopeDto] / [SchemeTaskRowDto]
 *      — per-task TIMELINE rows for ONE scheme. Carries `name` (display name —
 *        NOT `taskname`!), `starttime` (HH:MM:SS), `medianame`, `taskid` (REAL
 *        per-task id), `state/taskstate/enablestate/offlinestate` (four status
 *        ints — collapsed to one domain SchemeTaskStatus in SchemeMapper).
 *
 * PA-10 bug retrospective: TaskGuangboModel on the v3 server is REUSED across
 * both endpoints (same Java class, different per-row population). PA-10 read
 * /sechinfo rows as if they were timeline tasks; per-task fields (real starttime,
 * real medianame, real per-task name) come back null/absent because /sechinfo
 * doesn't carry them. The fix (PA-15) calls BOTH endpoints in series — sechinfo
 * to enumerate schemes, sechetaskinfo per scheme to populate its timeline.
 *
 * Sister lesson: see [[terminal-zone-field-is-not-membership]] memory — wire
 * fields that look like FKs aren't always; here the parallel is wire SHAPES that
 * look like one endpoint suffice often don't.
 */

// ────────────────────────────────────────────────────────────────────────────
// /task/sechinfo  — scheme SUMMARY list
// ────────────────────────────────────────────────────────────────────────────

/** `{ "data": [ SchemeRowDto ] }` — response of GET /task/sechinfo. */
data class SchemeEnvelopeDto(
    @SerializedName("data") val data: List<SchemeRowDto>? = null,
)

/**
 * One scheme summary row from GET /task/sechinfo (per CTO 2026-05-30 capture).
 *
 * IMPORTANT: this is SCHEME-level data only. It does NOT contain per-task
 * starttime / medianame / per-task display name. The timeline must be fetched
 * separately via POST /task/sechetaskinfo body `{name: sechename}` (see
 * [SchemeTaskRowDto]).
 *
 * [sechename] is the domain Scheme identity (v3 has no separate scheme id —
 * `taskid` here is the scheme's representative taskid, and IS sometimes equal
 * to a real per-task taskid in /sechetaskinfo by coincidence; do NOT use it
 * for FK matching).
 * [projectstate] is the scheme's run state: 0 = RUNNING (counter-intuitive
 * sign — pinned from TaskZuoxiActivity:248-254 + startOrStopProject).
 *
 * Defensive: the misspelled [projectStateTate] field lives on the OTHER v3 model
 * (TaskZuoxiModel, line 40), not on TaskGuangboModel (this wire). It is kept as
 * an OPTIONAL captured-verbatim field in case the server ever emits it; the
 * authoritative scheme-run flag on THIS wire is the clean [projectState].
 *
 * All fields nullable so a partial/renamed payload degrades to null, not NPE.
 */
data class SchemeRowDto(
    @SerializedName("taskid") val taskId: Int? = null,
    @SerializedName("taskstate") val taskState: Int? = null,
    @SerializedName("taskcount") val taskCount: Int? = null,
    @SerializedName("startdate") val startDate: String? = null,
    @SerializedName("enddate") val endDate: String? = null,
    @SerializedName("sechename") val schemeName: String? = null,
    @SerializedName("projectstate") val projectState: Int? = null,
    // Envelope-meta echoed on each row by the v3 server (CTO capture).
    @SerializedName("all") val all: Int? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("start") val start: Int? = null,
    @SerializedName("state") val state: Int? = null,
    // Defensive: misspelled field from the other model (TaskZuoxiModel:40). Kept
    // verbatim if the server ever emits it on this endpoint.
    @SerializedName("projectstatetate") val projectStateTate: String? = null,
)

// ────────────────────────────────────────────────────────────────────────────
// /task/sechetaskinfo  — per-scheme TIMELINE tasks
// ────────────────────────────────────────────────────────────────────────────

/** `{ "data": [ SchemeTaskRowDto ] }` — response of POST /task/sechetaskinfo
 *  body `{name: <sechename>}`. */
data class SchemeTaskEnvelopeDto(
    @SerializedName("data") val data: List<SchemeTaskRowDto>? = null,
)

/**
 * One per-task timeline row from POST /task/sechetaskinfo (per CTO 2026-05-30
 * capture of scheme "海王作息", 14 tasks).
 *
 * ★★★ DISPLAY NAME field = [name], NOT `taskname`. PA-10 mapped `taskname` and
 *   would have rendered blank titles here. The CTO capture confirms `name` is
 *   the per-task display string (e.g. "早读开始铃", "第一节课上课铃"). Defensive
 *   coverage of `taskname` is NOT added — it isn't on the wire for this endpoint.
 *
 * ★ FOUR status fields ([state], [taskState], [enableState], [offlineState]) —
 *   PA-14 terminal family pattern. The CTO capture shows all four populated per
 *   row but with mostly-zero values (single capture snapshot can't fully
 *   enumerate the value set). [SchemeMapper.deriveTaskStatus] derives a single
 *   Unknown-tolerant [SchemeTaskStatus]; the rule is a documented-assumption
 *   (R-003 safe — unrecognised → Unknown).
 *
 * ★ [info] field redundantly carries the parent scheme name (sechename
 *   back-pointer; e.g. "海王作息" on every row in the 海王 capture). NOT an FK
 *   — same lesson as [[terminal-zone-field-is-not-membership]]: don't trust a
 *   wire field's apparent linking semantics until verified.
 *
 * ★ [taskId] vs sechinfo.taskid: per-task [taskId] in this endpoint (e.g. 73657)
 *   is a REAL per-task id; sechinfo.taskid (scheme's own) sometimes coincidentally
 *   equals a per-task id (CTO capture: scheme 海王 taskid=73683, and per-task
 *   taskid 73683 is also a real task). They share a namespace but are different
 *   things; do NOT use this for cross-endpoint linking.
 *
 * ★ [execmode] is a weekday bitmask (62 = 0b111110 = Mon-Fri in the CTO capture).
 *   Domain promotion deferred; fe can render "周一-周五" later (TODO weekday
 *   bitmask UI). Captured here for ICD completeness.
 *
 * All 23 wire fields per CTO capture's `field_inventory_full_dto_coverage_required`
 * are present — even MVP-deferred ones — to avoid the PA-14 trap of a missing
 * field surfacing later. All nullable (R-003).
 */
data class SchemeTaskRowDto(
    // Identity + display
    @SerializedName("taskid") val taskId: Int? = null,
    /** ★★★ Per-task display name. CTO capture confirms field is `name`, NOT `taskname`. */
    @SerializedName("name") val name: String? = null,
    /** Redundant back-pointer to the parent sechename (NOT an FK). */
    @SerializedName("info") val info: String? = null,
    // Schedule fields (the timeline)
    @SerializedName("starttime") val startTime: String? = null,      // HH:MM:SS
    @SerializedName("startdate") val startDate: String? = null,      // task-level validity start
    @SerializedName("enddate") val endDate: String? = null,          // task-level validity end
    @SerializedName("execmode") val execMode: Int? = null,           // weekday bitmask (62 = Mon-Fri)
    @SerializedName("lengthtype") val lengthType: Int? = null,
    @SerializedName("length") val length: Int? = null,
    // Media
    @SerializedName("mediaid") val mediaId: Int? = null,
    @SerializedName("medianame") val mediaName: String? = null,
    // Status (4 fields — collapsed in SchemeMapper)
    @SerializedName("state") val state: Int? = null,
    @SerializedName("taskstate") val taskState: Int? = null,
    @SerializedName("enablestate") val enableState: Int? = null,
    @SerializedName("offlinestate") val offlineState: Int? = null,
    // Playback config (DTO-only today; MVP-deferred)
    @SerializedName("volume") val volume: Int? = null,
    @SerializedName("priority") val priority: Int? = null,
    @SerializedName("prepower") val prepower: Int? = null,
    @SerializedName("israndomplay") val isRandomPlay: Int? = null,
    @SerializedName("datasendmodel") val dataSendModel: Int? = null,
    // Envelope-meta echoed on each row
    @SerializedName("all") val all: Int? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("start") val start: Int? = null,
)

// ────────────────────────────────────────────────────────────────────────────
// /task/sechenableordisable  — enable/disable POST reply (unchanged from PA-10)
// ────────────────────────────────────────────────────────────────────────────

/**
 * `{ "data": [ { "state": Int, "id": String } ] }` — response of the enable/
 * disable POST (/task/sechenableordisable), mirroring v3 `TaskStateRsp` /
 * `TaskStateModel`. `state` 0 = ChangeSucess, 15 = TheStateIsSame, else = failed
 * (ErrorCode). Used by V3TaskRepository to confirm setSchemeActive succeeded.
 */
data class TaskStateEnvelopeDto(
    @SerializedName("data") val data: List<TaskStateDto>? = null,
)

data class TaskStateDto(
    @SerializedName("state") val state: Int? = null,
    @SerializedName("id") val id: String? = null,
)
