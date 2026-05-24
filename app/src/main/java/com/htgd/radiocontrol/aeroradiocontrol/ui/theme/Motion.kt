package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/** 6 motion tokens (3 durations + 3 easing curves). Read via [AeroTheme.motion]. */
@Immutable
class AeroMotion(
    val durationFast: Int,
    val durationStandard: Int,
    val durationSlow: Int,
    val easingStandard: Easing,
    val easingEmphasized: Easing,
    val easingDecelerate: Easing,
)

fun defaultAeroMotion(): AeroMotion = AeroMotion(
    durationFast = 120,
    durationStandard = 220,
    durationSlow = 320,
    easingStandard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    easingEmphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f),
    easingDecelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f),
)

val LocalAeroMotion = staticCompositionLocalOf { defaultAeroMotion() }
