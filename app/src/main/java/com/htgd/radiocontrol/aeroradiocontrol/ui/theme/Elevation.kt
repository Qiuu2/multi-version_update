package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v4 elevation tokens — Handoff.html §02 · Elevation.
 *
 * Compose uses [Dp] elevation for the default `shadow()` modifier; multi-layer
 * box-shadows from the CSS spec are approximated by the dp values design-system-spec
 * §5 lists as the M3 equivalents (e1≈1, e2≈2, e3≈8, fab≈6). We track those rather
 * than the raw CSS blur radii — dp elevation and CSS box-shadow do not map 1:1.
 * If exact shadow parity is ever needed, switch to drawShadow + custom RenderEffect.
 */
data class AeroElevation(
    /** e1 — static card. */
    val e1: Dp   = 1.dp,
    /** e2 — hovered / floating card. */
    val e2: Dp   = 2.dp,
    /** e3 — modal / sheet. */
    val e3: Dp   = 8.dp,
    /** FAB — primary action. */
    val fab: Dp  = 6.dp,
)
