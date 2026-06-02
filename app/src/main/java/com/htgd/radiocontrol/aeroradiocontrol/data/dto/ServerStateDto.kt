package com.htgd.radiocontrol.aeroradiocontrol.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Wire DTOs for the server-state endpoint, mirroring the legacy server payload.
 *
 * Envelope: GET /server/serverstate returns `{ "data": [ ServerStateDto ] }` — a
 * single-element list (v3 `TaskManageUtils.getServeNomber` reads `data.get(0)`),
 * same envelope shape as the terminal endpoints (AR-001 §10.3). Gson with
 * `setLenient()` (NetworkModule.provideGson) tolerates unknown/extra fields, so a
 * field the backend adds won't crash parsing (R-003).
 *
 * Field source: the LIVE reverse-engineered ICD-ServerStateDto-v1 (1:1 with v3
 * `model.responseModel.SeverStateModel`, the POJO `SeverStateRsp` deserializes
 * into). All fields nullable so a partial/renamed payload degrades to null rather
 * than NPE-ing. This is REAL contract source under Plan A (v3 跑通即真值), not a
 * to-be-confirmed assumption.
 */

/** `{ "data": [ ServerStateDto ] }` — response of GET /server/serverstate. */
data class ServerStateEnvelopeDto(
    @SerializedName("data") val data: List<ServerStateDto>? = null,
)

/**
 * The server's health snapshot. Mirrors legacy `SeverStateModel` field-for-field.
 *
 * [state] is a status-ish int collapsed to a domain ServerHealth in the Mapper;
 * the rest are plain numeric/string fields the Service Tab (系统健康度) renders.
 * `maxconnection` is `long` in v3 (kept as Long here). All nullable for safety.
 */
data class ServerStateDto(
    @SerializedName("state") val state: Int? = null,
    @SerializedName("connection") val connection: Int? = null,
    @SerializedName("taskcount") val taskCount: Int? = null,
    @SerializedName("bandwidth") val bandwidth: Int? = null,
    @SerializedName("maxconnection") val maxConnection: Long? = null,
    @SerializedName("ctrlport") val ctrlPort: Int? = null,
    @SerializedName("dataport") val dataPort: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("ip") val ip: String? = null,
    @SerializedName("gate") val gate: String? = null,
)
