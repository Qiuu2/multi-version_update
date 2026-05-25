package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/** AI tab placeholder: dark gradient, a large (disabled) prompt box, "敬请期待". */
@Composable
fun AiScreen(modifier: Modifier = Modifier) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val onDark = colors.onPrimary

    Box(modifier = modifier.fillMaxSize().background(AeroTheme.gradients.night)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Spacer(modifier = Modifier.size(spacing.xxl))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = onDark, modifier = Modifier.size(28.dp))
                Text("AI 广播助手", style = AeroTheme.typography.display, color = onDark)
            }
            Text(
                "用一句话描述需求，AI 帮你生成广播任务",
                style = AeroTheme.typography.body,
                color = onDark.copy(alpha = 0.75f),
            )

            Spacer(modifier = Modifier.weight(1f))

            ComingSoonBadge()

            Spacer(modifier = Modifier.size(spacing.md))

            PromptBox()
        }
    }
}

@Composable
private fun ComingSoonBadge() {
    val colors = AeroTheme.colors
    Box(
        modifier = Modifier
            .clip(AeroTheme.shapes.pill)
            .background(colors.onPrimary.copy(alpha = 0.12f))
            .padding(horizontal = AeroTheme.spacing.base, vertical = AeroTheme.spacing.sm),
    ) {
        Text("敬请期待", style = AeroTheme.typography.bodyStrong, color = colors.onPrimary)
    }
}

@Composable
private fun PromptBox() {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 120.dp)
            .clip(AeroTheme.shapes.lg)
            .background(colors.nightSurface.copy(alpha = 0.6f))
            .border(1.dp, colors.onPrimary.copy(alpha = 0.2f), AeroTheme.shapes.lg)
            .padding(spacing.base),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            "例如：下午 4 点给教学楼 A 播放放学通知",
            style = AeroTheme.typography.body,
            color = colors.onPrimary.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(AeroTheme.shapes.md)
                .background(colors.onPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "发送",
                tint = colors.onPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
