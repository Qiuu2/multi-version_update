package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.StatusPill
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * 130x130 terminal tile. Visual reflects [status]; [selected] overlays a
 * multi-select highlight. Fault terminals carry a danger border.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TerminalTile(
    name: String,
    status: TerminalStatus,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val shape = AeroTheme.shapes.md

    val container = when {
        selected -> colors.primaryContainer
        status == TerminalStatus.Fault -> colors.dangerContainer
        status == TerminalStatus.Offline -> colors.surfaceVariant
        else -> colors.surface
    }
    val borderColor = when {
        selected -> colors.primary
        status == TerminalStatus.Fault -> colors.danger
        else -> colors.outlineVariant
    }
    val iconTint = when (status) {
        TerminalStatus.Offline -> colors.onSurfaceVariant
        TerminalStatus.Fault -> colors.danger
        TerminalStatus.Playing, TerminalStatus.Paging -> colors.primary
        TerminalStatus.Online -> colors.secondary
    }

    Box(
        modifier = modifier
            .size(130.dp)
            .clip(shape)
            .background(container)
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(spacing.md),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Filled.Speaker,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = name,
                style = AeroTheme.typography.title,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Box(modifier = Modifier.weight(1f)) {}
            StatusPill(status = status)
        }

        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "已选择",
                tint = colors.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp),
            )
        }
    }
}
