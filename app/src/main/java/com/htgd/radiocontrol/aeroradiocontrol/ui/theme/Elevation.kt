package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4 elevation levels. Read via [AeroTheme.elevation]. */
@Immutable
class AeroElevation(
    val level0: Dp,
    val level1: Dp,
    val level2: Dp,
    val level3: Dp,
)

fun defaultAeroElevation(): AeroElevation = AeroElevation(
    level0 = 0.dp,
    level1 = 2.dp,
    level2 = 6.dp,
    level3 = 12.dp,
)

val LocalAeroElevation = staticCompositionLocalOf { defaultAeroElevation() }
