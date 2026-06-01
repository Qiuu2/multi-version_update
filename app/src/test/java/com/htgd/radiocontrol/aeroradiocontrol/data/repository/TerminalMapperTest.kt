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
 * PA-14 (2026-05-30): [TerminalDto.toTerminalOrNull] takes the parent zone id as
 * an explicit `containingZoneId` argument — domain [Terminal.zoneId] is the
 * NESTING zone (where the row appeared under in /terminal/terzone), NOT the
 * wire `terminal.zone` field (whose semantics are unknown and which broke the
 * old groupBy). [ZoneDto.toZoneOrNull] now consumes the nested terminal[] and
 * threads each parent zone's id in.
 *
 * The status-derivation cases lock the current documented-assumption ordering;
 * id-drop and Unknown-fallback are the R-003 contract and stay regardless.
 */
class TerminalMapperTest {

    @Test
    fun terminalWithNoId_isDropped() {
        assertNull(TerminalDto(id = null, name = "x").toTerminalOrNull("1"))
    }

    @Test
    fun terminalFields_mapThrough_zoneIdFromParent_notWireZone() {
        // ★ PA-14 contract: even though wire `zone = 99`, the domain zoneId is
        // the parent ("7") because membership comes from the nesting, not the field.
        val t = TerminalDto(
            id = 7, name = "广播点A", ip = "10.0.0.7", zone = 99, volume = 60,
            netState = 1, longitude = "120.1", latitude = "30.2",
        ).toTerminalOrNull(containingZoneId = "7")!!

        assertEquals("7", t.id)
        assertEquals("广播点A", t.name)
        assertEquals("7", t.zoneId)   // parent, not 99
        assertEquals(60, t.volume)
        assertEquals("120.1", t.longitude)
    }

    @Test
    fun noStatusFieldsAtAll_isUnknown() {
        val t = TerminalDto(id = 1).toTerminalOrNull("1")!!
        assertTrue(t.status is TerminalStatus.Unknown)
        assertEquals("no-status", (t.status as TerminalStatus.Unknown).raw)
    }

    @Test
    fun netStateZero_isOffline() {
        val t = TerminalDto(id = 1, netState = 0, taskState = 5).toTerminalOrNull("1")!!
        assertEquals(TerminalStatus.Offline, t.status)
    }

    @Test
    fun urgentFlag_isPaging() {
        val t = TerminalDto(id = 1, netState = 1, isUrgent = 1).toTerminalOrNull("1")!!
        assertEquals(TerminalStatus.Paging, t.status)
    }

    @Test
    fun speechActive_isPaging() {
        val t = TerminalDto(id = 1, netState = 1, speechState = 2).toTerminalOrNull("1")!!
        assertEquals(TerminalStatus.Paging, t.status)
    }

    @Test
    fun taskActive_isPlaying() {
        val t = TerminalDto(id = 1, netState = 1, taskState = 1).toTerminalOrNull("1")!!
        assertEquals(TerminalStatus.Playing, t.status)
    }

    @Test
    fun online_whenNetUpAndIdle() {
        val t = TerminalDto(
            id = 1, netState = 1, taskState = 0, speechState = 0, isUrgent = 0,
        ).toTerminalOrNull("1")!!
        assertEquals(TerminalStatus.Online, t.status)
    }

    // ── Zone ──────────────────────────────────────────────────────────────

    @Test
    fun zoneWithNoId_isDropped() {
        assertNull(ZoneDto(id = null, name = "z").toZoneOrNull())
    }

    @Test
    fun zoneFields_mapThrough_attachesNestedTerminals() {
        // PA-14: zone with nested terminals — the mapper attaches them directly,
        // threading parent id "4" into each Terminal.zoneId.
        val z = ZoneDto(
            id = 4, name = "教学楼A", description = "desc",
            terminal = listOf(
                TerminalDto(id = 100, name = "t100", netState = 1),
                TerminalDto(id = 101, name = "t101", netState = 1),
            ),
        ).toZoneOrNull()!!
        assertEquals("4", z.id)
        assertEquals("教学楼A", z.name)
        assertEquals("desc", z.description)
        assertEquals(listOf("t100", "t101"), z.terminals.map { it.name })
        assertTrue(z.terminals.all { it.zoneId == "4" })
    }

    @Test
    fun zone_emptyNestedTerminals_surfacesAsEmpty() {
        val z = ZoneDto(id = 9, name = "赣州角", terminal = emptyList()).toZoneOrNull()!!
        assertTrue(z.terminals.isEmpty())
    }
}
