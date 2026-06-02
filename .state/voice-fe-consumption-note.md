# VoiceTalkAdapter — FE Consumption Contract (HELD for broadcast-Tab relay)

> Producer: legacy-native (2026-05-29 fill-window, design-only). Grounded in landed AR-104 (data/voice/VoiceTalkAdapter.kt, VoiceTalkAdapterImpl.kt, VoiceCapability.kt — Critic PASSED HIGH, reserved valid under Plan A).
> Status: **HELD by PM. Relay to fe-business when the broadcast Tab (对讲 intercom mode) is slotted** (late in 5-Tab order). No ICD diff — ICD-VoiceAAR-v2 (LIVE structure / DRAFT-pending-device execution, AR-111) already covers it.

## (a) Idiom alignment + deliberate divergence
Aligns with V3CallbackAdapter at the **idiom** level, NOT signatures. Both wrap a v3 callback into a coroutine surface, fail closed, register-once + clean up. Shapes differ ON PURPOSE — **do not expect a Result-shaped voice API**:
- V3CallbackAdapter = one-shot req/resp → `suspend get/post(): Result<String>`.
- VoiceTalkAdapter = long-lived stream. AAR's single global CallBackIntf (21 cb) fires repeatedly across a session → state is a **Flow**, not a one-shot Result. fe consumes a STREAM for session state + a Result ONLY for the start() handshake.

## (b) Broadcast-Tab intercom consumption contract (verbatim AR-104 interface)
fe injects VoiceTalkAdapter (Hilt @Singleton, prod impl @Binds-able). Consumption order in the intercom ViewModel:

1. **GATE** — `isAvailable()` (capability: ARM ABI + AAR libs load; cached). false → render fallback UI (e.g. x86_64 emulator), do NOT call start. RECORD_AUDIO is NOT part of isAvailable; re-checked inside start().
2. **START** — `startTalk(targetTerminalIds: List<Int>)` (对讲) or `startPaging(targetTerminalIds: List<Int>)` (寻呼). Both `suspend → Result<VoiceSession>`. `List<Int>` = terminal ids (not strings). Success → VoiceSession handle (kind TALK/PAGING, targets, startedAt). start() does its own gate internally (gate1 isAvailable → gate2 RECORD_AUDIO), fails closed.
3. **OBSERVE** — `observeSession(session): Flow<VoiceState>`. States: Connecting → Waiting (far side not accepted) → Active (encoder up/talking) → terminal Ended | Refused | Error(cause). Flow completes on Ended/Refused; collect inside the session's coroutine scope, let cancellation tear it down (adapter unregisters native cb in awaitClose). DO NOT hold the Flow past the session.
4. **END** — `endSession(session): suspend`, best-effort, idempotent. Call on "hang up" and VM scope cancel.

**Degradation** (fe drives UI from failed Result / Error state — fe does NOT check permission itself, adapter owns the gate):
- `Result.failure(VoiceUnavailableException)` → "对讲不可用 (此设备不支持语音)" fallback; offer other modes (寻呼/点播 or v3 REST modes).
- `Result.failure(RecordAudioPermissionException)` → prompt + re-request RECORD_AUDIO, then retry start. (AR-010 requests at app entry, but grant can be revoked; adapter re-checks at call time — stale cold-start grant not trusted.)
- `VoiceState.Error(cause)` mid-session (e.g. VoiceCommandException(code) from AAR oncmderror) → surface error state, treat session as dead.

**Connection banner** — `deviceEvents: StateFlow<VoiceDeviceEvent>` (hot, app-scoped, NOT per-session). Collect once at broadcast-Tab level: Connected(Boolean) / LoggedIn(code) / CommandError(code) / VolumeChanged(volume) / IncomingSpeechRequest(from) / Idle. Use Connected for 在线/离线 banner, IncomingSpeechRequest for inbound 对讲 prompt.

**Boundary (avoid wrong-adapter wiring):** 对讲 + 寻呼 → VoiceTalkAdapter (AAR/native). 点播/媒体 metadata + target-terminal LISTS → v3 REST adapter (V3 repos). 4521 TCP shell-command → SEPARATE surface (LocalSocketClient, AR-007), not part of voice. fe must NOT call HTIntf/CallBackIntf directly; the only voice seam is this interface.

## (c) Open items (unchanged)
- HTIntf.init(...) arg semantics = UNK (7-8 args; adapter start() assumes session-level control methods, but init/connectserver handshake args not yet reverse-confirmed). Does NOT block the fe contract above — internal to adapter, resolved when wiring runs against a live host.
- R-001 device-verify = CTO's call. AR-104 = "wired, statically sound, NOT device-verified"; live startspeech producing audio pending one arm64 real-device run (.state/realdevice-checklist-handoff.md, D-05/D-06). fe treats happy path as unproven on-device until then; interface + degradation contract is final.
