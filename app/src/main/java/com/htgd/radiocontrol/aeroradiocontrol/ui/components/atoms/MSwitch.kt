package com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

@Composable
fun MSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AeroTheme.colors
    val motion = AeroTheme.motion
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.primary else colors.surfaceDim,
        animationSpec = tween(motion.durationStandard, easing = motion.easingStandard),
        label = "trackColor",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(motion.durationStandard, easing = motion.easingStandard),
        label = "thumbOffset",
    )
    val alpha = if (enabled) 1f else 0.38f

    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .clip(AeroTheme.shapes.pill)
            .background(trackColor.copy(alpha = alpha))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(colors.onPrimary),
        )
    }
}
