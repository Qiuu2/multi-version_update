package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * v4 FAB-bar — Handoff.html §03 · FAB-bar.
 *
 * A horizontal action bar that slides up from the bottom when the user enters
 * a multi-select state. Use as an overlay on top of the screen content (Box
 * with `Alignment.BottomCenter`). When [visible] is false, the bar is fully
 * hidden and consumes no space.
 *
 * Animation: 220ms emphasized easing — matches `fabIn` token in §02.
 */
@Composable
fun FabBar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    counterLabel: String? = null,                 // e.g. "已选 3 台"
    actions: @Composable () -> Unit,              // pass MButtons / IconButtons
) {
    val motion  = AeroTheme.motion
    val colors  = AeroTheme.colors
    val shape   = AeroTheme.shapes.rChip

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(motion.fabInMs, easing = motion.emphasizedEasing),
            initialOffsetY = { it },
        ) + fadeIn(tween(motion.fabInMs)),
        exit = slideOutVertically(
            animationSpec = tween(motion.fabInMs, easing = motion.emphasizedEasing),
            targetOffsetY = { it },
        ) + fadeOut(tween(motion.fabInMs)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .shadow(elevation = AeroTheme.elevation.fab, shape = shape, clip = false)
                .clip(shape)
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (counterLabel != null) {
                Text(
                    text  = counterLabel,
                    style = AeroTheme.typography.bodyLarge,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
            }
            actions()
        }
    }
}
