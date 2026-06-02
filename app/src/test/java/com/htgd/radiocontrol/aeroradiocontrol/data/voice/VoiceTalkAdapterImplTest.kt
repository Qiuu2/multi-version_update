package com.htgd.radiocontrol.aeroradiocontrol.data.voice

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [VoiceTalkAdapterImpl]'s capability + permission GATE — the
 * receipt-independent logic that runs before any native call.
 *
 * Runs on a plain JVM via fake [VoiceNativeProbe] / [AudioPermissionChecker]
 * seams (no device, no AAR static init). These tests cover the fail-closed
 * paths — the soul guarantee that an unavailable device or a revoked
 * RECORD_AUDIO never reaches (or crashes) the native layer.
 *
 * NOT covered here (pending R-001 real-device confirmation): the happy-path
 * `startTalk` that actually calls HTIntf.startspeech → EncodeService →
 * MediaCodec native. That requires an arm64 device; faking it would prove
 * nothing real. See .state/realdevice-checklist-handoff.md.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceTalkAdapterImplTest {

    private class FakeProbe(var loadable: Boolean) : VoiceNativeProbe {
        override fun nativeLibsLoadable(): Boolean = loadable
    }

    private class FakePermission(var granted: Boolean) : AudioPermissionChecker {
        override fun isRecordAudioGranted(): Boolean = granted
    }

    private fun adapter(loadable: Boolean, granted: Boolean) =
        VoiceTalkAdapterImpl(
            ioDispatcher = UnconfinedTestDispatcher(),
            nativeProbe = FakeProbe(loadable),
            permissionChecker = FakePermission(granted),
        )

    @Test
    fun isAvailable_reflectsProbe() {
        assertTrue(adapter(loadable = true, granted = true).isAvailable())
        assertFalse(adapter(loadable = false, granted = true).isAvailable())
    }

    @Test
    fun startTalk_whenUnavailable_failsClosedWithVoiceUnavailable() = runTest {
        // x86_64 emulator / 64-bit-only-no-lib device path.
        val result = adapter(loadable = false, granted = true).startTalk(listOf(1, 2))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is VoiceUnavailableException)
    }

    @Test
    fun startTalk_whenPermissionRevoked_failsClosedWithPermissionException() = runTest {
        // Available device but RECORD_AUDIO revoked at call time (AR-010 F-1).
        val result = adapter(loadable = true, granted = false).startTalk(listOf(1))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RecordAudioPermissionException)
    }

    @Test
    fun startPaging_whenUnavailable_failsClosed() = runTest {
        val result = adapter(loadable = false, granted = true).startPaging(listOf(3))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is VoiceUnavailableException)
    }

    @Test
    fun startPaging_whenPermissionRevoked_failsClosed() = runTest {
        val result = adapter(loadable = true, granted = false).startPaging(listOf(3))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RecordAudioPermissionException)
    }

    @Test
    fun gateOrder_unavailableTakesPrecedenceOverPermission() = runTest {
        // Both fail → availability is checked first (capability before permission).
        val result = adapter(loadable = false, granted = false).startTalk(listOf(1))
        assertTrue(result.exceptionOrNull() is VoiceUnavailableException)
    }

    @Test
    fun observeSession_whenUnavailable_emitsErrorAndCloses() = runTest {
        val session = VoiceSession(VoiceSession.Kind.TALK, listOf(1), 0L)
        val states = mutableListOf<VoiceState>()
        adapter(loadable = false, granted = true).observeSession(session).collect { states.add(it) }
        // Flow completes (closes) after a single Error emission.
        assertEquals(1, states.size)
        assertTrue(states.first() is VoiceState.Error)
    }
}
