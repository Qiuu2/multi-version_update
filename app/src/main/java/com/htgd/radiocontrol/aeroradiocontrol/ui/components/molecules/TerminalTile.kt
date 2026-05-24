package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.StatusPill
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * Terminal tile — Handoff.html §03 · Terminal Tile (5 states).
 *
 * | state    | visual                                                        |
 * |----------|----------------------------------------------------------------|
 * | default  | white surface, e1 shadow, 1.5dp transparent border             |
 * | selected | primary border + primary-soft fill + check badge top-right     |
 * | offline  | surface-2 fill, no shadow, name 55% opacity                    |
 * | fault    | red border + red-soft icon bg + ! badge                        |
 * | playing  | blue-soft icon bg + animated wave bars (static here for v0)    |
 *
 * Touch target: full tile is clickable; long-press for bulk-select mode can
 * be wired by a parent via `Modifier.combinedClickable` at the call site
 * (kept off this composable to avoid the experimental annotation).
 */
@Composable
fun TerminalTile(
    name: String,
    status: TerminalStatus,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val colors = AeroTheme.colors
    val shape  = AeroTheme.shapes.rTile

    val (bgColor, borderColor, hasShadow) = when {
        selected                          -> Triple(colors.primarySoft, colors.primary,    false)
        status == TerminalStatus.Offline  -> Triple(colors.surface2,    colors.line,       false)
        status == TerminalStatus.Fault    -> Triple(colors.surface,     colors.statusFault, true)
        else                              -> Triple(colors.surface,     Color.Transparent,  true)
    }

    Box(
        modifier = modifier
            .size(width = 130.dp, height = 130.dp)
            .let { if (hasShadow) it.shadow(elevation = AeroTheme.elevation.e1, shape = shape, clip = false) else it }
            .clip(shape)
            .background(bgColor)
            .border(width = 1.5.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconBadge(status = status)
                CornerBadge(status = status, selected = selected)
            }

            Column {
                Text(
                    text     = name,
                    style    = AeroTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color    = colors.ink,
                    modifier = Modifier.alpha(if (status == TerminalStatus.Offline) 0.55f else 1f),
                    maxLines = 1,
                )
                StatusPill(status = status, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun IconBadge(status: TerminalStatus) {
    val colors = AeroTheme.colors
    val (bg, fg) = when (status) {
        TerminalStatus.Online, TerminalStatus.Paging -> colors.primarySoft to colors.primaryInk
        TerminalStatus.Offline                       -> colors.surface3  to colors.ink3
        TerminalStatus.Fault                         -> colors.statusFaultSoft to colors.statusFault
        TerminalStatus.Playing                       -> colors.statusPlayingSoft to colors.statusPlaying
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(bg, AeroTheme.shapes.rCard),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Speaker,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CornerBadge(status: TerminalStatus, selected: Boolean) {
    val colors = AeroTheme.colors

    when {
        selected -> Box(
            modifier = Modifier
                .size(22.dp)
                .background(colors.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = "已选", tint = Color.White, modifier = Modifier.size(14.dp))
        }

        status == TerminalStatus.Fault -> Box(
            modifier = Modifier
                .size(18.dp)
                .background(colors.statusFault, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.PriorityHigh, contentDescription = "故障", tint = Color.White, modifier = Modifier.size(12.dp))
        }

        else -> {
            val dotColor = when (status) {
                TerminalStatus.Online  -> colors.statusOnline
                TerminalStatus.Offline -> colors.statusOffline
                TerminalStatus.Playing -> colors.statusPlaying
                TerminalStatus.Paging  -> colors.statusPaging
                TerminalStatus.Fault   -> colors.statusFault
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape)
                    .border(2.dp, colors.surface, CircleShape),
            )
        }
    }
}
