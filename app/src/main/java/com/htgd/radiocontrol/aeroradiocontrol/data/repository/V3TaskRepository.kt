package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import android.content.Context
import com.google.gson.Gson
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.SchemeEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.TaskStateEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Scheme
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TaskLog
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.V3CallbackAdapter
import com.htgd.radiocontrol.aeroradiocontrol.di.ApplicationScope
import com.htgd.radiocontrol.aeroradiocontrol.di.IoDispatcher
import com.htgd.radiocontrol.aeroradiocontrol.httptask.MyRequestBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plan A [TaskRepository] backed by the v3 stack (TASK-PA-10 — REAL impl, replaces
 * the PA-03b stub).
 *
 * Mirrors [V3TerminalRepository]: [V3CallbackAdapter] (v3 RequestManger → raw JSON)
 * → Gson parse into the reverse-engineered [SchemeEnvelopeDto] → [SchemeMapper]
 * (group flat rows into nested Schemes by scheme name) → domain → in-memory SSOT
 * → Flow.
 *
 * v3 wire (pinned against real code, PA-10):
 *  - LIST:    GET  /task/sechinfo (Constant.getsecheList) → TaskGuangboListRsp
 *             {data:[TaskGuangboModel]}. FLAT rows; each carries its scheme name
 *             (sechename) + scheme run state (projectstate); grouped into schemes
 *             by sechename (TaskZuoxiActivity:288/301-348).
 *  - ENABLE:  POST /task/sechenableordisable (Constant.postChangeTaskStatu), form
 *             {sechename, state}. state 0 = enable/running, 1 = disable (v3
 *             startOrStopProject + TaskZuoxiActivity:248-254 — note 0==on). Reply
 *             TaskStateRsp{data:[{state}]}: 0 ChangeSucess / 15 TheStateIsSame both
 *             OK, else failed (ErrorCode).
 *
 * I-3 (single source): setSchemeActive does NOT locally flip the SSOT; on a
 * successful POST it triggers a [refresh] so the new projectstate comes from the
 * server and re-emits through observeSchemes. No divergent local write.
 *
 * R-ADDR-SLOT (dormant): base URL is [ServerConfig].baseUrl() (= Constant.server-
 * address), like all Plan A repos — NOT v3's PreferencesUtil("serverAddress"). No
 * legacy Activity launched.
 *
 * SSOT contract (verbatim from V3TerminalRepository / Critic AC): observeSchemes
 * starts empty; refresh() publishes only on success; on failure the previous
 * snapshot is retained; concurrent refresh() shares ONE in-flight fetch.
 */
@Singleton
class V3TaskRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adapter: V3CallbackAdapter,
    private val gson: Gson,
    private val serverConfig: ServerConfig,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskRepository {

    private val schemesFlow = MutableStateFlow<List<Scheme>>(emptyList())

    // In-flight refresh sharing (Critic AC): concurrent refresh() callers share ONE
    // network fetch (launched in appScope so a caller cancelling doesn't kill the
    // shared work). The mutex only guards the start-or-join decision, never the
    // fetch, so it's never held across IO. A finished refresh clears the slot.
    private val inFlightMutex = Mutex()
    private var inFlight: Deferred<Result<Unit>>? = null

    override fun observeSchemes(): Flow<List<Scheme>> = schemesFlow.asStateFlow()

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
        val json = adapter.get(url(PATH_SCHEMES)).getOrThrow()
        val rows = gson.fromJson(json, SchemeEnvelopeDto::class.java)?.data.orEmpty()
        // Publish only after a successful parse — on any throw above the old
        // snapshot stays (failure).
        schemesFlow.value = rows.groupRowsIntoSchemes()
    }

    /**
     * Enable/disable a scheme. [schemeId] is the scheme name (sechename — v3's only
     * scheme identity). POSTs {sechename, state} where state = 0 to activate, 1 to
     * deactivate (v3 sign: 0 == running). On a successful reply (state 0 or 15) it
     * refreshes the SSOT so observeSchemes re-emits the server-confirmed state
     * (I-3 — no local flip).
     */
    override suspend fun setSchemeActive(schemeId: String, active: Boolean): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val state = if (active) STATE_ENABLE else STATE_DISABLE
                val builder = MyRequestBuilder(context).apply {
                    // Two-arg setUrl(url, _) sets the URL VERBATIM (no prepend). We
                    // build the absolute URL from ServerConfig ourselves — the
                    // single-arg setUrl(path) would prepend Constant.serveraddress AND
                    // can fall back to PreferencesUtil("serverAddress") (R-ADDR-SLOT).
                    // This keeps the base consistent with the GET path + repos dormant.
                    setUrl(url(PATH_ENABLE_DISABLE), "")
                    setBodyMap(hashMapOf("sechename" to schemeId, "state" to state.toString()))
                    setNeedToken(true)
                }
                val json = adapter.post(builder).getOrThrow()
                val replyState = gson.fromJson(json, TaskStateEnvelopeDto::class.java)
                    ?.data?.firstOrNull()?.state
                    ?: throw IOException("sechenableordisable reply had no state")
                if (replyState != STATE_CHANGE_SUCCESS && replyState != STATE_ALREADY_SAME) {
                    throw IOException("sechenableordisable failed (state=$replyState)")
                }
            }.mapCatching {
                // I-3: re-fetch so the SSOT reflects the server, not a guessed local
                // flip. A refresh failure after a successful toggle is surfaced so the
                // caller can retry the read; the toggle itself already succeeded.
                refresh().getOrThrow()
            }
        }

    /**
     * Execution log — one-shot (ICD-TaskRepository-v1 §13).
     *
     * documented-assumption (PINNED against real v3, PA-10): the v3 endpoint
     * inventory has NO dedicated execution-log REST endpoint (grep of Constant.java
     * finds only delete-task-record + a local on-device log DIR, no GET log/journal/
     * history). So there is no real source to wrap — this returns an empty list
     * (success), honestly reflecting "no server log feed". If a log endpoint is ever
     * found/added, wrap it here → ICD_UPDATE (the §13 signature stays). NOT faking
     * data from task rows.
     */
    override suspend fun getExecutionLog(): Result<List<TaskLog>> =
        Result.success(emptyList())

    /** Builds the absolute v3 URL: base (.../api, from ServerConfig) + path. */
    private fun url(path: String): String = serverConfig.baseUrl() + path

    private companion object {
        // Relative endpoint paths (mirror v3 Constant.getsecheList / postChangeTaskStatu).
        // Inlined so this class never loads v3 Constant (Android static init).
        // LIVE-verified: Constant.java:38 (/task/sechinfo), :61 (/task/sechenableordisable).
        const val PATH_SCHEMES = "/task/sechinfo"
        const val PATH_ENABLE_DISABLE = "/task/sechenableordisable"

        // Enable/disable state ints (v3 sign: 0 == running/on).
        const val STATE_ENABLE = 0
        const val STATE_DISABLE = 1

        // Reply states (v3 ErrorCode): both treated as success.
        const val STATE_CHANGE_SUCCESS = 0   // ErrorCode.ChangeSucess
        const val STATE_ALREADY_SAME = 15    // ErrorCode.TheStateIsSame (no-op, already in state)
    }
}
