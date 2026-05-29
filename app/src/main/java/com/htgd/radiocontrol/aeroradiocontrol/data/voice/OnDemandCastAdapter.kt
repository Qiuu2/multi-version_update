package com.htgd.radiocontrol.aeroradiocontrol.data.voice

/**
 * Kotlin facade over the htapplib.aar on-demand cast (点播) control surface — the
 * ICD-OnDemandCast contract that the broadcast Tab's 点播 mode (Frontend-Business)
 * consumes.
 *
 * Grounding (verified, not assumed):
 *   - v3 drives this directly via HTIntf at two identical call sites —
 *     `ActivityMusicOrder.orderMusic():236-260` and `MainMethod.startplay():34-49`:
 *     `newondemandlist()` → `setondemandterminal(id)` per terminal →
 *     `setondemandmedia(id)` per media → `int state = startondemand()`.
 *   - There is NO REST cast path (`/terminal/urgentplay` is a dead constant,
 *     never wired). 点播 is AAR/native, same SDK family as [VoiceTalkAdapter].
 *   - AAR (HTIntf, all static, javap-verified): `newondemandlist():void`,
 *     `setondemandterminal(int):int`, `setondemandmedia(int):int`,
 *     `setondemandvolume(int):int`, `startondemand():int`, `stopondemand():int`.
 *     Terminal and media ids are both `int` → [List]<[Int]> (same convention as
 *     [VoiceTalkAdapter]).
 *
 * Why a SEPARATE adapter (not folded into [VoiceTalkAdapter]): cast is a one-shot
 * fire-and-forget op (set the list → startondemand → done) → a one-shot suspend
 * [Result], NOT a [kotlinx.coroutines.flow.Flow]. Voice is a long-lived mic
 * session → a state stream. Same HTIntf SDK, distinct function → distinct seam,
 * shared idiom (isAvailable() gate, runCatching + Dispatchers.IO, fail-closed
 * Result, R-001 caveat). Idiom-not-signature, same reasoning as the voice contract.
 *
 * Permission: NO RECORD_AUDIO gate (unlike [VoiceTalkAdapter.startTalk]). 点播 is
 * server-side playback on remote terminals (cast a media id), not local mic
 * capture — the v3 orderMusic path requests no record permission. Gate is
 * [isAvailable] only.
 *
 * Callback: cast returns on `startondemand()`'s SYNCHRONOUS int — it does NOT
 * register a [com.example.htapplib.CallBackIntf]. The AAR has a single global
 * callback (registered via `HTIntf.setcallbackinterface`) which
 * [VoiceTalkAdapterImpl] owns per voice session; a second registration here would
 * clobber it. So this adapter never touches the callback (see the impl).
 *
 * R-001: AAR native (the MP3/audio path) → the on-device happy path is unproven
 * until an arm64 real-device confirmation (CTO's call; same caveat as voice). The
 * interface + degradation contract is final; the `startondemand()` happy path is
 * "wired, statically sound, NOT device-verified".
 */
interface OnDemandCastAdapter {

    /**
     * True iff this device can run the native cast path: same capability check as
     * [VoiceTalkAdapter.isAvailable] (supported ABI + AAR libs load), reusing the
     * shared [VoiceNativeProbe]. Does NOT consider RECORD_AUDIO (cast is playback,
     * not capture).
     */
    fun isAvailable(): Boolean

    /**
     * Casts [mediaIds] to [targetTerminalIds] (点播). Runs the v3 sequence:
     * `newondemandlist()` → `setondemandterminal(id)` per target →
     * `setondemandmedia(id)` per media → `startondemand()`.
     *
     * One-shot suspend [Result] (NOT a Flow) — fire-and-forget. Fails closed
     * (never throws) with:
     *   - [VoiceUnavailableException] if [isAvailable] is false.
     *   - [OnDemandCastException] if `startondemand()` returns a non-success state.
     *   - the wrapped throwable if any HTIntf call throws.
     * The UI consumes the Result as the cast confirmation / degradation signal.
     */
    suspend fun castMedia(mediaIds: List<Int>, targetTerminalIds: List<Int>): Result<Unit>

    /** Stops the current cast (`stopondemand()`). Best-effort, idempotent. */
    suspend fun stopCast(): Result<Unit>

    /** Sets the cast volume (`setondemandvolume(volume)`), out-of-band of a cast. */
    suspend fun setCastVolume(volume: Int): Result<Unit>
}

/**
 * `startondemand()` / `stopondemand()` returned a non-success state code.
 *
 * ⚠ The int code set is UNKNOWN: v3 declares `int state = ...startondemand()` at
 * both call sites but never reads it. So [state] is surfaced verbatim and the
 * success predicate is a documented-assumption (see [OnDemandCastAdapterImpl.isOk]),
 * pending the vendor inquiry on the HTIntf ondemand return codes.
 */
class OnDemandCastException(val state: Int) :
    RuntimeException("on-demand cast failed, state=$state")
