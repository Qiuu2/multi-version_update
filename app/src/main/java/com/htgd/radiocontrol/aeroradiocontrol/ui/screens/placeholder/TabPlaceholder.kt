package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * Generic placeholder used by tasks 6–9 sub-screens before they are built out.
 *
 * Shown as the body of a Tab in the main scaffold; the host's [TopBarV4] +
 * [TabBarV4] stay around it. Once a real screen replaces this, simply delete
 * the call site in [AppNavGraph].
 */
@Composable
fun TabPlaceholder(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.ink4,
                modifier = Modifier.size(72.dp),
            )
            Text(
                text  = title,
                style = AeroTheme.typography.sectionTitle,
                color = colors.ink2,
            )
            Text(
                text  = subtitle,
                style = AeroTheme.typography.bodySmall,
                color = colors.ink3,
            )
        }
    }
}
