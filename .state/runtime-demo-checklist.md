# Runtime Demo Checklist / Runbook — Plan A build COMPLETE

> **Trigger MET (2026-05-29): 5-Tab build functionally complete** (终端✓ 任务✓ 服务✓ 广播✓; AI 降级). Data layer all REAL on v3 wire. All Critic-PASSED.
> This is the "CTO runs it" runbook (not automated). PM has assembled it + verified assembleDebug.
> Commits: 5-Tab `4ccdd13`..`85ea115` + post-batch (real V3TaskRepository + re-touch) on `claude/v4-screens-on-refactor`.

## 0. Prerequisites (CTO provides)
- [ ] **A reachable v3 LAN host** (campus broadcast host): 21 REST endpoints, plaintext HTTP, base `http://<host>:<port>/api`. Device on the same network.
- [ ] Device:
  - **x86_64 emulator**: verifies ALL UI + REST data flows (terminal/task/service + 点播 media list + 寻呼/对讲 *gating*). **Voice/cast NATIVE not runnable** (AAR has no x86_64 .so) → 对讲/点播-cast fall to the isAvailable()=false fallback (EXPECTED, not a bug).
  - **arm64-v8a real device (Android 7.0+)**: verifies everything incl. voice/cast native — and closes R-001 (§6).
- [ ] Build env: `JAVA_HOME=/home/it1234/android-studio/jbr`; `--no-daemon`.

## 1. Build & install
- [ ] `JAVA_HOME=/home/it1234/android-studio/jbr ./gradlew --no-daemon :app:assembleDebug` (PM pre-verifies — see status note at bottom).
- [ ] `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- [ ] Single LAUNCHER = V4Activity (AR-006); cold start → permission orchestration (AR-010) → login.

## 2. Login (auth loop; token = v3 ServerToken / D-14)
- [ ] Enter v3 host (ServerAddress.parse validates host:port) + account/password.
- [ ] Success → token in v3 `ServerToken`; `Constant.serveraddress` = `http://host:port/api` via V3LoginAuthenticator (F-2, through ServerConfig).
- [ ] 5 states: idle/loading/error(bad addr / auth fail)/success → navigate into V4.

## 3. 终端 Tab (real, PA-01/02)
- [ ] Hub shows REAL terminals (/terminal/terminalinfo), zones (/terminal/terzone), zone nests terminals.
- [ ] StatusPill correct; unrecognized state → Unknown fallback (no crash).
- [ ] Polling: terminal 10s / detail 5s; foreground refresh, background pause, resume→immediate refresh; banner reflects PollingState.
- [ ] ZoneDetail; empty list no crash. Broadcast target-select screen multi-selects real terminals.
- [ ] ⚠ deriveStatus is a documented-assumption — if real status ≠ UI, record the raw value (Unknown(raw) carries it) → TerminalMapper.deriveStatus + ICD_UPDATE.

## 4. 任务 Tab (real, PA-10 — note the counter-intuitive bits)
- [ ] Scheme list + active scheme + task timeline (real /task/sechinfo → **TaskGuangboModel**, grouped by `sechename`).
- [ ] ⚠ **`projectstate == 0` means RUNNING/active** (counter-intuitive, pinned from v3). A scheme showing "active/on" has projectstate 0.
- [ ] Enable/disable a scheme → POST /task/sechenableordisable {sechename, state} (0=enable/1=disable); reply 0 or 15 = OK; then re-fetch → active flips (I-3 single-source).
- [ ] **Execution log shows the EMPTY state** ("当前服务器暂未提供执行日志…") — EXPECTED: v3 has NO log REST endpoint (PA-10 confirmed). Not a bug.
- [ ] Task card **zone line is blank** — EXPECTED: no zone field on the wire (was a media-name placeholder, now honestly blank).
- [ ] task.state → UI mapping (Running/Idle/Disabled/Unknown); 30s polling.
- [ ] SchemeEdit (CRUD) is still mock — deferred (no CRUD interface yet). Don't expect real create/edit.

## 5. 服务 / 广播 / AI Tab (real)
- [ ] **服务 Tab**: system health (real /server/serverstate → ServerState: connection / taskCount / bandwidth / maxConnection / ctrlPort / dataPort / name / ip / gate; absent → "--"). ServerHealth Online/Offline. 20s polling. No Empty state (single-object: empty→Error).
- [ ] **广播 Tab** (segmented; shared target selection persists across modes; targets are ZONES, resolved to terminal ids at the VM):
  - **点播 (Cast)**: media folder/file picker (real /terminal/mediafolderinfo + /terminal/mediainfo) → pick media + targets → cast. **Cast action = AAR (OnDemandCastAdapter)** → arm64 only; x86_64 → "点播不可用" fallback. **No mic prompt** for 点播 (playback). Result-based confirm (one-shot).
  - **寻呼 (Page) / 对讲 (Talk)**: VoiceTalkAdapter (AAR) → arm64 only; x86_64 → Unavailable fallback. **RECORD_AUDIO prompt appears here** (adapter-owned gate, re-checked at start; deny → error, grant → retry). **Tap-to-start / tap-to-end** (NOT press-hold PTT — D-15 design decision; session-based). Session states Connecting→Waiting→Active→Ended/Refused/Error; device-offline banner from deviceEvents.
- [ ] **AI Tab**: 降级 — placeholder/degraded notice, no crash.

## 6. R-001 voice/cast native real-device verify (only prerequisite to close R-001)
- [ ] Per `.state/realdevice-checklist-handoff.md` §A+B on arm64-v8a real device.
- [ ] Receipt: model + `ro.product.cpu.abi`=arm64-v8a + Android ver + no `UnsatisfiedLinkError` on class init + `Mp3EncodeInit` success + `Mp3EncodeBuffer>0` + no SIGSEGV + no dlopen alignment/relocation warning in logcat.
- [ ] Also exercise 对讲 startTalk + 点播 startondemand on-device (cast int-return semantics are a vendor-inquiry doc-assumption — note any failure code).
- [ ] Pass → R-001 LOW→CLOSED (D-05/D-06).

## 7. Pass criteria & evidence
- [ ] Per Tab: screenshot + key logcat (request URL, response code, parse OK).
- [ ] No crash; unknown-status / empty-data / network-fail all degrade (Unknown/Empty/Error), never crash.
- [ ] Polling cadence matches Handoff (terminal 10s / detail 5s / task 30s / service 20s).
- [ ] ⚠ **R-ADDR-SLOT re-check**: V4 login writes `Constant.serveraddress` only (not v3 PreferencesUtil("serverAddress")). All V4 screens use V3* repos (ServerConfig) → should be fine. **If any screen makes a request to a stale/empty address, that's R-ADDR-SLOT waking** (a retained legacy path leaked in) — capture the URL + screen, report. (tasks.yaml risks_added R-ADDR-SLOT.)
- [ ] Deviations (status derivation / log absence / enum value sets) → record → ICD_UPDATE (these are documented-assumptions, by design pending real-host data).

## Notes
- Realtime = **polling** (no WebSocket; R-WS-NEW / Plan A). Don't expect server push.
- Old stack httptask/*Method.java unchanged (Plan A red line); data flows V3CallbackAdapter→RequestManger.
- PM assembleDebug status: see the report accompanying this runbook (PM ran :app:assembleDebug to confirm the APK packages before handoff).
