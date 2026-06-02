package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * Unit tests for [temporalStateOf] (PA-14 Phase C — Task 3-state pill derivation).
 *
 * The pill state is derived in fe at render time from `now()` vs `task.time`:
 *   - past               → Done    (已完成)
 *   - within ±1 minute   → Running (进行中)
 *   - future / unparsable → Pending (待执行) safe default
 */
class TemporalStateTest {

    @Test
    fun `task an hour in the past is Done`() {
        assertEquals(TemporalState.Done, temporalStateOf("07:50", LocalTime.of(8, 50)))
    }

    @Test
    fun `task at exactly the same minute is Running`() {
        assertEquals(TemporalState.Running, temporalStateOf("08:00", LocalTime.of(8, 0)))
    }

    @Test
    fun `task one minute ago is still Running (window edge)`() {
        assertEquals(TemporalState.Running, temporalStateOf("08:00", LocalTime.of(8, 1)))
    }

    @Test
    fun `task two minutes ago is Done (outside window)`() {
        assertEquals(TemporalState.Done, temporalStateOf("08:00", LocalTime.of(8, 2)))
    }

    @Test
    fun `task one minute in the future is Running (window edge)`() {
        assertEquals(TemporalState.Running, temporalStateOf("08:00", LocalTime.of(7, 59)))
    }

    @Test
    fun `task two minutes in the future is Pending (outside window)`() {
        assertEquals(TemporalState.Pending, temporalStateOf("08:00", LocalTime.of(7, 58)))
    }

    @Test
    fun `task later today is Pending`() {
        assertEquals(TemporalState.Pending, temporalStateOf("22:00", LocalTime.of(9, 0)))
    }

    @Test
    fun `unparsable time string falls back to Pending`() {
        // R-003 safe default — don't crash, don't mis-classify as Done.
        assertEquals(TemporalState.Pending, temporalStateOf("", LocalTime.of(8, 0)))
        assertEquals(TemporalState.Pending, temporalStateOf("not-a-time", LocalTime.of(8, 0)))
        assertEquals(TemporalState.Pending, temporalStateOf("08", LocalTime.of(8, 0))) // missing minute
    }
}
