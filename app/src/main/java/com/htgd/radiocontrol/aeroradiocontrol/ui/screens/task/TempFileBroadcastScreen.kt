package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MChip
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.BackTopBar
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast.BroadcastTargetsViewModel
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

private val tempFiles = listOf("紧急通知.mp3", "防疫广播.wav", "校长讲话.mp3")

@Composable
fun TempFileBroadcastScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    targetsViewModel: BroadcastTargetsViewModel = hiltViewModel(),
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val zones by targetsViewModel.zones.collectAsStateWithLifecycle()

    var selectedFile by remember { mutableStateOf<String?>(null) }
    var selectedZones by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = modifier.fillMaxSize().background(colors.bg)) {
        BackTopBar(title = "临时文件广播", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(spacing.pageH),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text("选择文件", style = AeroTheme.typography.sectionTitle, color = colors.ink)
            tempFiles.forEach { file ->
                FileRow(name = file, selected = file == selectedFile, onClick = { selectedFile = file })
            }

            Text("目标终端", style = AeroTheme.typography.sectionTitle, color = colors.ink)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                zones.forEach { zone ->
                    MChip(
                        label = zone.name,
                        active = zone.id in selectedZones,
                        onClick = {
                            selectedZones =
                                if (zone.id in selectedZones) selectedZones - zone.id else selectedZones + zone.id
                        },
                    )
                }
            }
        }
        MButton(
            text = "开始广播",
            leading = { Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(18.dp)) },
            enabled = selectedFile != null && selectedZones.isNotEmpty(),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(spacing.pageH),
        )
    }
}

@Composable
private fun FileRow(name: String, selected: Boolean, onClick: () -> Unit) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.rCard)
            .background(if (selected) colors.primarySoft else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.pageH, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Icon(
            Icons.Filled.InsertDriveFile,
            contentDescription = null,
            tint = if (selected) colors.primary else colors.ink3,
            modifier = Modifier.size(24.dp),
        )
        Text(name, style = AeroTheme.typography.body, color = colors.ink)
    }
}
