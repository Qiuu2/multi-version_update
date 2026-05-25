package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.EmptyState
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.HeroStrip
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TerminalHubSkeleton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TerminalTile
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme
import kotlinx.coroutines.delay

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
        delay(1400)
        loading = false
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        DetailTopBar(title = zone?.name ?: "分区", onBack = onBack)

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
                    contentPadding = PaddingValues(spacing.base),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    item(key = "hero") {
                        HeroStrip(
                            title = zone.name,
                            subtitle = "在线 ${zone.onlineCount}/${zone.terminals.size}",
                        )
                    }
                    if (zone.faultCount > 0) {
                        item(key = "fault") { FaultBannerInline(count = zone.faultCount) }
                    }
                    items(terminals.chunked(2), key = { it.first().id }) { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                            rowItems.forEach { t ->
                                TerminalTile(name = t.name, status = t.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .height(56.dp)
            .padding(horizontal = spacing.base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "返回",
            tint = colors.onSurface,
            modifier = Modifier.clip(AeroTheme.shapes.pill).clickable(onClick = onBack).size(24.dp),
        )
        Text(title, style = AeroTheme.typography.titleLarge, color = colors.onSurface)
    }
}

@Composable
private fun FaultBannerInline(count: Int) {
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
        Text("本分区 $count 个终端故障", style = AeroTheme.typography.bodyStrong, color = colors.danger)
    }
}
