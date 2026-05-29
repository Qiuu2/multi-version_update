package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import app.cash.turbine.test
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Terminal
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus as DomainStatus
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
 * Unit tests for [ZoneDetailViewModel] (TASK-AR-105): per-zone selection + the
 * 5-state derivation against a [FakeTerminalRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ZoneDetailViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private class FakeTerminalRepository(
        private val refreshResult: Result<Unit> = Result.success(Unit),
        initialZones: List<Zone> = emptyList(),
    ) : TerminalRepository {
        val zones = MutableStateFlow(initialZones)
        var refreshCalls = 0
        override fun observeZones(): Flow<List<Zone>> = zones.asStateFlow()
        override fun observeTerminals(): Flow<List<Terminal>> =
            MutableStateFlow(zones.value.flatMap { it.terminals }).asStateFlow()
        override suspend fun refresh(): Result<Unit> { refreshCalls++; return refreshResult }
    }

    private fun terminal(id: String) =
        Terminal(id = id, name = "T-$id", zoneId = "z1", status = DomainStatus.Online)

    /** Collects emissions until one is of type [T], then asserts on it. */
    private suspend inline fun <reified T : ZoneDetailUiState> kotlinx.coroutines.flow.StateFlow<ZoneDetailUiState>.awaitState(
        crossinline assert: (T) -> Unit = {},
    ) = test {
        var item = awaitItem()
        while (item !is T) item = awaitItem()
        assert(item as T)
        cancelAndIgnoreRemainingEvents()
    }

    @Test
    fun `matched zone with terminals yields Success`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "教学楼", terminals = listOf(terminal("1")))),
        )
        val vm = ZoneDetailViewModel(repo)
        vm.load("z1")

        vm.uiState.awaitState<ZoneDetailUiState.Success> {
            assertEquals("教学楼", it.zone.name)
            assertEquals(1, it.zone.terminals.size)
        }
        assertTrue("refresh should fire on load", repo.refreshCalls >= 1)
    }

    @Test
    fun `matched zone with no terminals yields Empty`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "空区", terminals = emptyList())),
        )
        val vm = ZoneDetailViewModel(repo)
        vm.load("z1")
        vm.uiState.awaitState<ZoneDetailUiState.Empty> { assertEquals("空区", it.zone.name) }
    }

    @Test
    fun `unknown zone id after successful refresh yields NotFound`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "z", terminals = listOf(terminal("1")))),
        )
        val vm = ZoneDetailViewModel(repo)
        vm.load("does-not-exist")
        vm.uiState.awaitState<ZoneDetailUiState.NotFound>()
    }

    @Test
    fun `failed refresh with no matching zone yields Error`() = runTest {
        val repo = FakeTerminalRepository(
            refreshResult = Result.failure(IllegalStateException("网络错误")),
            initialZones = emptyList(),
        )
        val vm = ZoneDetailViewModel(repo)
        vm.load("z1")
        vm.uiState.awaitState<ZoneDetailUiState.Error> { assertEquals("网络错误", it.message) }
    }
}
