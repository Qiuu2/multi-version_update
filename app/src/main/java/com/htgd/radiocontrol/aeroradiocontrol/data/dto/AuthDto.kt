package com.htgd.radiocontrol.aeroradiocontrol.data.dto

import com.google.gson.annotations.SerializedName

/**
 * Auth-endpoint DTOs for `POST /api/authorizations` (login).
 *
 * Field names mirror the legacy `TokenModel` / `*Rsp` envelope verbatim (see
 * AR-001 ICD-Endpoints-v1 §10.4 and the legacy `httptask` read), so the same
 * server payload deserializes unchanged:
 *   response body = { "data": [ TokenDto ] }   ← no code/message (OPEN Q2)
 *
 * OPEN(INQ-O-1 Q2/D-2): the envelope has only `data` per the legacy read; if the
 * backend confirms a code/message wrapper, this gains those fields (ICD_UPDATE).
 */
data class TokenEnvelopeDto(
    @SerializedName("data") val data: List<TokenDto>?,
)

/**
 * One token record. Mirrors legacy `model.responseModel.TokenModel`.
 *
 * `refreshExpiredAt` is present in the legacy payload and hints a refresh token
 * may exist server-side though the legacy app never used a refresh endpoint
 * (OPEN INQ-O-1 D-1). No `refreshToken` field is mapped yet because the legacy
 * response did not carry one under a known name — added when D-1 is answered.
 */
data class TokenDto(
    @SerializedName("token") val token: String?,
    @SerializedName("expired_at") val expiredAt: String? = null,
    @SerializedName("refresh_expired_at") val refreshExpiredAt: String? = null,
    @SerializedName("priority") val priority: String? = null,
)
