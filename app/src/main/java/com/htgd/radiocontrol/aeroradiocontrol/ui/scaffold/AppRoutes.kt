package com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SupportAgent
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TabBarItem

/** Top-level navigation destinations for the v4 UI. */
object AppRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val MAIN = "main"

    const val ZONE_ARG = "zoneId"
    const val ZONE_DETAIL = "zone/{$ZONE_ARG}"
    fun zoneDetail(id: String) = "zone/$id"

    const val SCHEME_ARG = "schemeId"
    const val SCHEME_DETAIL = "scheme/{$SCHEME_ARG}"
    fun schemeDetail(id: String) = "scheme/$id"
    const val SCHEME_EDIT = "scheme/{$SCHEME_ARG}/edit"
    fun schemeEdit(id: String) = "scheme/$id/edit"

    const val EXEC_LOG = "exec_log"
    const val TEMP_BROADCAST = "temp_broadcast"
}

/**
 * The 5-tab terminal-centric IA:
 * 终端(找) -> 广播(做) -> AI(自动) -> 任务(自动) -> 服务(兜底).
 * AI sits at the centre and is rendered raised by [TabBarV4].
 */
val mainTabs: List<TabBarItem> = listOf(
    TabBarItem("终端", Icons.Filled.GridView),
    TabBarItem("广播", Icons.Filled.Campaign),
    TabBarItem("AI", Icons.Filled.AutoAwesome),
    TabBarItem("任务", Icons.Filled.Checklist),
    TabBarItem("服务", Icons.Filled.SupportAgent),
)
