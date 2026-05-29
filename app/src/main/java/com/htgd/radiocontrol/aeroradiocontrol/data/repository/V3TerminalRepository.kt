package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.google.gson.Gson
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.TerminalEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.ZoneEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Terminal
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.V3CallbackAdapter
import com.htgd.radiocontrol.aeroradiocontrol.di.ApplicationScope
import com.htgd.radiocontrol.aeroradiocontrol.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plan A [TerminalRepository] backed by the v3 stack (TASK-PA-01).
 *
 * Replaces the (now-dormant) Retrofit `TerminalRepositoryImpl` via a @Binds swap
 * — the INTERFACE, domain models, Mapper, and consuming ViewModels are unchanged
 * (the seam payoff). Data flows: [V3CallbackAdapter] (v3 RequestManger → raw JSON)
 * → Gson parse into the reverse-engineered DTOs → existing [toTerminalOrNull] /
 * [toZoneOrNull] Mapper → domain models → in-memory SSOT → Flow.
 *
 * SSOT contract (verbatim from AR-101, Critic AC):
 *  - observe* emits the current snapshot, starting at emptyList().
 *  - refresh() populates the SSOT only on full success; on failure the previous
 *    snapshot is retained (caller drives retry from Result.failure).
 *  - concurrent refresh() is de-duplicated so the SSOT can't be left stale:
 *    callers racing in share the one in-flight fetch (AR-102 refreshResult
 *    pattern) rather than interleaving partial updates.
 *
 * Realtime (Plan A): v3 has no server push, so "live" = the ViewModel calling
 * refresh() on entry + optional polling. This repo only provides the refresh
 * primitive; cadence is the UI's choice.
 */
@Singleton
class V3TerminalRepository @Inject constructor(
    private val adapter: V3CallbackAdapter,
    private val gson: Gson,
    private val serverConfig: ServerConfig,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TerminalRepository {

    private val zonesFlow = MutableStateFlow<List<Zone>>(emptyList())

    // In-flight refresh sharing (Critic AC): concurrent refresh() callers share
    // ONE network fetch (launched in appScope so a caller cancelling doesn't
    // kill the shared work) — they all await the same Deferred. The mutex only
    // guards the (start-or-join) decision, not the fetch itself, so it's never
    // held across IO. A finished refresh clears the slot so the next one re-fetches
    // (no permanent stale).
    private val inFlightMutex = Mutex()
    private var inFlight: Deferred<Result<Unit>>? = null

    override fun observeZones(): Flow<List<Zone>> = zonesFlow.asStateFlow()

    override fun observeTerminals(): Flow<List<Terminal>> =
        zonesFlow.map { zones -> zones.flatMap { it.terminals } }

    override suspend fun refresh(): Result<Unit> {
        val deferred = inFlightMutex.withLock {
            inFlight ?: appScope.async(ioDispatcher) { fetchAndPublish() }
                .also { inFlight = it }
        }
        return try {
            deferred.await()
        } finally {
            // Clear the slot once this attempt is done so a later refresh re-fetches.
            inFlightMutex.withLock { if (inFlight === deferred) inFlight = null }
        }
    }

    private suspend fun fetchAndPublish(): Result<Unit> = runCatching {
        val zonesJson = adapter.get(url(PATH_ZONES)).getOrThrow()
        val terminalsJson = adapter.get(url(PATH_TERMINALS)).getOrThrow()

        val terminals = gson.fromJson(terminalsJson, TerminalEnvelopeDto::class.java)
            ?.data?.mapNotNull { it.toTerminalOrNull() }.orEmpty()
        val byZone = terminals.groupBy { it.zoneId }

        val zones = gson.fromJson(zonesJson, ZoneEnvelopeDto::class.java)
            ?.data?.mapNotNull { it.toZoneOrNull() }
            ?.map { zone -> zone.copy(terminals = byZone[zone.id].orEmpty()) }
            .orEmpty()

        // Publish only after both fetches parse — observers never see a half-
        // updated snapshot; on any throw above the old snapshot stays (failure).
        zonesFlow.value = zones
    }

    /** Builds the absolute v3 URL: base (.../api, from ServerConfig) + path. */
    private fun url(path: String): String = serverConfig.baseUrl() + path

    private companion object {
        // Relative endpoint paths (mirror v3 Constant.SearchZone / getMahcinelistAll).
        // Inlined here so this class never loads v3 Constant (whose static init
        // touches Android → unit tests can't load it). Values are LIVE-verified
        // against Constant.java:99 (/terminal/terzone) and :24 (/terminal/terminalinfo).
        const val PATH_ZONES = "/terminal/terzone"
        const val PATH_TERMINALS = "/terminal/terminalinfo"
    }
}
