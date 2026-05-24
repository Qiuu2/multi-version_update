package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

@Composable
fun HeroStrip(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    gradient: Brush = AeroTheme.gradients.primary,
) {
    val spacing = AeroTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.lg)
            .background(gradient)
            .padding(horizontal = spacing.xl, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Text(text = title, style = AeroTheme.typography.headline, color = AeroTheme.colors.onPrimary)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = AeroTheme.typography.body,
                color = AeroTheme.colors.onPrimary.copy(alpha = 0.85f),
            )
        }
    }
}
