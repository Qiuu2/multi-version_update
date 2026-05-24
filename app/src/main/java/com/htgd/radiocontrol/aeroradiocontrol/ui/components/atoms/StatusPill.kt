package com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

enum class TerminalStatus { Online, Offline, Fault, Playing, Paging }

private data class StatusVisual(val dot: Color, val container: Color, val content: Color, val label: String)

@Composable
private fun visualFor(status: TerminalStatus): StatusVisual {
    val c = AeroTheme.colors
    return when (status) {
        TerminalStatus.Online -> StatusVisual(c.success, c.successContainer, c.success, "在线")
        TerminalStatus.Offline -> StatusVisual(c.onSurfaceVariant, c.surfaceVariant, c.onSurfaceVariant, "离线")
        TerminalStatus.Fault -> StatusVisual(c.danger, c.dangerContainer, c.danger, "故障")
        TerminalStatus.Playing -> StatusVisual(c.primary, c.primaryContainer, c.primaryDark, "播放中")
        TerminalStatus.Paging -> StatusVisual(c.warning, c.warningContainer, c.warning, "寻呼中")
    }
}

@Composable
fun StatusPill(
    status: TerminalStatus,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val v = visualFor(status)
    val spacing = AeroTheme.spacing
    Row(
        modifier = modifier
            .clip(AeroTheme.shapes.pill)
            .background(v.container)
            .padding(horizontal = spacing.sm, vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(v.dot),
        ) {}
        Text(
            text = label ?: v.label,
            style = AeroTheme.typography.overline,
            color = v.content,
        )
    }
}
