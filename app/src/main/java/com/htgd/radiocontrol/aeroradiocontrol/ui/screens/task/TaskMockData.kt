package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

/**
 * Mock data for the task Tab — RETAINED ONLY for the deferred [SchemeEditScreen]
 * (TASK-PA-03c).
 *
 * The read+control screens (TaskScreen / SchemeDetailScreen / ExecutionLogScreen)
 * were de-mocked onto TaskRepository in PA-03c — they no longer touch this file.
 * SchemeEdit is CRUD, and there is no CRUD interface on TaskRepository yet (out of
 * PA-03 scope → ICD_UPDATE), so it stays mock-backed until that increment lands;
 * this file is deleted then. Trimmed to just the scheme lookup SchemeEdit uses
 * (the `activeScheme`/`logs` mocks were dropped — nothing reads them anymore).
 */
object TaskMock {
    val schemes: List<SchemeUi> = listOf(
        SchemeUi(
            id = "s1", name = "春季作息", active = true,
            tasks = listOf(
                TaskItem("t1", "06:30", "起床铃", "宿舍区", TaskCardState.Normal),
                TaskItem("t2", "08:00", "上课铃", "教学楼 A", TaskCardState.Running),
                TaskItem("t3", "09:50", "课间操音乐", "运动场", TaskCardState.Swapped),
                TaskItem("t4", "12:00", "午餐通知", "宿舍区", TaskCardState.Migrated),
                TaskItem("t5", "14:00", "下午预备", "教学楼 A", TaskCardState.Cancelled),
                TaskItem("t6", "22:00", "熄灯铃", "宿舍区", TaskCardState.Deleted),
            ),
        ),
        SchemeUi(
            id = "s2", name = "考试作息", active = false,
            tasks = listOf(
                TaskItem("t7", "07:30", "入场提示", "教学楼 A", TaskCardState.Normal),
                TaskItem("t8", "09:00", "开考铃", "教学楼 A", TaskCardState.Normal),
            ),
        ),
    )

    fun scheme(id: String): SchemeUi? = schemes.firstOrNull { it.id == id }
}
