package com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

enum class MButtonVariant { Filled, Tonal, Outline, Text, Danger, Success }

private class MButtonColors(
    val container: Color,
    val content: Color,
    val border: Color?,
)

@Composable
private fun colorsFor(variant: MButtonVariant): MButtonColors {
    val c = AeroTheme.colors
    return when (variant) {
        MButtonVariant.Filled -> MButtonColors(c.primary, c.onPrimary, null)
        MButtonVariant.Tonal -> MButtonColors(c.primaryContainer, c.primaryDark, null)
        MButtonVariant.Outline -> MButtonColors(Color.Transparent, c.primary, c.outline)
        MButtonVariant.Text -> MButtonColors(Color.Transparent, c.primary, null)
        MButtonVariant.Danger -> MButtonColors(c.danger, c.onPrimary, null)
        MButtonVariant.Success -> MButtonColors(c.success, c.onPrimary, null)
    }
}

@Composable
fun MButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MButtonVariant = MButtonVariant.Filled,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val palette = colorsFor(variant)
    val shape: RoundedCornerShape = AeroTheme.shapes.sm
    val spacing = AeroTheme.spacing
    val alpha = if (enabled) 1f else 0.38f

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(palette.container.copy(alpha = palette.container.alpha * alpha))
            .then(
                if (palette.border != null)
                    Modifier.border(1.dp, palette.border.copy(alpha = alpha), shape)
                else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = spacing.xl, vertical = spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = palette.content.copy(alpha = alpha),
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = text,
            style = AeroTheme.typography.bodyStrong,
            color = palette.content.copy(alpha = alpha),
        )
    }
}
