package com.htgd.radiocontrol.aeroradiocontrol.data.repository

import com.htgd.radiocontrol.aeroradiocontrol.data.dto.TerminalDto
import com.htgd.radiocontrol.aeroradiocontrol.data.dto.ZoneDto
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for TerminalMapper (DTO -> domain).
 *
 * The status-derivation cases lock the CURRENT documented-assumption ordering
 * (OPEN INQ-O-1 D-3/O-2) — behaviour, not the "correct" mapping; when the
 * backend confirms, deriveStatus changes and these update with it. The id-drop
 * and Unknown-fallback cases are the R-003 contract and stay regardless.
 */
class TerminalMapperTest {

    @Test
    fun terminalWithNoId_isDropped() {
        assertNull(TerminalDto(id = null, name = "x").toTerminalOrNull())
    }

    @Test
    fun terminalFields_mapThrough() {
        val t = TerminalDto(
            id = 7, name = "广播点A", ip = "10.0.0.7", zone = 3, volume = 60,
            netState = 1, longitude = "120.1", latitude = "30.2",
        ).toTerminalOrNull()!!

        assertEquals("7", t.id)
        assertEquals("广播点A", t.name)
        assertEquals("3", t.zoneId)
        assertEquals(60, t.volume)
        assertEquals("120.1", t.longitude)
    }

    @Test
    fun noStatusFieldsAtAll_isUnknown() {
        val t = TerminalDto(id = 1).toTerminalOrNull()!!
        assertTrue(t.status is TerminalStatus.Unknown)
        assertEquals("no-status", (t.status as TerminalStatus.Unknown).raw)
    }

    @Test
    fun netStateZero_isOffline() {
        val t = TerminalDto(id = 1, netState = 0, taskState = 5).toTerminalOrNull()!!
        assertEquals(TerminalStatus.Offline, t.status)
    }

    @Test
    fun urgentFlag_isPaging() {
        val t = TerminalDto(id = 1, netState = 1, isUrgent = 1).toTerminalOrNull()!!
        assertEquals(TerminalStatus.Paging, t.status)
    }

    @Test
    fun speechActive_isPaging() {
        val t = TerminalDto(id = 1, netState = 1, speechState = 2).toTerminalOrNull()!!
        assertEquals(TerminalStatus.Paging, t.status)
    }

    @Test
    fun taskActive_isPlaying() {
        val t = TerminalDto(id = 1, netState = 1, taskState = 1).toTerminalOrNull()!!
        assertEquals(TerminalStatus.Playing, t.status)
    }

    @Test
    fun online_whenNetUpAndIdle() {
        val t = TerminalDto(
            id = 1, netState = 1, taskState = 0, speechState = 0, isUrgent = 0,
        ).toTerminalOrNull()!!
        assertEquals(TerminalStatus.Online, t.status)
    }

    // ── Zone ──────────────────────────────────────────────────────────────

    @Test
    fun zoneWithNoId_isDropped() {
        assertNull(ZoneDto(id = null, name = "z").toZoneOrNull())
    }

    @Test
    fun zoneFields_mapThrough_withEmptyTerminals() {
        val z = ZoneDto(id = 4, name = "教学楼A", description = "desc").toZoneOrNull()!!
        assertEquals("4", z.id)
        assertEquals("教学楼A", z.name)
        assertEquals("desc", z.description)
        // Mapper leaves terminals empty; the Repository attaches them.
        assertTrue(z.terminals.isEmpty())
    }
}
