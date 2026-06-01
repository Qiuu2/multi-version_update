package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.htgd.radiocontrol.aeroradiocontrol.R

/**
 * v4 type scale — Handoff.html §02 · Typography (line 547 "Noto Sans SC · JetBrains Mono").
 *
 * Two families, vendored as subset TTFs under `res/font/` per ICD-DesignTokens-v1.3 /
 * BL-FONT-ASSETS (CTO Q2=(ii) Subset GO, 2026-05-30):
 *   - Sans  → Noto Sans SC, 3 static weights (Regular 400 / Medium 500 / Bold 700)
 *            instanced from the upstream variable-font master, subset to:
 *              GB2312 L1+L2 (6763) ∪ project chars (Handoff.html + AeroRadio v4.html
 *              + .kt/.java) ∪ ASCII (U+0020-007E) ∪ CJK punctuation (U+3000-303F)
 *              ∪ fullwidth forms (U+FF00-FFEF).
 *            ≈ 2.25 MB per weight (3 weights total ≈ 6.75 MB).
 *   - Mono  → JetBrains Mono Regular + Medium, subset to ASCII + Latin-1
 *            extended (no CJK — only used for [kicker] / [numeric] tabular figures).
 *            ≈ 67 KB per weight.
 *
 * Total +6.7 MB APK growth. License compliance via `app/licenses/{NotoSansSC,JetBrainsMono}-OFL.txt`
 * (kept out of res/font because Android resource validation only accepts .ttf/.otf/.xml).
 *
 * Subset-miss strategy: if a real-device demo shows a 方块字 for some character not in
 * the subset, fix = append the char to the source corpus + re-subset + re-commit.
 * Expected workflow, not a bug.
 *
 * Numbers use `Mono` + `tabular-nums`. The monospace family already fixes glyph
 * advance, and [numeric] additionally sets `fontFeatureSettings = "tnum"` so the
 * tabular-figure feature is requested explicitly per design-system-spec §4 — this
 * keeps proportional-digit fallback fonts (if [AeroMono] is ever swapped) honest.
 */

internal val AeroSans: FontFamily = FontFamily(
    Font(R.font.noto_sans_sc_regular, FontWeight.Normal),
    Font(R.font.noto_sans_sc_medium,  FontWeight.Medium),
    Font(R.font.noto_sans_sc_bold,    FontWeight.Bold),
)

internal val AeroMono: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium,  FontWeight.Medium),
)

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
