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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.BackTopBar
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.EmptyState
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationBanner
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationType
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TerminalHubSkeleton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TerminalTile
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * Zone detail — stateful entry (TASK-AR-105).
 *
 * Observes [ZoneDetailViewModel] (→ [TerminalRepository], real data; same SSOT as
 * the hub) and renders the five [ZoneDetailUiState] states. No more mock data.
 */
@Composable
fun ZoneDetailScreen(
    zoneId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ZoneDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(zoneId) { viewModel.load(zoneId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ZoneDetailContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

/** Stateless renderer (previewable / testable without Hilt). */
@Composable
fun ZoneDetailContent(
    state: ZoneDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val title = when (state) {
        is ZoneDetailUiState.Empty -> state.zone.name
        is ZoneDetailUiState.Success -> state.zone.name
        else -> "分区"
    }

    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        BackTopBar(title = title, onBack = onBack)
        when (state) {
            is ZoneDetailUiState.Loading -> TerminalHubSkeleton()

            is ZoneDetailUiState.Error -> EmptyState(
                icon = Icons.Filled.Warning,
                title = "加载失败",
                description = state.message,
                actionLabel = "重试",
                onAction = onRetry,
            )

            is ZoneDetailUiState.NotFound -> EmptyState(
                icon = Icons.Filled.Warning,
                title = "未找到该分区",
                description = "该分区可能已被删除。",
            )

            is ZoneDetailUiState.Empty -> EmptyState(
                icon = Icons.Filled.Speaker,
                title = "该分区暂无终端",
                description = "为该分区添加终端后会显示在这里。",
            )

            is ZoneDetailUiState.Success -> ZoneTerminalList(zone = state.zone)
        }
    }
}

@Composable
private fun ZoneTerminalList(zone: ZoneUi) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
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
                    color = colors.ink2, // spec §4 次要文字→ink2
                )
            }
        }
        if (zone.faultCount > 0) {
            item(key = "fault") {
                NotificationBanner(type = NotificationType.Error, message = "本分区 ${zone.faultCount} 个终端故障")
            }
        }
        // Handoff:541/798 — 3-column terminal grid (was chunked(2), fixed Q3 #1)
        items(terminals.chunked(3), key = { it.first().id }) { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tileGap)) {
                rowItems.forEach { t -> TerminalTile(name = t.name, status = t.status) }
            }
        }
    }
}
