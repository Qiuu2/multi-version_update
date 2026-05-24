package com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TabBarV4
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.TopBarV4
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.placeholder.TabPlaceholder
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

private data class TabContent(val title: String, val subtitle: String)

private val tabContents = listOf(
    TabContent("终端", "找到并控制广播终端"),
    TabContent("广播", "寻呼 · 对讲 · 点播"),
    TabContent("AI", "智能广播助手"),
    TabContent("任务", "作息方案与定时任务"),
    TabContent("服务", "系统状态与支持"),
)

@Composable
fun MainScaffold(
    onSettingsClick: () -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val current = tabContents[selectedTab]

    Scaffold(
        containerColor = AeroTheme.colors.background,
        topBar = {
            TopBarV4(
                title = current.title,
                connected = true,
                onSettingsClick = onSettingsClick,
            )
        },
        bottomBar = {
            TabBarV4(
                items = mainTabs,
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AeroTheme.colors.background),
        ) {
            TabPlaceholder(title = current.title, subtitle = current.subtitle)
        }
    }
}
