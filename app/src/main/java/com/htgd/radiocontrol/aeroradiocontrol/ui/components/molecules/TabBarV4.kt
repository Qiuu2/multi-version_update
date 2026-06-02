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
 * Resolves an [AeroTab] to its identity color (design-system-spec §1.3 mode-colors).
 * The mapping below is the THREE-LEG trace's middle leg — pixels in [FlatTab] /
 * [RaisedAITab] inherit from here, so a token rename / new tab inserts in one place.
 *
 *   spec §1.3 row           spec hex     → AeroColors field    (internal value)
 *   ─────────────────────── ──────────── ──────────────────── ───────────────────
 *   终端    (TabTerminal)   #0E7C70      → colors.primary      (Primary    = #0E7C70)
 *   广播    (TabBroadcast)  #EA580C      → colors.pageWarm     (PageWarm   = #EA580C)
 *   AI      (TabAI)         #14B8A6      → colors.aiTeal       (AITeal     = #14B8A6)
 *   任务    (TabTask)       #7C3AED      → colors.taskPurple   (TaskPurple = #7C3AED)
 *   服务    (TabService)    #2563EB      → colors.serviceBlue  (ServiceBlue= #2563EB)
 *
 * Naming note: spec calls these `TabTerminal`/`TabBroadcast`/`TabAI`/`TabTask`/
 * `TabService`; the existing color fields use the property-style names
 * (`primary` / `pageWarm` / `aiTeal` / `taskPurple` / `serviceBlue`) defined in
 * `Color.kt`. The hexes match 1:1 against `Color.kt:34-38` (PageWarm/TalkBlue/
 * TaskPurple/AITeal/ServiceBlue internals) and `Color.kt:29` (Primary).
 */
private fun AeroTab.identityColor(
    colors: com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroColors,
) = when (this) {
    AeroTab.Terminal  -> colors.primary       // spec §1.3 终端    #0E7C70
    AeroTab.Broadcast -> colors.pageWarm      // spec §1.3 广播    #EA580C
    AeroTab.AI        -> colors.aiTeal        // spec §1.3 AI      #14B8A6
    AeroTab.Task      -> colors.taskPurple    // spec §1.3 任务    #7C3AED
    AeroTab.Service   -> colors.serviceBlue   // spec §1.3 服务    #2563EB
}

/**
 * v4 TabBar — Handoff.html §03 · TabBar + design-system-spec.md §1.3 / §7.2.
 *
 * Custom 5-slot bottom bar; the middle (AI) tab is raised by [tabRaise] dp
 * and filled with the primary gradient — this is **not** a stock M3
 * NavigationBar because the spec calls out "不要用 BottomNavigation 默认行为".
 *
 * Tab-identity colors (spec §7.2 "当前 Tab：图标 + 文字着 Tab 标识色"): each tab's
 * SELECTED icon + label takes its identity color (spec §1.3 mode-colors table —
 * Terminal Teal #0E7C70 / Broadcast Orange #EA580C / AI Cyan #14B8A6 / Task Purple
 * #7C3AED / Service Blue #2563EB). Unselected stays neutral (ink3 / ink2). See
 * [tabIdentityColor]; before PA-14 C-1 every selected tab tinted `colors.primary`
 * (teal) regardless of identity → CTO 2026-05-30 BLOCKER (real-device screencaps).
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
    // Selected → this tab's identity color (spec §7.2); unselected → neutral ink3.
    // Pre-PA-14-C-1 this was `colors.primary` for every tab → all selections rendered
    // teal regardless of identity (CTO 2026-05-30 screencaps).
    val tint   = if (isCurrent) tab.identityColor(colors) else colors.ink3

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
        // Q3 #6: spec kicker = UPPERCASE (Handoff:555). .uppercase() is no-op for CJK;
        // visible for "AI" and any future Latin tab labels.
        Text(
            text  = tab.label.uppercase(),
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
            // Raised-AI LABEL follows the same per-tab identity rule (spec §7.2): the
            // AI tab's identity color is `aiTeal` #14B8A6, NOT primary. Background
            // gradient stays Primary (PA-14 C-1 scope is label/icon colors only).
            // Q3 #6: spec kicker = UPPERCASE; "AI" → "AI" (no change for this label, but
            // uppercase() is applied consistently with FlatTab for correctness.
            Text(
                text  = AeroTab.AI.label.uppercase(),
                style = AeroTheme.typography.kicker.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                    color      = if (isCurrent) AeroTab.AI.identityColor(AeroTheme.colors) else AeroTheme.colors.ink2,
                ),
            )
        }
    }
}
