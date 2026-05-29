package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

import com.htgd.radiocontrol.aeroradiocontrol.data.model.Scheme
import com.htgd.radiocontrol.aeroradiocontrol.data.model.SchemeTask
import com.htgd.radiocontrol.aeroradiocontrol.data.model.SchemeTaskStatus
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TaskLog
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the task Tab domain→UI mapping (TASK-PA-03c). The Unknown-tolerant
 * status map is the key safety property (R-003: a new/unknown v3 status must not
 * crash the `when`).
 */
class TaskUiMappersTest {

    @Test
    fun `status maps to the right card state`() {
        assertEquals(TaskCardState.Running, SchemeTaskStatus.Running.toCardState())
        assertEquals(TaskCardState.Normal, SchemeTaskStatus.Idle.toCardState())
        assertEquals(TaskCardState.Cancelled, SchemeTaskStatus.Disabled.toCardState())
    }

    @Test
    fun `unknown status falls back to a non-crashing card state`() {
        // R-003: any unrecognised v3 status → Normal, never an exception.
        assertEquals(TaskCardState.Normal, SchemeTaskStatus.Unknown("weird-99").toCardState())
    }

    @Test
    fun `scheme maps with nested tasks and active flag`() {
        val scheme = Scheme(
            id = "s1", name = "春季作息", active = true,
            tasks = listOf(
                SchemeTask(id = "t1", name = "上课铃", status = SchemeTaskStatus.Running, startTime = "08:00", mediaName = "铃声A"),
                SchemeTask(id = "t2", name = "熄灯铃", status = SchemeTaskStatus.Idle, startTime = null, mediaName = null),
            ),
        )
        val ui = scheme.toSchemeUi()

        assertEquals("s1", ui.id)
        assertEquals("春季作息", ui.name)
        assertEquals(true, ui.active)
        assertEquals(2, ui.tasks.size)

        val first = ui.tasks[0]
        assertEquals("t1", first.id)
        assertEquals("08:00", first.time)
        assertEquals("上课铃", first.title)
        // No zone on the v3 wire (PA-10) → blank, even when a media name is present.
        assertEquals("", first.zone)
        assertEquals(TaskCardState.Running, first.state)

        val second = ui.tasks[1]
        assertEquals("", second.time)  // null startTime → blank-tolerant
        assertEquals("", second.zone)  // no zone → blank
        assertEquals(TaskCardState.Normal, second.state)
    }

    @Test
    fun `task log maps to a UI log entry`() {
        val log = TaskLog(id = "l1", taskName = "上课铃", timestamp = "08:00:02", message = "教学楼 A · 4 个终端")
        val entry = log.toLogEntry()
        assertEquals("08:00:02", entry.time)
        assertEquals("上课铃", entry.title)
        assertEquals("教学楼 A · 4 个终端", entry.detail)
        assertEquals(true, entry.success)
    }
}
