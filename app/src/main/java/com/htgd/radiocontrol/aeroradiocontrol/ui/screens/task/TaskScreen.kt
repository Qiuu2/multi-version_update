package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.EmptyState
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.HeroStrip
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.SkeletonBox
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme
import kotlinx.coroutines.delay

@Composable
fun TaskScreen(
    onOpenSchemeDetail: (String) -> Unit,
    onOpenSchemeEdit: (String) -> Unit,
    onOpenLog: () -> Unit,
    onOpenTempBroadcast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AeroTheme.spacing
    val scheme = TaskMock.activeScheme
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1400)
        loading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(AeroTheme.colors.background),
        contentPadding = PaddingValues(spacing.base),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "hero") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                HeroStrip(title = scheme.name, subtitle = "当前作息方案 · ${scheme.tasks.size} 项任务")
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    QuickAction("方案详情", Icons.AutoMirrored.Filled.ListAlt) { onOpenSchemeDetail(scheme.id) }
                    QuickAction("编辑", Icons.Filled.Edit) { onOpenSchemeEdit(scheme.id) }
                    QuickAction("执行日志", Icons.AutoMirrored.Filled.ListAlt, onClick = onOpenLog)
                    QuickAction("临时广播", Icons.Filled.Campaign, onClick = onOpenTempBroadcast)
                }
            }
        }
        when {
            loading -> items(4) {
                SkeletonBox(modifier = Modifier.fillMaxWidth().height(72.dp), shape = AeroTheme.shapes.md)
            }
            scheme.tasks.isEmpty() -> item(key = "empty") {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    title = "暂无任务",
                    description = "编辑作息方案，添加定时任务。",
                    actionLabel = "编辑方案",
                    onAction = { onOpenSchemeEdit(scheme.id) },
                )
            }
            else -> items(scheme.tasks, key = { it.id }) { task ->
                TimelineRow(task = task)
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val colors = AeroTheme.colors
    Column(
        modifier = Modifier
            .clip(AeroTheme.shapes.sm)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = AeroTheme.spacing.md, vertical = AeroTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeroTheme.spacing.xxs),
    ) {
        Icon(icon, contentDescription = label, tint = colors.primary, modifier = Modifier.size(22.dp))
        Text(label, style = AeroTheme.typography.overline, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun TimelineRow(task: TaskItem) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        // Time rail with connector line + state dot.
        Box(modifier = Modifier.width(56.dp).fillMaxHeight()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.TopCenter)
                    .background(colors.outlineVariant),
            )
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(task.time, style = AeroTheme.typography.overline, color = colors.onSurfaceVariant)
                Box(
                    modifier = Modifier
                        .padding(top = spacing.xs)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dotColor(task.state)),
                )
            }
        }
        TaskCard(task = task, modifier = Modifier.weight(1f).padding(bottom = spacing.md, start = spacing.sm))
    }
}

@Composable
private fun dotColor(state: TaskCardState): Color {
    val c = AeroTheme.colors
    return when (state) {
        TaskCardState.Running -> c.warning
        TaskCardState.Swapped -> c.secondary
        TaskCardState.Migrated -> c.warning
        TaskCardState.Deleted, TaskCardState.Cancelled -> c.onSurfaceVariant
        TaskCardState.Normal -> c.primary
    }
}

@Composable
private fun TaskCard(task: TaskItem, modifier: Modifier = Modifier) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val gold = colors.warning
    val shape = AeroTheme.shapes.md
    val mdRadius = AeroTheme.shapes.mdRadius
    val faded = task.state == TaskCardState.Deleted || task.state == TaskCardState.Cancelled

    val container = when (task.state) {
        TaskCardState.Running -> colors.warningContainer
        TaskCardState.Deleted, TaskCardState.Cancelled -> colors.surfaceVariant
        else -> colors.surface
    }

    var box = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(container)

    box = when (task.state) {
        TaskCardState.Running -> box.border(1.5.dp, gold, shape)
        TaskCardState.Swapped -> box.border(1.dp, colors.secondary, shape)
        TaskCardState.Migrated -> box.drawBehind {
            drawRoundRect(
                color = gold,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                ),
                cornerRadius = CornerRadius(mdRadius.toPx()),
            )
        }
        else -> box.border(1.dp, colors.outlineVariant, shape)
    }

    Column(
        modifier = box.padding(spacing.base),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = task.title,
                style = AeroTheme.typography.title,
                color = if (faded) colors.onSurfaceVariant else colors.onSurface,
                textDecoration = if (faded) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f),
            )
            StateTag(task.state)
        }
        Text(task.zone, style = AeroTheme.typography.caption, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun StateTag(state: TaskCardState) {
    val c = AeroTheme.colors
    val (label, fg, bg, icon) = when (state) {
        TaskCardState.Running -> Quad("进行中", c.warning, c.warningContainer, null)
        TaskCardState.Swapped -> Quad("对调", c.secondary, c.secondaryContainer, Icons.Filled.SwapHoriz)
        TaskCardState.Migrated -> Quad("迁移", c.warning, c.warningContainer, null)
        TaskCardState.Deleted -> Quad("已删除", c.onSurfaceVariant, c.surfaceDim, null)
        TaskCardState.Cancelled -> Quad("已取消", c.onSurfaceVariant, c.surfaceDim, null)
        TaskCardState.Normal -> return
    }
    Row(
        modifier = Modifier
            .clip(AeroTheme.shapes.pill)
            .background(bg)
            .padding(horizontal = AeroTheme.spacing.sm, vertical = AeroTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeroTheme.spacing.xxs),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        }
        Text(label, style = AeroTheme.typography.overline, color = fg)
    }
}

private data class Quad(
    val label: String,
    val fg: Color,
    val bg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
)
