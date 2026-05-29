package com.htgd.radiocontrol.aeroradiocontrol.ui.platform

/**
 * Exponential backoff schedule for WS reconnects: 2s, 4s, 8s, 16s, 32s, capped
 * at 60s (soul / RISK-AR-004 — bounded retry, never a tight reconnect loop).
 *
 * Pure and side-effect-free so the delay sequence is unit-testable in isolation
 * (no coroutines, no clock, no socket). [RealtimeClientImpl] owns the attempt
 * counter and `delay()`s by [delayMillisForAttempt]; this class only computes.
 *
 * @param baseMillis first retry delay (attempt 1). Default 2000ms.
 * @param maxMillis  ceiling for any single delay. Default 60000ms.
 */
class ExponentialBackoff(
    private val baseMillis: Long = 2_000L,
    private val maxMillis: Long = 60_000L,
) {
    init {
        require(baseMillis > 0) { "baseMillis must be > 0" }
        require(maxMillis >= baseMillis) { "maxMillis must be >= baseMillis" }
    }

    /**
     * Delay before the [attempt]-th retry (1-based): `base * 2^(attempt-1)`,
     * clamped to [maxMillis]. attempt ≤ 0 is treated as the first retry.
     *
     * Uses Double for the 2^n step so a large attempt count cannot overflow Long
     * via shifting — the result saturates at [maxMillis] long before that.
     */
    fun delayMillisForAttempt(attempt: Int): Long {
        val n = (attempt - 1).coerceAtLeast(0)
        // 2^n can exceed Long range for big n; compute as Double and clamp.
        val scaled = baseMillis.toDouble() * Math.pow(2.0, n.toDouble())
        if (scaled >= maxMillis.toDouble()) return maxMillis
        return scaled.toLong()
    }
}
