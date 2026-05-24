package com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * v4 button variants — Handoff.html §03 · Buttons.
 *
 * | variant   | usage                                   |
 * |-----------|------------------------------------------|
 * | Filled    | primary CTA (0–1 per screen)            |
 * | Tonal     | secondary action / form cancel          |
 * | Outline   | form reset / view toggle                |
 * | Text      | "view more" / "learn details"           |
 * | Danger    | stop paging / delete schedule           |
 * | Success   | large confirm (二次确认页)              |
 *
 * Shape: 999dp (pill). Min height 40dp via [defaultMinSize] + padding tokens.
 */
enum class MButtonVariant { Filled, Tonal, Outline, Text, Danger, Success }

@Composable
fun MButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MButtonVariant = MButtonVariant.Filled,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors  = AeroTheme.colors
    val shape   = AeroTheme.shapes.rChip
    val spacing = AeroTheme.spacing
    val typo    = AeroTheme.typography

    // Decode container / text / border per variant.
    val container = when (variant) {
        MButtonVariant.Filled  -> colors.primary
        MButtonVariant.Tonal   -> colors.primarySoft
        MButtonVariant.Outline -> Color.Transparent
        MButtonVariant.Text    -> Color.Transparent
        MButtonVariant.Danger  -> colors.statusFault
        MButtonVariant.Success -> colors.statusOnline
    }
    val contentColor = when (variant) {
        MButtonVariant.Filled, MButtonVariant.Danger, MButtonVariant.Success -> Color.White
        MButtonVariant.Tonal   -> colors.primaryInk
        MButtonVariant.Outline -> colors.ink
        MButtonVariant.Text    -> colors.primaryInk
    }
    val showBorder = variant == MButtonVariant.Outline

    // Build modifier chain in one expression so order is obvious.
    val rowModifier = modifier
        .clip(shape)
        .background(container)
        .let { if (showBorder) it.border(1.dp, colors.lineStrong, shape) else it }
        .clickable(enabled = enabled, onClick = onClick)
        .defaultMinSize(minHeight = 40.dp)
        .padding(horizontal = spacing.btnPadH, vertical = spacing.btnPadV)
        .alpha(if (enabled) 1f else 0.5f)

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            if (leading != null) leading()
            Text(text = text, style = typo.button, color = contentColor)
            if (trailing != null) trailing()
        }
    }
}
