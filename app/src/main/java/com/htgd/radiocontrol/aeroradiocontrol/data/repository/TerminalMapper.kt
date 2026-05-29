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
 * OPEN status derivation) is unit-testable in isolation and swappable when O-1
 * returns. A DTO missing its id is dropped (a terminal with no identity is
 * unusable) -- see [toTerminalOrNull].
 */

/**
 * Maps a [TerminalDto] to a [Terminal], or null if it has no usable id.
 *
 * Dropping id-less rows (rather than substituting a fake id) keeps the UI from
 * rendering an unaddressable terminal; the Repository filters nulls out.
 */
fun TerminalDto.toTerminalOrNull(): Terminal? {
    val realId = id ?: return null
    return Terminal(
        id = realId.toString(),
        name = name.orEmpty(),
        zoneId = zone?.toString().orEmpty(),
        status = deriveStatus(),
        volume = volume,
        longitude = longitude,
        latitude = latitude,
    )
}

/**
 * Derives the domain [TerminalStatus] from the legacy granular status ints.
 *
 * documented-assumption -- hard-blocked-by OPEN(INQ-O-1 D-3 + O-2).
 *   The legacy server sends four separate ints (taskstate/devicestate/netstate/
 *   speechstate) and there is NO confirmed mapping to a single status, nor
 *   confirmation the REST literals match the WS push literals (D-3). This is a
 *   PLACEHOLDER ordering chosen to be safe and legible:
 *     - no status fields present at all -> Unknown("no-status")  (R-003)
 *     - netState == 0 (or null)         -> Offline   (no network)
 *     - isUrgent == 1 / speechState != 0 -> Paging    (寻呼/语音通道占用)
 *     - taskState  != 0                 -> Playing   (任务/点播播放中)
 *     - otherwise                       -> Online
 *   "Fault" is NOT derivable from the known ints yet (no confirmed fault field)
 *   and the v4 set's talking/casting/urgent/alarm don't map cleanly here, so
 *   anything ambiguous resolves to the closest safe known case above; truly
 *   unclassifiable input -> [TerminalStatus.Unknown]. TODO(INQ-O-1 D-3/O-2):
 *   replace this whole function with the confirmed mapping; the domain type +
 *   fe's boundary map stay (only this derivation changes).
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

/** Maps a [ZoneDto] to a [Zone] (without terminals), or null if no usable id.
 *  The Repository attaches each zone's terminals after mapping. */
fun ZoneDto.toZoneOrNull(): Zone? {
    val realId = id ?: return null
    return Zone(
        id = realId.toString(),
        name = name.orEmpty(),
        description = description,
        terminals = emptyList(),
    )
}
