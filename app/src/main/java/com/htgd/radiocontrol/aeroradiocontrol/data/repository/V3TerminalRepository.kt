package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.google.gson.Gson
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
 * Plan A [TerminalRepository] backed by the v3 stack.
 *
 * ★ PA-14 (2026-05-30) — single endpoint, no client-side join:
 *   GET /terminal/terzone is the AUTHORITATIVE source for both the zone list
 *   AND each zone's terminals (they are nested on the wire as ZoneDto.terminal[]).
 *   We map each ZoneDto with its nested array — V3CallbackAdapter → Gson
 *   ZoneEnvelopeDto → [toZoneOrNull] (which threads the parent zone id into
 *   each nested terminal). NO second call to /terminal/terminalinfo, NO
 *   groupBy on `terminal.zone` (that field is not the membership key — root
 *   cause of the 操场 mismatch; see TerminalDto KDoc + zone-mapping-rootcause.md).
 *
 * Many-to-many preserved: one terminal can appear nested under multiple zones
 * (CTO capture: id=14 in 操场/英语角/航天/会议室). observeTerminals() therefore
 * returns the FLATTENED-with-duplicates list (one entry per nesting); the UI
 * dedupes if it wants a unique terminal set.
 *
 * SSOT contract (verbatim from AR-101, Critic AC):
 *  - observe* emits the current snapshot, starting at emptyList().
 *  - refresh() populates the SSOT only on success; on failure the previous
 *    snapshot is retained (caller drives retry from Result.failure).
 *  - concurrent refresh() is de-duplicated so the SSOT can't be left stale:
 *    callers racing in share the one in-flight fetch.
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
    // ONE network fetch (launched in appScope so a caller cancelling doesn't kill
    // the shared work). The mutex only guards the (start-or-join) decision, not
    // the fetch, so it's never held across IO. A finished refresh clears the slot
    // so the next one re-fetches (no permanent stale).
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
            inFlightMutex.withLock { if (inFlight === deferred) inFlight = null }
        }
    }

    private suspend fun fetchAndPublish(): Result<Unit> = runCatching {
        val json = adapter.get(url(PATH_ZONES)).getOrThrow()
        // Map each zone with its OWN nested terminal[] — no cross-zone join. The
        // Mapper threads each zone's id into the nested terminals' zoneId.
        val zones = gson.fromJson(json, ZoneEnvelopeDto::class.java)
            ?.data?.mapNotNull { it.toZoneOrNull() }
            .orEmpty()

        // Publish only after the fetch parses — observers never see a half-
        // updated snapshot; on any throw above the old snapshot stays (failure).
        zonesFlow.value = zones
    }

    /** Builds the absolute v3 URL: base (.../api, from ServerConfig) + path. */
    private fun url(path: String): String = serverConfig.baseUrl() + path

    private companion object {
        // Relative endpoint path — LIVE-verified against Constant.java:99
        // (/terminal/terzone, the AUTHORITATIVE source for zones + nested terminals).
        // /terminal/terminalinfo is NOT used here (PA-14); the previous two-fetch
        // join by `terminal.zone` was the root cause of the 操场 mismatch.
        const val PATH_ZONES = "/terminal/terzone"
    }
}
