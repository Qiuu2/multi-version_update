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
internal val BgBeige     = Color(0xFFF0EEE9) // 米色背景 — AeroRadio v4.html line 11/200; spec §1.1
internal val Surface1    = Color(0xFFFFFFFF)
internal val Surface2    = Color(0xFFF8FAFB)
internal val Surface3    = Color(0xFFEEF0F3)
internal val Ink         = Color(0xFF0D1117)
internal val Ink2        = Color(0xFF4A5260)
internal val Ink3        = Color(0xFF8A929F)
internal val Ink4        = Color(0xFFB8BEC8)
// Derived neutrals (ICD-DesignTokens-v1.1 §1.5, R-3): line/lineStrong are Ink alpha tints.
internal val Line        = Color(0x0F0D1117) // derived: Ink @ 6% alpha  — rgba(13,17,23,0.06)
internal val LineStrong  = Color(0x1F0D1117) // derived: Ink @ 12% alpha — rgba(13,17,23,0.12)
internal val Divider     = Color(0xFFE8EBEF) // neutral hairline between bg(F4F5F7) and surface-3(EEF0F3)

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

// ── Status soft backgrounds for pills (ICD-DesignTokens-v1.1 §1.5, R-3) ──────
// Each is a light tint of the matching status base color (same hue, no new hue):
internal val StatusOnlineSoft  = Color(0xFFE8F7EC) // ← StatusOnline  #16A34A tint
internal val StatusOfflineSoft = Color(0xFFEEF0F3) // ← StatusOffline #8A929F tint (= surface-3)
internal val StatusFaultSoft   = Color(0xFFFDECEC) // ← StatusFault   #DC2626 tint
internal val StatusPlayingSoft = Color(0xFFE8EFFD) // ← StatusPlaying #2563EB tint
internal val StatusPagingSoft  = Color(0xFFFDEEE2) // ← StatusPaging  #EA580C tint

// ── Task migration / swap accent (ICD-DesignTokens-v1.2) ─────────────────────
// 任务 迁移/对调 use a gold accent per Handoff §任务; refactor has no gold token.
internal val Gold     = Color(0xFFA8780A) // 迁移/对调 accent (border + tag fg)
internal val GoldSoft = Color(0xFFFAF0CC) // ← Gold soft tint (tag pill bg)

/**
 * Public color palette used throughout the v4 UI.
 *
 * Designed as an immutable data class so it can be supplied via a
 * CompositionLocal — see [LocalAeroColors] and [AeroTheme].
 */
data class AeroColors(
    val bg: Color = Bg,
    val bgBeige: Color = BgBeige,
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

    val gold: Color = Gold,
    val goldSoft: Color = GoldSoft,

    // ── ICD-DesignTokens-v1.3 semantic-role aliases (additive, non-breaking) ──
    // Per BL-TOKEN-RENAME + AR-009 R-1 alias pattern: same hex as existing
    // status / brand tokens, new semantic role names so screens reference
    // role-by-purpose ("modePaging" for the broadcast 寻呼 segmented control)
    // rather than reusing a status-pill name ("statusPaging") for a different
    // semantic surface. Zero new hex; old names stay valid — consumers migrate
    // at leisure. Spec citations as comments.

    // Broadcast 3-mode (Handoff.html:883-885 §s-broadcast 三档差异):
    val modePaging:   Color = PageWarm,   // = #EA580C — 寻呼
    val modeIntercom: Color = TalkBlue,   // = #2563EB — 对讲
    val modeCast:     Color = Primary,    // = #0E7C70 — 点播 (shares brand teal)

    // Terminal tile 5-state foreground (Handoff.html:621-658 §components):
    val tileOnline:   Color = StatusOnline,   // = #16A34A
    val tileOffline:  Color = StatusOffline,  // = #8A929F
    val tileFault:    Color = StatusFault,    // = #DC2626
    val tilePlaying:  Color = StatusPlaying,  // = #2563EB
    val tilePaging:   Color = StatusPaging,   // = #EA580C
    // Tile icon-bg soft tints (Handoff.html:625-657 — bg colors match v1.1
    // *Soft palette; aliased for parallel role-naming):
    val tileOnlineSoft:  Color = PrimarySoft,       // online icon bg → primary-soft
    val tileOfflineSoft: Color = Surface3,          // offline icon bg → surface-3
    val tileFaultSoft:   Color = StatusFaultSoft,   // = #FDECEC
    val tilePlayingSoft: Color = StatusPlayingSoft, // = #E8EFFD
    val tilePagingSoft:  Color = StatusPagingSoft,  // = #FDEEE2

    // Task-card state pill (PM-confirmed naming, P2). Handoff.html:930-931
    // §s-task names the task card states 已完成/进行中/待执行/已取消/已迁移/对调
    // but does NOT pin task-pill hex — this 3-alias set is a DOCUMENTED
    // ASSUMPTION, mapped to existing tokens so hex stays consistent across
    // the consumer surfaces; also reused for scheme-list 启用/停用 since the
    // semantic is the same. Hex re-source if CTO real-device review pushes back
    // (alias level absorbs the swap — consumers don't change).
    val taskCardStateDone:    Color = Ink3,         // = #8A929F — 已完成 / disabled-row text
    val taskCardStateRunning: Color = StatusPaging, // = #EA580C — 进行中 (matches TaskScreen.kt:205 existing in-code usage)
    val taskCardStatePending: Color = Ink2,         // = #4A5260 — 待执行 (preliminary, awaiting CTO real-device review)
)
