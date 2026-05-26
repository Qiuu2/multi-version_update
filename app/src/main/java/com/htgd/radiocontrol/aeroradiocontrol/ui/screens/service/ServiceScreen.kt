package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.service

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButtonVariant
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

private data class SystemStatus(val label: String, val value: String, val level: StatusLevel)

private enum class StatusLevel { Ok, Warn, Error }

/** 服务 tab: system status cards + contact-engineer fallback. */
@Composable
fun ServiceScreen(modifier: Modifier = Modifier) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing

    val statuses = listOf(
        SystemStatus("服务器", "运行正常", StatusLevel.Ok),
        SystemStatus("网络连接", "已连接", StatusLevel.Ok),
        SystemStatus("终端在线率", "12 / 14", StatusLevel.Ok),
        SystemStatus("存储空间", "78% 已用", StatusLevel.Warn),
        SystemStatus("待处理故障", "2 个终端", StatusLevel.Error),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(spacing.pageH),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text("系统状态", style = AeroTheme.typography.sectionTitle, color = colors.ink)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            statuses.forEach { StatusRow(it) }
        }

        Text("需要帮助", style = AeroTheme.typography.sectionTitle, color = colors.ink, modifier = Modifier.padding(top = spacing.sm))
        ContactCard()
    }
}

@Composable
private fun StatusRow(status: SystemStatus) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val dot = when (status.level) {
        StatusLevel.Ok -> colors.statusOnline
        StatusLevel.Warn -> colors.statusPaging
        StatusLevel.Error -> colors.statusFault
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.rCard)
            .background(colors.surface)
            .padding(horizontal = spacing.pageH, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        StatusDot(dot)
        Text(status.label, style = AeroTheme.typography.body, color = colors.ink, modifier = Modifier.weight(1f))
        Text(status.value, style = AeroTheme.typography.bodyLarge, color = dot)
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
private fun ContactCard() {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.rCard)
            .background(colors.surface)
            .padding(spacing.pageH),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = colors.primaryInk, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("技术支持工程师", style = AeroTheme.typography.sectionTitle, color = colors.ink)
                Text("工作日 8:00 - 18:00", style = AeroTheme.typography.bodySmall, color = colors.ink3)
            }
        }
        MButton(
            text = "联系工程师",
            variant = MButtonVariant.Tonal,
            leading = { Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
