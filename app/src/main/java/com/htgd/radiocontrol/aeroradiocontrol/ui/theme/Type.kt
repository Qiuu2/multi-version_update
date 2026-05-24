package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 8 type levels. Read via [AeroTheme.typography]. */
@Immutable
class AeroTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val titleLarge: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle,
)

fun defaultAeroTypography(): AeroTypography {
    val family = FontFamily.Default
    return AeroTypography(
        display = TextStyle(
            fontFamily = family, fontWeight = FontWeight.Bold,
            fontSize = 28.sp, lineHeight = 34.sp,
        ),
        headline = TextStyle(
            fontFamily = family, fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp, lineHeight = 28.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = family, fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp, lineHeight = 24.sp,
        ),
        title = TextStyle(
            fontFamily = family, fontWeight = FontWeight.Medium,
            fontSize = 16.sp, lineHeight = 22.sp,
        ),
        body = TextStyle(
            fontFamily = family, fontWeight = FontWeight.Normal,
            fontSize = 14.sp, lineHeight = 20.sp,
        ),
        bodyStrong = TextStyle(
            fontFamily = family, fontWeight = FontWeight.Medium,
            fontSize = 14.sp, lineHeight = 20.sp,
        ),
        caption = TextStyle(
            fontFamily = family, fontWeight = FontWeight.Normal,
            fontSize = 12.sp, lineHeight = 16.sp,
        ),
        overline = TextStyle(
            fontFamily = family, fontWeight = FontWeight.Medium,
            fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp,
        ),
    )
}

val LocalAeroTypography = staticCompositionLocalOf { defaultAeroTypography() }
