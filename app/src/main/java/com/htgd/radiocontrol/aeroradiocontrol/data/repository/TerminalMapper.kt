package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.dto.TerminalDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.ZoneDto
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Terminal
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone

/**
 * DTO -> domain mappers for the terminal domain.
 *
 * Kept separate from the Repository so the wire->UI translation (especially the
 * status derivation) is unit-testable in isolation. A DTO missing its id is
 * dropped (a terminal with no identity is unusable) — see [toTerminalOrNull].
 *
 * ★ PA-14 (2026-05-30) — Zone membership comes from the NESTED [ZoneDto.terminal]
 * array on /terminal/terzone, NOT from grouping by the `terminal.zone` wire field
 * (that field's semantics are unknown — see TerminalDto KDoc). The Mapper now
 * mirrors that: [toZoneOrNull] reads its own nested terminals and threads the
 * containing zone's id into each [Terminal.zoneId]. The Repository no longer
 * joins client-side.
 */

/**
 * Maps a [TerminalDto] to a [Terminal], or null if it has no usable id.
 *
 * [containingZoneId] is the id of the [ZoneDto] this terminal was nested under
 * on the wire. It becomes the domain [Terminal.zoneId] — the AUTHORITATIVE
 * membership (PA-14). The wire `TerminalDto.zone` field is intentionally NOT
 * used here; see TerminalDto KDoc.
 *
 * Dropping id-less rows (rather than substituting a fake id) keeps the UI from
 * rendering an unaddressable terminal; the Repository filters nulls out.
 */
fun TerminalDto.toTerminalOrNull(containingZoneId: String): Terminal? {
    val realId = id ?: return null
    return Terminal(
        id = realId.toString(),
        name = name.orEmpty(),
        zoneId = containingZoneId,
        status = deriveStatus(),
        volume = volume,
        longitude = longitude,
        latitude = latitude,
    )
}

/**
 * Derives the domain [TerminalStatus] from the legacy granular status ints.
 *
 * documented-assumption — the legacy server sends four separate ints
 * (taskstate/devicestate/netstate/speechstate) and there is no confirmed mapping
 * to a single status. This is a PLACEHOLDER ordering chosen to be safe + legible:
 *   - no status fields present at all -> Unknown("no-status")  (R-003)
 *   - netState == 0 (or null)         -> Offline   (no network)
 *   - isUrgent == 1 / speechState != 0 -> Paging    (寻呼/语音通道占用)
 *   - taskState  != 0                 -> Playing   (任务/点播播放中)
 *   - otherwise                       -> Online
 * "Fault" is NOT derivable from the known ints yet. Unknown is the unclassified
 * fallback. When the real mapping is confirmed, only this function changes — the
 * domain type + fe's boundary map stay.
 */
fun TerminalDto.deriveStatus(): TerminalStatus {
    val hasAnyStatus = listOf(taskState, deviceState, netState, speechState)
        .any { it != null }
    if (!hasAnyStatus) return TerminalStatus.Unknown("no-status")

    return when {
        (netState ?: 0) == 0 -> TerminalStatus.Offline
        isUrgent == 1 || (speechState ?: 0) != 0 -> TerminalStatus.Paging
        (taskState ?: 0) != 0 -> TerminalStatus.Playing
        else -> TerminalStatus.Online
    }
}

/**
 * Maps a [ZoneDto] to a [Zone] WITH its nested terminals attached (PA-14).
 *
 * Reads [ZoneDto.terminal] directly — the authoritative membership source — and
 * threads this zone's id into each mapped [Terminal.zoneId]. Returns null if the
 * zone has no usable id (unaddressable). A zone with a null/empty terminal list
 * maps to a Zone with an empty terminals list (a valid empty zone, e.g. 赣州角
 * in the CTO capture).
 */
fun ZoneDto.toZoneOrNull(): Zone? {
    val realId = id ?: return null
    val zoneIdStr = realId.toString()
    val terminals = terminal.orEmpty().mapNotNull { it.toTerminalOrNull(zoneIdStr) }
    return Zone(
        id = zoneIdStr,
        name = name.orEmpty(),
        description = description,
        terminals = terminals,
    )
}
