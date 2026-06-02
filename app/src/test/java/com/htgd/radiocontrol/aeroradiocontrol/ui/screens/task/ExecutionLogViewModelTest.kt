package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task

import com.htgd.radiocontrol.aeroradiocontrol.data.model.Scheme
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TaskLog
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TaskRepository
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
 * Unit tests for [ExecutionLogViewModel] (TASK-PA-03c): one-shot fetch → Success /
 * Empty / Error. No polling (a log is an on-demand snapshot), so the default
 * Unconfined rule + runTest suffice (no infinite loop to cancel).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExecutionLogViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private class FakeTaskRepository(
        private val logResult: Result<List<TaskLog>> = Result.success(emptyList()),
    ) : TaskRepository {
        override fun observeSchemes(): Flow<List<Scheme>> = MutableStateFlow(emptyList<Scheme>()).asStateFlow()
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
        override suspend fun setSchemeActive(schemeId: String, active: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun getExecutionLog(): Result<List<TaskLog>> = logResult
    }

    @Test
    fun `logs present yields Success with mapped entries`() = runTest {
        val repo = FakeTaskRepository(
            logResult = Result.success(
                listOf(TaskLog(id = "l1", taskName = "上课铃", timestamp = "08:00:02", message = "教学楼 A")),
            ),
        )
        val vm = ExecutionLogViewModel(repo)
        vm.load()
        val state = vm.uiState.value
        assertTrue(state is ExecutionLogUiState.Success)
        val entries = (state as ExecutionLogUiState.Success).entries
        assertEquals(1, entries.size)
        assertEquals("上课铃", entries.first().title)
    }

    @Test
    fun `empty log yields Empty (stub case)`() = runTest {
        val vm = ExecutionLogViewModel(FakeTaskRepository(logResult = Result.success(emptyList())))
        vm.load()
        assertEquals(ExecutionLogUiState.Empty, vm.uiState.value)
    }

    @Test
    fun `failed fetch yields Error with fixed copy`() = runTest {
        // ★ Task2: fixed copy "加载失败，请重试" — not the raw exception message.
        val vm = ExecutionLogViewModel(
            FakeTaskRepository(logResult = Result.failure(IllegalStateException("日志服务不可达"))),
        )
        vm.load()
        val state = vm.uiState.value
        assertTrue(state is ExecutionLogUiState.Error)
        assertEquals("加载失败，请重试", (state as ExecutionLogUiState.Error).message)
    }
}
