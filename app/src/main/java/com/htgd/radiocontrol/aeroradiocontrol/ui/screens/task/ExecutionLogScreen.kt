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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.StatusPill
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.BackTopBar
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

@Composable
fun ExecutionLogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AeroTheme.spacing
    Column(modifier = modifier.fillMaxSize().background(AeroTheme.colors.background)) {
        BackTopBar(title = "执行日志", onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(spacing.base),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(TaskMock.logs) { entry -> LogRow(entry) }
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
            .clip(AeroTheme.shapes.md)
            .background(colors.surface)
            .padding(horizontal = spacing.base, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(entry.time, style = AeroTheme.typography.caption, color = colors.onSurfaceVariant)
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, style = AeroTheme.typography.body, color = colors.onSurface)
            Text(entry.detail, style = AeroTheme.typography.caption, color = colors.onSurfaceVariant)
        }
        StatusPill(
            status = if (entry.success) TerminalStatus.Online else TerminalStatus.Fault,
            label = if (entry.success) "成功" else "失败",
        )
    }
}
