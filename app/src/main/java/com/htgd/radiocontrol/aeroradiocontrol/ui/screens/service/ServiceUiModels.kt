package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.service

import androidx.compose.runtime.Immutable

/**
 * Presentation view types for the Service Tab (系统健康度) — TASK-PA Service.
 *
 * The ViewModel maps the data layer's [com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerState]
 * into these UI types at its boundary (see ServiceUiMapper.kt), so the screen never
 * touches domain/DTO types. Mirrors the terminal/task Tab UiModels split.
 */

/** Server health as the screen renders it — Unknown-tolerant (domain
 *  ServerHealth.Unknown maps here, never crashes the `when`). */
enum class ServiceHealthUi { Online, Offline, Unknown }

/** One labelled health metric row (e.g. "连接数 12 / 5000"). [value] is preformatted
 *  by the ViewModel boundary; "--" is the blank-tolerant placeholder. */
@Immutable
data class ServiceMetric(
    val label: String,
    val value: String,
)

/** The whole server-health card the Service Tab shows. */
@Immutable
data class ServiceUi(
    val health: ServiceHealthUi,
    /** Display name for the server, or a fallback label when absent. */
    val name: String,
    val metrics: List<ServiceMetric>,
)
