package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Terminal
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus as DomainStatus
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import com.htgd.radiocontrol.aeroradiocontrol.testutil.MainDispatcherRule
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.AppForegroundState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [ZoneDetailViewModel] (TASK-AR-105 + PA-03 polling wiring):
 * per-zone selection, the 5-state derivation, and the 详情 5s polling cadence,
 * against a [FakeTerminalRepository].
 *
 * Polling note (STD-PERIODIC-TEST): same hang-avoidance as the hub test — bounded
 * [advanceTimeBy] + [runCurrent] (never `advanceUntilIdle()`), and the VM scope is
 * cancelled via a [ViewModelStore] (see [withDetail]) before runTest finalizes.
 * The rule's [StandardTestDispatcher] shares runTest's scheduler so virtual time
 * controls the loop on `viewModelScope`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ZoneDetailViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

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

    private class FakeForeground(initial: Boolean = true) : AppForegroundState {
        val flow = MutableStateFlow(initial)
        override val isForeground: StateFlow<Boolean> = flow.asStateFlow()
    }

    private fun terminal(id: String) =
        Terminal(id = id, name = "T-$id", zoneId = "z1", status = DomainStatus.Online)

    /**
     * Builds the VM inside a [ViewModelStore], runs [block], then clears the store
     * so the polling loop on viewModelScope is cancelled before runTest finalizes.
     */
    private inline fun withDetail(
        repo: FakeTerminalRepository,
        foreground: FakeForeground = FakeForeground(true),
        block: (ZoneDetailViewModel) -> Unit,
    ) {
        val store = ViewModelStore()
        val vm = ZoneDetailViewModel(repo, foreground)
        store.put("detail", vm as ViewModel)
        try {
            block(vm)
        } finally {
            store.clear()
        }
    }

    /** Collects emissions until one is of type [T], then asserts on it. */
    private suspend inline fun <reified T : ZoneDetailUiState> StateFlow<ZoneDetailUiState>.awaitState(
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
        withDetail(repo) { vm ->
            vm.load("z1")
            vm.uiState.awaitState<ZoneDetailUiState.Success> {
                assertEquals("教学楼", it.zone.name)
                assertEquals(1, it.zone.terminals.size)
            }
            assertTrue("refresh should fire on load", repo.refreshCalls >= 1)
        }
    }

    @Test
    fun `matched zone with no terminals yields Empty`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "空区", terminals = emptyList())),
        )
        withDetail(repo) { vm ->
            vm.load("z1")
            vm.uiState.awaitState<ZoneDetailUiState.Empty> { assertEquals("空区", it.zone.name) }
        }
    }

    @Test
    fun `unknown zone id after successful refresh yields NotFound`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "z", terminals = listOf(terminal("1")))),
        )
        withDetail(repo) { vm ->
            vm.load("does-not-exist")
            vm.uiState.awaitState<ZoneDetailUiState.NotFound>()
        }
    }

    @Test
    fun `failed refresh with no matching zone yields Error`() = runTest {
        val repo = FakeTerminalRepository(
            refreshResult = Result.failure(IllegalStateException("网络错误")),
            initialZones = emptyList(),
        )
        withDetail(repo) { vm ->
            vm.load("z1")
            vm.uiState.awaitState<ZoneDetailUiState.Error> { assertEquals("网络错误", it.message) }
        }
    }

    // ── PA-03 polling wiring ──────────────────────────────────────────────────

    @Test
    fun `polling starts on load and refreshes on the 5s detail cadence`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "z", terminals = listOf(terminal("1")))),
        )
        withDetail(repo) { vm ->
            runCurrent()
            assertEquals("no polling before load", 0, repo.refreshCalls)

            vm.load("z1")
            runCurrent()
            assertEquals("immediate refresh on load", 1, repo.refreshCalls)

            advanceTimeBy(5_001); runCurrent()
            assertEquals("ticks after one 5s detail cadence", 2, repo.refreshCalls)

            advanceTimeBy(5_001); runCurrent()
            assertEquals("ticks again at the next 5s", 3, repo.refreshCalls)
        }
    }

    @Test
    fun `re-load of the same zone id does not spawn a second poll loop`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "z", terminals = listOf(terminal("1")))),
        )
        withDetail(repo) { vm ->
            vm.load("z1")
            vm.load("z1") // idempotent per id
            runCurrent()
            assertEquals("single immediate refresh, not doubled", 1, repo.refreshCalls)

            advanceTimeBy(5_001); runCurrent()
            assertEquals("single cadence, not doubled", 2, repo.refreshCalls)
        }
    }
}
