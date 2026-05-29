package com.htgd.radiocontrol.aeroradiocontrol.data.model

/**
 * Domain model for the server's health snapshot, as the Service Tab (系统健康度)
 * consumes it (ICD-ServerStateRepository-v1).
 *
 * Repository OUTPUT type — distinct from the wire `ServerStateDto` (which mirrors
 * the server's raw fields). [health] is a domain type the UI maps at the
 * ViewModel boundary; the numeric/string fields below are rendered directly.
 * [connection] / [maxConnection] together give the connection-load gauge;
 * [ctrlPort] / [dataPort] are the SDK control/data ports (v3 reads ctrlport for
 * SDK_SERVER_NOMBER).
 *
 * Contract source: the LIVE reverse-engineered ServerStateDto (v3 `SeverStateModel`).
 */
data class ServerState(
    val health: ServerHealth,
    val name: String? = null,
    val ip: String? = null,
    val gate: String? = null,
    val connection: Int? = null,
    val maxConnection: Long? = null,
    val taskCount: Int? = null,
    val bandwidth: Int? = null,
    val ctrlPort: Int? = null,
    val dataPort: Int? = null,
)

/**
 * Domain server health — an **Unknown-tolerant sealed type** (same pattern as
 * [TerminalStatus], ESC-WATCH-2).
 *
 * The wire `state` is a single int whose value set is not separately documented
 * by the vendor; under Plan A it is pinned by "v3 适配实测验证" (real v3 JSON).
 * The known cases below are a conservative reading (0/absent = a server that
 * answered but reports nothing healthy → treat as Offline-ish; non-zero = Online,
 * i.e. the SDK considers it serving). Anything the mapper can't classify becomes
 * [Unknown] carrying the raw token, so the UI degrades gracefully (R-003) and
 * fe's `when` stays exhaustive (must keep an Unknown branch). When the real value
 * set is confirmed, only the mapper changes — this type and fe's boundary map stay.
 */
sealed interface ServerHealth {
    /** 在线（服务正常）. */
    data object Online : ServerHealth
    /** 离线 / 未就绪（state 为 0 或缺失）. */
    data object Offline : ServerHealth

    /** Unrecognised state — R-003 fallback; [raw] is the source token. */
    data class Unknown(val raw: String) : ServerHealth
}
