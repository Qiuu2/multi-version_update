package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MChip
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.FabAction
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.FabBar
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TerminalTile
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

private data class StatusFilter(val label: String, val status: TerminalStatus?)

private val filters = listOf(
    StatusFilter("全部", null),
    StatusFilter("在线", TerminalStatus.Online),
    StatusFilter("离线", TerminalStatus.Offline),
    StatusFilter("故障", TerminalStatus.Fault),
)

/**
 * Terminal hub. One screen, three variants driven by state:
 *  - default: zones collapsed
 *  - expanded: a zone open, showing its terminal tiles
 *  - bulk: multi-select with a contextual bar + [FabBar] of bulk actions
 * Fault terminals are pinned (red banner + fault-first ordering).
 */
@Composable
fun TerminalHubScreen(
    onOpenZone: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AeroTheme.spacing
    val zones = TerminalMock.zones
    val faults = remember { TerminalMock.faultTerminals() }

    var filter by remember { mutableStateOf<TerminalStatus?>(null) }
    var expandedZones by remember { mutableStateOf(setOf<String>()) }
    var bulkMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    fun matches(t: TerminalUi) = filter == null || t.status == filter

    fun toggleSelect(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
        if (selectedIds.isEmpty()) bulkMode = false
    }

    fun enterBulk(id: String) {
        bulkMode = true
        selectedIds = selectedIds + id
    }

    fun exitBulk() {
        bulkMode = false
        selectedIds = emptySet()
    }

    Box(modifier = modifier.fillMaxSize().background(AeroTheme.colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (bulkMode) {
                BulkBar(
                    count = selectedIds.size,
                    onCancel = ::exitBulk,
                    onSelectAll = {
                        selectedIds = zones.flatMap { it.terminals }.map { it.id }.toSet()
                    },
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(spacing.base),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                item(key = "filters") {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        filters.forEach { f ->
                            MChip(
                                label = f.label,
                                active = filter == f.status,
                                onClick = { filter = f.status },
                            )
                        }
                    }
                }

                if (faults.isNotEmpty()) {
                    item(key = "fault-banner") { FaultBanner(count = faults.size) }
                }

                zones.forEach { zone ->
                    val visibleTerminals = zone.terminals
                        .filter { matches(it) }
                        .sortedByDescending { it.status == TerminalStatus.Fault }
                    if (visibleTerminals.isEmpty()) return@forEach

                    val expanded = zone.id in expandedZones
                    item(key = "zone-${zone.id}") {
                        ZoneHeader(
                            name = zone.name,
                            online = zone.onlineCount,
                            total = zone.terminals.size,
                            fault = zone.faultCount,
                            expanded = expanded,
                            onToggle = {
                                expandedZones =
                                    if (expanded) expandedZones - zone.id else expandedZones + zone.id
                            },
                            onOpenDetail = { onOpenZone(zone.id) },
                        )
                    }
                    if (expanded) {
                        items(visibleTerminals.chunked(2), key = { it.first().id }) { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                                rowItems.forEach { t ->
                                    TerminalTile(
                                        name = t.name,
                                        status = t.status,
                                        selected = t.id in selectedIds,
                                        onClick = { if (bulkMode) toggleSelect(t.id) },
                                        onLongClick = { enterBulk(t.id) },
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
            actions = listOf(
                FabAction("寻呼", Icons.Filled.Campaign) {},
                FabAction("播放", Icons.Filled.PlayArrow) {},
                FabAction("停止", Icons.Filled.Stop) {},
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = spacing.base),
        )
    }
}

@Composable
private fun BulkBar(count: Int, onCancel: () -> Unit, onSelectAll: () -> Unit) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primaryContainer)
            .padding(horizontal = spacing.base, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Icon(
            Icons.Filled.Close, contentDescription = "取消",
            tint = colors.primaryDark,
            modifier = Modifier.clip(AeroTheme.shapes.pill).clickable(onClick = onCancel),
        )
        Text("已选 $count 个", style = AeroTheme.typography.title, color = colors.primaryDark, modifier = Modifier.weight(1f))
        Row(
            modifier = Modifier.clickable(onClick = onSelectAll),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Icon(Icons.Filled.DoneAll, contentDescription = null, tint = colors.primaryDark, modifier = Modifier.size(20.dp))
            Text("全选", style = AeroTheme.typography.bodyStrong, color = colors.primaryDark)
        }
    }
}

@Composable
private fun FaultBanner(count: Int) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.md)
            .background(colors.dangerContainer)
            .padding(horizontal = spacing.base, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.danger, modifier = Modifier.size(20.dp))
        Text(
            "$count 个终端故障，请尽快检查",
            style = AeroTheme.typography.bodyStrong,
            color = colors.danger,
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
            .clip(AeroTheme.shapes.md)
            .background(colors.surface)
            .clickable(onClick = onToggle)
            .padding(horizontal = spacing.base, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = AeroTheme.typography.title, color = colors.onSurface)
            Text(
                buildString {
                    append("在线 $online/$total")
                    if (fault > 0) append(" · 故障 $fault")
                },
                style = AeroTheme.typography.caption,
                color = if (fault > 0) colors.danger else colors.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = "查看分区",
            tint = colors.onSurfaceVariant,
            modifier = Modifier.clip(AeroTheme.shapes.pill).clickable(onClick = onOpenDetail),
        )
    }
}
