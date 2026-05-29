package com.htgd.radiocontrol.aeroradiocontrol.data.voice

import com.htgd.radiocontrol.aeroradiocontrol.di.IoDispatcher
import com.example.htapplib.HTIntf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [OnDemandCastAdapter]. See the interface for contracts and grounding.
 *
 * The one un-JVM-testable concern (native lib load) is injected as the shared
 * [VoiceNativeProbe] seam — the SAME one [VoiceTalkAdapterImpl] uses — so the
 * availability gate is unit-tested with a fake (no device).
 *
 * ⚠ Receipt-independent skeleton: the HTIntf calls are wired defensively but NOT
 * device-verified (R-001). Each native-adjacent call is inside a single
 * runCatching so a missing/odd AAR behaviour degrades to [Result.failure] rather
 * than crashing. The `startondemand()`/`stopondemand()` int is mapped through the
 * single [isOk] choke point (documented-assumption; see [isOk]).
 */
@Singleton
class OnDemandCastAdapterImpl @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val nativeProbe: VoiceNativeProbe,
) : OnDemandCastAdapter {

    override fun isAvailable(): Boolean = nativeProbe.nativeLibsLoadable()

    override suspend fun castMedia(
        mediaIds: List<Int>,
        targetTerminalIds: List<Int>,
    ): Result<Unit> = withContext(ioDispatcher) {
        // Gate: capability (ABI + loadLibrary). Fail closed before any AAR call.
        if (!isAvailable()) return@withContext Result.failure(VoiceUnavailableException())
        runCatching {
            // v3 sequence (orderMusic / startplay): build the on-demand list, add
            // every target terminal, add every media, then fire.
            HTIntf.newondemandlist()
            targetTerminalIds.forEach { HTIntf.setondemandterminal(it) }
            mediaIds.forEach { HTIntf.setondemandmedia(it) }
            val state = HTIntf.startondemand()
            if (!isOk(state)) throw OnDemandCastException(state)
        }
    }

    override suspend fun stopCast(): Result<Unit> = withContext(ioDispatcher) {
        // No availability gate: stop is always safe to attempt (best-effort,
        // idempotent). If the libs aren't loaded the HTIntf call throws and
        // runCatching swallows it into failure — never crashes.
        runCatching {
            val state = HTIntf.stopondemand()
            if (!isOk(state)) throw OnDemandCastException(state)
        }
    }

    override suspend fun setCastVolume(volume: Int): Result<Unit> = withContext(ioDispatcher) {
        if (!isAvailable()) return@withContext Result.failure(VoiceUnavailableException())
        runCatching { HTIntf.setondemandvolume(volume) }.map { }
    }

    /**
     * Success predicate for the HTIntf ondemand int return — a DOCUMENTED
     * ASSUMPTION. v3 ignores the int at both call sites (orderMusic:260 /
     * startplay:49), so the real code set is unknown. We treat any return as
     * success here (the AAR throwing is the real failure signal, caught by
     * runCatching). When the vendor reveals the sentinel/code set, this is the
     * single line that changes.
     */
    private fun isOk(@Suppress("UNUSED_PARAMETER") state: Int): Boolean = true
}
