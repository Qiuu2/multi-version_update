package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * v4 TopBar — Handoff.html §03 · TopBar.
 *
 * Layout: padding (14h / 16top / 12bottom), 56dp tall.
 * Left:   optional back button.
 * Center: title (22sp/SemiBold).
 * Right:  connection-status dot + settings cog.
 */
@Composable
fun TopBarV4(
    title: String,
    modifier: Modifier = Modifier,
    connected: Boolean = true,
    onSettingsClick: () -> Unit = {},
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AeroTheme.colors
    val typo   = AeroTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .height(AeroTheme.spacing.topBarH)
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Leading slot (back button etc.) — fixed 40dp so the title stays optically centered.
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (leading != null) leading()
        }

        Text(
            text     = title,
            style    = typo.topBar.copy(fontWeight = FontWeight.SemiBold, color = colors.ink),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )

        ConnectionDot(connected = connected)

        if (trailing != null) {
            trailing()
        } else {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = colors.ink2,
                )
            }
        }
    }
}

@Composable
private fun ConnectionDot(connected: Boolean) {
    val colors = AeroTheme.colors
    val color  = if (connected) colors.statusOnline else colors.statusFault
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(10.dp)
            .background(color, CircleShape),
    )
}

