package com.htgd.radiocontrol.aeroradiocontrol.data.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Kotlin facade over the htapplib.aar voice intercom native stack — the
 * ICD-VoiceAAR-v1 contract surface that the broadcast Tab's intercom/paging
 * modes (Frontend-Business) consume.
 *
 * Grounding (SPIKE-AAR64, verified):
 *   - The AAR exposes ONE global callback interface `com.example.htapplib.CallBackIntf`
 *     (21 callbacks), registered via `HTIntf.setcallbackinterface`, plus static
 *     control methods (`startspeech/startpaging/...`). Those control methods are
 *     plain Java — NOT native. The only native surface is MP3 encoding
 *     (`MediaCodec.Mp3Encode*` → libaudioplay.so → libmp3lame.so), exercised by
 *     the foreground `EncodeService` (foregroundServiceType=microphone).
 *   - So this adapter wraps `HTIntf` + the single `CallBackIntf` and fans the
 *     callbacks out to per-session [Flow]s. It deliberately does NOT use the
 *     skill-template's `NativeTalkListener` (which does not exist in the AAR).
 *
 * Scope of THIS deliverable (TASK-AR-104, receipt-independent skeleton):
 *   Adapter structure, CallBackIntf→Flow bridge, [isAvailable] capability
 *   detection, and the RECORD_AUDIO runtime gate are built now against the AAR's
 *   known interface. The REAL end-to-end (a live `startspeech` session producing
 *   audio on a device) is NOT claimed here — it stays pending the R-001
 *   real-device confirmation (.state/realdevice-checklist-handoff.md). Until
 *   that回执, treat [startTalk]/[startPaging] as "wired, statically sound, not
 *   yet device-verified".
 *
 * Soul contracts upheld:
 *   - Defensive wrapping: every native-adjacent call is runCatching → [Result];
 *     UnsatisfiedLinkError / SecurityException never escape to the caller.
 *   - Graceful degradation: [isAvailable] is checked first; unavailable device
 *     (e.g. x86_64 emulator — AAR ships no x86_64 .so) fails closed with a typed
 *     Result, never a crash. UI shows a fallback.
 *   - Capability + permission gate before any session (see [startTalk]).
 */
interface VoiceTalkAdapter {

    /**
     * True iff this device can run voice: it has a supported ABI AND the AAR's
     * native libs load. arm64-v8a / armeabi-v7a have the libs (SPIKE-AAR64);
     * x86 / x86_64 do not, so emulators return false. Cached after first check.
     *
     * This is a capability check only — it does NOT consider RECORD_AUDIO
     * (a runtime permission, re-checked at [startTalk]/[startPaging] time).
     */
    fun isAvailable(): Boolean

    /**
     * Starts an intercom (对讲) session to [targetTerminalIds].
     *
     * Fails closed (never throws) with:
     *   - [VoiceUnavailableException] if [isAvailable] is false.
     *   - [RecordAudioPermissionException] if RECORD_AUDIO is not currently
     *     granted (re-checked here, not trusted from cold start — AR-010 F-1
     *     co-sign: users can revoke; Android 14 microphone FGS throws without it).
     * The UI consumes the failed [Result] to route to a fallback / re-request,
     * so the native path never starts unprivileged.
     */
    suspend fun startTalk(targetTerminalIds: List<Int>): Result<VoiceSession>

    /**
     * Starts a paging (寻呼) session to [targetTerminalIds]. Same availability +
     * RECORD_AUDIO gate as [startTalk].
     */
    suspend fun startPaging(targetTerminalIds: List<Int>): Result<VoiceSession>

    /** Ends [session] (best-effort; safe to call on an already-ended session). */
    suspend fun endSession(session: VoiceSession)

    /**
     * Per-session state stream, fed by the AAR's [CallBackIntf] callbacks
     * (onstartspeech/onstopspeech/onspeechwait/onspeechrefuse/onstartencode/...).
     * Completes when the session ends.
     */
    fun observeSession(session: VoiceSession): Flow<VoiceState>

    /**
     * Connection/device-level events not tied to one session
     * (onconnect/onlogin/oncmderror/onsetvolume/...). Hot, app-scoped.
     */
    val deviceEvents: StateFlow<VoiceDeviceEvent>
}

/** A handle to an in-flight voice session (intercom or paging). */
data class VoiceSession(
    val kind: Kind,
    val targetTerminalIds: List<Int>,
    val startedAt: Long,
) {
    enum class Kind { TALK, PAGING }
}

/** Per-session lifecycle, mapped from [CallBackIntf]. */
sealed interface VoiceState {
    /** Session requested; mic encoder not yet up. */
    data object Connecting : VoiceState
    /** Waiting for the far side to accept (onspeechwait). */
    data object Waiting : VoiceState
    /** Far side refused (onspeechrefuse). */
    data object Refused : VoiceState
    /** Active: encoder running / talking (onstartencode + onstartspeech). */
    data object Active : VoiceState
    /** Ended normally (onstopspeech/onstopencode). */
    data object Ended : VoiceState
    /** A native/command error (oncmderror) or wrapped failure. */
    data class Error(val cause: Throwable) : VoiceState
}

/** App-scoped device/connection events from [CallBackIntf]. */
sealed interface VoiceDeviceEvent {
    data object Idle : VoiceDeviceEvent
    data class Connected(val connected: Boolean) : VoiceDeviceEvent   // onconnect
    data class LoggedIn(val code: Int) : VoiceDeviceEvent             // onlogin
    data class CommandError(val code: Int) : VoiceDeviceEvent         // oncmderror
    data class VolumeChanged(val volume: Int) : VoiceDeviceEvent      // onsetvolume
    data class IncomingSpeechRequest(val from: String) : VoiceDeviceEvent // onspeechrequest
}

/** Device lacks a supported ABI or the AAR native libs failed to load. */
class VoiceUnavailableException :
    IllegalStateException("Voice intercom unavailable: native library not loadable on this device/ABI")

/** RECORD_AUDIO is not currently granted; UI must request it before retrying. */
class RecordAudioPermissionException :
    SecurityException("RECORD_AUDIO permission not granted; cannot start microphone session")
