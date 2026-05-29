package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Terminal
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus as DomainStatus
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import com.htgd.radiocontrol.aeroradiocontrol.testutil.MainDispatcherRule
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus as UiStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.AppForegroundState
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.PollingState
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
 * Unit tests for [TerminalHubViewModel] (TASK-AR-102 + PA-03 polling wiring):
 * the 5-state derivation, the domain→UI mapping, and the 终端 10s polling cadence,
 * against a [FakeTerminalRepository].
 *
 * Polling note (STD-PERIODIC-TEST / memory periodic-coroutine-test-no-advanceuntilidle):
 * the VM now runs an unbounded `delay`-loop on its `viewModelScope`. Two rules keep
 * the suite from hanging:
 *   1. Step virtual time with bounded [advanceTimeBy] + [runCurrent] — NEVER
 *      `advanceUntilIdle()`, which would advance the never-idle poll loop forever.
 *   2. Cancel the VM's scope before the `runTest` body ends, via a [ViewModelStore]
 *      ([store].clear() → ViewModel.clear() → viewModelScope cancelled). Done in
 *      [withHub]'s finally so the loop is gone before runTest's finalization
 *      `advanceUntilIdle()` runs and cannot deadlock it. (viewModelScope can't be
 *      a TestScope.backgroundScope the way the scheduler's own test does it, so we
 *      cancel it explicitly instead.)
 *
 * To control virtual time, the rule's dispatcher is a [StandardTestDispatcher] and
 * `runTest` shares its scheduler — so `viewModelScope` (Dispatchers.Main = the rule)
 * advances under the same clock as [advanceTimeBy].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TerminalHubViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

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

    private class FakeForeground(initial: Boolean = true) : AppForegroundState {
        val flow = MutableStateFlow(initial)
        override val isForeground: StateFlow<Boolean> = flow.asStateFlow()
    }

    private fun terminal(id: String, status: DomainStatus) =
        Terminal(id = id, name = "T-$id", zoneId = "z1", status = status)

    /**
     * Builds the VM inside a [ViewModelStore], runs [block], then clears the store
     * so the polling loop on viewModelScope is cancelled before runTest finalizes.
     */
    private inline fun withHub(
        repo: FakeTerminalRepository,
        foreground: FakeForeground = FakeForeground(true),
        block: (TerminalHubViewModel) -> Unit,
    ) {
        val store = ViewModelStore()
        val vm = TerminalHubViewModel(repo, foreground)
        store.put("hub", vm as ViewModel)
        try {
            block(vm)
        } finally {
            store.clear() // cancels viewModelScope → ends the poll loop
        }
    }

    @Test
    fun `zones present yields Success with mapped UI status`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(
                Zone("z1", "教学楼", terminals = listOf(terminal("1", DomainStatus.Online))),
            ),
        )
        withHub(repo) { vm ->
            vm.uiState.test {
                val emissions = mutableListOf<TerminalHubUiState>()
                emissions += awaitItem()
                while (emissions.last() !is TerminalHubUiState.Success) emissions += awaitItem()
                val success = emissions.last() as TerminalHubUiState.Success
                assertEquals(1, success.zones.size)
                assertEquals(UiStatus.Online, success.zones.first().terminals.first().status)
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue("refresh should fire on init (polling start)", repo.refreshCalls >= 1)
        }
    }

    @Test
    fun `empty zones after successful refresh yields Empty`() = runTest {
        val repo = FakeTerminalRepository(refreshResult = Result.success(Unit), initialZones = emptyList())
        withHub(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is TerminalHubUiState.Empty) s = awaitItem()
                assertTrue(s is TerminalHubUiState.Empty)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `failed refresh with no data yields Error carrying the message`() = runTest {
        val repo = FakeTerminalRepository(
            refreshResult = Result.failure(IllegalStateException("网络不可达")),
            initialZones = emptyList(),
        )
        withHub(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is TerminalHubUiState.Error) s = awaitItem()
                assertEquals("网络不可达", (s as TerminalHubUiState.Error).message)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `unknown domain status maps to a non-crashing UI status`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(
                Zone("z1", "z", terminals = listOf(terminal("1", DomainStatus.Unknown("weird-42")))),
            ),
        )
        withHub(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is TerminalHubUiState.Success) s = awaitItem()
                // Unknown maps to the safe neutral UI state (Offline) — no crash, renders.
                assertEquals(UiStatus.Offline, (s as TerminalHubUiState.Success).zones.first().terminals.first().status)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `manual refresh re-invokes the repository`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "z", terminals = listOf(terminal("1", DomainStatus.Online)))),
        )
        withHub(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is TerminalHubUiState.Success) s = awaitItem()
                val before = repo.refreshCalls
                vm.refresh()
                runCurrent()
                assertEquals(before + 1, repo.refreshCalls)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    // ── PA-03 polling wiring ──────────────────────────────────────────────────

    @Test
    fun `polling refreshes immediately on init then on the 10s terminal cadence`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "z", terminals = listOf(terminal("1", DomainStatus.Online)))),
        )
        withHub(repo) {
            runCurrent()
            assertEquals("immediate first refresh", 1, repo.refreshCalls)

            advanceTimeBy(10_001); runCurrent()
            assertEquals("ticks after one 10s cadence", 2, repo.refreshCalls)

            // A 5s advance must NOT tick (terminal cadence is 10s, not the detail 5s).
            advanceTimeBy(5_000); runCurrent()
            assertEquals("no extra tick before the full 10s", 2, repo.refreshCalls)

            advanceTimeBy(5_001); runCurrent()
            assertEquals("ticks again at the next 10s", 3, repo.refreshCalls)
        }
    }

    @Test
    fun `pollingState reflects refresh outcome`() = runTest {
        val okRepo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "z", terminals = listOf(terminal("1", DomainStatus.Online)))),
        )
        withHub(okRepo) { vm ->
            runCurrent()
            assertEquals(PollingState.POLLING, vm.pollingState.value) // success → POLLING
        }

        val failRepo = FakeTerminalRepository(refreshResult = Result.failure(IllegalStateException("net")))
        withHub(failRepo) { vm ->
            runCurrent()
            assertEquals(PollingState.ERROR, vm.pollingState.value) // failure → ERROR banner
        }
    }

    @Test
    fun `backgrounded app does not poll until foreground`() = runTest {
        val repo = FakeTerminalRepository(
            initialZones = listOf(Zone("z1", "z", terminals = listOf(terminal("1", DomainStatus.Online)))),
        )
        val foreground = FakeForeground(initial = false)
        withHub(repo, foreground) {
            runCurrent()
            assertEquals("backgrounded: no refresh", 0, repo.refreshCalls)

            foreground.flow.value = true
            runCurrent()
            assertEquals("resumes on foreground", 1, repo.refreshCalls)
        }
    }
}
