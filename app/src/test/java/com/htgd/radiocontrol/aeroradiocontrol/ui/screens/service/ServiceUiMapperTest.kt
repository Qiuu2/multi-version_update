package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.service

import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerHealth
import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Service Tab domain→UI mapping (TASK-PA Service). The
 * Unknown-tolerant health map is the key safety property (R-003: an unrecognised v3
 * health must not crash the `when`).
 */
class ServiceUiMapperTest {

    @Test
    fun `health maps to the right UI enum`() {
        assertEquals(ServiceHealthUi.Online, ServerHealth.Online.toHealthUi())
        assertEquals(ServiceHealthUi.Offline, ServerHealth.Offline.toHealthUi())
    }

    @Test
    fun `unknown health falls back to a non-crashing UI value`() {
        // R-003: any unrecognised v3 health → Unknown, never an exception.
        assertEquals(ServiceHealthUi.Unknown, ServerHealth.Unknown("weird-7").toHealthUi())
    }

    @Test
    fun `full server state maps name health and metrics`() {
        val ui = ServerState(
            health = ServerHealth.Online,
            name = "广播主机",
            ip = "192.168.1.10",
            gate = "192.168.1.1",
            connection = 12,
            maxConnection = 5000L,
            taskCount = 3,
            bandwidth = 100,
            ctrlPort = 4520,
            dataPort = 4521,
        ).toServiceUi()

        assertEquals(ServiceHealthUi.Online, ui.health)
        assertEquals("广播主机", ui.name)
        assertEquals("12 / 5000", ui.metrics.first { it.label == "连接数" }.value)
        assertEquals("3", ui.metrics.first { it.label == "任务数" }.value)
        assertEquals("100 Mbps", ui.metrics.first { it.label == "带宽" }.value)
        assertEquals("192.168.1.10", ui.metrics.first { it.label == "IP 地址" }.value)
        assertEquals("4520", ui.metrics.first { it.label == "控制端口" }.value)
    }

    @Test
    fun `null fields render as blank-tolerant placeholders`() {
        val ui = ServerState(health = ServerHealth.Offline).toServiceUi()
        assertEquals(ServiceHealthUi.Offline, ui.health)
        assertEquals("广播服务器", ui.name) // null name → fallback label
        assertTrue(ui.metrics.all { it.value == "--" }) // every metric absent → "--"
    }
}
