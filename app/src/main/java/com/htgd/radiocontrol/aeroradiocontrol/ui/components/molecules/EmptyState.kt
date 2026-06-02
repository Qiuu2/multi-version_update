package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButtonVariant
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/** Empty-state block: icon + title + optional description + optional action. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = AeroTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AeroTheme.spacing.xl, vertical = AeroTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeroTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(colors.surface3, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.ink3, modifier = Modifier.size(34.dp))
        }
        Text(title, style = AeroTheme.typography.sectionTitle, color = colors.ink, textAlign = TextAlign.Center)
        if (description != null) {
            Text(description, style = AeroTheme.typography.body, color = colors.ink3, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            MButton(text = actionLabel, onClick = onAction, variant = MButtonVariant.Tonal)
        }
    }
}
