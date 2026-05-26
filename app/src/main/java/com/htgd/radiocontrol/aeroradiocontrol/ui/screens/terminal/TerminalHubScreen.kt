package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButtonVariant
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MChip
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.EmptyState
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.FabBar
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationBanner
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationType
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TerminalHubSkeleton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TerminalTile
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/** Terminal tab root: swaps between the hub and a zone-detail page via local state. */
@Composable
fun TerminalTab(modifier: Modifier = Modifier) {
    var openZoneId by remember { mutableStateOf<String?>(null) }
    val zoneId = openZoneId
    if (zoneId != null) {
        ZoneDetailScreen(zoneId = zoneId, onBack = { openZoneId = null }, modifier = modifier)
    } else {
        TerminalHubScreen(onOpenZone = { openZoneId = it }, modifier = modifier)
    }
}

private data class StatusFilter(val label: String, val status: TerminalStatus?)

private val filters = listOf(
    StatusFilter("全部", null),
    StatusFilter("在线", TerminalStatus.Online),
    StatusFilter("离线", TerminalStatus.Offline),
    StatusFilter("故障", TerminalStatus.Fault),
)

@Composable
fun TerminalHubScreen(
    onOpenZone: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val zones = TerminalMock.zones
    val faultCount = remember { TerminalMock.faultCount() }

    var filter by remember { mutableStateOf<TerminalStatus?>(null) }
    var expandedZones by remember { mutableStateOf(setOf<String>()) }
    var bulkMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1600)
        loading = false
    }

    fun matches(t: TerminalUi) = filter == null || t.status == filter
    val anyVisible = zones.any { z -> z.terminals.any { matches(it) } }

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        if (loading) {
            TerminalHubSkeleton()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(spacing.pageH),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                item(key = "controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        ) {
                            filters.forEach { f ->
                                MChip(label = f.label, active = filter == f.status, onClick = { filter = f.status })
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (bulkMode) "已选 ${selectedIds.size} 台" else "长按或点「多选」批量操作",
                                style = AeroTheme.typography.bodySmall,
                                color = colors.ink3,
                                modifier = Modifier.weight(1f),
                            )
                            MButton(
                                text = if (bulkMode) "完成" else "多选",
                                variant = MButtonVariant.Text,
                                onClick = {
                                    bulkMode = !bulkMode
                                    if (!bulkMode) selectedIds = emptySet()
                                },
                            )
                        }
                    }
                }

                if (faultCount > 0) {
                    item(key = "fault") {
                        NotificationBanner(type = NotificationType.Error, message = "$faultCount 个终端故障，请尽快检查")
                    }
                }

                if (!anyVisible) {
                    item(key = "empty") {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = "没有匹配的终端",
                            description = "当前筛选条件下没有终端，试试切换筛选。",
                            actionLabel = "清除筛选",
                            onAction = { filter = null },
                        )
                    }
                }

                zones.forEach { zone ->
                    val visible = zone.terminals
                        .filter { matches(it) }
                        .sortedByDescending { it.status == TerminalStatus.Fault }
                    if (visible.isEmpty()) return@forEach
                    val expanded = zone.id in expandedZones
                    item(key = "zone-${zone.id}") {
                        ZoneHeader(
                            name = zone.name,
                            online = zone.onlineCount,
                            total = zone.terminals.size,
                            fault = zone.faultCount,
                            expanded = expanded,
                            onToggle = {
                                expandedZones = if (expanded) expandedZones - zone.id else expandedZones + zone.id
                            },
                            onOpenDetail = { onOpenZone(zone.id) },
                        )
                    }
                    if (expanded) {
                        items(visible.chunked(2), key = { it.first().id }) { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tileGap)) {
                                rowItems.forEach { t ->
                                    TerminalTile(
                                        name = t.name,
                                        status = t.status,
                                        selected = t.id in selectedIds,
                                        onClick = {
                                            if (bulkMode) {
                                                selectedIds = if (t.id in selectedIds) selectedIds - t.id else selectedIds + t.id
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FabBar(
            visible = bulkMode && selectedIds.isNotEmpty(),
            counterLabel = "已选 ${selectedIds.size} 台",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = spacing.lg),
            actions = {
                MButton(text = "寻呼", variant = MButtonVariant.Filled, onClick = {})
                MButton(text = "停止", variant = MButtonVariant.Danger, onClick = {})
            },
        )
    }
}

@Composable
private fun ZoneHeader(
    name: String,
    online: Int,
    total: Int,
    fault: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, AeroTheme.shapes.rCard)
            .clickable(onClick = onToggle)
            .padding(horizontal = spacing.pageH, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = colors.ink3,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = AeroTheme.typography.bodyLarge, color = colors.ink)
            Text(
                buildString {
                    append("在线 $online/$total")
                    if (fault > 0) append(" · 故障 $fault")
                },
                style = AeroTheme.typography.bodySmall,
                color = if (fault > 0) colors.statusFault else colors.ink3,
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = "查看分区",
            tint = colors.ink3,
            modifier = Modifier.clickable(onClick = onOpenDetail).padding(4.dp),
        )
    }
}
