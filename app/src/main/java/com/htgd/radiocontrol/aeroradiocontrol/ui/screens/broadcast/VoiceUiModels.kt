package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import androidx.compose.runtime.Immutable

/**
 * Presentation view types for the broadcast Tab 寻呼/对讲 (Page/Talk) voice modes
 * (broadcast SD2).
 *
 * The voice ViewModel maps VoiceTalkAdapter's session [com.htgd.radiocontrol.aeroradiocontrol.data.voice.VoiceState]
 * + start()-Result + deviceEvents into these UI types at its boundary, so the screen
 * never touches the adapter's domain types. Session state is a Flow (long-lived
 * mic session), NOT a Result — only start() returns a Result.
 */

/**
 * The voice panel's state for one mode (寻呼 or 对讲).
 *
 *   - [Unavailable] this device can't run voice (isAvailable()=false → fallback).
 *   - [Idle]        ready; no session in flight (the "按住说话 / 开始对讲" affordance).
 *   - [Connecting]  start() succeeded; mic encoder not yet up.
 *   - [Waiting]     far side hasn't accepted yet (onspeechwait).
 *   - [Active]      talking (encoder up).
 *   - [Refused]     far side refused → transient, returns to [Idle] on dismiss.
 *   - [Error]       start() failed (other than the typed gates) or a mid-session
 *                   native error; [message] explains, dismiss → [Idle].
 */
@Immutable
sealed interface VoiceUiState {
    data object Unavailable : VoiceUiState
    data object Idle : VoiceUiState
    data object Connecting : VoiceUiState
    data object Waiting : VoiceUiState
    data object Active : VoiceUiState
    data object Refused : VoiceUiState
    data class Error(val message: String) : VoiceUiState

    /** True while a session is live (Connecting/Waiting/Active) — drives "结束" UI. */
    val isInSession: Boolean
        get() = this is Connecting || this is Waiting || this is Active
}

/**
 * One-shot effects the voice VM asks the screen to perform (SharedFlow, not state).
 */
sealed interface VoiceEffect {
    /** Mic not granted at call time → screen requests RECORD_AUDIO, then retries. */
    data class RequestMicPermission(val retryKind: VoiceKind) : VoiceEffect

    /** A transient toast-worthy message (e.g. "请选择目标终端"). */
    data class Message(val text: String) : VoiceEffect
}

/** Which voice mode a start refers to — mirrors VoiceSession.Kind without leaking it. */
enum class VoiceKind { Page, Talk }

/** Connection banner state at the broadcast-Tab level, from deviceEvents. */
enum class VoiceConnection { Unknown, Connected, Disconnected }
