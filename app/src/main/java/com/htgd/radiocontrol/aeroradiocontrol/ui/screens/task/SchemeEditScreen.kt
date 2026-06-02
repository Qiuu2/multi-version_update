package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButtonVariant
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MInput
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MSwitch
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.BackTopBar
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

@Composable
fun SchemeEditScreen(
    schemeId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AeroTheme.spacing
    val scheme = TaskMock.scheme(schemeId)

    var name by remember { mutableStateOf(scheme?.name.orEmpty()) }
    var active by remember { mutableStateOf(scheme?.active ?: false) }
    val tasks = remember { (scheme?.tasks ?: emptyList()).toMutableStateList() }

    Column(modifier = modifier.fillMaxSize().background(AeroTheme.colors.bg)) {
        BackTopBar(title = "编辑方案", onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(spacing.pageH),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item(key = "name") {
                MInput(value = name, onValueChange = { name = it }, label = "方案名称", placeholder = "请输入方案名称")
            }
            item(key = "active") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("启用该方案", style = AeroTheme.typography.body, color = AeroTheme.colors.ink, modifier = Modifier.weight(1f))
                    MSwitch(checked = active, onCheckedChange = { active = it })
                }
            }
            item(key = "tasks-title") {
                Text("任务列表", style = AeroTheme.typography.sectionTitle, color = AeroTheme.colors.ink)
            }
            items(tasks, key = { it.id }) { task ->
                EditTaskRow(task = task, onDelete = { tasks.remove(task) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.pageH),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            MButton(
                text = "添加任务",
                variant = MButtonVariant.Tonal,
                onClick = {
                    tasks.add(
                        TaskItem(
                            id = "new-${tasks.size}-${System.currentTimeMillis()}",
                            time = "00:00", title = "新任务", zone = "未分配",
                            state = TaskCardState.Normal,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
            )
            MButton(text = "保存", onClick = onBack, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EditTaskRow(task: TaskItem, onDelete: () -> Unit) {
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
        Text(task.time, style = AeroTheme.typography.bodyLarge, color = colors.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, style = AeroTheme.typography.body, color = colors.ink)
            Text(task.zone, style = AeroTheme.typography.bodySmall, color = colors.ink2) // spec §4 次要文字→ink2
        }
        Icon(
            Icons.Filled.Delete,
            contentDescription = "删除",
            tint = colors.statusFault,
            modifier = Modifier.clip(AeroTheme.shapes.rChip).clickable(onClick = onDelete).size(22.dp),
        )
    }
}
