package com.htgd.radiocontrol.aeroradiocontrol.ui.platform

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for [PollingRefreshScheduler] — Plan A polling (no WS).
 *
 * Follows STD-PERIODIC-TEST: the scheduler runs an unbounded `delay`-loop. Two
 * rules make that testable without hanging:
 *   1. Step virtual time with bounded [advanceTimeBy] + [runCurrent] — NEVER
 *      `advanceUntilIdle()`, which would advance the never-idle loop forever.
 *   2. Run the scheduler on `runTest`'s [TestScope.backgroundScope]. `runTest`
 *      cancels `backgroundScope` BEFORE its own end-of-body `advanceUntilIdle()`,
 *      so the infinite loop is gone by the time that idle check runs and cannot
 *      deadlock it. (A hand-rolled scope on the same scheduler does NOT get
 *      auto-cancelled in time and hangs the suite — learned the hard way on PA-02.)
 *
 * Covers: immediate first refresh, cadence interval, foreground gating,
 * backoff-on-failure + reset-on-success, and start/stop idempotency.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PollingRefreshSchedulerTest {

    private class FakeForeground(initial: Boolean = true) : AppForegroundState {
        val flow = MutableStateFlow(initial)
        override val isForeground: StateFlow<Boolean> = flow.asStateFlow()
    }

    /** Scriptable refresh: counts calls, returns queued results (default success). */
    private class FakeRefresh {
        val calls = AtomicInteger(0)
        private val results = ArrayDeque<Result<Unit>>()
        // List, not vararg: kotlin.Result is a @JvmInline value class and Kotlin
        // forbids value classes as vararg parameter types.
        fun enqueue(results: List<Result<Unit>>) { this.results.addAll(results) }
        suspend fun invoke(): Result<Unit> {
            calls.incrementAndGet()
            return results.removeFirstOrNull() ?: Result.success(Unit)
        }
    }

    /**
     * Builds a scheduler whose loop runs on [TestScope.backgroundScope] (shares the
     * test's virtual-time scheduler, auto-cancelled by runTest before finalization).
     */
    private fun TestScope.newScheduler(
        refresh: FakeRefresh,
        foreground: FakeForeground,
        cadence: PollingCadence = PollingCadence.Terminal,
    ): PollingRefreshScheduler = PollingRefreshScheduler(
        scope = backgroundScope,
        refresh = refresh::invoke,
        cadence = cadence,
        foregroundState = foreground,
    )

    @Test
    fun start_refreshesImmediately_thenOnCadence() = runTest {
        val refresh = FakeRefresh()
        val scheduler = newScheduler(refresh, FakeForeground(true), PollingCadence.Terminal)

        scheduler.start()
        runCurrent()
        assertEquals(1, refresh.calls.get()) // immediate first tick
        assertEquals(PollingState.POLLING, scheduler.state.value)

        advanceTimeBy(10_001); runCurrent()
        assertEquals(2, refresh.calls.get()) // after one 10s cadence
        advanceTimeBy(10_001); runCurrent()
        assertEquals(3, refresh.calls.get())
    }

    @Test
    fun detailCadence_pollsEvery5s() = runTest {
        val refresh = FakeRefresh()
        val scheduler = newScheduler(refresh, FakeForeground(true), PollingCadence.Detail)

        scheduler.start(); runCurrent()
        assertEquals(1, refresh.calls.get())
        advanceTimeBy(5_001); runCurrent()
        assertEquals(2, refresh.calls.get())
        // Terminal cadence (10s) would NOT have ticked yet at 5s — confirms 5s.
    }

    @Test
    fun background_parksPolling_noRefresh_untilForeground() = runTest {
        val refresh = FakeRefresh()
        val foreground = FakeForeground(initial = false)
        val scheduler = newScheduler(refresh, foreground, PollingCadence.Terminal)

        scheduler.start(); runCurrent()
        assertEquals(0, refresh.calls.get()) // backgrounded: no refresh
        assertEquals(PollingState.IDLE, scheduler.state.value)

        foreground.flow.value = true
        runCurrent()
        assertEquals(1, refresh.calls.get()) // resumes on foreground
    }

    @Test
    fun failure_backsOff_thenResetsToNormalCadenceOnSuccess() = runTest {
        val refresh = FakeRefresh().apply {
            enqueue(listOf(Result.failure(IOException("net")), Result.failure(IOException("net"))))
            // 3rd call onward: success (default)
        }
        val scheduler = newScheduler(refresh, FakeForeground(true), PollingCadence.Terminal)

        scheduler.start(); runCurrent()
        assertEquals(1, refresh.calls.get())
        assertEquals(PollingState.ERROR, scheduler.state.value)

        // After 1 failure, next delay = backoff attempt(1) = 2s, NOT the 10s cadence.
        advanceTimeBy(2_001); runCurrent()
        assertEquals(2, refresh.calls.get()) // backoff retry fired at 2s
        assertEquals(PollingState.ERROR, scheduler.state.value)

        // After 2 failures, delay = backoff attempt(2) = 4s; that retry succeeds.
        advanceTimeBy(4_001); runCurrent()
        assertEquals(3, refresh.calls.get())
        assertEquals(PollingState.POLLING, scheduler.state.value) // recovered

        // Recovered → back to normal 10s cadence (a 4s advance must NOT tick).
        advanceTimeBy(4_001); runCurrent()
        assertEquals(3, refresh.calls.get())
        advanceTimeBy(6_001); runCurrent()
        assertEquals(4, refresh.calls.get()) // ticks at the full 10s
    }

    @Test
    fun start_isIdempotent_oneLoop() = runTest {
        val refresh = FakeRefresh()
        val scheduler = newScheduler(refresh, FakeForeground(true), PollingCadence.Terminal)

        scheduler.start()
        scheduler.start() // second start must not spawn a second loop
        runCurrent()
        assertEquals(1, refresh.calls.get())
        advanceTimeBy(10_001); runCurrent()
        assertEquals(2, refresh.calls.get()) // still single cadence, not doubled
    }

    @Test
    fun stop_haltsPolling_andGoesIdle() = runTest {
        val refresh = FakeRefresh()
        val scheduler = newScheduler(refresh, FakeForeground(true), PollingCadence.Terminal)

        scheduler.start(); runCurrent()
        assertEquals(1, refresh.calls.get())

        scheduler.stop()
        assertEquals(PollingState.IDLE, scheduler.state.value)
        advanceTimeBy(60_000); runCurrent()
        assertEquals(1, refresh.calls.get()) // no further refreshes after stop
    }
}
