package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerHealth
import com.htgd.radiocontrol.aeroradiocontrol.data.model.ServerState
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.ServerStateRepository
import com.htgd.radiocontrol.aeroradiocontrol.testutil.MainDispatcherRule
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
 * Unit tests for [ServiceViewModel] (TASK-PA Service): the state derivation, the
 * domain→UI health mapping (incl. Unknown), and the 20s health polling cadence,
 * against a [FakeServerStateRepository].
 *
 * Polling/hang note (STD-PERIODIC-TEST, memory vm-polling-test-viewmodelstore-cancel):
 * bounded advanceTimeBy+runCurrent (never advanceUntilIdle); the VM scope is cancelled
 * via a [ViewModelStore] in [withVm]'s finally; the rule's StandardTestDispatcher
 * shares runTest's scheduler so virtual time drives the loop on viewModelScope.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServiceViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private class FakeServerStateRepository(
        private val refreshResult: Result<Unit> = Result.success(Unit),
        initial: ServerState? = null,
    ) : ServerStateRepository {
        val server = MutableStateFlow(initial)
        var refreshCalls = 0
        override fun observeServerState(): Flow<ServerState?> = server.asStateFlow()
        override suspend fun refresh(): Result<Unit> { refreshCalls++; return refreshResult }
    }

    private class FakeForeground(initial: Boolean = true) : AppForegroundState {
        val flow = MutableStateFlow(initial)
        override val isForeground: StateFlow<Boolean> = flow.asStateFlow()
    }

    private inline fun withVm(
        repo: FakeServerStateRepository,
        foreground: FakeForeground = FakeForeground(true),
        block: (ServiceViewModel) -> Unit,
    ) {
        val store = ViewModelStore()
        val vm = ServiceViewModel(repo, foreground)
        store.put("service", vm as ViewModel)
        try {
            block(vm)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `server snapshot present yields Success with mapped health`() = runTest {
        val repo = FakeServerStateRepository(
            initial = ServerState(health = ServerHealth.Online, name = "广播主机", connection = 5, maxConnection = 100L),
        )
        withVm(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is ServiceUiState.Success) s = awaitItem()
                val ui = (s as ServiceUiState.Success).server
                assertEquals(ServiceHealthUi.Online, ui.health)
                assertEquals("广播主机", ui.name)
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue("refresh fires on init (polling)", repo.refreshCalls >= 1)
        }
    }

    @Test
    fun `unknown health maps to a non-crashing UI value`() = runTest {
        val repo = FakeServerStateRepository(
            initial = ServerState(health = ServerHealth.Unknown("x-9")),
        )
        withVm(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is ServiceUiState.Success) s = awaitItem()
                assertEquals(ServiceHealthUi.Unknown, (s as ServiceUiState.Success).server.health)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `failed refresh with no snapshot yields Error carrying the message`() = runTest {
        val repo = FakeServerStateRepository(
            refreshResult = Result.failure(IllegalStateException("服务器无响应")),
            initial = null,
        )
        withVm(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is ServiceUiState.Error) s = awaitItem()
                assertEquals("服务器无响应", (s as ServiceUiState.Error).message)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `polling refreshes immediately then on the 20s cadence`() = runTest {
        val repo = FakeServerStateRepository(initial = ServerState(health = ServerHealth.Online))
        withVm(repo) {
            runCurrent()
            assertEquals("immediate first refresh", 1, repo.refreshCalls)

            // 10s (terminal cadence) must NOT tick — service cadence is 20s.
            advanceTimeBy(10_000); runCurrent()
            assertEquals("no tick before 20s", 1, repo.refreshCalls)

            advanceTimeBy(10_001); runCurrent()
            assertEquals("ticks at the full 20s", 2, repo.refreshCalls)
        }
    }

    @Test
    fun `pollingState reflects refresh outcome`() = runTest {
        val okRepo = FakeServerStateRepository(initial = ServerState(health = ServerHealth.Online))
        withVm(okRepo) { vm ->
            runCurrent()
            assertEquals(PollingState.POLLING, vm.pollingState.value)
        }
        val failRepo = FakeServerStateRepository(refreshResult = Result.failure(IllegalStateException("net")))
        withVm(failRepo) { vm ->
            runCurrent()
            assertEquals(PollingState.ERROR, vm.pollingState.value)
        }
    }

    @Test
    fun `backgrounded app does not poll until foreground`() = runTest {
        val repo = FakeServerStateRepository(initial = ServerState(health = ServerHealth.Online))
        val foreground = FakeForeground(initial = false)
        withVm(repo, foreground) {
            runCurrent()
            assertEquals("backgrounded: no refresh", 0, repo.refreshCalls)
            foreground.flow.value = true
            runCurrent()
            assertEquals("resumes on foreground", 1, repo.refreshCalls)
        }
    }
}
