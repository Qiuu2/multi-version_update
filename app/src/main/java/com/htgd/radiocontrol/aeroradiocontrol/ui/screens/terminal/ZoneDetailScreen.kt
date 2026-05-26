package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.BackTopBar
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.EmptyState
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationBanner
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationType
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TerminalHubSkeleton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TerminalTile
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

@Composable
fun ZoneDetailScreen(
    zoneId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val zone = TerminalMock.zone(zoneId)
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(zoneId) {
        loading = true
        kotlinx.coroutines.delay(1400)
        loading = false
    }

    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        BackTopBar(title = zone?.name ?: "分区", onBack = onBack)
        when {
            loading -> TerminalHubSkeleton()
            zone == null -> EmptyState(
                icon = Icons.Filled.Warning,
                title = "未找到该分区",
                description = "该分区可能已被删除。",
            )
            zone.terminals.isEmpty() -> EmptyState(
                icon = Icons.Filled.Speaker,
                title = "该分区暂无终端",
                description = "为该分区添加终端后会显示在这里。",
            )
            else -> {
                val terminals = zone.terminals.sortedByDescending { it.status == TerminalStatus.Fault }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(spacing.pageH),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    item(key = "summary") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surface, AeroTheme.shapes.rCard)
                                .padding(spacing.cardPad),
                            verticalArrangement = Arrangement.spacedBy(spacing.xs),
                        ) {
                            Text(zone.name, style = AeroTheme.typography.sectionTitle, color = colors.ink)
                            Text(
                                "在线 ${zone.onlineCount}/${zone.terminals.size}",
                                style = AeroTheme.typography.bodySmall,
                                color = colors.ink3,
                            )
                        }
                    }
                    if (zone.faultCount > 0) {
                        item(key = "fault") {
                            NotificationBanner(type = NotificationType.Error, message = "本分区 ${zone.faultCount} 个终端故障")
                        }
                    }
                    items(terminals.chunked(2), key = { it.first().id }) { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.tileGap)) {
                            rowItems.forEach { t -> TerminalTile(name = t.name, status = t.status) }
                        }
                    }
                }
            }
        }
    }
}
