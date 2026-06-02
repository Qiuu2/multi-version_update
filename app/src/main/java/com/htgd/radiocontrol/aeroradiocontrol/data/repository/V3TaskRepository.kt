package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import android.content.Context
import com.google.gson.Gson
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.SchemeEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.SchemeTaskEnvelopeDto
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
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
 * Plan A [TaskRepository] backed by the v3 stack (PA-10 + PA-15 fix).
 *
 * ★ PA-15 (2026-05-30) — TWO-endpoint composition:
 *   refresh() composes the SSOT from:
 *   1. GET  /task/sechinfo        — enumerate schemes (sechename + projectstate)
 *   2. POST /task/sechetaskinfo per scheme, body `{name: sechename}` — fetch each
 *      scheme's per-task timeline; issued in PARALLEL via appScope.async + awaitAll.
 *   3. Atomic publish: if ALL per-scheme task fetches succeed, publish the full
 *      snapshot. If ANY per-scheme fetch fails, the entire publish is skipped
 *      and the prior snapshot is retained (same SSOT contract as
 *      V3TerminalRepository — Critic AC).
 *
 * Why two endpoints: /sechinfo carries only scheme-summary fields (`taskcount`
 * as int, NOT a task array; per-task `starttime`/`medianame`/per-task `name`
 * are absent). The timeline data must come from /sechetaskinfo per scheme. PA-10
 * called only the summary and mis-rendered timeline as blank rows — PA-15 root
 * cause `.state/api-snapshots/task-schedule-rootcause.md`.
 *
 * v3 wire (pinned against CTO 2026-05-30 capture — admin Bearer token):
 *  - LIST    GET  /task/sechinfo (Constant.getsecheList) → 6 scheme summary rows.
 *  - PER-SCHEME  POST /task/sechetaskinfo (Constant.postSchemeInfo /
 *               postTaskListInfo), form `{name: sechename}` → N per-task rows
 *               (sechinfo.taskcount). Wire reconciles per CTO capture (海王
 *               taskcount=14, sechetaskinfo[海王] = 14 rows).
 *  - ENABLE  POST /task/sechenableordisable (Constant.postChangeTaskStatu), form
 *               `{sechename, state}`. 0=enable/running, 1=disable. Reply
 *               TaskStateRsp{data:[{state}]}: 0 ChangeSucess / 15 TheStateIsSame
 *               both OK, else failed (ErrorCode).
 *
 * MULTI-ACTIVE (PA-15 CTO capture): multiple schemes can have projectState=0
 * simultaneously (海王 and 日本 both active in the capture). The SSOT carries
 * all schemes with per-scheme `active` flag; TaskHomeViewModel picks the active
 * scheme (Option A — first-active wins; documented-assumption fe may upgrade).
 *
 * I-3 (single source): setSchemeActive does NOT locally flip the SSOT; on a
 * successful POST it triggers refresh() so the new projectstate comes from the
 * server and re-emits through observeSchemes. No divergent local write.
 *
 * R-ADDR-SLOT (dormant): base URL is [ServerConfig].baseUrl() (= Constant.server-
 * address). The POSTs use [MyRequestBuilder]'s TWO-arg setUrl(url, "") which
 * sets the URL verbatim, deliberately AVOIDING the single-arg setUrl(path)
 * which would prepend Constant.serveraddress AND can fall back to
 * PreferencesUtil("serverAddress") (R-ADDR-SLOT). No legacy Activity launched.
 *
 * SSOT contract (verbatim from V3TerminalRepository / Critic AC): observeSchemes
 * starts empty; refresh() publishes only on full success; on failure the previous
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

    // In-flight refresh sharing (Critic AC): concurrent refresh() callers share
    // ONE network fetch (launched in appScope so a caller cancelling doesn't
    // kill the shared work). The mutex only guards the start-or-join decision,
    // never the fetch itself, so it's never held across IO. A finished refresh
    // clears the slot.
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

    /**
     * Two-step fetch: enumerate schemes → fetch each scheme's tasks in parallel
     * → atomic publish on full success. Any per-scheme task-fetch failure
     * surfaces as [Result.failure] and the prior snapshot is preserved.
     */
    private suspend fun fetchAndPublish(): Result<Unit> = runCatching {
        // Step 1: enumerate schemes.
        val sechinfoJson = adapter.get(url(PATH_SCHEMES)).getOrThrow()
        val summaries = gson.fromJson(sechinfoJson, SchemeEnvelopeDto::class.java)
            ?.data?.mapNotNull { it.toSchemeSummary() }.orEmpty()

        // Step 2: per-scheme parallel POST to /sechetaskinfo. Issued under a
        // coroutineScope so a failure of any child cancels the rest cleanly +
        // surfaces the first exception; awaitAll preserves order.
        val schemes = coroutineScope {
            summaries.map { summary ->
                async(ioDispatcher) {
                    val tasksJson = postSchemeTasks(summary.name).getOrThrow()
                    val taskRows = gson.fromJson(tasksJson, SchemeTaskEnvelopeDto::class.java)
                        ?.data.orEmpty()
                    summary.copy(
                        tasks = taskRows.map { it.toSchemeTask(summary.active) },
                    )
                }
            }.awaitAll()
        }

        // Step 3: atomic publish — observers never see a partial snapshot. On
        // any throw above, control jumps to runCatching's failure branch and the
        // existing schemesFlow.value is retained.
        schemesFlow.value = schemes
    }

    /**
     * POSTs /task/sechetaskinfo body `{name: sechename}` and returns the raw
     * JSON body. Uses MyRequestBuilder's two-arg setUrl(url, "") to set the URL
     * verbatim — single-arg setUrl(path) would prepend Constant.serveraddress
     * AND can fall back to PreferencesUtil("serverAddress") (R-ADDR-SLOT
     * dormant).
     */
    private suspend fun postSchemeTasks(sechename: String): Result<String> {
        val builder = MyRequestBuilder(context).apply {
            setUrl(url(PATH_SCHEME_TASKS), "")
            setBodyMap(hashMapOf("name" to sechename))
            setNeedToken(true)
        }
        return adapter.post(builder)
    }

    /**
     * Enable/disable a scheme. [schemeId] is the scheme name (sechename — v3's
     * only scheme identity). POSTs {sechename, state} where state = 0 to
     * activate, 1 to deactivate (v3 sign: 0 == running). On a successful reply
     * (state 0 or 15) it refreshes the SSOT so observeSchemes re-emits the
     * server-confirmed state (I-3 — no local flip).
     */
    override suspend fun setSchemeActive(schemeId: String, active: Boolean): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val state = if (active) STATE_ENABLE else STATE_DISABLE
                val builder = MyRequestBuilder(context).apply {
                    // Two-arg setUrl(url, _) sets the URL VERBATIM (no prepend);
                    // single-arg setUrl(path) would prepend Constant.serveraddress
                    // AND can fall back to PreferencesUtil("serverAddress")
                    // (R-ADDR-SLOT). Keeps base consistent with the GET path.
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
                // I-3: re-fetch so the SSOT reflects the server, not a guessed
                // local flip. A refresh failure after a successful toggle is
                // surfaced; the toggle itself already succeeded.
                refresh().getOrThrow()
            }
        }

    /**
     * Execution log — one-shot (ICD-TaskRepository-v1 §13).
     *
     * documented-assumption (PINNED, PA-10): the v3 endpoint inventory has NO
     * dedicated execution-log REST endpoint (grep of Constant.java + swagger
     * sweep PA-15 confirms — only delete-task-record + a local on-device log
     * DIR, no GET log/journal/history). Returns empty (success), honestly
     * reflecting "no server log feed". If a log endpoint is ever found/added,
     * wrap it here → ICD_UPDATE (the §13 signature stays). NOT faking data.
     */
    override suspend fun getExecutionLog(): Result<List<TaskLog>> =
        Result.success(emptyList())

    /** Builds the absolute v3 URL: base (.../api, from ServerConfig) + path. */
    private fun url(path: String): String = serverConfig.baseUrl() + path

    private companion object {
        // Relative endpoint paths (mirror v3 Constant). Inlined so this class
        // never loads v3 Constant (Android static init). LIVE-verified:
        //   Constant.java:38 = "/task/sechinfo"
        //   Constant.java:59 = "/task/sechetaskinfo"
        //   Constant.java:61 = "/task/sechenableordisable"
        const val PATH_SCHEMES = "/task/sechinfo"
        const val PATH_SCHEME_TASKS = "/task/sechetaskinfo"
        const val PATH_ENABLE_DISABLE = "/task/sechenableordisable"

        // Enable/disable state ints (v3 sign: 0 == running/on).
        const val STATE_ENABLE = 0
        const val STATE_DISABLE = 1

        // Reply states (v3 ErrorCode): both treated as success.
        const val STATE_CHANGE_SUCCESS = 0   // ErrorCode.ChangeSucess
        const val STATE_ALREADY_SAME = 15    // ErrorCode.TheStateIsSame (no-op, already in state)
    }
}
