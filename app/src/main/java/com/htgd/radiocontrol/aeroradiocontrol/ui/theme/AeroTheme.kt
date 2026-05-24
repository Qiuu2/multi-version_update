package com.htgd.radiocontrol.aeroradiocontrol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/**
 * Root theme for the v4 UI. Wraps [MaterialTheme] and exposes the design tokens
 * through the [AeroTheme] accessor object. All UI must read colors / spacing /
 * shapes / type / elevation / motion / gradients from [AeroTheme.*] — never hardcode.
 */
@Composable
fun AeroTheme(content: @Composable () -> Unit) {
    val colors = remember { lightAeroColors() }
    val typography = remember { defaultAeroTypography() }
    val shapes = remember { defaultAeroShapes() }
    val spacing = remember { defaultAeroSpacing() }
    val elevation = remember { defaultAeroElevation() }
    val motion = remember { defaultAeroMotion() }
    val gradients = remember(colors) { defaultAeroGradients(colors) }

    val materialColorScheme = remember(colors) {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryContainer,
            onPrimaryContainer = colors.primaryDark,
            secondary = colors.secondary,
            onSecondary = colors.onPrimary,
            secondaryContainer = colors.secondaryContainer,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant,
            onSurfaceVariant = colors.onSurfaceVariant,
            outline = colors.outline,
            outlineVariant = colors.outlineVariant,
            error = colors.danger,
            onError = colors.onPrimary,
            errorContainer = colors.dangerContainer,
            scrim = colors.scrim,
        )
    }

    val materialTypography = remember(typography) {
        Typography(
            headlineLarge = typography.display,
            headlineMedium = typography.headline,
            titleLarge = typography.titleLarge,
            titleMedium = typography.title,
            bodyLarge = typography.body,
            bodyMedium = typography.body,
            labelLarge = typography.bodyStrong,
            labelMedium = typography.caption,
            labelSmall = typography.overline,
        )
    }

    CompositionLocalProvider(
        LocalAeroColors provides colors,
        LocalAeroTypography provides typography,
        LocalAeroShapes provides shapes,
        LocalAeroSpacing provides spacing,
        LocalAeroElevation provides elevation,
        LocalAeroMotion provides motion,
        LocalAeroGradients provides gradients,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = materialTypography,
            content = content,
        )
    }
}

object AeroTheme {
    val colors: AeroColorScheme
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
    val gradients: AeroGradients
        @Composable @ReadOnlyComposable get() = LocalAeroGradients.current
}
