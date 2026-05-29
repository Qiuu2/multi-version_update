package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

/**
 * Mock data for the task Tab, still mock-backed until V3TaskRepository + the task
 * ViewModels land (TASK-PA-03b). The view types ([TaskItem]/[SchemeUi]/[LogEntry]/
 * [TaskCardState]) moved to TaskUiModels.kt; this file holds only the mock
 * instances and is deleted once the task screens are de-mocked.
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

    val activeScheme: SchemeUi get() = schemes.firstOrNull { it.active } ?: schemes.first()

    val logs: List<LogEntry> = listOf(
        LogEntry("08:00:02", "上课铃", true, "教学楼 A · 4 个终端"),
        LogEntry("09:50:00", "课间操音乐", true, "运动场 · 3 个终端"),
        LogEntry("12:00:05", "午餐通知", false, "宿舍区 · 1 个终端离线"),
        LogEntry("14:00:00", "下午预备", false, "已取消"),
    )
}
