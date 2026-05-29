package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * v4 type scale — Handoff.html §02 · Typography.
 *
 * Two families:
 *   - Sans  → Noto Sans SC (CJK + Latin). We currently use [FontFamily.Default];
 *            the system shipped Source Han Sans / Noto Sans CJK renders correctly.
 *            To pin Noto Sans SC exactly, drop the .otf into res/font/ and swap.
 *   - Mono  → JetBrains Mono. Same story — [FontFamily.Monospace] is the
 *            placeholder; res/font swap upgrades to JetBrains Mono.
 *
 * Numbers use `Mono` + `tabular-nums`. The monospace family already fixes glyph
 * advance, and [numeric] additionally sets `fontFeatureSettings = "tnum"` so the
 * tabular-figure feature is requested explicitly per design-system-spec §4 — this
 * keeps proportional-digit fallback fonts (if [AeroMono] is ever swapped) honest.
 */

internal val AeroSans: FontFamily = FontFamily.Default
internal val AeroMono: FontFamily = FontFamily.Monospace

/** Reusable typography building blocks (named after Handoff.html type-row labels). */
data class AeroTypography(
    val display:    TextStyle = TextStyle(fontFamily = AeroSans, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    val topBar:     TextStyle = TextStyle(fontFamily = AeroSans, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    val sectionTitle: TextStyle = TextStyle(fontFamily = AeroSans, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    val bodyLarge:  TextStyle = TextStyle(fontFamily = AeroSans, fontSize = 15.sp, fontWeight = FontWeight.Medium),
    val body:       TextStyle = TextStyle(fontFamily = AeroSans, fontSize = 14.sp, fontWeight = FontWeight.Normal),
    val bodySmall:  TextStyle = TextStyle(fontFamily = AeroSans, fontSize = 13.sp, fontWeight = FontWeight.Normal),
    val kicker:     TextStyle = TextStyle(fontFamily = AeroMono, fontSize = 11.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.6.sp),
    val numeric:    TextStyle = TextStyle(fontFamily = AeroMono, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
    val label:      TextStyle = TextStyle(fontFamily = AeroSans, fontSize = 12.sp, fontWeight = FontWeight.Medium),
    val button:     TextStyle = TextStyle(fontFamily = AeroSans, fontSize = 14.sp, fontWeight = FontWeight.Medium),
)

/** Material 3 [Typography] bridge — wires our tokens into MaterialTheme.typography. */
internal fun aeroToMaterial3Typography(t: AeroTypography): Typography = Typography(
    displayLarge   = t.display,
    headlineMedium = t.topBar,
    titleLarge     = t.sectionTitle,
    titleMedium    = t.bodyLarge,
    bodyLarge      = t.body,
    bodyMedium     = t.bodySmall,
    labelLarge     = t.button,
    labelMedium    = t.label,
    labelSmall     = t.kicker,
)
