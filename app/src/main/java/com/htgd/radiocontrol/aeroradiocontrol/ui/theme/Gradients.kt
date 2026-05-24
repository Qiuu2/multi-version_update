package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * v4 gradients — Handoff.html §02 · Gradients (decoration only).
 *
 * Use sparingly: hero strips, FAB, Splash background, full-screen incoming call.
 * Never apply to text containers (degrades CJK readability).
 */
object AeroGradients {

    /** Brand gradient: teal → bright teal → cyan. FAB / hero / raised Tab. */
    val Primary: Brush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFF0E7C70),
            0.6f to Color(0xFF14B8A6),
            1.0f to Color(0xFF06B6D4),
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    /** Warm gradient: deep orange → orange → light orange. 寻呼 mode. */
    val Warm: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFEA580C),
            Color(0xFFF97316),
            Color(0xFFFB923C),
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    /** Night gradient: near-black → teal. AI tab / full-screen incoming call. */
    val Night: Brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFF0D2826),
            0.8f to Color(0xFF0E7C70),
            1.0f to Color(0xFF14B8A6),
        ),
    )
}
