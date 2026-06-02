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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.PollingState
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

/**
 * Terminal status filter set — PA-14 C-3 / Phase C residual extension from 4 to 6
 * chips. Spec §1.2 + Handoff.html:807 list 5 terminal states (online/offline/fault/
 * playing/paging); the "全部" all-pass is the 6th. The row container already uses
 * `Modifier.horizontalScroll` so the chip strip scrolls on narrow phones (per spec
 * §1.2 I-2 overflow guidance).
 */
private val filters = listOf(
    StatusFilter("全部", null),
    StatusFilter("在线", TerminalStatus.Online),
    StatusFilter("离线", TerminalStatus.Offline),
    StatusFilter("故障", TerminalStatus.Fault),
    StatusFilter("播放中", TerminalStatus.Playing),
    StatusFilter("寻呼中", TerminalStatus.Paging),
)

/**
 * Terminal hub — stateful entry (TASK-AR-102).
 *
 * Observes [TerminalHubViewModel] (→ [TerminalRepository], real data) and renders
 * the five [TerminalHubUiState] states. The success/partial branch keeps the
 * existing filter / bulk-select / zone-expand controls. No more mock data.
 */
@Composable
fun TerminalHubScreen(
    onOpenZone: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TerminalHubViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pollingState by viewModel.pollingState.collectAsStateWithLifecycle()
    TerminalHubContent(
        state = state,
        pollingState = pollingState,
        onOpenZone = onOpenZone,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

/** Stateless renderer for the five states (previewable / testable without Hilt). */
@Composable
fun TerminalHubContent(
    state: TerminalHubUiState,
    onOpenZone: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    pollingState: PollingState = PollingState.IDLE,
) {
    val colors = AeroTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.bg)) {
        when (state) {
            is TerminalHubUiState.Loading -> TerminalHubSkeleton()

            is TerminalHubUiState.Empty -> EmptyState(
                icon = Icons.Filled.Search,
                title = "暂无终端",
                description = "还没有终端数据，点击重试刷新。",
                actionLabel = "重试",
                onAction = onRetry,
            )

            is TerminalHubUiState.Error -> EmptyState(
                icon = Icons.Filled.Search,
                title = "加载失败",
                description = state.message,
                actionLabel = "重试",
                onAction = onRetry,
            )

            is TerminalHubUiState.Success ->
                TerminalHubList(
                    zones = state.zones,
                    // Data is up; a background poll just failed → non-blocking notice,
                    // not a hard Error (the list stays visible, retry happens on cadence).
                    staleMessage = pollLapseMessage(pollingState),
                    onOpenZone = onOpenZone,
                )

            is TerminalHubUiState.Partial ->
                TerminalHubList(zones = state.zones, staleMessage = state.staleMessage, onOpenZone = onOpenZone)
        }
    }
}

/** A warning banner message when polling is degraded while data is shown, else null. */
private fun pollLapseMessage(pollingState: PollingState): String? =
    if (pollingState == PollingState.ERROR) "刷新失败，正在自动重试…" else null

/**
 * The populated hub: controls + fault banner + per-zone terminal grid + bulk FAB.
 * [staleMessage] (non-null only for [TerminalHubUiState.Partial]) shows a warning
 * banner above the list (e.g. realtime disconnected, data may be stale).
 */
@Composable
private fun TerminalHubList(
    zones: List<ZoneUi>,
    staleMessage: String?,
    onOpenZone: (String) -> Unit,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val faultCount = remember(zones) { zones.sumOf { it.faultCount } }

    var filter by remember { mutableStateOf<TerminalStatus?>(null) }
    var expandedZones by remember { mutableStateOf(setOf<String>()) }
    var bulkMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    fun matches(t: TerminalUi) = filter == null || t.status == filter
    val anyVisible = zones.any { z -> z.terminals.any { matches(it) } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(spacing.pageH),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (staleMessage != null) {
                item(key = "stale") {
                    NotificationBanner(type = NotificationType.Warning, message = staleMessage)
                }
            }

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
                            color = colors.ink2, // spec §4 次要文字→ink2
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
                item(key = "empty-filter") {
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
                    // Handoff:541/798 — 3-column terminal grid (was chunked(2), fixed Q3 #1)
                    items(visible.chunked(3), key = { it.first().id }) { rowItems ->
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

        FabBar(
            visible = bulkMode && selectedIds.isNotEmpty(),
            counterLabel = "已选 ${selectedIds.size} 台",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = spacing.lg),
            actions = {
                MButton(text = "寻呼", variant = MButtonVariant.Filled, onClick = {})
                MButton(text = "对讲", variant = MButtonVariant.Tonal, onClick = {})
                MButton(text = "点播", variant = MButtonVariant.Tonal, onClick = {})
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
                color = if (fault > 0) colors.statusFault else colors.ink2, // spec §4 次要文字→ink2
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
