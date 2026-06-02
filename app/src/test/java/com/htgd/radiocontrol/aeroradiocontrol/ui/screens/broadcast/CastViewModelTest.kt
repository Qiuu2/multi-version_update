package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import app.cash.turbine.test
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Media
import com.htgd.radiocontrol.aeroradiocontrol.data.model.MediaFolder
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Terminal
import com.htgd.radiocontrol.aeroradiocontrol.data.model.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.data.model.Zone
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.MediaRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.OnDemandCastAdapter
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.OnDemandCastException
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
 * Unit tests for [CastViewModel] (broadcast SD1). Fakes the adapter (no AAR) +
 * repos. Covers the isAvailable gate, library states, zone→terminal resolution, the
 * no-media / no-targets guards, and castMedia success/failure. NO RECORD_AUDIO is
 * ever involved (点播 is playback) — verified implicitly: the fake adapter has no
 * permission concept and the VM never references one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CastViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private class FakeMediaRepository(
        private val refreshResult: Result<Unit> = Result.success(Unit),
        initialMedia: List<Media> = emptyList(),
    ) : MediaRepository {
        val media = MutableStateFlow(initialMedia)
        var refreshCalls = 0
        override fun observeFolders(): Flow<List<MediaFolder>> = MutableStateFlow(emptyList<MediaFolder>()).asStateFlow()
        override fun observeMedia(): Flow<List<Media>> = media.asStateFlow()
        override suspend fun refresh(): Result<Unit> { refreshCalls++; return refreshResult }
    }

    private class FakeTerminalRepository(zones: List<Zone> = emptyList()) : TerminalRepository {
        val zonesFlow = MutableStateFlow(zones)
        override fun observeZones(): Flow<List<Zone>> = zonesFlow.asStateFlow()
        override fun observeTerminals(): Flow<List<Terminal>> =
            MutableStateFlow(zonesFlow.value.flatMap { it.terminals }).asStateFlow()
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    }

    private class FakeCastAdapter(
        private val available: Boolean = true,
        private val castResult: Result<Unit> = Result.success(Unit),
    ) : OnDemandCastAdapter {
        var lastMediaIds: List<Int>? = null
        var lastTargetIds: List<Int>? = null
        var castCalls = 0
        override fun isAvailable(): Boolean = available
        override suspend fun castMedia(mediaIds: List<Int>, targetTerminalIds: List<Int>): Result<Unit> {
            castCalls++; lastMediaIds = mediaIds; lastTargetIds = targetTerminalIds
            return castResult
        }
        override suspend fun stopCast(): Result<Unit> = Result.success(Unit)
        override suspend fun setCastVolume(volume: Int): Result<Unit> = Result.success(Unit)
    }

    private fun media(id: String) = Media(id = id, name = "media-$id", folderId = "3", durationSeconds = 10)
    private fun terminal(id: String) = Terminal(id = id, name = "T-$id", zoneId = "z1", status = TerminalStatus.Online)
    private fun zone(id: String, terminals: List<Terminal>) = Zone(id = id, name = "Z-$id", terminals = terminals)

    @Test
    fun `unavailable device yields Unavailable and never loads the library`() = runTest {
        val mediaRepo = FakeMediaRepository(initialMedia = listOf(media("1")))
        val vm = CastViewModel(mediaRepo, FakeTerminalRepository(), FakeCastAdapter(available = false))
        assertEquals(CastUiState.Unavailable, vm.uiState.value)
        assertEquals("no library fetch when unavailable", 0, mediaRepo.refreshCalls)
    }

    @Test
    fun `media present yields Ready with mapped items`() = runTest {
        val vm = CastViewModel(
            FakeMediaRepository(initialMedia = listOf(media("1"), media("2"))),
            FakeTerminalRepository(),
            FakeCastAdapter(available = true),
        )
        vm.uiState.test {
            var s = awaitItem()
            while (s !is CastUiState.Ready) s = awaitItem()
            assertEquals(2, (s as CastUiState.Ready).media.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty library after success is Ready empty not Error`() = runTest {
        val vm = CastViewModel(
            FakeMediaRepository(refreshResult = Result.success(Unit), initialMedia = emptyList()),
            FakeTerminalRepository(),
            FakeCastAdapter(available = true),
        )
        vm.uiState.test {
            var s = awaitItem()
            while (s !is CastUiState.Ready) s = awaitItem()
            assertTrue((s as CastUiState.Ready).media.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `failed library load with nothing to show yields Error with fixed copy`() = runTest {
        // ★ Task2: fixed copy "加载失败，请重试" — not the raw exception message.
        val vm = CastViewModel(
            FakeMediaRepository(refreshResult = Result.failure(IllegalStateException("媒体库不可达")), initialMedia = emptyList()),
            FakeTerminalRepository(),
            FakeCastAdapter(available = true),
        )
        vm.uiState.test {
            var s = awaitItem()
            while (s !is CastUiState.Error) s = awaitItem()
            assertEquals("加载失败，请重试", (s as CastUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `castMedia resolves selected zones to numeric terminal ids and casts`() = runTest {
        val terminalRepo = FakeTerminalRepository(
            zones = listOf(
                zone("z1", listOf(terminal("11"), terminal("12"))),
                zone("z2", listOf(terminal("21"))),
                zone("z3", listOf(terminal("31"))), // not selected
            ),
        )
        val adapter = FakeCastAdapter(available = true)
        val vm = CastViewModel(FakeMediaRepository(initialMedia = listOf(media("5"))), terminalRepo, adapter)

        vm.castMedia(selectedMediaIds = setOf("5"), selectedZoneIds = setOf("z1", "z2"))

        assertEquals(1, adapter.castCalls)
        assertEquals(listOf(5), adapter.lastMediaIds)
        assertEquals(listOf(11, 12, 21), adapter.lastTargetIds) // z1+z2 terminals, z3 excluded
        assertEquals(CastResult.Success, vm.castResult.value)
    }

    @Test
    fun `castMedia drops non-numeric terminal ids defensively`() = runTest {
        val terminalRepo = FakeTerminalRepository(
            zones = listOf(zone("z1", listOf(terminal("11"), terminal("not-a-number"), terminal("13")))),
        )
        val adapter = FakeCastAdapter(available = true)
        val vm = CastViewModel(FakeMediaRepository(initialMedia = listOf(media("5"))), terminalRepo, adapter)

        vm.castMedia(setOf("5"), setOf("z1"))
        assertEquals(listOf(11, 13), adapter.lastTargetIds) // "not-a-number" dropped
    }

    @Test
    fun `castMedia with no media selected is a Failure and never calls the adapter`() = runTest {
        val adapter = FakeCastAdapter(available = true)
        val vm = CastViewModel(
            FakeMediaRepository(initialMedia = listOf(media("5"))),
            FakeTerminalRepository(zones = listOf(zone("z1", listOf(terminal("11"))))),
            adapter,
        )
        vm.castMedia(selectedMediaIds = emptySet(), selectedZoneIds = setOf("z1"))
        assertEquals(0, adapter.castCalls)
        assertTrue(vm.castResult.value is CastResult.Failure)
    }

    @Test
    fun `castMedia with no targets is a Failure and never calls the adapter`() = runTest {
        val adapter = FakeCastAdapter(available = true)
        val vm = CastViewModel(
            FakeMediaRepository(initialMedia = listOf(media("5"))),
            FakeTerminalRepository(zones = emptyList()),
            adapter,
        )
        vm.castMedia(selectedMediaIds = setOf("5"), selectedZoneIds = setOf("z1"))
        assertEquals(0, adapter.castCalls)
        assertTrue(vm.castResult.value is CastResult.Failure)
    }

    @Test
    fun `castMedia surfaces adapter failure as a Failure result`() = runTest {
        val adapter = FakeCastAdapter(available = true, castResult = Result.failure(OnDemandCastException(state = -1)))
        val vm = CastViewModel(
            FakeMediaRepository(initialMedia = listOf(media("5"))),
            FakeTerminalRepository(zones = listOf(zone("z1", listOf(terminal("11"))))),
            adapter,
        )
        vm.castMedia(setOf("5"), setOf("z1"))
        assertTrue(vm.castResult.value is CastResult.Failure)
    }

    @Test
    fun `consumeResult clears the one-shot banner`() = runTest {
        val vm = CastViewModel(
            FakeMediaRepository(initialMedia = listOf(media("5"))),
            FakeTerminalRepository(zones = listOf(zone("z1", listOf(terminal("11"))))),
            FakeCastAdapter(available = true),
        )
        vm.castMedia(setOf("5"), setOf("z1"))
        assertTrue(vm.castResult.value != null)
        vm.consumeResult()
        assertEquals(null, vm.castResult.value)
    }
}
