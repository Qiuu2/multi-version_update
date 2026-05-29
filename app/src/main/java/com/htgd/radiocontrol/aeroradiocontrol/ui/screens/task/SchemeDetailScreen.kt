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
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MSwitch
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.BackTopBar
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.EmptyState
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.HeroStrip
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.ListSkeleton
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * Scheme detail — stateful entry (TASK-PA-03c, de-mocked: read + enable/disable).
 *
 * Observes [SchemeDetailViewModel] (→ [TaskRepository], same SSOT as the task home).
 * Renders the 5 [SchemeDetailUiState] states; the enable/disable toggle calls
 * [SchemeDetailViewModel.setActive]. Edit (CRUD) still routes to the mock-backed
 * [SchemeEditScreen] (no CRUD interface yet — out of PA-03 scope).
 */
@Composable
fun SchemeDetailScreen(
    schemeId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SchemeDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(schemeId) { viewModel.load(schemeId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SchemeDetailContent(
        state = state,
        onBack = onBack,
        onEdit = onEdit,
        onRetry = viewModel::refresh,
        onToggleActive = viewModel::setActive,
        modifier = modifier,
    )
}

/** Stateless renderer (previewable / testable without Hilt). */
@Composable
fun SchemeDetailContent(
    state: SchemeDetailUiState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onRetry: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AeroTheme.spacing
    val scheme = when (state) {
        is SchemeDetailUiState.Success -> state.scheme
        is SchemeDetailUiState.Empty -> state.scheme
        else -> null
    }

    Column(modifier = modifier.fillMaxSize().background(AeroTheme.colors.bg)) {
        BackTopBar(
            title = "方案详情",
            onBack = onBack,
            action = {
                if (scheme != null) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "编辑",
                        tint = AeroTheme.colors.primary,
                        modifier = Modifier
                            .clip(AeroTheme.shapes.rChip)
                            .clickable { onEdit(scheme.id) }
                            .size(24.dp),
                    )
                }
            },
        )
        when (state) {
            is SchemeDetailUiState.Loading -> ListSkeleton(rows = 5)

            is SchemeDetailUiState.NotFound -> CenterMessage("未找到该方案")

            is SchemeDetailUiState.Error -> EmptyState(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                title = "加载失败",
                description = state.message,
                actionLabel = "重试",
                onAction = onRetry,
            )

            is SchemeDetailUiState.Empty -> SchemeDetailBody(
                scheme = state.scheme,
                spacing = spacing,
                onToggleActive = onToggleActive,
                tasksEmpty = true,
            )

            is SchemeDetailUiState.Success -> SchemeDetailBody(
                scheme = state.scheme,
                spacing = spacing,
                onToggleActive = onToggleActive,
                tasksEmpty = false,
            )
        }
    }
}

@Composable
private fun SchemeDetailBody(
    scheme: SchemeUi,
    spacing: com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroSpacing,
    onToggleActive: (Boolean) -> Unit,
    tasksEmpty: Boolean,
) {
    LazyColumn(
        contentPadding = PaddingValues(spacing.pageH),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "hero") {
            HeroStrip(
                kicker = "${scheme.tasks.size} 项任务 · ${if (scheme.active) "已启用" else "未启用"}",
                title = scheme.name,
            )
        }
        item(key = "toggle") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AeroTheme.shapes.rCard)
                    .background(AeroTheme.colors.surface)
                    .padding(horizontal = spacing.pageH, vertical = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "启用该方案",
                    style = AeroTheme.typography.body,
                    color = AeroTheme.colors.ink,
                    modifier = Modifier.weight(1f),
                )
                MSwitch(checked = scheme.active, onCheckedChange = onToggleActive)
            }
        }
        if (tasksEmpty) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    title = "暂无任务",
                    description = "该方案还没有定时任务。",
                )
            }
        } else {
            items(scheme.tasks, key = { it.id }) { task -> CompactTaskRow(task) }
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = AeroTheme.typography.body, color = AeroTheme.colors.ink3)
    }
}

@Composable
internal fun CompactTaskRow(task: TaskItem) {
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
        Text(task.time, style = AeroTheme.typography.bodyLarge, color = colors.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, style = AeroTheme.typography.body, color = colors.ink)
            Text(task.zone, style = AeroTheme.typography.bodySmall, color = colors.ink3)
        }
        StateTagText(task.state)
    }
}

@Composable
private fun StateTagText(state: TaskCardState) {
    val c = AeroTheme.colors
    val (label, color) = when (state) {
        TaskCardState.Running -> "进行中" to c.statusPaging
        TaskCardState.Swapped -> "对调" to c.talkBlue
        TaskCardState.Migrated -> "迁移" to c.statusPaging
        TaskCardState.Deleted -> "已删除" to c.ink3
        TaskCardState.Cancelled -> "已取消" to c.ink3
        TaskCardState.Normal -> return
    }
    Text(label, style = AeroTheme.typography.label, color = color)
}
