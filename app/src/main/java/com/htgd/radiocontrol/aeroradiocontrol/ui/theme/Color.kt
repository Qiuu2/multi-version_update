package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * v4 color tokens — mirror modern.css.
 *
 * SOURCE OF TRUTH: Handoff.html §02 · Design Tokens · Colors.
 * Do NOT hardcode hex values in screens; always reference [AeroColors] via
 * [LocalAeroColors] or [MaterialTheme] extension.
 */

// ── Surfaces & neutrals ─────────────────────────────────────────────────────
internal val Bg          = Color(0xFFF4F5F7)
internal val Surface1    = Color(0xFFFFFFFF)
internal val Surface2    = Color(0xFFF8FAFB)
internal val Surface3    = Color(0xFFEEF0F3)
internal val Ink         = Color(0xFF0D1117)
internal val Ink2        = Color(0xFF4A5260)
internal val Ink3        = Color(0xFF8A929F)
internal val Ink4        = Color(0xFFB8BEC8)
internal val Line        = Color(0x0F0D1117) // rgba(13,17,23,0.06)
internal val LineStrong  = Color(0x1F0D1117) // rgba(13,17,23,0.12)
internal val Divider     = Color(0xFFE8EBEF)

// ── Brand ───────────────────────────────────────────────────────────────────
internal val Primary     = Color(0xFF0E7C70)
internal val PrimaryInk  = Color(0xFF095C54)
internal val PrimarySoft = Color(0xFFE6F4F2)

// ── Per-tab accents (segment colors) ────────────────────────────────────────
internal val PageWarm    = Color(0xFFEA580C) // 寻呼
internal val TalkBlue    = Color(0xFF2563EB) // 对讲
internal val TaskPurple  = Color(0xFF7C3AED) // 任务
internal val AITeal      = Color(0xFF14B8A6) // AI
internal val ServiceBlue = Color(0xFF2563EB) // 服务

// ── Status ──────────────────────────────────────────────────────────────────
internal val StatusOnline  = Color(0xFF16A34A)
internal val StatusOffline = Color(0xFF8A929F)
internal val StatusFault   = Color(0xFFDC2626)
internal val StatusPlaying = Color(0xFF2563EB)
internal val StatusPaging  = Color(0xFFEA580C)

// ── Status soft backgrounds (for pills) ─────────────────────────────────────
internal val StatusOnlineSoft  = Color(0xFFE8F7EC)
internal val StatusOfflineSoft = Color(0xFFEEF0F3)
internal val StatusFaultSoft   = Color(0xFFFDECEC)
internal val StatusPlayingSoft = Color(0xFFE8EFFD)
internal val StatusPagingSoft  = Color(0xFFFDEEE2)

/**
 * Public color palette used throughout the v4 UI.
 *
 * Designed as an immutable data class so it can be supplied via a
 * CompositionLocal — see [LocalAeroColors] and [AeroTheme].
 */
data class AeroColors(
    val bg: Color = Bg,
    val surface: Color = Surface1,
    val surface2: Color = Surface2,
    val surface3: Color = Surface3,
    val ink: Color = Ink,
    val ink2: Color = Ink2,
    val ink3: Color = Ink3,
    val ink4: Color = Ink4,
    val line: Color = Line,
    val lineStrong: Color = LineStrong,
    val divider: Color = Divider,

    val primary: Color = Primary,
    val primaryInk: Color = PrimaryInk,
    val primarySoft: Color = PrimarySoft,

    val pageWarm: Color = PageWarm,
    val talkBlue: Color = TalkBlue,
    val taskPurple: Color = TaskPurple,
    val aiTeal: Color = AITeal,
    val serviceBlue: Color = ServiceBlue,

    val statusOnline: Color = StatusOnline,
    val statusOffline: Color = StatusOffline,
    val statusFault: Color = StatusFault,
    val statusPlaying: Color = StatusPlaying,
    val statusPaging: Color = StatusPaging,

    val statusOnlineSoft: Color = StatusOnlineSoft,
    val statusOfflineSoft: Color = StatusOfflineSoft,
    val statusFaultSoft: Color = StatusFaultSoft,
    val statusPlayingSoft: Color = StatusPlayingSoft,
    val statusPagingSoft: Color = StatusPagingSoft,
)
