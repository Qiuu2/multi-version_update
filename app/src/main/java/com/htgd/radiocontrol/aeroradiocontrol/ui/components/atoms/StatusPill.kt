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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * Terminal status — Handoff.html §03 · Status Pill / Component Gallery.
 *
 * Accessibility: status color is never the sole signal — every pill carries a
 * label (在线 / 离线 / 故障 / 播放中 / 寻呼中) per the a11y rule in §06.
 */
enum class TerminalStatus { Online, Offline, Fault, Playing, Paging }

@Composable
fun StatusPill(
    status: TerminalStatus,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val typo   = AeroTheme.typography

    val (bg, fg, label) = when (status) {
        TerminalStatus.Online  -> Triple(colors.statusOnlineSoft,  colors.statusOnline,  "在线")
        TerminalStatus.Offline -> Triple(colors.statusOfflineSoft, colors.statusOffline, "离线")
        TerminalStatus.Fault   -> Triple(colors.statusFaultSoft,   colors.statusFault,   "故障")
        TerminalStatus.Playing -> Triple(colors.statusPlayingSoft, colors.statusPlaying, "播放中")
        TerminalStatus.Paging  -> Triple(colors.statusPagingSoft,  colors.statusPaging,  "寻呼中")
    }

    Row(
        modifier = modifier
            .background(bg, AeroTheme.shapes.rChip)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Dot(color = fg)
        Text(text = label, style = typo.kicker.copy(fontWeight = FontWeight.Medium, color = fg))
    }
}

@Composable
private fun Dot(color: Color, size: androidx.compose.ui.unit.Dp = 6.dp) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape),
    )
}
