package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

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
import androidx.compose.material.icons.automirrored.filled.ListAlt
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
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.BackTopBar
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.EmptyState
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.ListSkeleton
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme
import kotlinx.coroutines.delay

@Composable
fun ExecutionLogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AeroTheme.spacing
    val logs = TaskMock.logs
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1200)
        loading = false
    }

    Column(modifier = modifier.fillMaxSize().background(AeroTheme.colors.bg)) {
        BackTopBar(title = "执行日志", onBack = onBack)
        when {
            loading -> ListSkeleton(rows = 6)
            logs.isEmpty() -> EmptyState(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                title = "暂无执行日志",
                description = "任务执行后会在这里留下记录。",
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(spacing.pageH),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                items(logs) { entry -> LogRow(entry) }
            }
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.rCard)
            .background(colors.surface)
            .padding(horizontal = spacing.pageH, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(entry.time, style = AeroTheme.typography.bodySmall, color = colors.ink3)
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, style = AeroTheme.typography.body, color = colors.ink)
            Text(entry.detail, style = AeroTheme.typography.bodySmall, color = colors.ink3)
        }
        ResultPill(success = entry.success)
    }
}

@Composable
private fun ResultPill(success: Boolean) {
    val colors = AeroTheme.colors
    val bg = if (success) colors.statusOnlineSoft else colors.statusFaultSoft
    val fg = if (success) colors.statusOnline else colors.statusFault
    Text(
        text = if (success) "成功" else "失败",
        style = AeroTheme.typography.kicker,
        color = fg,
        modifier = Modifier
            .clip(AeroTheme.shapes.rChip)
            .background(bg)
            .padding(horizontal = AeroTheme.spacing.sm, vertical = AeroTheme.spacing.xs),
    )
}
