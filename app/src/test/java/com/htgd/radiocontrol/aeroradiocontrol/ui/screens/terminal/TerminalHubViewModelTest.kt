package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import app.cash.turbine.test
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Terminal
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus as DomainStatus
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import com.htgd.radiocontrol.aeroradiocontrol.testutil.MainDispatcherRule
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus as UiStatus
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
 * Unit tests for [TerminalHubViewModel] (TASK-AR-102): the 5-state derivation and
 * the domain→UI mapping, against a [FakeTerminalRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TerminalHubViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    /** In-memory repo: tests push zones into the SSOT and pick refresh outcome. */
    private class FakeTerminalRepository(
        private val refreshResult: Result<Unit> = Result.success(Unit),
        initialZones: List<Zone> = emptyList(),
    ) : TerminalRepository {
        val zones = MutableStateFlow(initialZones)
        var refreshCalls = 0

        override fun observeZones(): Flow<List<Zone>> = zones.asStateFlow()
        override fun observeTerminals(): Flow<List<Terminal>> =
            MutableStateFlow(zones.value.flatMap { it.terminals }).asStateFlow()

        override suspend fun refresh(): Result<Unit> {
            refreshCalls++
            return refreshResult
        }
    }

    private fun terminal(id: String, status: DomainStatus) =
        Terminal(id = id, name = "T-$id", zoneId = "z1", status = status)

    @Test
    fun `zones present yields Success with mapped UI status`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(
                Zone("z1", "教学楼", terminals = listOf(terminal("1", DomainStatus.Online))),
            ),
        )
        val vm = TerminalHubViewModel(repo)

        vm.uiState.test {
            // WhileSubscribed emits the initial value, then the combined success.
            val emissions = mutableListOf<TerminalHubUiState>()
            emissions += awaitItem()
            while (emissions.last() !is TerminalHubUiState.Success) emissions += awaitItem()
            val success = emissions.last() as TerminalHubUiState.Success
            assertEquals(1, success.zones.size)
            assertEquals(UiStatus.Online, success.zones.first().terminals.first().status)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue("refresh should fire on init", repo.refreshCalls >= 1)
    }

    @Test
    fun `empty zones after successful refresh yields Empty`() = runTest {
        val repo = FakeTerminalRepository(refreshResult = Result.success(Unit), initialZones = emptyList())
        val vm = TerminalHubViewModel(repo)

        vm.uiState.test {
            var s = awaitItem()
            while (s !is TerminalHubUiState.Empty) s = awaitItem()
            assertTrue(s is TerminalHubUiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed refresh with no data yields Error carrying the message`() = runTest {
        val repo = FakeTerminalRepository(
            refreshResult = Result.failure(IllegalStateException("网络不可达")),
            initialZones = emptyList(),
        )
        val vm = TerminalHubViewModel(repo)

        vm.uiState.test {
            var s = awaitItem()
            while (s !is TerminalHubUiState.Error) s = awaitItem()
            assertEquals("网络不可达", (s as TerminalHubUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unknown domain status maps to a non-crashing UI status`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(
                Zone("z1", "z", terminals = listOf(terminal("1", DomainStatus.Unknown("weird-42")))),
            ),
        )
        val vm = TerminalHubViewModel(repo)

        vm.uiState.test {
            var s = awaitItem()
            while (s !is TerminalHubUiState.Success) s = awaitItem()
            // Unknown maps to the safe neutral UI state (Offline) — no crash, renders.
            assertEquals(UiStatus.Offline, (s as TerminalHubUiState.Success).zones.first().terminals.first().status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `manual refresh re-invokes the repository`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "z", terminals = listOf(terminal("1", DomainStatus.Online)))),
        )
        val vm = TerminalHubViewModel(repo)

        vm.uiState.test {
            var s = awaitItem()
            while (s !is TerminalHubUiState.Success) s = awaitItem()
            val before = repo.refreshCalls
            vm.refresh()
            assertEquals(before + 1, repo.refreshCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
