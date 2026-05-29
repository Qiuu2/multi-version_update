package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.RecordAudioPermissionException
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceDeviceEvent
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceSession
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceState
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceTalkAdapter
import com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceUnavailableException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the broadcast Tab 寻呼 (Page) + 对讲 (Talk) voice modes — SD2.
 *
 * Consumes [VoiceTalkAdapter] (voice-fe-consumption-note.md) + reuses the SD1
 * [BroadcastTargetResolver] (zones → terminal ids, Option A). One VM serves both
 * modes ([VoiceKind] picks startPaging vs startTalk); the screen shows whichever
 * mode is active — the session is single (you can't page and talk at once).
 *
 * Gate / start / observe sequence (the mic-permission gate is the key difference
 * from 点播 — RECORD_AUDIO enters HERE):
 *  1. GATE — [VoiceTalkAdapter.isAvailable] (capability: ABI + AAR libs). false →
 *     [VoiceUiState.Unavailable] fallback, start never called. RECORD_AUDIO is NOT
 *     part of isAvailable; the adapter re-checks it inside start().
 *  2. START — [start] resolves targets (dedup + empty guard), then startPaging/
 *     startTalk(List<Int>) → Result<VoiceSession>. The adapter owns the gate and
 *     fails closed; fe drives the UI off the FAILED Result (fe does NOT check the
 *     permission itself):
 *       - Result.failure(VoiceUnavailableException)      → [VoiceUiState.Unavailable]
 *       - Result.failure(RecordAudioPermissionException) → emit
 *         [VoiceEffect.RequestMicPermission]; the screen prompts RECORD_AUDIO and
 *         calls [onMicPermissionResult] to retry start (stale cold-start grant not
 *         trusted — adapter re-checks at call time).
 *       - other failure                                  → [VoiceUiState.Error]
 *  3. OBSERVE — on success, collect [VoiceTalkAdapter.observeSession] (a Flow, NOT a
 *     Result) into [uiState]: Connecting→Waiting→Active→ terminal Ended/Refused/Error.
 *     The collect runs in a tracked [sessionJob] on viewModelScope; [stop] / a new
 *     start / scope-cancel tears it down (adapter unregisters its native cb in
 *     awaitClose). The Flow is not held past the session.
 *  4. END — [stop] calls endSession (best-effort, idempotent) and returns to Idle.
 *
 * Connection banner — [connection] is derived once from [VoiceTalkAdapter.deviceEvents]
 * (hot, app-scoped, NOT per-session): Connected(true/false) → 在线/离线.
 */
@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val adapter: VoiceTalkAdapter,
    terminalRepository: TerminalRepository,
) : ViewModel() {

    private val targetResolver = BroadcastTargetResolver(terminalRepository)

    private val available: Boolean = adapter.isAvailable()

    private val _uiState = MutableStateFlow<VoiceUiState>(
        if (available) VoiceUiState.Idle else VoiceUiState.Unavailable,
    )
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<VoiceEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<VoiceEffect> = _effects.asSharedFlow()

    /** Connection banner, derived from the adapter's app-scoped device events. */
    val connection: StateFlow<VoiceConnection> =
        adapter.deviceEvents
            .map { event ->
                when (event) {
                    is VoiceDeviceEvent.Connected ->
                        if (event.connected) VoiceConnection.Connected else VoiceConnection.Disconnected
                    else -> VoiceConnection.Unknown
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = VoiceConnection.Unknown,
            )

    /** The collect of the current session's state Flow; cancelled on stop / re-start. */
    private var sessionJob: Job? = null
    private var currentSession: VoiceSession? = null

    /**
     * Starts a [kind] voice session to the terminals under [selectedZoneIds].
     * No-op if the device is unavailable or a session is already live.
     */
    fun start(kind: VoiceKind, selectedZoneIds: Set<String>) {
        if (!available) {
            _uiState.value = VoiceUiState.Unavailable
            return
        }
        if (_uiState.value.isInSession) return // one session at a time

        // Flip to Connecting SYNCHRONOUSLY (before the launch) so a same-frame second
        // start() is caught by the isInSession guard above — the previous version set
        // Connecting inside the coroutine, so two synchronous calls both slipped past
        // the guard before either ran (BL-VOICE-DOUBLESTART). If targets resolve empty
        // below, we revert to Idle.
        _uiState.value = VoiceUiState.Connecting

        viewModelScope.launch {
            val terminalIds = targetResolver.resolveTerminalIds(selectedZoneIds)
            if (terminalIds.isEmpty()) {
                _uiState.value = VoiceUiState.Idle // revert the pre-emptive Connecting
                _effects.tryEmit(VoiceEffect.Message("请选择目标终端"))
                return@launch
            }
            val result = when (kind) {
                VoiceKind.Page -> adapter.startPaging(terminalIds)
                VoiceKind.Talk -> adapter.startTalk(terminalIds)
            }
            result
                .onSuccess { session -> observe(session) }
                .onFailure { e -> handleStartFailure(e, kind) }
        }
    }

    /** Screen calls this after prompting RECORD_AUDIO on [VoiceEffect.RequestMicPermission]. */
    fun onMicPermissionResult(granted: Boolean, kind: VoiceKind, selectedZoneIds: Set<String>) {
        _uiState.value = VoiceUiState.Idle // reset the transient Connecting from the failed attempt
        if (granted) {
            start(kind, selectedZoneIds)
        } else {
            _uiState.value = VoiceUiState.Error("未授予麦克风权限，无法发起语音")
        }
    }

    /** Ends the current session (hang up); back to Idle. Safe when no session. */
    fun stop() {
        val session = currentSession
        sessionJob?.cancel()
        sessionJob = null
        currentSession = null
        if (session != null) {
            viewModelScope.launch { adapter.endSession(session) }
        }
        _uiState.value = if (available) VoiceUiState.Idle else VoiceUiState.Unavailable
    }

    /** Dismiss a transient terminal state (Refused/Error/Ended) back to Idle. */
    fun dismiss() {
        if (!_uiState.value.isInSession) {
            _uiState.value = if (available) VoiceUiState.Idle else VoiceUiState.Unavailable
        }
    }

    private fun observe(session: VoiceSession) {
        currentSession = session
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            adapter.observeSession(session).collect { state ->
                _uiState.value = state.toUiState()
            }
        }
    }

    private fun handleStartFailure(e: Throwable, kind: VoiceKind) {
        when (e) {
            is VoiceUnavailableException -> _uiState.value = VoiceUiState.Unavailable
            is RecordAudioPermissionException -> {
                // Adapter re-checked at call time and found mic not granted. fe (not
                // the VM) owns the prompt → emit an effect; the screen requests then
                // calls onMicPermissionResult to retry. Reset Connecting in the meantime.
                _uiState.value = VoiceUiState.Idle
                _effects.tryEmit(VoiceEffect.RequestMicPermission(kind))
            }
            else -> _uiState.value = VoiceUiState.Error(e.message ?: "发起语音失败")
        }
    }

    private fun VoiceState.toUiState(): VoiceUiState = when (this) {
        VoiceState.Connecting -> VoiceUiState.Connecting
        VoiceState.Waiting -> VoiceUiState.Waiting
        VoiceState.Active -> VoiceUiState.Active
        VoiceState.Refused -> VoiceUiState.Refused
        VoiceState.Ended -> if (available) VoiceUiState.Idle else VoiceUiState.Unavailable
        is VoiceState.Error -> VoiceUiState.Error(cause.message ?: "语音会话出错")
    }
}
