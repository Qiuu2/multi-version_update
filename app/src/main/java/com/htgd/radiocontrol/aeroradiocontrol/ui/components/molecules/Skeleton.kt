package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/** Animated shimmer brush, sweeping left-to-right on the skelMs (1.6s) loop. */
@Composable
fun rememberShimmerBrush(): Brush {
    val colors = AeroTheme.colors
    val band = listOf(colors.surface3, colors.surface, colors.surface3)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AeroTheme.motion.skelMs),
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
fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = AeroTheme.shapes.rCard) {
    Box(modifier = modifier.clip(shape).background(rememberShimmerBrush()))
}

/** N card-height placeholder rows for list-style data screens. */
@Composable
fun ListSkeleton(modifier: Modifier = Modifier, rows: Int = 5) {
    Column(
        modifier = modifier.fillMaxWidth().padding(AeroTheme.spacing.pageH),
        verticalArrangement = Arrangement.spacedBy(AeroTheme.spacing.sm),
    ) {
        repeat(rows) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(64.dp), shape = AeroTheme.shapes.rCard)
        }
    }
}

/** Zone headers + tile grid placeholder for the terminal hub. */
@Composable
fun TerminalHubSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(AeroTheme.spacing.pageH),
        verticalArrangement = Arrangement.spacedBy(AeroTheme.spacing.md),
    ) {
        repeat(3) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(52.dp), shape = AeroTheme.shapes.rCard)
            // Handoff:541 — 3-column terminal grid (was 2 boxes, fixed Q3 #1)
            Row(horizontalArrangement = Arrangement.spacedBy(AeroTheme.spacing.tileGap)) {
                SkeletonBox(modifier = Modifier.size(130.dp), shape = AeroTheme.shapes.rTile)
                SkeletonBox(modifier = Modifier.size(130.dp), shape = AeroTheme.shapes.rTile)
                SkeletonBox(modifier = Modifier.size(130.dp), shape = AeroTheme.shapes.rTile)
            }
        }
    }
}
