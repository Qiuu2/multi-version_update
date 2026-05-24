package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * v4 motion tokens — Handoff.html §02 · Motion.
 *
 * | name           | duration ms | easing           | usage                       |
 * |----------------|-------------|------------------|-----------------------------|
 * | fabIn          | 220         | (.2,.7,.3,1)     | FAB-bar / action-bar slide  |
 * | tabBadgePulse  | 2200        | ease-out · loop  | unread / fault dot pulse    |
 * | mPulse         | 1500        | linear · loop    | calling / paging ring       |
 * | wave           | 900         | ease-in-out      | playing bars                |
 * | skel           | 1600        | linear · loop    | skeleton sweep              |
 * | tap            | 130         | ease             | chip / tile press feedback  |
 */
data class AeroMotion(
    val fabInMs: Int        = 220,
    val tapMs: Int          = 130,
    val pulseMs: Int        = 1500,
    val tabBadgePulseMs: Int = 2200,
    val waveMs: Int         = 900,
    val skelMs: Int         = 1600,

    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1.0f),
    val standardEasing:   Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
)
