package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroGradients
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * Hero strip — Handoff.html §03 · Hero.
 *
 * A gradient band used at the top of "destination" surfaces (Terminal Hub,
 * AI home). Hosts a small kicker label, a large title, and an optional inline
 * stats block (`.glass` in CSS — translucent white pill).
 *
 * Variants are driven by [brush]; pass [AeroGradients.Primary] / Warm / Night.
 */
@Composable
fun HeroStrip(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier,
    brush: Brush = AeroGradients.Primary,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.rTile)
            .background(brush)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Column {
            Text(
                text  = kicker,
                style = AeroTheme.typography.kicker.copy(color = Color.White.copy(alpha = 0.85f)),
            )
            Text(
                text  = title,
                style = AeroTheme.typography.topBar.copy(color = Color.White),
                modifier = Modifier.padding(top = 4.dp),
            )
            if (trailing != null) {
                Box(modifier = Modifier.padding(top = 12.dp)) { trailing() }
            }
        }
    }
}
