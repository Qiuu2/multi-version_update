package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Theme entry point. Wrap the root composable of every Compose screen with
 * [AeroTheme] so that the [AeroColors] / [AeroTypography] / [AeroShapes] /
 * [AeroSpacing] / [AeroElevation] / [AeroMotion] are available via the
 * `AeroTheme.xxx` accessors defined at the bottom.
 *
 * Example:
 * ```
 * setContent {
 *   AeroTheme {
 *     SplashScreen()
 *   }
 * }
 * ```
 *
 * `MaterialTheme.colorScheme` is also wired up so that any Material 3 component
 * (e.g. `Button`, `TextField`) inherits brand colors without extra config.
 */

val LocalAeroColors     = staticCompositionLocalOf { AeroColors() }
val LocalAeroTypography = staticCompositionLocalOf { AeroTypography() }
val LocalAeroShapes     = staticCompositionLocalOf { AeroShapes() }
val LocalAeroSpacing    = staticCompositionLocalOf { AeroSpacing() }
val LocalAeroElevation  = staticCompositionLocalOf { AeroElevation() }
val LocalAeroMotion     = staticCompositionLocalOf { AeroMotion() }

@Composable
fun AeroTheme(content: @Composable () -> Unit) {
    val colors     = AeroColors()
    val typography = AeroTypography()
    val shapes     = AeroShapes()
    val spacing    = AeroSpacing()
    val elevation  = AeroElevation()
    val motion     = AeroMotion()

    // Bridge Aero tokens into Material 3 so plain Material components inherit the brand.
    val materialColors = lightColorScheme(
        primary           = colors.primary,
        onPrimary          = Surface1,
        primaryContainer   = colors.primarySoft,
        onPrimaryContainer = colors.primaryInk,
        secondary          = colors.taskPurple,
        background         = colors.bg,
        onBackground       = colors.ink,
        surface            = colors.surface,
        onSurface          = colors.ink,
        surfaceVariant     = colors.surface2,
        onSurfaceVariant   = colors.ink2,
        outline            = colors.divider,
        outlineVariant     = colors.line,
        error              = colors.statusFault,
        onError            = Surface1,
    )

    CompositionLocalProvider(
        LocalAeroColors     provides colors,
        LocalAeroTypography provides typography,
        LocalAeroShapes     provides shapes,
        LocalAeroSpacing    provides spacing,
        LocalAeroElevation  provides elevation,
        LocalAeroMotion     provides motion,
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography  = aeroToMaterial3Typography(typography),
            shapes      = aeroToMaterial3Shapes(shapes),
            content     = content,
        )
    }
}

/** Convenience accessors so screen code can write `AeroTheme.colors.primary`. */
object AeroTheme {
    val colors: AeroColors
        @Composable @ReadOnlyComposable get() = LocalAeroColors.current
    val typography: AeroTypography
        @Composable @ReadOnlyComposable get() = LocalAeroTypography.current
    val shapes: AeroShapes
        @Composable @ReadOnlyComposable get() = LocalAeroShapes.current
    val spacing: AeroSpacing
        @Composable @ReadOnlyComposable get() = LocalAeroSpacing.current
    val elevation: AeroElevation
        @Composable @ReadOnlyComposable get() = LocalAeroElevation.current
    val motion: AeroMotion
        @Composable @ReadOnlyComposable get() = LocalAeroMotion.current
}
