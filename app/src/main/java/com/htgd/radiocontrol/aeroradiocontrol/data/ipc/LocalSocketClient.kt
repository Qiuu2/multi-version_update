package com.htgd.radiocontrol.aeroradiocontrol.data.ipc

import kotlinx.coroutines.flow.StateFlow

/**
 * Coroutine wrapper around the on-device IPC socket at 127.0.0.1:4521.
 *
 * This is the ICD-IPCSocket-v1 contract surface and the modern replacement for
 * the legacy [com.htgd.radiocontrol.aeroradiocontrol.utils.SocketClient], which
 * opened a raw socket on a bare [Thread], wrote one command in its constructor,
 * and pushed every response line through an `onServiceSend` callback.
 *
 * What changes vs. the legacy client (soul: Defensive Wrapping):
 *   - All socket I/O runs on Dispatchers.IO; nothing blocks the main thread.
 *   - Connection lifecycle is explicit ([connect] / [disconnect]) and serialized
 *     by a Mutex, instead of "connect implicitly in a constructor".
 *   - Failures return [Result] rather than being swallowed into a callback
 *     string ("###ShellRunError:..."); callers decide how to react.
 *   - State is observable via [connectionState] instead of inferred from
 *     callback side effects.
 *
 * Usage constraints (skill §1.3):
 *   - Connect only while a foreground screen needs it; disconnect when it goes
 *     away, to avoid holding the socket / risking ANR.
 *   - 5s connect timeout; auto-reconnect up to 3 times on transport loss.
 *   - There is currently NO live consumer in the app (the legacy SocketClient
 *     was already orphaned — SPIKE/AR-007 current-state scan), so this ships as
 *     forward-looking infrastructure for whichever Phase 1 screen needs local
 *     IPC. It is unit-tested via an injected [SocketConnectionFactory] seam.
 *
 * OPEN(INQ-O-4 / UNK-002): the daemon's command vocabulary and response framing
 * are undocumented; see [ShellCommand]. [sendCommand] currently assumes a
 * single-line response per command, matching the legacy reader.
 */
interface LocalSocketClient {

    /**
     * Observable connection state; starts [ConnectionState.DISCONNECTED].
     *
     * Named to match the ICD-IPCSocket-v1 registry contract that
     * Frontend-Business consumes (icd-contracts.md §7).
     */
    val connectionStateFlow: StateFlow<ConnectionState>

    /**
     * Connects to 127.0.0.1:4521 (idempotent: a no-op success if already
     * connected). Serialized so concurrent callers share one attempt. On
     * failure the state moves to [ConnectionState.ERROR] and a bounded
     * auto-reconnect is scheduled.
     */
    suspend fun connect(): Result<Unit>

    /** Closes the socket and returns to [ConnectionState.DISCONNECTED]. Safe to call when not connected. */
    suspend fun disconnect()

    /**
     * Sends one [ShellCommand] and returns the daemon's single-line response.
     *
     * Fails (without throwing) if not connected. I/O is mutex-serialized so a
     * command's write+read pair is atomic with respect to other commands on
     * this client.
     */
    suspend fun sendCommand(command: ShellCommand): Result<String>
}

/** Lifecycle states for the local IPC socket. */
enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }
