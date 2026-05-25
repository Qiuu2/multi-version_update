package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/** Animated shimmer brush that sweeps left-to-right on a 1.6s loop. */
@Composable
fun rememberShimmerBrush(): Brush {
    val colors = AeroTheme.colors
    val band = listOf(
        colors.surfaceVariant,
        colors.surface,
        colors.surfaceVariant,
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "translate",
    )
    return Brush.linearGradient(
        colors = band,
        start = Offset(translate - 400f, 0f),
        end = Offset(translate, 0f),
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.clip(shape).background(rememberShimmerBrush()),
    )
}

/** Reference skeleton for the terminal hub: a few zone headers + tile grids. */
@Composable
fun TerminalHubSkeleton(modifier: Modifier = Modifier) {
    val spacing = AeroTheme.spacing
    Column(
        modifier = modifier.fillMaxWidth().padding(spacing.base),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        repeat(3) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(56.dp), shape = AeroTheme.shapes.md)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                SkeletonBox(modifier = Modifier.size(130.dp), shape = AeroTheme.shapes.md)
                SkeletonBox(modifier = Modifier.size(130.dp), shape = AeroTheme.shapes.md)
            }
            Spacer(modifier = Modifier.height(spacing.xs))
        }
    }
}

/** Generic single-line skeleton, useful as a list-row placeholder. */
@Composable
fun SkeletonLine(modifier: Modifier = Modifier, width: Int = 160, height: Int = 16) {
    SkeletonBox(
        modifier = modifier.width(width.dp).height(height.dp),
        shape = AeroTheme.shapes.xs,
    )
}
