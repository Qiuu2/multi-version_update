package com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.automirrored.filled.Dvr
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroGradients
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * The five primary destinations in v4. Order is fixed by IA — see Handoff §01.
 * Middle item (AI) is rendered with a raised gradient pill.
 */
enum class AeroTab(val label: String, val icon: ImageVector) {
    Terminal ("终端", Icons.AutoMirrored.Filled.Dvr),
    Broadcast("广播", Icons.Filled.Campaign),
    AI       ("AI",  Icons.Filled.AutoAwesome),
    Task     ("任务", Icons.Filled.EventAvailable),
    Service  ("服务", Icons.Filled.SupportAgent),
}

/**
 * v4 TabBar — Handoff.html §03 · TabBar.
 *
 * Custom 5-slot bottom bar; the middle (AI) tab is raised by [tabRaise] dp
 * and filled with the primary gradient — this is **not** a stock M3
 * NavigationBar because the spec calls out "不要用 BottomNavigation 默认行为".
 *
 * Badge support is intentionally minimal here (just a red dot prop); fancier
 * dot / num / 99+ rendering is wired in a follow-up.
 */
@Composable
fun TabBarV4(
    current: AeroTab,
    onSelect: (AeroTab) -> Unit,
    modifier: Modifier = Modifier,
    badges: Map<AeroTab, Boolean> = emptyMap(), // simple dot badge for v0
) {
    val colors = AeroTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .height(AeroTheme.spacing.tabBarH)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        AeroTab.entries.forEach { tab ->
            TabSlot(
                tab       = tab,
                isCurrent = tab == current,
                hasBadge  = badges[tab] == true,
                onClick   = { onSelect(tab) },
                modifier  = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabSlot(
    tab: AeroTab,
    isCurrent: Boolean,
    hasBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tab == AeroTab.AI) {
        RaisedAITab(isCurrent = isCurrent, onClick = onClick, modifier = modifier)
    } else {
        FlatTab(tab = tab, isCurrent = isCurrent, hasBadge = hasBadge, onClick = onClick, modifier = modifier)
    }
}

@Composable
private fun FlatTab(
    tab: AeroTab,
    isCurrent: Boolean,
    hasBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val tint   = if (isCurrent) colors.primary else colors.ink3

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .padding(start = 18.dp)
                        .size(8.dp)
                        .background(colors.statusFault, CircleShape),
                )
            }
        }
        Text(
            text  = tab.label,
            style = AeroTheme.typography.kicker.copy(
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color      = tint,
            ),
        )
    }
}

@Composable
private fun RaisedAITab(
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val raise = AeroTheme.spacing.tabRaise

    // Outer column sits in the row but the inner pill is offset up by `raise`.
    Box(
        modifier = modifier
            .offset(y = -raise),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(elevation = AeroTheme.elevation.fab, shape = CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(AeroGradients.Primary)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AeroTab.AI.icon,
                    contentDescription = AeroTab.AI.label,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text  = AeroTab.AI.label,
                style = AeroTheme.typography.kicker.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    color      = if (isCurrent) AeroTheme.colors.primary else AeroTheme.colors.ink2,
                ),
            )
        }
    }
}
