package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 11 semantic spacing steps. Read via [AeroTheme.spacing]. */
@Immutable
class AeroSpacing(
    val none: Dp,
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val base: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val huge: Dp,
    val section: Dp,
)

fun defaultAeroSpacing(): AeroSpacing = AeroSpacing(
    none = 0.dp,
    xxs = 2.dp,
    xs = 4.dp,
    sm = 8.dp,
    md = 12.dp,
    base = 16.dp,
    lg = 20.dp,
    xl = 24.dp,
    xxl = 32.dp,
    huge = 40.dp,
    section = 56.dp,
)

val LocalAeroSpacing = staticCompositionLocalOf { defaultAeroSpacing() }
