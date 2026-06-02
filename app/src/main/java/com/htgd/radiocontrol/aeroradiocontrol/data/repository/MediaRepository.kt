package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.model.Media
import com.htgd.radiocontrol.aeroradiocontrol.data.model.MediaFolder
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the 点播 (cast) media library — LIST/selection only
 * (ICD-MediaRepository-v1, GET /terminal/mediainfo + /terminal/mediafolderinfo).
 *
 * This INTERFACE is the contract fe-business's broadcast Tab media picker depends
 * on. Scope is LIST only: list folders (each with nested media) + the flat media
 * list + refresh. The CAST action (push chosen media to terminals) is NOT here —
 * v3 fires it through the HTIntf AAR control plane, so it is a SEPARATE cast-seam
 * owned by legacy-native (PA-07 decision A). fe composes: this repo (selection) +
 * legacy cast-seam (action) + BroadcastTargetsViewModel (targets).
 *
 *   observeFolders(): Flow<List<MediaFolder>> — each MediaFolder carries its
 *                                               nested media (consistent with
 *                                               Zone nesting terminals).
 *   observeMedia(): Flow<List<Media>>         — flat media list (same SSOT).
 *   suspend refresh(): Result<Unit>           — fetch into the SSOT.
 *
 * SSOT contract (identical to [TerminalRepository]): observe* emits the current
 * snapshot starting at emptyList(); refresh() publishes only on full success; on
 * failure the previous snapshot is retained (caller drives retry). In-memory SSOT
 * (cleared on process death); a Room cache could replace the backing Flow later
 * without changing this surface.
 */
interface MediaRepository {

    /** Observe all media folders (each with its nested media). Emits the current
     *  snapshot immediately, then re-emits after each successful refresh. */
    fun observeFolders(): Flow<List<MediaFolder>>

    /** Observe the flat media list (for a screen that wants files, not grouped by
     *  folder). Same SSOT as observeFolders. */
    fun observeMedia(): Flow<List<Media>>

    /** Fetch folders+media from the v3 stack into the SSOT. Failure = network/
     *  parse error; the previous snapshot stays until a refresh succeeds. */
    suspend fun refresh(): Result<Unit>
}
