package com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

@Composable
fun MChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val shape = AeroTheme.shapes.pill
    val contentColor = if (active) colors.onPrimary else colors.onSurfaceVariant

    val base = Modifier
        .clip(shape)
        .then(
            if (active) Modifier.background(AeroTheme.gradients.primary)
            else Modifier.background(colors.surfaceVariant)
        )
        .clickable(onClick = onClick)
        .padding(horizontal = spacing.base, vertical = spacing.sm)

    Row(
        modifier = modifier.then(base),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(text = label, style = AeroTheme.typography.caption, color = contentColor)
    }
}
