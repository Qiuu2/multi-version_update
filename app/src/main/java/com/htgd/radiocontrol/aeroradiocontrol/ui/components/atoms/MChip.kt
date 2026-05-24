package com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroGradients
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * v4 chip — Handoff.html §03 · Chip.
 *
 * Two states:
 *   - default: white surface + 1px ring of [colors.lineStrong]
 *   - active : primary gradient fill (brand teal → cyan), white text
 *
 * Used for: zone filter rows, segmented controls, quick-pick tags.
 */
@Composable
fun MChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = AeroTheme.colors
    val shape  = AeroTheme.shapes.rChip
    val typo   = AeroTheme.typography

    val base = modifier
        .clip(shape)
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 8.dp)

    val chipModifier = if (active) {
        base
            .clip(shape)
            .background(AeroGradients.Primary)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    } else {
        base
            .background(colors.surface, shape)
            .border(1.dp, colors.lineStrong, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    }

    Row(
        modifier = chipModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leading != null) leading()
        Text(
            text  = label,
            style = typo.bodySmall,
            color = if (active) Color.White else colors.ink2,
        )
    }
}
