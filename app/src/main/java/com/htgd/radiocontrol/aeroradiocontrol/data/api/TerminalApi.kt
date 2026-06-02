package com.htgd.radiocontrol.aeroradiocontrol.data.api

import com.htgd.radiocontrol.aeroradiocontrol.data.dto.TerminalEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.ZoneEnvelopeDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for terminal/zone endpoints (ICD-Endpoints-v1).
 *
 * Relative paths only — DynamicBaseUrlInterceptor (AR-002) supplies the host and
 * the `/api` base segment, so these match the legacy `Constant.java` paths 1:1.
 * `Response<T>` exposes the HTTP status for ResponseSuccessPolicy.
 *
 * ⚠ documented-assumption — OPEN(INQ-O-1):
 *   - Q4: `/terminal/zoneterminal/{zoneId}` uses a PATH segment (legacy手拼
 *     `url + "/" + id`); confirm path-param vs query.
 *   - Q1/Q2: response shape `{ "data": [...] }`; confirm.
 *   Pinned when O-1 returns → ICD_UPDATE.
 */
interface TerminalApi {

    /** All terminals. GET /api/terminal/terminalinfo */
    @GET("/terminal/terminalinfo")
    suspend fun getTerminals(): Response<TerminalEnvelopeDto>

    /** All zones. GET /api/terminal/terzone */
    @GET("/terminal/terzone")
    suspend fun getZones(): Response<ZoneEnvelopeDto>

    /** Terminals in one zone. GET /api/terminal/zoneterminal/{zoneId} */
    @GET("/terminal/zoneterminal/{zoneId}")
    suspend fun getZoneTerminals(@Path("zoneId") zoneId: String): Response<TerminalEnvelopeDto>
}
