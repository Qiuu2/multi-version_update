package com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.AeroTab
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TabBarV4
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TopBarV4
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.ai.AiScreen
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast.BroadcastScreen
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.service.ServiceScreen
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task.TaskTab
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal.TerminalTab
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * Top-level scaffold visible after login.
 *
 * Layout = TopBar (fixed) + content slot (per-tab) + TabBar (fixed).
 * The selected tab is local UI state for now; once we wire up
 * `navigation-compose`, the selection moves into the NavController back stack.
 *
 * Each tab currently routes to a [TabPlaceholder]. As tasks 6–9 land, swap
 * the corresponding `when` branch for the real screen.
 */
@Composable
fun MainScaffold(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
) {
    val colors = AeroTheme.colors
    var selected by remember { mutableStateOf(AeroTab.Terminal) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBarV4(
                title = selected.label,
                connected = true,
                onSettingsClick = onSettingsClick,
            )

            Box(modifier = Modifier.weight(1f)) {
                when (selected) {
                    AeroTab.Terminal  -> TerminalTab()
                    AeroTab.Broadcast -> BroadcastScreen()
                    AeroTab.AI        -> AiScreen(onGoToTasks = { selected = AeroTab.Task })
                    AeroTab.Task      -> TaskTab()
                    AeroTab.Service   -> ServiceScreen()
                }
            }

            // Reserve room for the raised tab — the bar itself is 80dp, plus the
            // 16dp lift means the visual top of the AI pill is +16 above the bar.
            TabBarV4(
                current = selected,
                onSelect = { selected = it },
                modifier = Modifier.padding(top = AeroTheme.spacing.tabRaise),
            )
        }
    }
}
