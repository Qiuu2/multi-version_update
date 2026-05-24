package com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * v4 switch — Handoff.html §03 · Switch.
 *
 * Wraps Material 3 [Switch] so brand colors are inherited automatically.
 * The handoff spec (52×32) matches Material 3 defaults, so we don't override
 * the geometry.
 */
@Composable
fun MSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AeroTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor   = androidx.compose.ui.graphics.Color.White,
            checkedTrackColor   = colors.primary,
            uncheckedThumbColor = androidx.compose.ui.graphics.Color.White,
            uncheckedTrackColor = colors.ink4,
            uncheckedBorderColor = colors.ink4,
        ),
    )
}
