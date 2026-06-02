package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.service

import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerHealth
import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerState

/**
 * Domain → UI mapping for the Service Tab, done at the ViewModel boundary
 * (TASK-PA Service). Mirrors TerminalStatusMapper / TaskUiMappers: the screen sees
 * only [ServiceUi]/[ServiceHealthUi]/[ServiceMetric]; the data layer's
 * [ServerState]/[ServerHealth] never reach a Composable. Health maps with an
 * Unknown-tolerant `when` (R-003): any health the mapper can't classify falls back
 * to [ServiceHealthUi.Unknown] (non-crashing).
 */

/** Blank-tolerant placeholder for an absent metric value. */
private const val NA = "--"

/** [ServerHealth] → the UI health enum. Unknown-tolerant. */
fun ServerHealth.toHealthUi(): ServiceHealthUi = when (this) {
    ServerHealth.Online -> ServiceHealthUi.Online
    ServerHealth.Offline -> ServiceHealthUi.Offline
    is ServerHealth.Unknown -> ServiceHealthUi.Unknown // R-003: never crash on a new value
}

fun ServerState.toServiceUi(): ServiceUi = ServiceUi(
    health = health.toHealthUi(),
    name = name?.takeIf { it.isNotBlank() } ?: "广播服务器",
    metrics = listOf(
        ServiceMetric("连接数", connectionLoad()),
        ServiceMetric("任务数", taskCount?.toString() ?: NA),
        ServiceMetric("带宽", bandwidth?.let { "$it Mbps" } ?: NA),
        ServiceMetric("IP 地址", ip?.takeIf { it.isNotBlank() } ?: NA),
        ServiceMetric("网关", gate?.takeIf { it.isNotBlank() } ?: NA),
        ServiceMetric("控制端口", ctrlPort?.toString() ?: NA),
        ServiceMetric("数据端口", dataPort?.toString() ?: NA),
    ),
)

/** "12 / 5000" when both present; one side alone still renders; "--" when neither. */
private fun ServerState.connectionLoad(): String = when {
    connection != null && maxConnection != null -> "$connection / $maxConnection"
    connection != null -> connection.toString()
    maxConnection != null -> "— / $maxConnection"
    else -> NA
}
