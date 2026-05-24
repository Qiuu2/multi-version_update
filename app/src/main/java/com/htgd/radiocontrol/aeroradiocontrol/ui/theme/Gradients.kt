package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Brand gradient brushes. Read via [AeroTheme.gradients]. */
@Immutable
class AeroGradients(
    val primary: Brush,
    val warm: Brush,
    val night: Brush,
)

fun defaultAeroGradients(colors: AeroColorScheme): AeroGradients = AeroGradients(
    primary = Brush.linearGradient(
        listOf(colors.primary, colors.secondary),
    ),
    warm = Brush.linearGradient(
        listOf(Color(0xFFFF8A4C), Color(0xFFF59E0B)),
    ),
    night = Brush.linearGradient(
        listOf(Color(0xFF1B2956), colors.nightSurface),
    ),
)

val LocalAeroGradients = staticCompositionLocalOf { defaultAeroGradients(lightAeroColors()) }
