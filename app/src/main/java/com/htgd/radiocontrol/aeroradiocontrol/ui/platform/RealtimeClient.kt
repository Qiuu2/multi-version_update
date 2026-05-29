package com.htgd.radiocontrol.aeroradiocontrol.ui.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * The realtime channel the 5 Tabs stand on: a single WS connection with
 * heartbeat, exponential-backoff reconnect, and a polling fallback, hidden
 * behind this interface so business screens consume *state + a message stream*
 * and never touch OkHttp or the reconnect machinery (soul: Silent Operation).
 *
 * Scope of TASK-AR-103 (receipt-independent skeleton):
 *   - [connectionState] state machine, exponential backoff, single-instance
 *     reconnect, foreground/background pause-resume, heartbeat *scheduling* are
 *     final.
 *   - [messages] payload *parsing*, the WS URL/auth scheme, and the heartbeat
 *     *frame format* are DRAFT placeholders pending OPEN INQ-O-2; see
 *     [BroadcastWSMessage] and the impl's TODO(O-2) markers.
 *
 * Bound as a `@Singleton` (one connection per process — RISK-AR-004: many
 * instances would each reconnect and create a storm).
 */
interface RealtimeClient {

    /** Current channel state for the UI (ConnectionBanner). Hot, conflated. */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Decoded realtime messages. Hot stream shared across collectors.
     *
     * ⚠ Until O-2 lands every emission is parsed against DRAFT assumptions and
     * unclassifiable frames arrive as [BroadcastWSMessage.Unknown] rather than
     * being dropped or throwing.
     */
    val messages: Flow<BroadcastWSMessage>

    /**
     * Start (or resume) maintaining the connection. Idempotent: calling it while
     * already connected/connecting is a no-op. Returns immediately; progress is
     * observed via [connectionState]. Actual socket opening only happens while
     * the app is foreground (background calls arm it for the next resume).
     */
    fun connect()

    /**
     * Tear down the connection and stop all reconnect/polling/heartbeat work.
     * Moves to [ConnectionState.DISCONNECTED]. Idempotent.
     */
    fun disconnect()
}
