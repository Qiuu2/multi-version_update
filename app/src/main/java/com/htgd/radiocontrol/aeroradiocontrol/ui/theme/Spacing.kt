package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * v4 spacing tokens — Handoff.html §02 · Spacing (8px base).
 *
 * Semantic names (e.g. [pageH], [tileGap]) are preferred over raw values
 * (e.g. 14.dp). When you find yourself reaching for a literal dp value,
 * check whether it should become a token here first.
 */
data class AeroSpacing(
    /** Page horizontal padding (left/right). */
    val pageH: Dp        = 16.dp,
    /** Section vertical padding between blocks. */
    val sectionV: Dp     = 12.dp,
    /** Card / list-item interior padding. */
    val cardPad: Dp      = 14.dp,
    /** Gap between tiles in the 3-column terminal grid. */
    val tileGap: Dp      = 10.dp,
    /** Button vertical padding. */
    val btnPadV: Dp      = 10.dp,
    /** Button horizontal padding. */
    val btnPadH: Dp      = 18.dp,
    /** TopBar height (incl. status bar safe inset rendering). */
    val topBarH: Dp      = 56.dp,
    /** TabBar height (incl. raised center safe area). */
    val tabBarH: Dp      = 80.dp,
    /** Raised tab item vertical offset (中央 AI 抬起). */
    val tabRaise: Dp     = 16.dp,
    /** Common 8px-grid values for ad-hoc use. */
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)
