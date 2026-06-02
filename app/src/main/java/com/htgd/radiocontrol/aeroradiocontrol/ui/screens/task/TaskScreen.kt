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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.EmptyState
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.HeroStrip
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationBanner
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationType
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.SkeletonBox
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.PollingState
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme
import java.time.LocalTime

/** Task tab root: home timeline + 4 sub-pages, navigated via local state. */
private sealed interface TaskRoute {
    data object Home : TaskRoute
    data class Detail(val id: String) : TaskRoute
    data class Edit(val id: String) : TaskRoute
    data object Log : TaskRoute
    data object Temp : TaskRoute
}

@Composable
fun TaskTab(modifier: Modifier = Modifier) {
    var route by remember { mutableStateOf<TaskRoute>(TaskRoute.Home) }
    when (val r = route) {
        TaskRoute.Home -> TaskScreen(
            onOpenSchemeDetail = { route = TaskRoute.Detail(it) },
            onOpenSchemeEdit = { route = TaskRoute.Edit(it) },
            onOpenLog = { route = TaskRoute.Log },
            onOpenTempBroadcast = { route = TaskRoute.Temp },
            modifier = modifier,
        )
        is TaskRoute.Detail -> SchemeDetailScreen(
            schemeId = r.id,
            onBack = { route = TaskRoute.Home },
            onEdit = { route = TaskRoute.Edit(it) },
            modifier = modifier,
        )
        is TaskRoute.Edit -> SchemeEditScreen(
            schemeId = r.id,
            onBack = { route = TaskRoute.Home },
            modifier = modifier,
        )
        TaskRoute.Log -> ExecutionLogScreen(onBack = { route = TaskRoute.Home }, modifier = modifier)
        TaskRoute.Temp -> TempFileBroadcastScreen(onBack = { route = TaskRoute.Home }, modifier = modifier)
    }
}

/**
 * Task home — stateful entry (TASK-PA-03c, de-mocked; ★ Task3 multi-scheme).
 *
 * Observes [TaskHomeViewModel] (→ [TaskRepository], real data) and renders the
 * [TaskHomeUiState] states. No more mock data.
 *
 * ★ Task3 (2026-06-01): wires [TaskHomeViewModel.selectScheme] into the hero header
 * switch button so the user can cycle through all available schemes without activating
 * them (Handoff line 929: "当前作息方案名 + 切换按钮").
 */
@Composable
fun TaskScreen(
    onOpenSchemeDetail: (String) -> Unit,
    onOpenSchemeEdit: (String) -> Unit,
    onOpenLog: () -> Unit,
    onOpenTempBroadcast: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pollingState by viewModel.pollingState.collectAsStateWithLifecycle()
    TaskHomeContent(
        state = state,
        pollingState = pollingState,
        onOpenSchemeDetail = onOpenSchemeDetail,
        onOpenSchemeEdit = onOpenSchemeEdit,
        onOpenLog = onOpenLog,
        onOpenTempBroadcast = onOpenTempBroadcast,
        onRetry = viewModel::refresh,
        onSelectScheme = viewModel::selectScheme,
        modifier = modifier,
    )
}

/** Stateless renderer for the home states (previewable / testable without Hilt). */
@Composable
fun TaskHomeContent(
    state: TaskHomeUiState,
    onOpenSchemeDetail: (String) -> Unit,
    onOpenSchemeEdit: (String) -> Unit,
    onOpenLog: () -> Unit,
    onOpenTempBroadcast: () -> Unit,
    onRetry: () -> Unit,
    /**
     * Called when the user taps the "切换" button to view a different scheme.
     * Receives the next index to select (0-based). ★ Task3.
     */
    onSelectScheme: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    pollingState: PollingState = PollingState.IDLE,
) {
    Box(modifier = modifier.fillMaxSize().background(AeroTheme.colors.bg)) {
        when (state) {
            is TaskHomeUiState.Loading -> TaskHomeSkeleton()

            is TaskHomeUiState.Empty -> EmptyState(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                title = "暂无作息方案",
                description = "还没有作息方案数据，点击重试刷新。",
                actionLabel = "重试",
                onAction = onRetry,
            )

            is TaskHomeUiState.Error -> EmptyState(
                icon = Icons.AutoMirrored.Filled.ListAlt,
                title = "加载失败",
                description = state.message,
                actionLabel = "重试",
                onAction = onRetry,
            )

            is TaskHomeUiState.Success -> TaskHomeList(
                scheme = state.scheme,
                allSchemesCount = state.allSchemes.size,
                selectedIndex = state.selectedIndex,
                staleMessage = pollLapseMessage(pollingState),
                onOpenSchemeDetail = onOpenSchemeDetail,
                onOpenSchemeEdit = onOpenSchemeEdit,
                onOpenLog = onOpenLog,
                onOpenTempBroadcast = onOpenTempBroadcast,
                onSelectScheme = onSelectScheme,
            )

            is TaskHomeUiState.Partial -> TaskHomeList(
                scheme = state.scheme,
                allSchemesCount = state.allSchemes.size,
                selectedIndex = state.selectedIndex,
                staleMessage = state.staleMessage,
                onOpenSchemeDetail = onOpenSchemeDetail,
                onOpenSchemeEdit = onOpenSchemeEdit,
                onOpenLog = onOpenLog,
                onOpenTempBroadcast = onOpenTempBroadcast,
                onSelectScheme = onSelectScheme,
            )
        }
    }
}

/** A warning banner message when polling is degraded while data is shown, else null. */
private fun pollLapseMessage(pollingState: PollingState): String? =
    if (pollingState == PollingState.ERROR) "刷新失败，正在自动重试…" else null

@Composable
private fun TaskHomeSkeleton() {
    val spacing = AeroTheme.spacing
    Column(
        modifier = Modifier.fillMaxSize().padding(spacing.pageH),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        repeat(4) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(72.dp), shape = AeroTheme.shapes.rCard)
        }
    }
}

/**
 * The populated task home list.
 *
 * ★ Task3 (2026-06-01): the hero header now includes a "切换" button next to the
 * scheme name when [allSchemesCount] > 1, per Handoff line 929 ("当前作息方案名 +
 * 切换按钮"). Tapping it advances to the next scheme index (wrapping around). The
 * button is hidden when there is only one scheme, so the single-scheme experience is
 * unchanged.
 *
 * The switch is a viewing selection only — it calls [onSelectScheme] with the next
 * index and does NOT activate the scheme on the server (setSchemeActive is a separate
 * action, reachable from SchemeDetailScreen).
 */
@Composable
private fun TaskHomeList(
    scheme: SchemeUi,
    allSchemesCount: Int,
    selectedIndex: Int,
    staleMessage: String?,
    onOpenSchemeDetail: (String) -> Unit,
    onOpenSchemeEdit: (String) -> Unit,
    onOpenLog: () -> Unit,
    onOpenTempBroadcast: () -> Unit,
    onSelectScheme: (Int) -> Unit,
) {
    val spacing = AeroTheme.spacing
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
        item(key = "hero") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                // ★ Task3: HeroStrip + inline scheme-switch button (Handoff line 929).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    HeroStrip(
                        kicker = "当前作息方案 · ${scheme.tasks.size} 项任务",
                        title = scheme.name,
                        modifier = Modifier.weight(1f),
                    )
                    // Only render the switch button when there are 2+ schemes so
                    // single-scheme campuses see no UI change.
                    if (allSchemesCount > 1) {
                        SchemeSwitchButton(
                            currentIndex = selectedIndex,
                            total = allSchemesCount,
                            onClick = { onSelectScheme((selectedIndex + 1) % allSchemesCount) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    QuickAction("方案详情", Icons.AutoMirrored.Filled.ListAlt) { onOpenSchemeDetail(scheme.id) }
                    QuickAction("编辑", Icons.Filled.Edit) { onOpenSchemeEdit(scheme.id) }
                    QuickAction("执行日志", Icons.AutoMirrored.Filled.ListAlt, onClick = onOpenLog)
                    QuickAction("临时广播", Icons.Filled.Campaign, onClick = onOpenTempBroadcast)
                }
            }
        }
        if (scheme.tasks.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    title = "暂无任务",
                    description = "编辑作息方案，添加定时任务。",
                    actionLabel = "编辑方案",
                    onAction = { onOpenSchemeEdit(scheme.id) },
                )
            }
        } else {
            listOf("上午", "下午", "晚上").forEach { period ->
                val inPeriod = scheme.tasks.filter { periodOf(it.time) == period }
                if (inPeriod.isNotEmpty()) {
                    item(key = "period-$period") {
                        Text(
                            period,
                            style = AeroTheme.typography.label,
                            color = AeroTheme.colors.ink3,
                            modifier = Modifier.padding(top = spacing.sm, start = 56.dp),
                        )
                    }
                    items(inPeriod, key = { it.id }) { task -> TimelineRow(task = task) }
                }
            }
        }
    }
}

/**
 * Scheme-switch button shown in the hero header when there are 2+ schemes
 * (Handoff line 929: "当前作息方案名 + 切换按钮"). Renders as a compact chip that
 * shows the current position ("1/2") and a swap icon. Uses existing tokens:
 * `c.surface` background, `c.primary` tint, `rCard` shape — no new tokens. ★ Task3.
 */
@Composable
private fun SchemeSwitchButton(
    currentIndex: Int,
    total: Int,
    onClick: () -> Unit,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .clip(AeroTheme.shapes.rCard)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Icon(
            Icons.Filled.SwapHoriz,
            contentDescription = "切换方案",
            tint = colors.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "${currentIndex + 1}/$total",
            style = AeroTheme.typography.label,
            color = colors.primary,
        )
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, onClick: () -> Unit) {
    val colors = AeroTheme.colors
    Column(
        modifier = Modifier
            .clip(AeroTheme.shapes.rCard)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = AeroTheme.spacing.md, vertical = AeroTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeroTheme.spacing.xs),
    ) {
        Icon(icon, contentDescription = label, tint = colors.primary, modifier = Modifier.size(22.dp))
        Text(label, style = AeroTheme.typography.label, color = colors.ink3)
    }
}

@Composable
private fun TimelineRow(task: TaskItem) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(56.dp).fillMaxHeight()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.TopCenter)
                    .background(colors.line),
            )
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(task.time, style = AeroTheme.typography.label, color = colors.ink3)
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
        TaskCardState.Running -> c.statusPaging
        TaskCardState.Swapped -> c.gold
        TaskCardState.Migrated -> c.gold
        TaskCardState.Deleted, TaskCardState.Cancelled -> c.ink3
        TaskCardState.Normal -> c.primary
    }
}

@Composable
private fun TaskCard(task: TaskItem, modifier: Modifier = Modifier) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val runningBorder = colors.statusPaging
    val shape = AeroTheme.shapes.rCard
    val faded = task.state == TaskCardState.Deleted || task.state == TaskCardState.Cancelled

    val container = when (task.state) {
        TaskCardState.Running -> colors.statusPagingSoft
        TaskCardState.Deleted, TaskCardState.Cancelled -> colors.surface3
        else -> colors.surface
    }

    var box = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(container)

    box = when (task.state) {
        TaskCardState.Running -> box.border(1.5.dp, runningBorder, shape)
        TaskCardState.Swapped -> box.border(1.dp, colors.gold, shape)
        TaskCardState.Migrated -> box.drawBehind {
            drawRoundRect(
                color = colors.gold,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                ),
                cornerRadius = CornerRadius(12.dp.toPx()),
            )
        }
        else -> box.border(1.dp, colors.line, shape)
    }

    // PA-14 Phase C — 3-state timeliness pill derived from now() vs task.time
    // ("已完成 / 进行中 / 待执行"). Pure presentation derivation — domain unchanged.
    val temporalState = remember(task.time) { temporalStateOf(task.time, LocalTime.now()) }

    Column(
        modifier = box.padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = task.title,
                style = AeroTheme.typography.bodyLarge,
                color = if (faded) colors.ink3 else colors.ink,
                textDecoration = if (faded) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f),
            )
            // 进行中/已完成/待执行 — PA-14 Phase C v1.3 `c.taskCardState*` tokens.
            TemporalPill(state = temporalState)
            // Existing design-showcase state (Swapped/Migrated/Deleted/Cancelled/Running)
            // stays alongside — they're disjoint from the temporal pill.
            StateTag(task.state)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            // Target-zone tag — placeholder "—" until the v3 wire exposes a target
            // zone for tasks (PA-10 confirmed SchemeTask domain has NO zone field;
            // fe-business INFO for backlog, do NOT add a domain field unilaterally).
            TargetZoneTag(label = task.zone.ifBlank { "—" })
        }
    }
}

/** Timeliness of a task vs the current time — derived in fe at render time. */
internal enum class TemporalState { Done, Running, Pending }

/**
 * Derives the 3-state timeliness from a "HH:mm" task time vs the current [now]:
 *   - past               → [TemporalState.Done]      (已完成)
 *   - within ±1 minute   → [TemporalState.Running]   (进行中) — bells ring on the minute
 *   - future / unparsable → [TemporalState.Pending]  (待执行) safe default
 *
 * Pure function (visible-for-testing); see [TemporalPill] for the rendered pill.
 */
internal fun temporalStateOf(taskTime: String, now: LocalTime): TemporalState {
    val parts = taskTime.split(':')
    val hh = parts.getOrNull(0)?.toIntOrNull()
    val mm = parts.getOrNull(1)?.toIntOrNull()
    if (hh == null || mm == null) return TemporalState.Pending
    val taskMinutes = hh * 60 + mm
    val nowMinutes = now.hour * 60 + now.minute
    return when {
        nowMinutes < taskMinutes - 1 -> TemporalState.Pending
        nowMinutes > taskMinutes + 1 -> TemporalState.Done
        else -> TemporalState.Running
    }
}

/**
 * Temporal-state pill — PA-14 Phase C row 19. Bound to the v1.3 role tokens
 * `c.taskCardStateDone / Running / Pending` (#8A929F / #EA580C / #4A5260) so a
 * future hex re-pin lands in one alias, not 3 pill sites. Soft background reuses
 * the matching `c.status*Soft` palette for visual lift without enlarging the soft-
 * palette set.
 */
@Composable
private fun TemporalPill(state: TemporalState) {
    val c = AeroTheme.colors
    val (label, fg, bg) = when (state) {
        TemporalState.Done    -> Triple("已完成", c.taskCardStateDone,    c.surface3)
        TemporalState.Running -> Triple("进行中", c.taskCardStateRunning, c.statusPagingSoft)
        TemporalState.Pending -> Triple("待执行", c.taskCardStatePending, c.surface3)
    }
    Box(
        modifier = Modifier
            .clip(AeroTheme.shapes.rChip)
            .background(bg)
            .padding(horizontal = AeroTheme.spacing.sm, vertical = AeroTheme.spacing.xs),
    ) {
        Text(label, style = AeroTheme.typography.label, color = fg)
    }
}

/**
 * Target-zone tag (PA-14 Phase C row 20) — placeholder presentation until the v3
 * task wire surfaces a zone field. Renders the [label] as a small neutral chip;
 * the existing palette is used (`c.surface3` bg + `c.ink3` fg) per the施工图's
 * "no new token — use existing c.ink3" guidance.
 */
@Composable
private fun TargetZoneTag(label: String) {
    val c = AeroTheme.colors
    Box(
        modifier = Modifier
            .clip(AeroTheme.shapes.rChip)
            .background(c.surface3)
            .padding(horizontal = AeroTheme.spacing.sm, vertical = AeroTheme.spacing.xs),
    ) {
        Text("目标分区 · $label", style = AeroTheme.typography.label, color = c.ink3)
    }
}

@Composable
private fun StateTag(state: TaskCardState) {
    val c = AeroTheme.colors
    val (label, fg, bg, icon) = when (state) {
        TaskCardState.Running -> Quad("进行中", c.statusPaging, c.statusPagingSoft, null)
        TaskCardState.Swapped -> Quad("对调", c.gold, c.goldSoft, Icons.Filled.SwapHoriz)
        TaskCardState.Migrated -> Quad("迁移", c.gold, c.goldSoft, null)
        TaskCardState.Deleted -> Quad("已删除", c.ink3, c.surface3, null)
        TaskCardState.Cancelled -> Quad("已取消", c.ink3, c.surface3, null)
        TaskCardState.Normal -> return
    }
    Row(
        modifier = Modifier
            .clip(AeroTheme.shapes.rChip)
            .background(bg)
            .padding(horizontal = AeroTheme.spacing.sm, vertical = AeroTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeroTheme.spacing.xs),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        }
        Text(label, style = AeroTheme.typography.label, color = fg)
    }
}

private data class Quad(
    val label: String,
    val fg: Color,
    val bg: Color,
    val icon: ImageVector?,
)

private fun periodOf(time: String): String {
    val hour = time.substringBefore(":").toIntOrNull() ?: 0
    return when {
        hour < 12 -> "上午"
        hour < 18 -> "下午"
        else -> "晚上"
    }
}
