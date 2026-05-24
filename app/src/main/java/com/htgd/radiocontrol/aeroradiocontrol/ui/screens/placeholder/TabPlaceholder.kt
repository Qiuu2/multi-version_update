package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.HeroStrip
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/** Generic tab placeholder used until each Tab's full screen lands in later phases. */
@Composable
fun TabPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AeroTheme.spacing.base),
        verticalArrangement = Arrangement.spacedBy(AeroTheme.spacing.base),
    ) {
        HeroStrip(title = title, subtitle = subtitle)
        Text(
            text = "敬请期待",
            style = AeroTheme.typography.titleLarge,
            color = AeroTheme.colors.onSurfaceVariant,
        )
    }
}
