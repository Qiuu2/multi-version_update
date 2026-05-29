package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.google.gson.Gson
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.MediaEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.MediaFolderEnvelopeDto
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Media
import com.htgd.radiocontrol.aeroradiocontrol.data.model.MediaFolder
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
 * Plan A [MediaRepository] backed by the v3 stack (TASK-PA-07, LIST half only).
 *
 * REAL impl mirroring [V3TerminalRepository] exactly. Data flows:
 * [V3CallbackAdapter] (v3 RequestManger → raw JSON) → Gson parse into the
 * reverse-engineered [MediaFolderEnvelopeDto] / [MediaEnvelopeDto] → existing
 * [toMediaFolderOrNull] / [toMediaOrNull] Mapper → domain → in-memory SSOT → Flow.
 * Folders nest media by folderId (like zones nest terminals).
 *
 * Endpoints (v3 ActivityMusicOrder lists via plain RequestManger.get):
 *  - GET /terminal/mediafolderinfo (Constant.getFolderInfo) — folder tree.
 *  - GET /terminal/mediainfo       (Constant.getAllMusicInfo) — all media files.
 * We use the all-media endpoint (not the per-folder /mediainfo/{id} recursion v3
 * does for its tree-view) and group client-side by folderId — one GET pair, the
 * same two-fetch+join shape as V3TerminalRepository.
 *
 * R-ADDR-SLOT (dormant): the URL base is [ServerConfig].baseUrl() (=
 * Constant.serveraddress, the base login set), like all Plan A repos — NOT v3's
 * separate `PreferencesUtil("serverAddress")` slot that ActivityMusicOrder reads.
 * No legacy media Activity is launched.
 *
 * SSOT contract (verbatim from V3TerminalRepository / Critic AC): observe* starts
 * empty; refresh() publishes only on full success; on failure the previous
 * snapshot is retained; concurrent refresh() shares ONE in-flight fetch.
 *
 * The 点播 CAST action is NOT here — it's the HTIntf AAR cast-seam (legacy-native).
 */
@Singleton
class V3MediaRepository @Inject constructor(
    private val adapter: V3CallbackAdapter,
    private val gson: Gson,
    private val serverConfig: ServerConfig,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MediaRepository {

    private val foldersFlow = MutableStateFlow<List<MediaFolder>>(emptyList())

    // In-flight refresh sharing (Critic AC): concurrent refresh() callers share
    // ONE network fetch (launched in appScope so a caller cancelling doesn't kill
    // the shared work). The mutex only guards the start-or-join decision, never the
    // fetch, so it's never held across IO. A finished refresh clears the slot so the
    // next one re-fetches (no permanent stale).
    private val inFlightMutex = Mutex()
    private var inFlight: Deferred<Result<Unit>>? = null

    override fun observeFolders(): Flow<List<MediaFolder>> = foldersFlow.asStateFlow()

    override fun observeMedia(): Flow<List<Media>> =
        foldersFlow.map { folders -> folders.flatMap { it.media } }

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
        val foldersJson = adapter.get(url(PATH_FOLDERS)).getOrThrow()
        val mediaJson = adapter.get(url(PATH_MEDIA)).getOrThrow()

        val media = gson.fromJson(mediaJson, MediaEnvelopeDto::class.java)
            ?.data?.mapNotNull { it.toMediaOrNull() }.orEmpty()
        val byFolder = media.groupBy { it.folderId }

        val folders = gson.fromJson(foldersJson, MediaFolderEnvelopeDto::class.java)
            ?.data?.mapNotNull { it.toMediaFolderOrNull() }
            ?.map { folder -> folder.copy(media = byFolder[folder.id].orEmpty()) }
            .orEmpty()

        // Publish only after both fetches parse — observers never see a half-
        // updated snapshot; on any throw above the old snapshot stays (failure).
        foldersFlow.value = folders
    }

    /** Builds the absolute v3 URL: base (.../api, from ServerConfig) + path. */
    private fun url(path: String): String = serverConfig.baseUrl() + path

    private companion object {
        // Relative endpoint paths (mirror v3 Constant.getFolderInfo / getAllMusicInfo).
        // Inlined here so this class never loads v3 Constant (whose static init
        // touches Android → unit tests can't load it). LIVE-verified against
        // Constant.java:77 (/terminal/mediafolderinfo) and :75 (/terminal/mediainfo).
        const val PATH_FOLDERS = "/terminal/mediafolderinfo"
        const val PATH_MEDIA = "/terminal/mediainfo"
    }
}
