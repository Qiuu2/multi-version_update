package com.htgd.radiocontrol.aeroradiocontrol.ui.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Plan A "realtime" (D-13): there is no WebSocket — freshness comes from polling
 * the existing v3 data path. This scheduler periodically invokes a [refresh]
 * primitive (e.g. `TerminalRepository.refresh()`), pausing in the background and
 * backing off on failure, and exposes a light [state] for a status banner.
 *
 * Layering (CTO/PM decision): scheduling lives in the **ViewModel/UI layer**, not
 * the Repository — the Repository only provides the `refresh()` primitive. A
 * ViewModel constructs one of these in its `viewModelScope`, [start]s it when the
 * screen is shown and [stop]s it (or cancels the scope) when gone. The detail
 * page's 5s cadence vs the hub's 10s is just a different [cadence] on the same
 * primitive; the screen's lifecycle drives start/stop (coordinated with
 * Frontend-Business).
 *
 * Decoupled on purpose: it takes a `suspend () -> Result<Unit>`, not a specific
 * repository, so terminal / task / any pollable source reuses it without this
 * class depending on the data layer or any ICD.
 *
 * Concurrency: a single polling [Job] at a time (start is idempotent); the loop
 * runs on the provided [scope] (the caller's viewModelScope). Foreground gating
 * reuses [AppForegroundState] (the same signal the shelved WS client used).
 *
 * @param scope the loop's lifecycle owner (typically `viewModelScope`).
 * @param refresh the data primitive to invoke each tick; returns success/failure.
 * @param cadence normal polling interval (see [PollingCadence]).
 * @param foregroundState pauses polling while the app is backgrounded.
 * @param backoff failure retry schedule; resets to [cadence] on the next success.
 */
class PollingRefreshScheduler(
    private val scope: CoroutineScope,
    private val refresh: suspend () -> Result<Unit>,
    private val cadence: PollingCadence,
    private val foregroundState: AppForegroundState,
    private val backoff: ExponentialBackoff = ExponentialBackoff(),
) {
    private val _state = MutableStateFlow(PollingState.IDLE)
    /** Light status for a "刷新中 / 刷新失败" banner. Hot, conflated. */
    val state: StateFlow<PollingState> = _state.asStateFlow()

    private var loopJob: Job? = null

    /** Consecutive failures since the last success; drives [backoff]. */
    private var failureStreak = 0

    /**
     * Begins polling. Idempotent: a no-op if already running. The loop refreshes
     * immediately (so the screen gets fresh data on open), then waits one
     * interval and repeats. While the app is backgrounded the loop parks and does
     * not fire `refresh`, resuming on the next foreground.
     */
    fun start() {
        if (loopJob?.isActive == true) return
        failureStreak = 0
        loopJob = scope.launch {
            while (isActive) {
                if (!foregroundState.isForeground.value) {
                    // Backgrounded: park until foreground returns, then loop back to
                    // the top so we refresh IMMEDIATELY on resume (fresh data when
                    // the user comes back) rather than waiting a full interval first.
                    _state.value = PollingState.IDLE
                    foregroundState.isForeground.first { it }
                    continue
                }
                tickOnce()
                delay(nextDelayMillis())
            }
        }
    }

    /** Stops polling and returns to [PollingState.IDLE]. Safe to call when stopped. */
    fun stop() {
        loopJob?.cancel()
        loopJob = null
        failureStreak = 0
        _state.value = PollingState.IDLE
    }

    /** One refresh attempt, updating [state] and the failure streak. */
    private suspend fun tickOnce() {
        _state.value = PollingState.REFRESHING
        refresh()
            .onSuccess {
                failureStreak = 0
                _state.value = PollingState.POLLING
            }
            .onFailure {
                failureStreak++
                _state.value = PollingState.ERROR
            }
    }

    /**
     * Delay before the next tick: the normal [cadence] after a success, or the
     * [backoff] schedule after consecutive failures (so a flapping server is not
     * hammered every cadence interval). Returns to [cadence] once a refresh
     * succeeds (failureStreak resets to 0).
     */
    private fun nextDelayMillis(): Long =
        if (failureStreak == 0) cadence.intervalMillis
        else backoff.delayMillisForAttempt(failureStreak)
}

/**
 * Polling intervals from Handoff §实时性. Named presets keep call sites legible;
 * [Custom] allows an arbitrary interval (e.g. config-driven).
 */
sealed class PollingCadence(val intervalMillis: Long) {
    /** 终端状态：10s (terminal hub / list). */
    data object Terminal : PollingCadence(10_000L)

    /** 任务进度（会话结束后）：30s. */
    data object TaskEnded : PollingCadence(30_000L)

    /** 详情页：5s (terminal/zone detail screen). */
    data object Detail : PollingCadence(5_000L)

    /** Arbitrary interval, e.g. from remote config. */
    data class Custom(val millis: Long) : PollingCadence(millis)
}

/** Light scheduler state for a status banner — NOT a connection state (no WS). */
enum class PollingState {
    /** Not polling (stopped, or backgrounded). */
    IDLE,

    /** A refresh is in flight. */
    REFRESHING,

    /** Last refresh succeeded; waiting for the next tick. */
    POLLING,

    /** Last refresh failed; backing off and retrying. */
    ERROR,
}
