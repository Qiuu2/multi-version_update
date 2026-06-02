package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.api.TerminalApi
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Terminal
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.data.network.ResponseSuccessPolicy
import com.htgd.radiocontrol.aeroradiocontrol.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * New-stack [TerminalRepository]: Retrofit ([TerminalApi]) + AR-002 interceptors
 * (host/`/api`/auth applied automatically) + [ResponseSuccessPolicy], over an
 * in-memory source of truth.
 *
 * SSOT: [zonesFlow] holds the last successfully-fetched zones (each with its
 * terminals nested). [observeZones]/[observeTerminals] read from it; [refresh]
 * fetches the network and replaces it. On refresh failure the previous snapshot
 * is kept (the caller gets Result.failure to drive a retry UI). The in-memory
 * flow is a stand-in for a Room-backed Flow (a later increment) — swapping the
 * backing store won't change the [TerminalRepository] surface.
 *
 * Success is decided by [ResponseSuccessPolicy] (documented-assumption HTTP 2xx,
 * switchable on OPEN Q3). DTOs map through [TerminalMapper], dropping id-less
 * rows; unrecognised statuses become [com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus.Unknown]
 * (R-003).
 */
@Singleton
class TerminalRepositoryImpl @Inject constructor(
    private val terminalApi: TerminalApi,
    private val successPolicy: ResponseSuccessPolicy,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TerminalRepository {

    private val zonesFlow = MutableStateFlow<List<Zone>>(emptyList())

    override fun observeZones(): Flow<List<Zone>> = zonesFlow.asStateFlow()

    override fun observeTerminals(): Flow<List<Terminal>> =
        zonesFlow.map { zones -> zones.flatMap { it.terminals } }

    override suspend fun refresh(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val zonesResponse = terminalApi.getZones()
            requireSuccess(zonesResponse)
            val terminalsResponse = terminalApi.getTerminals()
            requireSuccess(terminalsResponse)

            // DORMANT impl (PA-14: V3TerminalRepository is bound; this kept as
            // migration asset). Pass wire `zone` field as containingZoneId — the
            // dormant code path's only reachable here if @Binds is ever flipped
            // back, in which case this preserves its old behavior (flawed but
            // unchanged from before PA-14). The REAL impl is V3TerminalRepository.
            val terminals = terminalsResponse.body()?.data
                ?.mapNotNull { it.toTerminalOrNull(it.zone?.toString().orEmpty()) }
                .orEmpty()
            val terminalsByZone: Map<String, List<Terminal>> =
                terminals.groupBy { it.zoneId }

            val zones = zonesResponse.body()?.data
                ?.mapNotNull { dto -> dto.toZoneOrNull() }
                ?.map { zone -> zone.copy(terminals = terminalsByZone[zone.id].orEmpty()) }
                .orEmpty()

            // Publish only after both fetches succeed and are joined — observers
            // never see a half-updated snapshot.
            zonesFlow.value = zones
        }
    }

    /** Throws (-> Result.failure) when the policy rejects the response. */
    private fun requireSuccess(response: Response<*>) {
        if (!successPolicy.isSuccess(response)) {
            throw IOException("Request failed: HTTP ${response.code()}")
        }
    }
}
