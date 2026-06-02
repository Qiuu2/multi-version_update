package com.htgd.radiocontrol.aeroradiocontrol.ui.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit tests for [ExponentialBackoff] — the pure reconnect-delay schedule.
 *
 * No coroutines/clock: the sequence is computed, so it is asserted directly.
 * This is the "退避正确" piece the Critic flagged as must-be-provably-correct.
 */
class ExponentialBackoffTest {

    @Test
    fun defaultSchedule_is_2_4_8_16_32_thenCapsAt60() {
        val b = ExponentialBackoff() // base 2s, cap 60s
        assertEquals(2_000L, b.delayMillisForAttempt(1))
        assertEquals(4_000L, b.delayMillisForAttempt(2))
        assertEquals(8_000L, b.delayMillisForAttempt(3))
        assertEquals(16_000L, b.delayMillisForAttempt(4))
        assertEquals(32_000L, b.delayMillisForAttempt(5))
        // 64s would exceed the 60s cap -> clamped.
        assertEquals(60_000L, b.delayMillisForAttempt(6))
        assertEquals(60_000L, b.delayMillisForAttempt(7))
    }

    @Test
    fun staysCapped_forVeryLargeAttempt_noOverflow() {
        val b = ExponentialBackoff()
        // 2^(1000) would overflow Long via shifting; Double path saturates at cap.
        assertEquals(60_000L, b.delayMillisForAttempt(1000))
        assertEquals(60_000L, b.delayMillisForAttempt(Int.MAX_VALUE))
    }

    @Test
    fun attemptZeroOrNegative_treatedAsFirstRetry() {
        val b = ExponentialBackoff()
        assertEquals(2_000L, b.delayMillisForAttempt(0))
        assertEquals(2_000L, b.delayMillisForAttempt(-5))
    }

    @Test
    fun customBaseAndCap_areHonoured() {
        val b = ExponentialBackoff(baseMillis = 1_000L, maxMillis = 5_000L)
        assertEquals(1_000L, b.delayMillisForAttempt(1))
        assertEquals(2_000L, b.delayMillisForAttempt(2))
        assertEquals(4_000L, b.delayMillisForAttempt(3))
        assertEquals(5_000L, b.delayMillisForAttempt(4)) // 8000 clamped to 5000
    }

    @Test
    fun invalidConfig_throws() {
        assertThrows(IllegalArgumentException::class.java) { ExponentialBackoff(baseMillis = 0) }
        assertThrows(IllegalArgumentException::class.java) {
            ExponentialBackoff(baseMillis = 10, maxMillis = 5)
        }
    }
}
