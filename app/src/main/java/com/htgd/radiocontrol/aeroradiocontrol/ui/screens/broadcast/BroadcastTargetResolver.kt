package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import kotlinx.coroutines.flow.first

/**
 * Resolves the broadcast Tab's ZONE selection → the TERMINAL ids the AAR adapters
 * require (broadcast SD1/SD2; PM-approved Option A).
 *
 * Why this exists: the shared target picker (BroadcastTargetsViewModel) selects
 * ZONES (how users pick), but the AAR control surface is inherently per-terminal —
 * OnDemandCastAdapter.castMedia / VoiceTalkAdapter.startTalk|startPaging all take
 * `List<Int>` terminal ids (hardware reality, NOT a contract to change). So every
 * broadcast action expands the selected zones to their terminals at the ViewModel
 * boundary, reading the same [TerminalRepository] SSOT BroadcastTargetsViewModel
 * already consumes. Lifted out of CastViewModel so 点播 and 寻呼/对讲 share ONE
 * resolver (no divergence).
 *
 * documented-assumption: v3 terminal ids are numeric (the wire `id` is an int), so
 * [String.toIntOrNull] parses them; the defensive drop of any non-numeric id is
 * belt-and-suspenders (R-003 — a malformed id is skipped, never crashes the cast).
 * Result is [distinct]ed so overlapping selections (a zone + a terminal already in
 * it, or two zones sharing a terminal) collapse to one id.
 */
class BroadcastTargetResolver(
    private val terminalRepository: TerminalRepository,
) {
    /** Snapshot the SSOT once, expand [selectedZoneIds] → distinct numeric terminal ids. */
    suspend fun resolveTerminalIds(selectedZoneIds: Set<String>): List<Int> {
        if (selectedZoneIds.isEmpty()) return emptyList()
        val zones = terminalRepository.observeZones().first()
        return zones
            .filter { it.id in selectedZoneIds }
            .flatMap { it.terminals }
            .mapNotNull { it.id.toIntOrNull() }
            .distinct()
    }
}
