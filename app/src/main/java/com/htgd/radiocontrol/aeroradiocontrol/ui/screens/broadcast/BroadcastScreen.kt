package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButtonVariant
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MChip
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.StatusPill
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal.TerminalMock
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

enum class BroadcastMode(val label: String) { Page("寻呼"), Talk("对讲"), Cast("点播") }

private data class MediaFile(val id: String, val name: String, val duration: String)

private val mockMedia = listOf(
    MediaFile("m1", "上课铃声", "00:08"),
    MediaFile("m2", "课间操音乐", "03:20"),
    MediaFile("m3", "放学通知", "00:15"),
)

/**
 * Broadcast tab. Segmented switch over three modes; the target-terminal
 * selection is shared across all modes. Page mode uses push-to-talk with a
 * pulsing button while pressed.
 */
@Composable
fun BroadcastScreen(modifier: Modifier = Modifier) {
    val spacing = AeroTheme.spacing
    val zones = TerminalMock.zones

    var mode by remember { mutableStateOf(BroadcastMode.Page) }
    var selectedZones by remember { mutableStateOf(setOf(zones.first().id)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AeroTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(spacing.base),
        verticalArrangement = Arrangement.spacedBy(spacing.base),
    ) {
        ModeSegmented(selected = mode, onSelect = { mode = it })

        TargetSection(
            zoneLabels = zones.map { it.id to it.name },
            selectedIds = selectedZones,
            onToggle = { id ->
                selectedZones =
                    if (id in selectedZones) selectedZones - id else selectedZones + id
            },
        )

        when (mode) {
            BroadcastMode.Page -> PagePanel(targetCount = selectedZones.size)
            BroadcastMode.Talk -> TalkPanel()
            BroadcastMode.Cast -> CastPanel()
        }
    }
}

@Composable
private fun ModeSegmented(selected: BroadcastMode, onSelect: (BroadcastMode) -> Unit) {
    val colors = AeroTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.pill)
            .background(colors.surfaceVariant)
            .padding(AeroTheme.spacing.xs),
    ) {
        BroadcastMode.values().forEach { m ->
            val active = m == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(AeroTheme.shapes.pill)
                    .then(if (active) Modifier.background(AeroTheme.gradients.primary) else Modifier)
                    .clickable { onSelect(m) }
                    .padding(vertical = AeroTheme.spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = m.label,
                    style = AeroTheme.typography.bodyStrong,
                    color = if (active) colors.onPrimary else colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TargetSection(
    zoneLabels: List<Pair<String, String>>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("目标终端", style = AeroTheme.typography.title, color = colors.onSurface, modifier = Modifier.weight(1f))
            Text("已选 ${selectedIds.size} 区", style = AeroTheme.typography.caption, color = colors.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            zoneLabels.forEach { (id, name) ->
                MChip(label = name, active = id in selectedIds, onClick = { onToggle(id) })
            }
        }
    }
}

@Composable
private fun PagePanel(targetCount: Int) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    var pressing by remember { mutableStateOf(false) }

    val transition = rememberInfiniteTransition(label = "ptt")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "pulse",
    )
    val scale = if (pressing) pulse else 1f

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.base),
    ) {
        if (pressing) {
            StatusPill(status = TerminalStatus.Paging, label = "正在寻呼 $targetCount 区")
        } else {
            Text(
                "按住下方按钮开始寻呼",
                style = AeroTheme.typography.body,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(if (pressing) AeroTheme.gradients.warm else AeroTheme.gradients.primary)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressing = true
                            tryAwaitRelease()
                            pressing = false
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.Mic, contentDescription = "寻呼", tint = colors.onPrimary, modifier = Modifier.size(48.dp))
                Text(if (pressing) "松开结束" else "按住说话", style = AeroTheme.typography.bodyStrong, color = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun TalkPanel() {
    val spacing = AeroTheme.spacing
    var talking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.base),
    ) {
        StatusPill(
            status = if (talking) TerminalStatus.Playing else TerminalStatus.Offline,
            label = if (talking) "对讲进行中" else "未连接",
        )
        MButton(
            text = if (talking) "结束对讲" else "开始对讲",
            variant = if (talking) MButtonVariant.Danger else MButtonVariant.Success,
            leadingIcon = Icons.Filled.Mic,
            onClick = { talking = !talking },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CastPanel() {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    var selectedId by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text("选择音频", style = AeroTheme.typography.title, color = colors.onSurface)
        mockMedia.forEach { file ->
            MediaRow(
                file = file,
                selected = file.id == selectedId,
                onClick = { selectedId = file.id },
            )
        }
        Spacer(modifier = Modifier.height(spacing.sm))
        MButton(
            text = if (playing) "停止播放" else "开始点播",
            variant = if (playing) MButtonVariant.Danger else MButtonVariant.Filled,
            leadingIcon = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            enabled = selectedId != null,
            onClick = { playing = !playing },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MediaRow(file: MediaFile, selected: Boolean, onClick: () -> Unit) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.md)
            .background(if (selected) colors.primaryContainer else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.base, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = if (selected) colors.primary else colors.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Text(file.name, style = AeroTheme.typography.body, color = colors.onSurface, modifier = Modifier.weight(1f))
        Text(file.duration, style = AeroTheme.typography.caption, color = colors.onSurfaceVariant)
    }
}
