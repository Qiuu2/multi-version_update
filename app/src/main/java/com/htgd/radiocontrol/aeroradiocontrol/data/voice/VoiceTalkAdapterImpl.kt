package com.htgd.radiocontrol.aeroradiocontrol.data.voice

import com.htgd.radiocontrol.aeroradiocontrol.di.IoDispatcher
import com.example.htapplib.CallBackIntf
import com.example.htapplib.HTIntf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [VoiceTalkAdapter]. See the interface for contracts. The two
 * un-JVM-testable concerns (native lib load, RECORD_AUDIO) are injected as
 * [VoiceNativeProbe] / [AudioPermissionChecker] seams so the gate order is
 * unit-tested with fakes.
 *
 * ⚠ Receipt-independent skeleton (TASK-AR-104): the HTIntf control calls are
 * wired defensively but NOT device-verified — real end-to-end is pending the
 * R-001 real-device confirmation. Each native-adjacent call is runCatching so a
 * missing/odd AAR behaviour degrades to [Result.failure] / [VoiceState.Error]
 * rather than crashing.
 */
@Singleton
class VoiceTalkAdapterImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeProbe: VoiceNativeProbe,
    private val permissionChecker: AudioPermissionChecker,
) : VoiceTalkAdapter {

    private val _deviceEvents = MutableStateFlow<VoiceDeviceEvent>(VoiceDeviceEvent.Idle)
    override val deviceEvents: StateFlow<VoiceDeviceEvent> = _deviceEvents.asStateFlow()

    override fun isAvailable(): Boolean = nativeProbe.nativeLibsLoadable()

    override suspend fun startTalk(targetTerminalIds: List<Int>): Result<VoiceSession> =
        startSession(VoiceSession.Kind.TALK, targetTerminalIds)

    override suspend fun startPaging(targetTerminalIds: List<Int>): Result<VoiceSession> =
        startSession(VoiceSession.Kind.PAGING, targetTerminalIds)

    /** Shared gate + native start for both session kinds. */
    private suspend fun startSession(
        kind: VoiceSession.Kind,
        targetTerminalIds: List<Int>,
    ): Result<VoiceSession> = withContext(ioDispatcher) {
        // Gate 1: capability (ABI + loadLibrary). Fail closed.
        if (!isAvailable()) return@withContext Result.failure(VoiceUnavailableException())
        // Gate 2: RECORD_AUDIO re-checked here (cold-start grant ≠ grant now;
        // Android 14 microphone FGS throws without it). AR-010 F-1 co-sign.
        if (!permissionChecker.isRecordAudioGranted()) {
            return@withContext Result.failure(RecordAudioPermissionException())
        }
        runCatching {
            when (kind) {
                VoiceSession.Kind.TALK -> {
                    HTIntf.newspeechitem()
                    targetTerminalIds.forEach { HTIntf.setspeechitem(it) }
                    HTIntf.startspeech()
                }
                VoiceSession.Kind.PAGING -> {
                    HTIntf.newpagingslist()
                    targetTerminalIds.forEach { HTIntf.setpaginglistitem(it) }
                    HTIntf.startpaging()
                }
            }
            VoiceSession(kind, targetTerminalIds, System.currentTimeMillis())
        }
    }

    override suspend fun endSession(session: VoiceSession) {
        withContext(ioDispatcher) {
            runCatching {
                when (session.kind) {
                    VoiceSession.Kind.TALK -> HTIntf.stopspeech()
                    VoiceSession.Kind.PAGING -> HTIntf.stoppaging()
                }
            }
            Unit
        }
    }

    /**
     * Bridges the AAR's single global [CallBackIntf] to a per-session
     * [Flow]<[VoiceState]>. The registered callback also updates [_deviceEvents]
     * for the app-scoped events. Unregisters (sets a no-op callback) on close so
     * the native side holds no stale reference.
     */
    override fun observeSession(session: VoiceSession): Flow<VoiceState> = callbackFlow {
        if (!isAvailable()) {
            trySend(VoiceState.Error(VoiceUnavailableException()))
            close()
            return@callbackFlow
        }

        val callback = object : CallBackIntf {
            // ── per-session lifecycle ──
            override fun onstartencode() { trySend(VoiceState.Active) }
            override fun onstopencode() { trySend(VoiceState.Ended) }
            override fun onstartspeech(p0: String?) { trySend(VoiceState.Active) }
            override fun onstopspeech() { trySend(VoiceState.Ended); close() }
            override fun onspeechwait() { trySend(VoiceState.Waiting) }
            override fun onspeechrefuse() { trySend(VoiceState.Refused); close() }
            override fun oncmderror(p0: Int) {
                trySend(VoiceState.Error(VoiceCommandException(p0)))
                _deviceEvents.value = VoiceDeviceEvent.CommandError(p0)
            }
            override fun onstartondemand(p0: String?) { /* ondemand 非本 session 范畴 */ }
            override fun onstopondemand() {}
            override fun onstartshortcuttask(p0: String?) {}
            override fun onstopshortcuttask() {}
            override fun onstartplay(p0: String?) {}
            override fun onstopplay() {}

            // ── app-scoped device events ──
            override fun onlogin(p0: Int) { _deviceEvents.value = VoiceDeviceEvent.LoggedIn(p0) }
            override fun onconnect(p0: Boolean) { _deviceEvents.value = VoiceDeviceEvent.Connected(p0) }
            override fun onsetvolume(p0: Int) { _deviceEvents.value = VoiceDeviceEvent.VolumeChanged(p0) }
            override fun onspeechrequest(p0: String?) {
                _deviceEvents.value = VoiceDeviceEvent.IncomingSpeechRequest(p0.orEmpty())
            }
            override fun onsettime(p0: Short, p1: Byte, p2: Byte, p3: Byte, p4: Byte, p5: Byte) {}
            override fun onreboot() {}
            override fun onsetdevicestate(p0: Int) {}
            override fun onupdate(p0: String?) {}
        }

        // Register defensively; a failure here surfaces as an error state.
        val registered = runCatching { HTIntf.setcallbackinterface(callback) }
        if (registered.isFailure) {
            trySend(VoiceState.Error(registered.exceptionOrNull() ?: VoiceUnavailableException()))
            close()
            return@callbackFlow
        }
        trySend(VoiceState.Connecting)

        awaitClose {
            // Drop our callback so the native side keeps no stale reference.
            runCatching { HTIntf.setcallbackinterface(null) }
        }
    }
}

/** A non-zero error code reported by the AAR via CallBackIntf.oncmderror. */
class VoiceCommandException(val code: Int) :
    RuntimeException("Voice command error code=$code")
