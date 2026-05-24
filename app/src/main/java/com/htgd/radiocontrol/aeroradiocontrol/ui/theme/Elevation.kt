package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v4 elevation tokens — Handoff.html §02 · Elevation.
 *
 * Compose uses [Dp] elevation for the default `shadow()` modifier; multi-layer
 * box-shadows from the CSS spec are approximated by picking the largest blur.
 * If exact parity is needed, switch to drawShadow + custom RenderEffect.
 */
data class AeroElevation(
    /** e1 — static card. */
    val e1: Dp   = 1.dp,
    /** e2 — hovered / floating card. */
    val e2: Dp   = 4.dp,
    /** e3 — modal / sheet. */
    val e3: Dp   = 12.dp,
    /** FAB — primary action. */
    val fab: Dp  = 6.dp,
)
