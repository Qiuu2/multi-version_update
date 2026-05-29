package com.htgd.radiocontrol.aeroradiocontrol.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Wire DTOs for the terminal endpoints, mirroring the legacy server payload.
 *
 * Envelope: every list endpoint returns `{ "data": [ ... ] }` with no
 * code/message (AR-001 §10.3, OPEN Q2). Gson is the project's JSON lib (not
 * kotlinx.serialization); `GsonBuilder().setLenient()` in NetworkModule already
 * tolerates unknown/extra fields, satisfying the R-003 defensive-parsing
 * requirement (no crash on a field the backend adds).
 *
 * ⚠ documented-assumption — field names from the legacy `MachineInfo` /
 * `ZoneModel` read; the real `/terminal/terminalinfo` response is unconfirmed
 * until OPEN(INQ-O-1 Q1). All fields nullable so a partial/renamed payload
 * degrades gracefully rather than NPE-ing. Pinned when O-1 returns → ICD_UPDATE.
 */

/** `{ "data": [ TerminalDto ] }` — response of GET /terminal/terminalinfo. */
data class TerminalEnvelopeDto(
    @SerializedName("data") val data: List<TerminalDto>? = null,
)

/** `{ "data": [ ZoneDto ] }` — response of GET /terminal/terzone (zones). */
data class ZoneEnvelopeDto(
    @SerializedName("data") val data: List<ZoneDto>? = null,
)

/**
 * One terminal. Mirrors legacy `model.responseModel.MachineInfo`.
 *
 * Note the FOUR granular int status fields (taskstate/devicestate/netstate/
 * speechstate) — the legacy server does NOT send a single "state". How they
 * collapse into the UI's one [com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus]
 * is OPEN(INQ-O-1 D-3 + O-2); see TerminalMapper. All nullable for safety.
 */
data class TerminalDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("ip") val ip: String? = null,
    @SerializedName("zone") val zone: Int? = null,
    @SerializedName("groupid") val groupId: Int? = null,
    @SerializedName("type") val type: Int? = null,
    @SerializedName("taskstate") val taskState: Int? = null,
    @SerializedName("devicestate") val deviceState: Int? = null,
    @SerializedName("netstate") val netState: Int? = null,
    @SerializedName("speechstate") val speechState: Int? = null,
    @SerializedName("volume") val volume: Int? = null,
    @SerializedName("isinstancy") val isUrgent: Int? = null,
    @SerializedName("longitude") val longitude: String? = null,
    @SerializedName("latitude") val latitude: String? = null,
)

/**
 * One zone. Mirrors legacy `model.ZoneModel`. `online`/`offline`/`busyline` are
 * the per-zone terminal counts the v4 zone header shows.
 */
data class ZoneDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("online") val online: Int? = null,
    @SerializedName("offline") val offline: Int? = null,
    @SerializedName("busyline") val busyLine: Int? = null,
)
