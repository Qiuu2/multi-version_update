package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import app.cash.turbine.test
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Terminal
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import com.htgd.radiocontrol.aeroradiocontrol.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [BroadcastTargetsViewModel] (TASK-AR-106): the zone→ZoneTarget
 * projection for the broadcast / temp-file target pickers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BroadcastTargetsViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private class FakeTerminalRepository(zones: List<Zone>) : TerminalRepository {
        private val state = MutableStateFlow(zones)
        var refreshCalls = 0
        override fun observeZones(): Flow<List<Zone>> = state.asStateFlow()
        override fun observeTerminals(): Flow<List<Terminal>> = MutableStateFlow(emptyList<Terminal>()).asStateFlow()
        override suspend fun refresh(): Result<Unit> { refreshCalls++; return Result.success(Unit) }
    }

    @Test
    fun `projects zones to id-name targets and refreshes on init`() = runTest {
        val repo = FakeTerminalRepository(
            listOf(
                Zone("z1", "教学楼", terminals = listOf(Terminal("t1", "T", "z1", TerminalStatus.Online))),
                Zone("z2", "运动场"),
            ),
        )
        val vm = BroadcastTargetsViewModel(repo)

        vm.zones.test {
            var item = awaitItem()
            while (item.isEmpty()) item = awaitItem()
            assertEquals(listOf(ZoneTarget("z1", "教学楼"), ZoneTarget("z2", "运动场")), item)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue("init should trigger a refresh", repo.refreshCalls >= 1)
    }

    @Test
    fun `empty zones projects to empty target list (no crash)`() = runTest {
        val vm = BroadcastTargetsViewModel(FakeTerminalRepository(emptyList()))
        assertEquals(emptyList<ZoneTarget>(), vm.zones.value)
    }
}
