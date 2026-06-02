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
 * Unit tests for [SchemeDetailViewModel] (TASK-PA-03c): per-scheme selection, the
 * 5-state derivation (incl. NotFound ≠ Empty), the 5s detail cadence, and setActive
 * delegation. Same hang-avoidance as [TaskHomeViewModelTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SchemeDetailViewModelTest {

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
            setActiveCalls += schemeId to active; return Result.success(Unit)
        }
        override suspend fun getExecutionLog(): Result<List<TaskLog>> = Result.success(emptyList())
    }

    private class FakeForeground(initial: Boolean = true) : AppForegroundState {
        val flow = MutableStateFlow(initial)
        override val isForeground: StateFlow<Boolean> = flow.asStateFlow()
    }

    private fun scheme(id: String, tasks: List<SchemeTask> = emptyList()) =
        Scheme(id = id, name = "方案-$id", active = true, tasks = tasks)

    private fun task(id: String) =
        SchemeTask(id = id, name = "任务-$id", status = SchemeTaskStatus.Idle, startTime = "08:00")

    private inline fun withDetail(
        repo: FakeTaskRepository,
        foreground: FakeForeground = FakeForeground(true),
        block: (SchemeDetailViewModel) -> Unit,
    ) {
        val store = ViewModelStore()
        val vm = SchemeDetailViewModel(repo, foreground)
        store.put("detail", vm as ViewModel)
        try {
            block(vm)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `matched scheme with tasks yields Success`() = runTest {
        val repo = FakeTaskRepository(initialSchemes = listOf(scheme("s1", tasks = listOf(task("t1")))))
        withDetail(repo) { vm ->
            vm.load("s1")
            vm.uiState.test {
                var s = awaitItem()
                while (s !is SchemeDetailUiState.Success) s = awaitItem()
                assertEquals("方案-s1", (s as SchemeDetailUiState.Success).scheme.name)
                cancelAndIgnoreRemainingEvents()
            }
            assertTrue("refresh fires on load", repo.refreshCalls >= 1)
        }
    }

    @Test
    fun `matched scheme with no tasks yields Empty`() = runTest {
        val repo = FakeTaskRepository(initialSchemes = listOf(scheme("s1", tasks = emptyList())))
        withDetail(repo) { vm ->
            vm.load("s1")
            vm.uiState.test {
                var s = awaitItem()
                while (s !is SchemeDetailUiState.Empty) s = awaitItem()
                assertEquals("方案-s1", (s as SchemeDetailUiState.Empty).scheme.name)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `unknown scheme id after successful refresh yields NotFound`() = runTest {
        val repo = FakeTaskRepository(initialSchemes = listOf(scheme("s1", tasks = listOf(task("t1")))))
        withDetail(repo) { vm ->
            vm.load("does-not-exist")
            vm.uiState.test {
                var s = awaitItem()
                while (s !is SchemeDetailUiState.NotFound) s = awaitItem()
                assertTrue(s is SchemeDetailUiState.NotFound)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `failed refresh with no matching scheme yields Error with fixed copy`() = runTest {
        // ★ Task2: fixed copy "加载失败，请重试" — not the raw exception message.
        val repo = FakeTaskRepository(
            refreshResult = Result.failure(IllegalStateException("网络错误")),
            initialSchemes = emptyList(),
        )
        withDetail(repo) { vm ->
            vm.load("s1")
            vm.uiState.test {
                var s = awaitItem()
                while (s !is SchemeDetailUiState.Error) s = awaitItem()
                assertEquals("加载失败，请重试", (s as SchemeDetailUiState.Error).message)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `polling starts on load and refreshes on the 5s detail cadence`() = runTest {
        val repo = FakeTaskRepository(initialSchemes = listOf(scheme("s1", tasks = listOf(task("t1")))))
        withDetail(repo) { vm ->
            runCurrent()
            assertEquals("no polling before load", 0, repo.refreshCalls)

            vm.load("s1")
            runCurrent()
            assertEquals("immediate refresh on load", 1, repo.refreshCalls)

            advanceTimeBy(5_001); runCurrent()
            assertEquals("ticks after one 5s cadence", 2, repo.refreshCalls)
        }
    }

    @Test
    fun `setActive delegates to the repository with the loaded id`() = runTest {
        val repo = FakeTaskRepository(initialSchemes = listOf(scheme("s1", tasks = listOf(task("t1")))))
        withDetail(repo) { vm ->
            vm.load("s1")
            runCurrent()
            vm.setActive(false)
            runCurrent()
            assertEquals(listOf("s1" to false), repo.setActiveCalls)
        }
    }

    @Test
    fun `setActive before load is a no-op`() = runTest {
        val repo = FakeTaskRepository(initialSchemes = listOf(scheme("s1")))
        withDetail(repo) { vm ->
            vm.setActive(true) // no id bound yet
            runCurrent()
            assertTrue("no repo call without a loaded id", repo.setActiveCalls.isEmpty())
        }
    }
}
