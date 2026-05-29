package com.htgd.radiocontrol.aeroradiocontrol.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Wire DTOs for the 作息方案 (schedule scheme) list endpoint, mirroring the legacy
 * server payload (ICD-SchemeDto-v1 §12).
 *
 * Envelope: GET /task/sechinfo returns `{ "data": [ ... ] }` (AR-001 §10.3), same
 * shape as the terminal/serverstate/media endpoints. Gson with `setLenient()`
 * (NetworkModule.provideGson) tolerates unknown/extra fields → no crash (R-003).
 *
 * WIRE-SHAPE NOTE (pinned against real v3, PA-10): /task/sechinfo deserializes
 * into v3 `TaskGuangboListRsp{data:[TaskGuangboModel]}` (NOT TaskZuoxiModel — see
 * TaskZuoxiActivity:288). Each row is ONE TASK that also carries its parent
 * scheme's name (`sechename`) and the scheme's run state (`projectstate`); the v3
 * UI groups rows by `sechename` to form schemes (TaskZuoxiActivity:301-348).
 * SchemeRowDto therefore mirrors the per-row TaskGuangboModel; V3TaskRepository
 * groups rows into domain Schemes by [sechename].
 *
 * projectstatetate typo (ICD §12): the misspelled field lives on the OTHER v3
 * model (TaskZuoxiModel), not on TaskGuangboModel (this wire). It is included here
 * as an OPTIONAL extra field defensively (cost-free, R-003) in case the server
 * emits it on this endpoint too — if present it is NOT lost. The authoritative
 * scheme-run field on THIS wire is the clean `projectstate`.
 *
 * All fields nullable so a partial/renamed payload degrades to null, not NPE.
 */

/** `{ "data": [ SchemeRowDto ] }` — response of GET /task/sechinfo. */
data class SchemeEnvelopeDto(
    @SerializedName("data") val data: List<SchemeRowDto>? = null,
)

/**
 * One scheme-task row. Mirrors the subset of legacy `TaskGuangboModel` the v4 Task
 * Tab needs. [sechename] is the parent scheme name (the grouping key + domain
 * Scheme id); [projectstate] is the scheme's run state (0 = running, see Mapper);
 * [taskstate] is the per-task state.
 */
data class SchemeRowDto(
    @SerializedName("taskid") val taskId: String? = null,
    @SerializedName("taskname") val taskName: String? = null,
    @SerializedName("sechename") val schemeName: String? = null,
    @SerializedName("starttime") val startTime: String? = null,
    @SerializedName("medianame") val mediaName: String? = null,
    @SerializedName("volume") val volume: Int? = null,
    @SerializedName("taskstate") val taskState: Int? = null,
    @SerializedName("projectstate") val projectState: Int? = null,
    @SerializedName("enablestate") val enableState: Int? = null,
    // Defensive: the misspelled TaskZuoxiModel field, captured verbatim if the
    // server ever sends it on this endpoint (ICD §12). Not the authoritative source.
    @SerializedName("projectstatetate") val projectStateTate: String? = null,
)

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
