package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.service

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButtonVariant
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.EmptyState
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationBanner
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationType
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.SkeletonBox
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.PollingState
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * 服务 tab — stateful entry (系统健康度, de-mocked: TASK-PA Service).
 *
 * Observes [ServiceViewModel] (→ [ServerStateRepository], REAL /server/serverstate).
 * Renders the [ServiceUiState] states; the contact-engineer fallback is static and
 * always shown. No more mock data.
 */
@Composable
fun ServiceScreen(
    modifier: Modifier = Modifier,
    viewModel: ServiceViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pollingState by viewModel.pollingState.collectAsStateWithLifecycle()
    ServiceContent(
        state = state,
        pollingState = pollingState,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

/** Stateless renderer (previewable / testable without Hilt). */
@Composable
fun ServiceContent(
    state: ServiceUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    pollingState: PollingState = PollingState.IDLE,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(spacing.pageH),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text("系统状态", style = AeroTheme.typography.sectionTitle, color = colors.ink)

        when (state) {
            is ServiceUiState.Loading -> ServiceSkeleton()

            is ServiceUiState.Error -> EmptyState(
                icon = Icons.Filled.Dns,
                title = "加载失败",
                description = state.message,
                actionLabel = "重试",
                onAction = onRetry,
            )

            is ServiceUiState.Success -> {
                // Data is up; a background poll just failed → non-blocking notice.
                if (pollingState == PollingState.ERROR) {
                    NotificationBanner(type = NotificationType.Warning, message = "刷新失败，正在自动重试…")
                }
                ServerHealthCard(state.server)
            }
        }

        Text(
            "需要帮助",
            style = AeroTheme.typography.sectionTitle,
            color = colors.ink,
            modifier = Modifier.padding(top = spacing.sm),
        )
        ContactCard()
    }
}

@Composable
private fun ServiceSkeleton() {
    val spacing = AeroTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        repeat(4) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(56.dp), shape = AeroTheme.shapes.rCard)
        }
    }
}

@Composable
private fun ServerHealthCard(server: ServiceUi) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val (healthLabel, healthColor) = when (server.health) {
        ServiceHealthUi.Online -> "运行正常" to colors.statusOnline
        ServiceHealthUi.Offline -> "离线" to colors.statusFault
        ServiceHealthUi.Unknown -> "状态未知" to colors.ink3
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AeroTheme.shapes.rCard)
                .background(colors.surface)
                .padding(horizontal = spacing.pageH, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            StatusDot(healthColor)
            Text(server.name, style = AeroTheme.typography.body, color = colors.ink, modifier = Modifier.weight(1f))
            Text(healthLabel, style = AeroTheme.typography.bodyLarge, color = healthColor)
        }
        server.metrics.forEach { MetricRow(it) }
    }
}

@Composable
private fun MetricRow(metric: ServiceMetric) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.rCard)
            .background(colors.surface)
            .padding(horizontal = spacing.pageH, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(metric.label, style = AeroTheme.typography.body, color = colors.ink, modifier = Modifier.weight(1f))
        Text(metric.value, style = AeroTheme.typography.bodyLarge, color = colors.ink3)
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
    val context = LocalContext.current
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
            onClick = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
