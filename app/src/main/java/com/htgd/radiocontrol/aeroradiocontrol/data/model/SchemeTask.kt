package com.htgd.radiocontrol.aeroradiocontrol.data.model

/**
 * Domain model for one task within a [Scheme] (ICD-TaskRepository-v1).
 *
 * Repository OUTPUT type. Fields are the subset the v4 Task Tab renders; the wire
 * source is the LIVE reverse-engineered SchemeDto / TaskDto (v3 TaskZuoxiModel /
 * TaskGuangboModel). The UI maps [status] (a domain type) at the ViewModel
 * boundary; it never sees the raw v3 ints.
 */
data class SchemeTask(
    val id: String,
    val name: String,
    val status: SchemeTaskStatus,
    val startTime: String? = null,
    val mediaName: String? = null,
    val volume: Int? = null,
)

/**
 * Domain task status — an **Unknown-tolerant sealed type** (same pattern as
 * [TerminalStatus], ESC-WATCH-2).
 *
 * The real value set / derivation from the v3 ints (taskstate / projectstate) is
 * NOT yet confirmed — under Plan A it is pinned by "v3 适配实测验证" (running real
 * v3 JSON), not by a backend contract. The known cases below are a conservative
 * placeholder; any value the mapper can't classify becomes [Unknown] so the UI
 * degrades gracefully (R-003) and fe's `when` stays exhaustive (must keep an
 * Unknown branch). When the derivation is confirmed, only the mapper changes —
 * the type and fe's boundary map stay.
 */
sealed interface SchemeTaskStatus {
    /** 未运行（空闲）. */
    data object Idle : SchemeTaskStatus
    /** 运行中. */
    data object Running : SchemeTaskStatus
    /** 已停用. */
    data object Disabled : SchemeTaskStatus

    /** Unrecognised status — R-003 fallback; [raw] is the source token. */
    data class Unknown(val raw: String) : SchemeTaskStatus
}
