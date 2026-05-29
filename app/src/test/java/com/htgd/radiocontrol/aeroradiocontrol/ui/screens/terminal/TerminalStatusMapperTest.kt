package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus as DomainStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus as UiStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for the domain→UI [toUiStatus] mapping (TASK-AR-102). */
class TerminalStatusMapperTest {

    @Test
    fun `known statuses map one to one`() {
        assertEquals(UiStatus.Online, DomainStatus.Online.toUiStatus())
        assertEquals(UiStatus.Offline, DomainStatus.Offline.toUiStatus())
        assertEquals(UiStatus.Fault, DomainStatus.Fault.toUiStatus())
        assertEquals(UiStatus.Playing, DomainStatus.Playing.toUiStatus())
        assertEquals(UiStatus.Paging, DomainStatus.Paging.toUiStatus())
    }

    @Test
    fun `unknown maps to the safe neutral Offline fallback`() {
        assertEquals(UiStatus.Offline, DomainStatus.Unknown("anything").toUiStatus())
        assertEquals(UiStatus.Offline, DomainStatus.Unknown("").toUiStatus())
    }
}
