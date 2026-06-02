# OnDemandCastAdapter — 点播 Cast Seam Contract (ACCEPTED, HELD for broadcast-Tab relay)

> Producer: legacy-native (2026-05-29, design-only). Grounded in v3 ActivityMusicOrder.orderMusic():236-260 + MainMethod.startplay():34-49 (identical HTIntf sequence) + AAR javap. PM ACCEPTED 2026-05-29.
> Status: **HELD by PM. Relay to fe-business with the voice contract when the broadcast Tab is slotted.** legacy IMPLEMENTS at its build slot AFTER data's MediaRepository (list) lands + Critic-passes. Formal ICD-OnDemandCast registry entry lands at impl time (Critic reviews impl+ICD; structure LIVE / int-return DRAFT-pending, mirroring ICD-VoiceAAR-v2 split).

## Why a separate adapter (not folded into VoiceTalkAdapter)
点播 cast = one-shot fire-and-forget (set list → startondemand → done) → `Result`. Voice = long-lived mic session → Flow stream. Same HTIntf SDK, distinct function → separate seam, shared idiom (isAvailable() gate, runCatching+Dispatchers.IO, fail-closed Result, R-001 caveat). Same idiom-not-signature reasoning as the voice contract.

## Proposed seam (data/voice/OnDemandCastAdapter.kt)
```kotlin
interface OnDemandCastAdapter {
    fun isAvailable(): Boolean   // AAR/ABI probe (reuses VoiceNativeProbe/VoiceCapability, cached). NOT RECORD_AUDIO.
    suspend fun castMedia(mediaIds: List<Int>, targetTerminalIds: List<Int>): Result<Unit>  // newondemandlist→setondemandterminal*×N→setondemandmedia*×N→startondemand(); one-shot, NOT a Flow
    suspend fun stopCast(): Result<Unit>           // stopondemand; best-effort, idempotent
    suspend fun setCastVolume(volume: Int): Result<Unit>  // setondemandvolume; out-of-band of a cast call
}
class OnDemandCastException(val state: Int) : RuntimeException("on-demand cast failed, state=$state")
```
AAR (HTIntf static, javap-verified): newondemandlist():void / setondemandterminal(int):int / setondemandmedia(int):int / setondemandvolume(int):int / startondemand():int / stopondemand():int. Terminal & media ids both `int` → `List<Int>` (same convention as VoiceTalkAdapter).

## Locked decisions (PM)
1. **Separate OnDemandCastAdapter** — accepted.
2. **isAvailable()** via existing VoiceNativeProbe (no 2nd probe — same MP3/audio native path).
3. **NO RECORD_AUDIO gate** — 点播 is server-side playback on remote terminals (cast a media id), not local mic capture. v3 orderMusic requests no record permission. fe's 点播 screen must NOT show a mic-permission prompt (gate = isAvailable() only). ← fe confirm.
4. **int-return = documented-assumption (DRAFT-pending)**: startondemand()/stopondemand() return int but v3 IGNORES it (both call sites unused — verified). Map defensively: runCatching (AAR throw → failure) + treat int as success via a single `private fun isOk(state: Int)` choke point → one-line change when the code set is known. **Vendor-inquiry candidate** (HTIntf ondemand/voice int return codes — register alongside O-4 vendor package).
5. **SINGLETON-CALLBACK tension**: CallBackIntf is ONE global registered impl (HTIntf.setcallbackinterface); VoiceTalkAdapterImpl registers/unregisters per voice session. castMedia returns on startondemand()'s SYNCHRONOUS int — it does NOT need onstartondemand callback. **Cast UI uses the castMedia Result as confirmation (no Flow); do NOT register a 2nd CallBackIntf** (would clobber voice). Consolidating into one shared CallBackIntf dispatcher is a SEPARATE future task, only if async cast state is ever wanted.
6. **R-001**: AAR native → on-device happy-path unproven until arm64 device-verify (CTO's call, same as voice). Interface + degradation contract final; happy path = "wired, statically sound, NOT device-verified".

## Screen surface (one coherent 点播 surface for fe)
MediaRepository list [data] → user picks media ids + BroadcastTargetsViewModel terminal ids [ready] → OnDemandCastAdapter.castMedia(mediaIds, targetTerminalIds) [legacy] → Result drives cast-confirmation/degradation UI. isAvailable()=false → fallback (same as voice). No REST cast endpoint exists.
