package com.htgd.radiocontrol.aeroradiocontrol.data.model

/**
 * Domain model for a terminal (broadcast endpoint device), as the UI consumes it.
 *
 * Repository OUTPUT type — distinct from the wire `TerminalDto` (which mirrors
 * the server's raw fields). The UI maps [status] (a domain type) to its own
 * presentation enum at the ViewModel boundary; it never sees the wire ints.
 */
data class Terminal(
    val id: String,
    val name: String,
    val zoneId: String,
    val status: TerminalStatus,
    val volume: Int? = null,
    val longitude: String? = null,
    val latitude: String? = null,
)

/**
 * Domain terminal status — an **Unknown-tolerant sealed type** (ESC-WATCH-2).
 *
 * Why sealed-with-Unknown (not a plain enum):
 *   The real status value set is NOT yet agreed (OPEN INQ-O-1 D-3 + O-2):
 *   Handoff lists online/offline/fault/playing/paging, the early ICD-TerminalDto
 *   draft listed online/offline/paging/talking/casting/urgent/alarm, and the
 *   legacy wire has four separate int fields entirely. Until the vendor confirms,
 *   any value the mapper can't classify MUST NOT crash the UI — [Unknown]
 *   carries the raw token so it degrades gracefully (and is debuggable) rather
 *   than throwing or silently dropping the terminal (R-003).
 *
 * The known cases mirror the fe UI's status set (StatusPill.TerminalStatus =
 * Online/Offline/Fault/Playing/Paging) so the ViewModel's domain→UI map is 1:1
 * for known values; [Unknown] maps to a safe UI default (fe's call). When O-1
 * D-3/O-2 return, the mapper changes and any new known case is added here →
 * ICD-TerminalDto ICD_UPDATE; the sealed shape means fe's `when` stays
 * exhaustive (it must keep an else/Unknown branch).
 */
sealed interface TerminalStatus {
    /** 在线（空闲、可用）. */
    data object Online : TerminalStatus
    /** 离线（不可达）. */
    data object Offline : TerminalStatus
    /** 故障. */
    data object Fault : TerminalStatus
    /** 播放中（点播/任务播放）. */
    data object Playing : TerminalStatus
    /** 寻呼中. */
    data object Paging : TerminalStatus

    /**
     * Unrecognised status — R-003 fallback. [raw] is the source token (e.g. the
     * derived label or a stringified wire value) for logging/diagnostics. Real
     * mapping pending OPEN(INQ-O-1 D-3 + O-2).
     */
    data class Unknown(val raw: String) : TerminalStatus
}
