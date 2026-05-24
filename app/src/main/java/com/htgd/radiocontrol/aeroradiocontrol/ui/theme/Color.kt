package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design-token color palette for the v4 UI (terminal-centric IA).
 * 24 semantic tokens. Never hardcode a Color in UI code — read [AeroTheme.colors].
 */
@Immutable
class AeroColorScheme(
    // Brand
    val primary: Color,
    val primaryDark: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    // Surfaces / backgrounds
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceDim: Color,
    val nightSurface: Color,
    // Content
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    // Lines
    val outline: Color,
    val outlineVariant: Color,
    // Status
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val info: Color,
    val scrim: Color,
)

fun lightAeroColors(): AeroColorScheme = AeroColorScheme(
    primary = Color(0xFF3986F9),
    primaryDark = Color(0xFF2563EB),
    primaryContainer = Color(0xFFDCE9FF),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF00A0E9),
    secondaryContainer = Color(0xFFD9F2FF),
    background = Color(0xFFF5F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF2F8),
    surfaceDim = Color(0xFFE4E9F2),
    nightSurface = Color(0xFF0F1830),
    onBackground = Color(0xFF1A2233),
    onSurface = Color(0xFF1A2233),
    onSurfaceVariant = Color(0xFF5B6473),
    outline = Color(0xFFD5DCE6),
    outlineVariant = Color(0xFFE8EDF3),
    success = Color(0xFF22C55E),
    successContainer = Color(0xFFDCFCE7),
    warning = Color(0xFFF59E0B),
    warningContainer = Color(0xFFFEF3C7),
    danger = Color(0xFFEF4444),
    dangerContainer = Color(0xFFFEE2E2),
    info = Color(0xFF3B82F6),
    scrim = Color(0x99000000),
)

val LocalAeroColors = staticCompositionLocalOf { lightAeroColors() }
