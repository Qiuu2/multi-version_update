package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Terminal
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.RecordAudioPermissionException
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceDeviceEvent
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceSession
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceState
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceTalkAdapter
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceUnavailableException
import com.htgd.radiocontrol.aeroradiocontrol.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [VoiceViewModel] (broadcast SD2). FAKES VoiceTalkAdapter (no AAR).
 * Covers: isAvailable gate, empty-target guard, startPaging/startTalk routing, the
 * session-state Flow collection, both typed degradation paths (VoiceUnavailable →
 * Unavailable; RecordAudioPermission → RequestMicPermission effect + retry), stop →
 * endSession + Idle, and the deviceEvents → connection banner.
 *
 * Hang-avoidance (STD-PERIODIC-TEST / memory vm-polling-test-viewmodelstore-cancel):
 * the session collect runs on viewModelScope; the VM is cancelled via a
 * [ViewModelStore] in [withVm]'s finally before runTest finalizes. Default Unconfined
 * rule is fine (no periodic loop — sessions are finite flows).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private class FakeVoiceAdapter(
        private val available: Boolean = true,
        private val startResult: (List<Int>) -> Result<VoiceSession> = { ids ->
            Result.success(VoiceSession(VoiceSession.Kind.TALK, ids, 0L))
        },
        private val sessionStates: List<VoiceState> = listOf(VoiceState.Active),
    ) : VoiceTalkAdapter {
        val deviceEventsFlow = MutableStateFlow<VoiceDeviceEvent>(VoiceDeviceEvent.Idle)
        var startTalkCalls = 0
        var startPagingCalls = 0
        var endSessionCalls = 0
        var lastTargets: List<Int>? = null

        override fun isAvailable(): Boolean = available
        override suspend fun startTalk(targetTerminalIds: List<Int>): Result<VoiceSession> {
            startTalkCalls++; lastTargets = targetTerminalIds; return startResult(targetTerminalIds)
        }
        override suspend fun startPaging(targetTerminalIds: List<Int>): Result<VoiceSession> {
            startPagingCalls++; lastTargets = targetTerminalIds; return startResult(targetTerminalIds)
        }
        override suspend fun endSession(session: VoiceSession) { endSessionCalls++ }
        override fun observeSession(session: VoiceSession): Flow<VoiceState> = flowOf(*sessionStates.toTypedArray())
        override val deviceEvents: StateFlow<VoiceDeviceEvent> get() = deviceEventsFlow.asStateFlow()
    }

    private class FakeTerminalRepository(zones: List<Zone>) : TerminalRepository {
        val zonesFlow = MutableStateFlow(zones)
        override fun observeZones(): Flow<List<Zone>> = zonesFlow.asStateFlow()
        override fun observeTerminals(): Flow<List<Terminal>> =
            MutableStateFlow(zonesFlow.value.flatMap { it.terminals }).asStateFlow()
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    }

    private fun terminal(id: String) = Terminal(id = id, name = "T-$id", zoneId = "z1", status = TerminalStatus.Online)
    private fun zone(id: String, terminals: List<Terminal>) = Zone(id = id, name = "Z-$id", terminals = terminals)
    private fun repoWithZone() = FakeTerminalRepository(listOf(zone("z1", listOf(terminal("11"), terminal("12")))))

    private inline fun withVm(
        adapter: FakeVoiceAdapter,
        terminalRepo: FakeTerminalRepository = repoWithZone(),
        block: (VoiceViewModel) -> Unit,
    ) {
        val store = ViewModelStore()
        val vm = VoiceViewModel(adapter, terminalRepo)
        store.put("voice", vm as ViewModel)
        try {
            block(vm)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `unavailable device yields Unavailable and start is a no-op`() = runTest {
        val adapter = FakeVoiceAdapter(available = false)
        withVm(adapter) { vm ->
            assertEquals(VoiceUiState.Unavailable, vm.uiState.value)
            vm.start(VoiceKind.Talk, setOf("z1"))
            runCurrent()
            assertEquals(0, adapter.startTalkCalls)
            assertEquals(VoiceUiState.Unavailable, vm.uiState.value)
        }
    }

    @Test
    fun `available device starts Idle`() = runTest {
        withVm(FakeVoiceAdapter(available = true)) { vm ->
            assertEquals(VoiceUiState.Idle, vm.uiState.value)
        }
    }

    @Test
    fun `empty targets emits a Message effect and never calls the adapter`() = runTest {
        val adapter = FakeVoiceAdapter()
        withVm(adapter, terminalRepo = FakeTerminalRepository(emptyList())) { vm ->
            vm.effects.test {
                vm.start(VoiceKind.Talk, setOf("z1")) // z1 doesn't exist → no terminals
                runCurrent()
                assertTrue(awaitItem() is VoiceEffect.Message)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(0, adapter.startTalkCalls)
        }
    }

    @Test
    fun `startPaging routes to paging and resolves zone to terminal ids`() = runTest {
        val adapter = FakeVoiceAdapter(sessionStates = listOf(VoiceState.Active))
        withVm(adapter) { vm ->
            vm.start(VoiceKind.Page, setOf("z1"))
            runCurrent()
            assertEquals(1, adapter.startPagingCalls)
            assertEquals(0, adapter.startTalkCalls)
            assertEquals(listOf(11, 12), adapter.lastTargets) // resolved + dedup
        }
    }

    @Test
    fun `talk session flows through to Active`() = runTest {
        val adapter = FakeVoiceAdapter(
            sessionStates = listOf(VoiceState.Connecting, VoiceState.Waiting, VoiceState.Active),
        )
        withVm(adapter) { vm ->
            vm.start(VoiceKind.Talk, setOf("z1"))
            runCurrent()
            assertEquals(1, adapter.startTalkCalls)
            assertEquals(VoiceUiState.Active, vm.uiState.value) // last emitted state
        }
    }

    @Test
    fun `session Ended returns to Idle`() = runTest {
        val adapter = FakeVoiceAdapter(sessionStates = listOf(VoiceState.Active, VoiceState.Ended))
        withVm(adapter) { vm ->
            vm.start(VoiceKind.Talk, setOf("z1"))
            runCurrent()
            assertEquals(VoiceUiState.Idle, vm.uiState.value)
        }
    }

    @Test
    fun `session Refused surfaces Refused`() = runTest {
        val adapter = FakeVoiceAdapter(sessionStates = listOf(VoiceState.Refused))
        withVm(adapter) { vm ->
            vm.start(VoiceKind.Talk, setOf("z1"))
            runCurrent()
            assertEquals(VoiceUiState.Refused, vm.uiState.value)
        }
    }

    @Test
    fun `VoiceUnavailable failure degrades to Unavailable`() = runTest {
        val adapter = FakeVoiceAdapter(startResult = { Result.failure(VoiceUnavailableException()) })
        withVm(adapter) { vm ->
            vm.start(VoiceKind.Talk, setOf("z1"))
            runCurrent()
            assertEquals(VoiceUiState.Unavailable, vm.uiState.value)
        }
    }

    @Test
    fun `RecordAudioPermission failure emits RequestMicPermission with the kind`() = runTest {
        val adapter = FakeVoiceAdapter(startResult = { Result.failure(RecordAudioPermissionException()) })
        withVm(adapter) { vm ->
            vm.effects.test {
                vm.start(VoiceKind.Page, setOf("z1"))
                runCurrent()
                val effect = awaitItem()
                assertTrue(effect is VoiceEffect.RequestMicPermission)
                assertEquals(VoiceKind.Page, (effect as VoiceEffect.RequestMicPermission).retryKind)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(VoiceUiState.Idle, vm.uiState.value) // Connecting reset
        }
    }

    @Test
    fun `onMicPermissionResult granted retries the start`() = runTest {
        val adapter = FakeVoiceAdapter(sessionStates = listOf(VoiceState.Active))
        withVm(adapter) { vm ->
            vm.onMicPermissionResult(granted = true, kind = VoiceKind.Talk, selectedZoneIds = setOf("z1"))
            runCurrent()
            assertEquals(1, adapter.startTalkCalls)
            assertEquals(VoiceUiState.Active, vm.uiState.value)
        }
    }

    @Test
    fun `onMicPermissionResult denied yields Error`() = runTest {
        val adapter = FakeVoiceAdapter()
        withVm(adapter) { vm ->
            vm.onMicPermissionResult(granted = false, kind = VoiceKind.Talk, selectedZoneIds = setOf("z1"))
            runCurrent()
            assertEquals(0, adapter.startTalkCalls)
            assertTrue(vm.uiState.value is VoiceUiState.Error)
        }
    }

    @Test
    fun `start failure other than typed gates yields Error`() = runTest {
        val adapter = FakeVoiceAdapter(startResult = { Result.failure(IllegalStateException("会话拒绝")) })
        withVm(adapter) { vm ->
            vm.start(VoiceKind.Talk, setOf("z1"))
            runCurrent()
            assertEquals(VoiceUiState.Error("会话拒绝"), vm.uiState.value)
        }
    }

    @Test
    fun `stop ends the session and returns to Idle`() = runTest {
        // A never-completing session so it stays Active until we stop it.
        val adapter = FakeVoiceAdapter(sessionStates = listOf(VoiceState.Active))
        withVm(adapter) { vm ->
            vm.start(VoiceKind.Talk, setOf("z1"))
            runCurrent()
            vm.stop()
            runCurrent()
            assertEquals(1, adapter.endSessionCalls)
            assertEquals(VoiceUiState.Idle, vm.uiState.value)
        }
    }

    @Test
    fun `connection reflects deviceEvents`() = runTest {
        val adapter = FakeVoiceAdapter()
        withVm(adapter) { vm ->
            vm.connection.test {
                assertEquals(VoiceConnection.Unknown, awaitItem())
                adapter.deviceEventsFlow.value = VoiceDeviceEvent.Connected(true)
                assertEquals(VoiceConnection.Connected, awaitItem())
                adapter.deviceEventsFlow.value = VoiceDeviceEvent.Connected(false)
                assertEquals(VoiceConnection.Disconnected, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
