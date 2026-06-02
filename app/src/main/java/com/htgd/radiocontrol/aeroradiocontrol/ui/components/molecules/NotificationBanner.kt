package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

enum class NotificationType { Success, Warning, Error }

/** Inline notification banner — three states (success / warning / error). */
@Composable
fun NotificationBanner(
    type: NotificationType,
    message: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val colors = AeroTheme.colors
    val container: Color
    val content: Color
    val icon: ImageVector
    when (type) {
        NotificationType.Success -> { container = colors.statusOnlineSoft; content = colors.statusOnline; icon = Icons.Filled.CheckCircle }
        NotificationType.Warning -> { container = colors.statusPagingSoft; content = colors.statusPaging; icon = Icons.Filled.Warning }
        NotificationType.Error   -> { container = colors.statusFaultSoft;  content = colors.statusFault;  icon = Icons.Filled.Error }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(container, AeroTheme.shapes.rCard)
            .padding(horizontal = AeroTheme.spacing.pageH, vertical = AeroTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeroTheme.spacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Text(message, style = AeroTheme.typography.bodyLarge, color = content, modifier = Modifier.weight(1f))
        if (onDismiss != null) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "关闭",
                tint = content,
                modifier = Modifier.clickable(onClick = onDismiss).size(20.dp),
            )
        }
    }
}
