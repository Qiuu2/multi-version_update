package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 5 corner radii. Read via [AeroTheme.shapes]. */
@Immutable
class AeroShapes(
    val xs: RoundedCornerShape,
    val sm: RoundedCornerShape,
    val md: RoundedCornerShape,
    val lg: RoundedCornerShape,
    val pill: RoundedCornerShape,
    // Raw radii, for cases that need a Dp (e.g. custom draw).
    val xsRadius: Dp,
    val smRadius: Dp,
    val mdRadius: Dp,
    val lgRadius: Dp,
)

fun defaultAeroShapes(): AeroShapes = AeroShapes(
    xs = RoundedCornerShape(8.dp),
    sm = RoundedCornerShape(12.dp),
    md = RoundedCornerShape(16.dp),
    lg = RoundedCornerShape(24.dp),
    pill = RoundedCornerShape(percent = 50),
    xsRadius = 8.dp,
    smRadius = 12.dp,
    mdRadius = 16.dp,
    lgRadius = 24.dp,
)

val LocalAeroShapes = staticCompositionLocalOf { defaultAeroShapes() }
