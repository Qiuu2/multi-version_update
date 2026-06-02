package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import com.htgd.radiocontrol.aeroradiocontrol.data.model.Media
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the 点播 (Cast) media domain→UI mapping (broadcast SD1).
 */
class CastUiMapperTest {

    @Test
    fun `media maps with formatted duration`() {
        val ui = Media(id = "7", name = "课间操音乐", folderId = "3", durationSeconds = 200).toMediaUi()
        assertEquals("7", ui.id)
        assertEquals("课间操音乐", ui.name)
        assertEquals("03:20", ui.duration) // 200s = 3:20
    }

    @Test
    fun `null or negative duration renders as blank-tolerant placeholder`() {
        assertEquals("--", Media(id = "1", name = "a", folderId = "3", durationSeconds = null).toMediaUi().duration)
        assertEquals("--", Media(id = "2", name = "b", folderId = "3", durationSeconds = -5).toMediaUi().duration)
    }

    @Test
    fun `sub-minute duration zero-pads`() {
        assertEquals("00:08", Media(id = "3", name = "铃", folderId = "3", durationSeconds = 8).toMediaUi().duration)
    }
}
