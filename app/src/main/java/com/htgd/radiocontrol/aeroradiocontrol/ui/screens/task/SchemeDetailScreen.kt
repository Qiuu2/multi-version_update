package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.BackTopBar
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.HeroStrip
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

@Composable
fun SchemeDetailScreen(
    schemeId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AeroTheme.spacing
    val scheme = TaskMock.scheme(schemeId)

    Column(modifier = modifier.fillMaxSize().background(AeroTheme.colors.background)) {
        BackTopBar(
            title = "方案详情",
            onBack = onBack,
            action = {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "编辑",
                    tint = AeroTheme.colors.primary,
                    modifier = Modifier
                        .clip(AeroTheme.shapes.pill)
                        .clickable { if (scheme != null) onEdit(scheme.id) }
                        .size(24.dp),
                )
            },
        )
        if (scheme == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未找到该方案", style = AeroTheme.typography.body, color = AeroTheme.colors.onSurfaceVariant)
            }
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(spacing.base),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item(key = "hero") {
                HeroStrip(
                    title = scheme.name,
                    subtitle = "${scheme.tasks.size} 项任务 · ${if (scheme.active) "已启用" else "未启用"}",
                )
            }
            items(scheme.tasks, key = { it.id }) { task -> CompactTaskRow(task) }
        }
    }
}

@Composable
internal fun CompactTaskRow(task: TaskItem) {
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
        Text(task.time, style = AeroTheme.typography.bodyStrong, color = colors.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, style = AeroTheme.typography.body, color = colors.onSurface)
            Text(task.zone, style = AeroTheme.typography.caption, color = colors.onSurfaceVariant)
        }
        StateTagText(task.state)
    }
}

@Composable
private fun StateTagText(state: TaskCardState) {
    val c = AeroTheme.colors
    val (label, color) = when (state) {
        TaskCardState.Running -> "进行中" to c.warning
        TaskCardState.Swapped -> "对调" to c.secondary
        TaskCardState.Migrated -> "迁移" to c.warning
        TaskCardState.Deleted -> "已删除" to c.onSurfaceVariant
        TaskCardState.Cancelled -> "已取消" to c.onSurfaceVariant
        TaskCardState.Normal -> return
    }
    Text(label, style = AeroTheme.typography.overline, color = color)
}
