package com.htgd.radiocontrol.aeroradiocontrol.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Wire DTOs for the terminal endpoints, mirroring the legacy server payload.
 *
 * Envelope: every list endpoint returns `{ "data": [ ... ] }` with no
 * code/message (AR-001 §10.3). Gson `setLenient()` tolerates unknown/extra
 * fields, satisfying R-003 defensive parsing (no crash on a field the backend
 * adds).
 *
 * ★ PA-14 root-cause pin (2026-05-30): the response shape below is the LIVE
 * ground truth, captured by CTO via Bearer-token curl against
 * GET /api/terminal/terzone — see `.state/api-snapshots/terzone-cto-capture-2026-05-30.json`
 * and `.state/api-snapshots/zone-mapping-rootcause.md`. The previous DTOs were
 * incomplete and the Repository misjoined by `terminal.zone`; both are fixed
 * here and in V3TerminalRepository.
 */

/** `{ "data": [ TerminalDto ] }` — response of GET /terminal/terminalinfo (flat
 *  all-terminals list). NOT used by V3TerminalRepository anymore (PA-14): the
 *  Zone view reads only `/terminal/terzone` and pulls terminals from each
 *  ZoneDto's nested [ZoneDto.terminal]. Other consumers (e.g. a future flat
 *  picker) may still hit this endpoint. */
data class TerminalEnvelopeDto(
    @SerializedName("data") val data: List<TerminalDto>? = null,
)

/** `{ "data": [ ZoneDto ] }` — response of GET /terminal/terzone (zones with
 *  nested terminals). This is the AUTHORITATIVE source for zone↔terminal
 *  membership (PA-14). */
data class ZoneEnvelopeDto(
    @SerializedName("data") val data: List<ZoneDto>? = null,
)

/**
 * One terminal — captures every field present on the LIVE terzone capture
 * (24 fields), plus envelope-meta (`all/count/start/state`) that each row
 * carries on the wire. Mirrors legacy `model.responseModel.MachineInfo`.
 *
 * ★ [zone] field — DO NOT use for zone membership (PA-14 root cause).
 *   CTO capture shows zone "操场" (id=1) has terminals whose `terminal.zone`
 *   values are 0,0,0,8 — none equal the zone id 1. This field's semantics are
 *   not documented and v3 itself does NOT use it for grouping; v3 calls
 *   /terminal/zoneterminal/{id} per zone to read membership from the server.
 *   In Plan A we use the nested [ZoneDto.terminal] array from /terminal/terzone
 *   as the single source of membership. Kept on the DTO for round-trip + future
 *   use only.
 *
 * Status: four granular int fields (taskstate/devicestate/netstate/speechstate)
 * are collapsed to the domain [com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus]
 * in TerminalMapper. All fields nullable for safety (R-003).
 */
data class TerminalDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("type") val type: Int? = null,
    @SerializedName("ip") val ip: String? = null,
    // Granular status ints (collapse to TerminalStatus in TerminalMapper).
    @SerializedName("taskstate") val taskState: Int? = null,
    @SerializedName("devicestate") val deviceState: Int? = null,
    @SerializedName("netstate") val netState: Int? = null,
    @SerializedName("speechstate") val speechState: Int? = null,
    @SerializedName("volume") val volume: Int? = null,
    @SerializedName("isinstancy") val isUrgent: Int? = null,
    // ⚠ NOT zone membership — see class KDoc.
    @SerializedName("zone") val zone: Int? = null,
    @SerializedName("longitude") val longitude: String? = null,
    @SerializedName("latitude") val latitude: String? = null,
    // PA-14 new fields (from CTO ground truth):
    @SerializedName("isrecord") val isRecord: Int? = null,
    @SerializedName("issponsor") val isSponsor: Int? = null,
    @SerializedName("shortcircuit") val shortCircuit: Int? = null,
    @SerializedName("lopencircuit") val lOpenCircuit: Int? = null,
    @SerializedName("ropencircuit") val rOpenCircuit: Int? = null,
    @SerializedName("temperature") val temperature: Int? = null,
    @SerializedName("humidity") val humidity: Int? = null,
    @SerializedName("isdecode") val isDecode: Int? = null,
    @SerializedName("isencode") val isEncode: Int? = null,
    @SerializedName("switchcount") val switchCount: Int? = null,
    // Envelope-meta the server echoes on every row.
    @SerializedName("all") val all: Int? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("start") val start: Int? = null,
    @SerializedName("state") val state: Int? = null,
    // Defensive: `groupid` was in the previous DTO + legacy MachineInfo. Not in
    // the CTO terzone capture; kept as optional in case other endpoints emit it.
    @SerializedName("groupid") val groupId: Int? = null,
)

/**
 * One zone, with its nested terminals. Mirrors legacy `model.ZoneModel` plus
 * the LIVE terzone-capture fields (envelope-meta + nested terminal array).
 *
 * ★ [terminal] is the AUTHORITATIVE zone-membership source (PA-14). It is a
 *   list of full [TerminalDto] objects (NOT id references); read it directly.
 *   Do NOT reconstruct membership from /terminal/terminalinfo + groupBy on
 *   `terminal.zone` — that field is not the membership key (see TerminalDto KDoc).
 *
 * Many-to-many: one terminal can appear nested under multiple zones (CTO
 * capture shows id=14 in 操场+英语角+航天+会议室). This is preserved by reading
 * each zone's terminal[] independently; v4 must NOT dedupe across zones.
 */
data class ZoneDto(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("datetime") val datetime: String? = null,
    // Nested terminals (PA-14 — the membership source).
    @SerializedName("terminal") val terminal: List<TerminalDto>? = null,
    // Envelope-meta (echoed on every zone row per CTO capture).
    @SerializedName("all") val all: String? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("start") val start: Int? = null,
    @SerializedName("state") val state: Int? = null,
    // Per-zone derived counts (legacy ZoneModel fields; absent in CTO capture
    // but kept defensively in case other endpoints emit them).
    @SerializedName("online") val online: Int? = null,
    @SerializedName("offline") val offline: Int? = null,
    @SerializedName("busyline") val busyLine: Int? = null,
)
