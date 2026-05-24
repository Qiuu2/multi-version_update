package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

data class FabAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Pop-out action bar. Springs in over the standard 220ms motion token. */
@Composable
fun FabBar(
    visible: Boolean,
    actions: List<FabAction>,
    modifier: Modifier = Modifier,
) {
    val motion = AeroTheme.motion
    val spec = tween<Float>(motion.durationStandard, easing = motion.easingEmphasized)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spec) + scaleIn(spec, initialScale = 0.85f),
        exit = fadeOut(spec) + scaleOut(spec, targetScale = 0.85f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .clip(AeroTheme.shapes.pill)
                .background(AeroTheme.colors.surface)
                .padding(horizontal = AeroTheme.spacing.sm, vertical = AeroTheme.spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AeroTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { action ->
                FabActionButton(action)
            }
        }
    }
}

@Composable
private fun FabActionButton(action: FabAction) {
    val colors = AeroTheme.colors
    Column(
        modifier = Modifier
            .clip(AeroTheme.shapes.sm)
            .clickable(onClick = action.onClick)
            .padding(horizontal = AeroTheme.spacing.sm, vertical = AeroTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeroTheme.spacing.xxs),
    ) {
        Icon(action.icon, contentDescription = action.label, tint = colors.primary, modifier = Modifier.size(24.dp))
        Text(action.label, style = AeroTheme.typography.overline, color = colors.onSurfaceVariant)
    }
}
