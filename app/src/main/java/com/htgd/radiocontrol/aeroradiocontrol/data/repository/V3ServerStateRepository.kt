package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.google.gson.Gson
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.ServerStateEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerState
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plan A [ServerStateRepository] backed by the v3 stack (TASK-PA-05).
 *
 * REAL impl (not a stub) — the Service Tab is a single health endpoint, so it's
 * built directly, mirroring [V3TerminalRepository] exactly. Data flows:
 * [V3CallbackAdapter] (v3 RequestManger → raw JSON) → Gson parse into the
 * reverse-engineered [ServerStateEnvelopeDto] → [toServerState] Mapper → domain
 * [ServerState] → in-memory SSOT → Flow.
 *
 * The v3 origin of this endpoint is `TaskManageUtils.getServeNomber`, which calls
 * the same `RequestManger.get(url, onRequestLister)` primitive V3CallbackAdapter
 * wraps; it reads `data.get(0)` (single-element list). We do NOT touch
 * TaskManageUtils / *Method / the SeverState POJOs, and we do NOT use the dormant
 * new-stack `HealthApiService` (Plan A). The URL base comes from [ServerConfig]
 * (= Constant.serveraddress, the same base login set and V3TerminalRepository
 * uses) — consistent with the other Plan A repos; we do not read v3's separate
 * `PreferencesUtil("serverAddress")` slot.
 *
 * SSOT contract (verbatim from V3TerminalRepository / Critic AC):
 *  - observeServerState emits the current snapshot, starting at null.
 *  - refresh() publishes only on full success; on failure (network/parse, or an
 *    empty data array) the previous snapshot is retained (caller drives retry).
 *  - concurrent refresh() is de-duplicated so the SSOT can't be left stale:
 *    racing callers share the one in-flight fetch rather than interleaving.
 *
 * Realtime (Plan A): v3 has no server push, so "live" = the ViewModel calling
 * refresh() on entry + optional polling. This repo only provides the refresh
 * primitive; cadence is the UI's choice.
 */
@Singleton
class V3ServerStateRepository @Inject constructor(
    private val adapter: V3CallbackAdapter,
    private val gson: Gson,
    private val serverConfig: ServerConfig,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ServerStateRepository {

    private val stateFlow = MutableStateFlow<ServerState?>(null)

    // In-flight refresh sharing (Critic AC): concurrent refresh() callers share
    // ONE network fetch (launched in appScope so a caller cancelling doesn't kill
    // the shared work) — they all await the same Deferred. The mutex only guards
    // the (start-or-join) decision, never the fetch, so it's never held across IO.
    // A finished refresh clears the slot so the next one re-fetches (no permanent stale).
    private val inFlightMutex = Mutex()
    private var inFlight: Deferred<Result<Unit>>? = null

    override fun observeServerState(): Flow<ServerState?> = stateFlow.asStateFlow()

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
        val json = adapter.get(url(PATH_SERVER_STATE)).getOrThrow()

        val first = gson.fromJson(json, ServerStateEnvelopeDto::class.java)
            ?.data?.firstOrNull()
            ?: throw IOException("serverstate response had no data")

        // Publish only after a successful parse — on any throw above the old
        // snapshot stays (failure). v3 reads data.get(0); we mirror that.
        stateFlow.value = first.toServerState()
    }

    /** Builds the absolute v3 URL: base (.../api, from ServerConfig) + path. */
    private fun url(path: String): String = serverConfig.baseUrl() + path

    private companion object {
        // Relative endpoint path (mirrors v3 Constant.getServerState). Inlined here
        // so this class never loads v3 Constant (whose static init touches Android →
        // unit tests can't load it). LIVE-verified against Constant.java:97.
        const val PATH_SERVER_STATE = "/server/serverstate"
    }
}
