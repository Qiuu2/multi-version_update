package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Scheme
import com.htgd.radiocontrol.aeroradiocontrol.data.model.SchemeTask
import com.htgd.radiocontrol.aeroradiocontrol.data.model.SchemeTaskStatus
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TaskLog
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TaskRepository
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
 * Unit tests for [TaskHomeViewModel] (TASK-PA-03c): active-scheme pick, the 5-state
 * derivation, the 任务 30s polling cadence, and setSchemeActive delegation, against a
 * [FakeTaskRepository].
 *
 * Polling/hang note (STD-PERIODIC-TEST, see memory vm-polling-test-viewmodelstore-
 * cancel): bounded advanceTimeBy+runCurrent (never advanceUntilIdle); the VM scope is
 * cancelled via a [ViewModelStore] in [withHome]'s finally before runTest finalizes;
 * the rule's StandardTestDispatcher shares runTest's scheduler so virtual time drives
 * the loop on viewModelScope.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskHomeViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private class FakeTaskRepository(
        private val refreshResult: Result<Unit> = Result.success(Unit),
        initialSchemes: List<Scheme> = emptyList(),
    ) : TaskRepository {
        val schemes = MutableStateFlow(initialSchemes)
        var refreshCalls = 0
        val setActiveCalls = mutableListOf<Pair<String, Boolean>>()

        override fun observeSchemes(): Flow<List<Scheme>> = schemes.asStateFlow()
        override suspend fun refresh(): Result<Unit> { refreshCalls++; return refreshResult }
        override suspend fun setSchemeActive(schemeId: String, active: Boolean): Result<Unit> {
            setActiveCalls += schemeId to active
            return Result.success(Unit)
        }
        override suspend fun getExecutionLog(): Result<List<TaskLog>> = Result.success(emptyList())
    }

    private class FakeForeground(initial: Boolean = true) : AppForegroundState {
        val flow = MutableStateFlow(initial)
        override val isForeground: StateFlow<Boolean> = flow.asStateFlow()
    }

    private fun scheme(id: String, active: Boolean, tasks: List<SchemeTask> = emptyList()) =
        Scheme(id = id, name = "方案-$id", active = active, tasks = tasks)

    private fun task(id: String) =
        SchemeTask(id = id, name = "任务-$id", status = SchemeTaskStatus.Running, startTime = "08:00")

    private inline fun withHome(
        repo: FakeTaskRepository,
        foreground: FakeForeground = FakeForeground(true),
        block: (TaskHomeViewModel) -> Unit,
    ) {
        val store = ViewModelStore()
        val vm = TaskHomeViewModel(repo, foreground)
        store.put("home", vm as ViewModel)
        try {
            block(vm)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `active scheme is shown as Success`() = runTest {
        val repo = FakeTaskRepository(
            initialSchemes = listOf(
                scheme("s1", active = false),
                scheme("s2", active = true, tasks = listOf(task("t1"))),
            ),
        )
        withHome(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is TaskHomeUiState.Success) s = awaitItem()
                assertEquals("方案-s2", (s as TaskHomeUiState.Success).scheme.name) // active one wins
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue("refresh fires on init (polling)", repo.refreshCalls >= 1)
        }
    }

    @Test
    fun `no active flag falls back to the first scheme`() = runTest {
        val repo = FakeTaskRepository(
            initialSchemes = listOf(scheme("s1", active = false), scheme("s2", active = false)),
        )
        withHome(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is TaskHomeUiState.Success) s = awaitItem()
                assertEquals("方案-s1", (s as TaskHomeUiState.Success).scheme.name) // first
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `empty schemes after successful refresh yields Empty (stub case)`() = runTest {
        val repo = FakeTaskRepository(refreshResult = Result.success(Unit), initialSchemes = emptyList())
        withHome(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is TaskHomeUiState.Empty) s = awaitItem()
                assertTrue(s is TaskHomeUiState.Empty)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `failed refresh with no schemes yields Error with fixed copy`() = runTest {
        // ★ Task2: fixed copy "加载失败，请重试" — not the raw exception message.
        val repo = FakeTaskRepository(
            refreshResult = Result.failure(IllegalStateException("网络不可达")),
            initialSchemes = emptyList(),
        )
        withHome(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is TaskHomeUiState.Error) s = awaitItem()
                assertEquals("加载失败，请重试", (s as TaskHomeUiState.Error).message)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `polling refreshes immediately then on the 30s task cadence`() = runTest {
        val repo = FakeTaskRepository(initialSchemes = listOf(scheme("s1", active = true)))
        withHome(repo) {
            runCurrent()
            assertEquals("immediate first refresh", 1, repo.refreshCalls)

            // 10s (terminal cadence) must NOT tick — task cadence is 30s.
            advanceTimeBy(10_000); runCurrent()
            assertEquals("no tick before 30s", 1, repo.refreshCalls)

            advanceTimeBy(20_001); runCurrent()
            assertEquals("ticks at the full 30s", 2, repo.refreshCalls)
        }
    }

    @Test
    fun `pollingState reflects refresh outcome`() = runTest {
        val okRepo = FakeTaskRepository(initialSchemes = listOf(scheme("s1", active = true)))
        withHome(okRepo) { vm ->
            runCurrent()
            assertEquals(PollingState.POLLING, vm.pollingState.value)
        }
        val failRepo = FakeTaskRepository(refreshResult = Result.failure(IllegalStateException("net")))
        withHome(failRepo) { vm ->
            runCurrent()
            assertEquals(PollingState.ERROR, vm.pollingState.value)
        }
    }

    @Test
    fun `setSchemeActive delegates to the repository`() = runTest {
        val repo = FakeTaskRepository(initialSchemes = listOf(scheme("s1", active = false)))
        withHome(repo) { vm ->
            vm.setSchemeActive("s1", true)
            runCurrent()
            assertEquals(listOf("s1" to true), repo.setActiveCalls)
        }
    }

    // ── Task3 (2026-06-01): multi-scheme viewing upgrade ─────────────────────

    @Test
    fun `all schemes are exposed in Success allSchemes and selectScheme cycles through them`() = runTest {
        // Two schemes; both inactive (matches the real-device sechinfo snapshot).
        val repo = FakeTaskRepository(
            initialSchemes = listOf(
                scheme("s1", active = false, tasks = listOf(task("t1"))),
                scheme("s2", active = false, tasks = listOf(task("t2"))),
            ),
        )
        withHome(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is TaskHomeUiState.Success) s = awaitItem()
                val success = s as TaskHomeUiState.Success

                // Both schemes are in allSchemes.
                assertEquals(2, success.allSchemes.size)
                // Default: first scheme (index 0) since no active flag.
                assertEquals(0, success.selectedIndex)
                assertEquals("方案-s1", success.scheme.name)

                // User taps "切换" — selectScheme(1).
                vm.selectScheme(1)
                val next = awaitItem()
                assertTrue(next is TaskHomeUiState.Success)
                assertEquals(1, (next as TaskHomeUiState.Success).selectedIndex)
                assertEquals("方案-s2", next.scheme.name)
                // allSchemes is still the full list.
                assertEquals(2, next.allSchemes.size)

                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `selectScheme does not call setSchemeActive on the repository`() = runTest {
        // "切换查看" ≠ "激活方案" — the repository must never see a setSchemeActive call
        // when the user merely switches the viewed scheme via the header button.
        val repo = FakeTaskRepository(
            initialSchemes = listOf(
                scheme("s1", active = false),
                scheme("s2", active = false),
            ),
        )
        withHome(repo) { vm ->
            vm.uiState.test {
                var s = awaitItem()
                while (s !is TaskHomeUiState.Success) s = awaitItem()

                vm.selectScheme(1)
                awaitItem() // consume the index update

                assertTrue(
                    "selectScheme must not activate a scheme server-side",
                    repo.setActiveCalls.isEmpty(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
