package com.htgd.radiocontrol.aeroradiocontrol.data.voice

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [OnDemandCastAdapterImpl]'s capability GATE — the
 * receipt-independent logic that runs before any native HTIntf call.
 *
 * Runs on a plain JVM via a fake [VoiceNativeProbe] (no device, no AAR static
 * init). These cover the fail-closed paths — the soul guarantee that an
 * unavailable device never reaches (or crashes) the native cast layer.
 *
 * NOT covered here (pending R-001 real-device confirmation): the happy-path
 * [OnDemandCastAdapterImpl.castMedia] that actually calls
 * HTIntf.newondemandlist → setondemandterminal* → setondemandmedia* →
 * startondemand. That requires the AAR's native libs (arm64 device); faking the
 * static HTIntf would prove nothing real. See .state/realdevice-checklist-handoff.md.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnDemandCastAdapterImplTest {

    private class FakeProbe(var loadable: Boolean) : VoiceNativeProbe {
        override fun nativeLibsLoadable(): Boolean = loadable
    }

    private fun adapter(loadable: Boolean) =
        OnDemandCastAdapterImpl(
            ioDispatcher = UnconfinedTestDispatcher(),
            nativeProbe = FakeProbe(loadable),
        )

    @Test
    fun isAvailable_reflectsProbe() {
        assertTrue(adapter(loadable = true).isAvailable())
        assertFalse(adapter(loadable = false).isAvailable())
    }

    @Test
    fun castMedia_whenUnavailable_failsClosedWithVoiceUnavailable() = runTest {
        // x86_64 emulator / 64-bit-only-no-lib device: the gate must short-circuit
        // BEFORE any HTIntf call (which would throw UnsatisfiedLinkError on a plain
        // JVM and crash this test if the gate were skipped).
        val result = adapter(loadable = false).castMedia(
            mediaIds = listOf(10, 11),
            targetTerminalIds = listOf(1, 2),
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is VoiceUnavailableException)
    }

    @Test
    fun setCastVolume_whenUnavailable_failsClosed() = runTest {
        val result = adapter(loadable = false).setCastVolume(50)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is VoiceUnavailableException)
    }

    @Test
    fun stopCast_neverCrashes_yieldsResult() = runTest {
        // stopCast has no availability gate (always safe to attempt). HTIntf's
        // control methods are pure Java (only MediaCodec is native — SPIKE-AAR64),
        // so HTIntf.stopondemand() does NOT throw on a plain JVM and runCatching
        // yields success. The contract asserted: stopCast NEVER lets a throwable
        // escape to the caller — runCatching contains it. (On a real device the
        // int-return → isOk mapping is what's exercised; that's R-001-pending.)
        // The test simply reaching this assertion proves no exception escaped.
        val result = adapter(loadable = false).stopCast()
        assertTrue(result.isSuccess)
    }

    @Test
    fun onDemandCastException_carriesStateAndIsRuntime() {
        val ex = OnDemandCastException(state = -7)
        assertEquals(-7, ex.state)
        assertTrue(ex is RuntimeException)
    }
}
