# Handover Snapshot — AeroRadioControl v4 (PM)

> Refreshed **2026-06-01** (NEXT-2 D-16 closed + 24 commits on `claude/v4-screens-on-refactor`). Supersedes 2026-05-29 mid-session version.
> You are **team-lead, playing Project Manager**. Agent-Teams workflow (`CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`).
> Startup: read `aeroradio-workflow/SKILL.md` + `aeroradio-workflow/agents/project-manager/*.md`. Authority ledger = `.state/tasks.yaml` (STD-CANONICAL-STATE). Decisions = `.state/decision-log.md`.

---

## 0. ⚠ FIRST: team re-spawn after /clear
The team `aeroradio-v4` config + task list persist on disk, but live teammate processes die on /clear. Re-spawn the 5 teammates via the Agent tool (team_name="aeroradio-v4", name=<role>, subagent_type general-purpose, model sonnet, run_in_background). **Re-spawning appends a numeric suffix** because the dead members still hold the original names — most recent live names were **`data-integration-2 / critic-2 / fe-business-2 / fe-platform-2 / legacy-native-2`** (next re-spawn likely becomes `-3`). Verify spawn-result names; SendMessage to those exact names. Each teammate prompt: read its `aeroradio-workflow/agents/<role>/{profile,soul,skill,memory}.md` + MEMORY.md recall + handover + tasks.yaml + decision-log. (See memory [[agent-team-respawn-suffix]].)

## 0.9 LATEST (2026-06-01) — NEXT-2 D-16 BLOCKER-3 CLOSED + 24 commits pushed

- **NEXT-2 D-16 鉴权 split-brain CLOSED** (kill-app 路径 + uninstall 路径双闭环):
  - data: AuthStore v2.1 → **v2.2** (L1/L2 two-layer storage + tokenExpiry + rememberMe + 4-path logout matrix + L2 atomic invariant)
  - NEW StartupAuthDecider seam (synchronous atomic check + ServerConfig rehydrate)
  - V4Activity.onCreate atomic check BEFORE setContent + startDestination
  - AppNavGraph Splash hack 删 + safer default fallback
  - AuthInterceptor 401 path + setBaseUrl("") fail-fast 联动
  - A2 backup_rules.xml + data_extraction_rules.xml + Manifest 双 attr (exclude auth_secure.xml only, keep allowBackup=true for L1 restorability)
  - fe rememberMe wiring (PA-NEXT2-FE): LoginRoute destructure 'remember' + LoginViewModel.onSubmit 5-param + saveLogin 6-arg explicit (Critic 5-cycle MAJOR catch)
  - 54 + 10 tests green; **Critic 5-cycle emulator PASS_W_MINOR HIGH**; Cycle 2 logcat URL rehydrate + Main render verify (kill-app BLOCKER 闭环).
- **Phase C visual fidelity closed**: Color.kt v1.3 **16 semantic alias** (broadcast 3-mode + tile 5-state fg+soft + task-card 3-state, zero new hex) + Noto Sans SC subset 6.75MB (3 weight) + JetBrains Mono subset 135KB (2 weight) 真接 res/font + Type.kt FontFamily.Default/Monospace purged. 4 屏 consume v1.3 tokens: BroadcastScreen 3-mode + TerminalHub 6-chip + TerminalTile semantic rename + TaskScreen TemporalPill + TargetZoneTag + HH:mm format.
- **PA-12 cleartext + PA-13 foreground.register + PA-14 V3TerminalRepo terzone-only + PA-15 V3TaskRepo two-step + Phase B LoginScreen + PA-14 C-1 TabBar identity** 全 commit + closed (前几个 PA 由 PM ICD 落 7-commit batch 2026-05-30，NEXT-2 由本日 3-commit batch close)。
- **git**: branch `claude/v4-screens-on-refactor` HEAD `7b0d4f0`. **本地 24 commits 已 push 到 origin** (含本 handover refresh commit) — `9d9b571..7b0d4f0`. .idea/.claude/skill_matlab_addendum.md uncommitted (intended skip per .gitattributes / D-15)。
- **APK current**: `app/build/outputs/apk/debug/app-debug.apk` 56,785,878 bytes mtime 2026-06-01 13:02 (Phase C + NEXT-2 full)。
- **Closures**: R-ADDR-SLOT DORMANT-TRACKED → **CLOSED**; ESC-WATCH-1 (legacy no-refresh) downgraded LOW (60h tokenExpiry + 401→clearLogin strategy preempt)。
- **Team**: all 5 (-2 names) STANDBY。下次 re-spawn 用 `-3` suffix。
- **Awaiting CTO** real-device cycle-2 belt-and-suspenders verify (登录 → 后台杀 → 重启 → MainScreen 不 NPE)。

## 1. Current state — ALL Phase 0 + Phase 1 + Plan A + NEXT-2 closed (2026-06-01)

| Phase | Status | Critic verdict |
|---|---|---|
| Phase 0 + 1 prep + ICD 逆推 (AR-001~111, 110) | CLOSED | PASSED all |
| Plan A 5-Tab build (PA-01~09 + V3Adapter + Repos) | CLOSED | PASSED HIGH all |
| PA-10 V3TaskRepo real impl | CLOSED | PASSED HIGH big-review |
| PA-11 task mapper re-touch + voice double-start guard | CLOSED | PASSED |
| **PA-12 cleartext (Manifest networkSecurityConfig + xml)** | CLOSED | retroactively verified PA-15 |
| **PA-13 ActivityLifecycle.foregroundState.register** | CLOSED | retroactively verified PA-15 |
| **PA-14 V3TerminalRepo terzone-only + ★ terminal.zone NOT membership** | CLOSED | PASSED_W_MINOR HIGH 5-leg |
| **PA-14 Phase B LoginScreen rebuild** | CLOSED | PASSED |
| **PA-14 C-1 TabBar 5-color identity** | CLOSED | PASSED_HIGH retroactive (PA-15 + Phase C cycle) |
| **PA-15 V3TaskRepo two-step + ★ name not taskname** | CLOSED | PASSED HIGH 5-leg, 13/14 named tasks on-device |
| **Phase C-residual base** (Color.kt v1.3 + Noto Sans SC) | CLOSED | PASSED_W_MINOR HIGH |
| **Phase C-residual app** (BroadcastScreen 3-mode + Terminal 6-chip + TaskScreen pill+zone+HH:mm) | CLOSED | same |
| **NEXT-2 D-16 audit + fix + critic + fe MAJOR fix** | CLOSED | PASSED_W_MINOR HIGH 5-cycle |

5-Tab build state: **终端 ✓** real V3TerminalRepo (28-field + ★ membership warning) **任务 ✓** real V3TaskRepo two-step (sechinfo SUMMARY → 并发 sechetaskinfo per-scheme TIMELINE → atomic publish; 海王作息 14 named tasks 验) **服务 ✓** real V3ServerStateRepository **广播 ✓** MediaRepository list + OnDemandCastAdapter + VoiceTalkAdapter 3-mode color identity **AI** 降级 (not built, INFO).

## 2. What REMAINS (the endgame — Optional/non-BLOCKER)

1. **CTO real-device APK install** + cycle-2 verify (登录 → 后台杀 → 重启 → MainScreen ✓) = belt-and-suspenders NEXT-2 closure. APK at `app/build/outputs/apk/debug/app-debug.apk` mtime 2026-06-01 13:02.
2. **Logout UI affordance** in 5-tab scaffold (退出登录 menu / overflow) — Critic INFO-flagged as separate small fe PA; data seam (clearLogin / clearL2Atomically / clearL1Account) already exposed.
3. **AI Tab + Service Tab 真机视觉验** (emulator 已验 5-Tab identity，CTO 真机顺手验)
4. **R-001 arm64 真机 verify** (voice + cast AAR happy paths static-sound, 待 real arm64 device)
5. **R-16KB-NATIVE** (Android 16+ BLOCKER, htapplib + Baidu Map natives 4KB-aligned) — DORMANT-TRACKED, **发布前 / Android 16 设备前必处理**
6. **SchemeEdit CRUD** (mock-backed) — when task-CRUD becomes needed
7. **Phase 3 polish** (rotation-saveable BL-ROTATION-SAVEABLE, BL-TOKEN-RENAME breaking) — G4 发布前批
8. **PTT design call** (CTO) — fe 当前 tap-to-start/end，press-hold PTT 还原 = UX-only follow-up
9. **NEXT-3?** — CTO 你有别的优先级吗？

## 3. Key architecture facts (updated post-NEXT-2)

- **All v3 repos use the `ServerConfig` seam** (=`Constant.serveraddress`)。**Truth model (NEXT-2 D-16 formalized)**: SOURCE OF TRUTH = `AuthStore.serverAddress` (persisted L1 plain); DERIVED CACHE = `Constant.serveraddress` (Java static)。Cache rehydrate path:
  - App start: `StartupAuthDecider.resumeSessionIfValid()` (NEW NEXT-2 seam) reads AuthStore + calls `serverConfig.setBaseUrl("http://...")` synchronously BEFORE V4Activity setContent
  - Login: `V3LoginAuthenticator.authenticate()` writes
  - 401: `AuthInterceptor` calls `serverConfig.setBaseUrl("")` lockstep with clearLogin (fail-fast, OkHttp rejects non-absolute)
- **AuthStore v2.2 = two-layer storage** (D-16): L1 plain (account/host/port/rememberMe) + L2 encrypted (jwt/refreshToken/tokenExpiry, EncryptedSharedPreferences AES256)。**L2 four-tuple atomic invariant**: {jwt, serverAddress, account, tokenExpiry-not-past} 全有或全无；任一缺失 = clearL2Atomically + LoginScreen。**4-path logout matrix**: clearLogin (L2-only) / clearL2Atomically (decider residue wipe) / clearL1Account (toggle off) / reset (factory)。
- **PA-15 V3TaskRepository real impl** (replaces PA-10 single-fetch): `refresh()` = GET /sechinfo enumerate → coroutineScope { each scheme async POST /sechetaskinfo body name=<sechename> } awaitAll → atomic publish; ★ per-task `name` field NOT `taskname` (CTO capture confirmed); multi-active = Option A first-active-wins (documented-assumption).
- **PA-14 V3TerminalRepository real impl**: `refresh()` only GET /terminal/terzone → mapper iterates nested terminal[]; ★ `terminal.zone` 字段 NOT membership FK warning pinned (CTO terzone capture: 操场 id=1 下 4 终端 zone 值 0,0,0,8); 多对多保留 (id=14 在 4 zone)。
- **广播 Tab is 3-domain**: 点播 = MediaRepository(list, §15) + OnDemandCastAdapter(cast, §16, AAR); 寻呼/对讲 = VoiceTalkAdapter(legacy, AR-104/§8); targets = BroadcastTargetsViewModel via shared **BroadcastTargetResolver** zone→terminal (Option A SSOT, dedup + non-numeric drop + empty-guard)。**3-mode color identity** (Phase C): `BroadcastMode.identityColor(colors)` mirrors `AeroTab.identityColor()` C-1 pattern; ModeSegmented + VoicePanel status-line + Idle CTA + CastPanel CTA 全 binding 经此函数; resting chip label 也带 mode-color (design-spec row 12-13)。
- **Phase C visual fidelity**: 16 semantic-role alias on AeroColors (broadcast 3-mode + tile 5-state fg+soft + task-card 3-state) = additive zero new hex; Type.kt 真接 res/font (Noto Sans SC subset 6763 chars + JetBrains Mono ASCII subset); BL-FONT-ASSETS closed; BL-TOKEN-RENAME 仍 backlog (Phase 3 breaking)。
- **Permission asymmetry**: 寻呼/对讲 need RECORD_AUDIO (adapter-owned gate, fe prompts on RecordAudioPermissionException + retries); 点播 does NOT (playback)。
- **Held cross-domain contracts**: `.state/voice-fe-consumption-note.md` + `.state/cast-seam-contract.md` + `.state/auth-split-brain-framing.md` v2 (D-16)。

## 4. Risks / watch-items (updated)

| Risk | Status |
|---|---|
| **R-ADDR-SLOT** | **CLOSED** post-NEXT-2 (ServerConfig canonical write path; legacy PreferencesUtil slot v4-unreachable) |
| **R-001** | LOW-conditional, GO-pending-device (voice/cast AAR native arm64 real-device verify = CTO) |
| **R-16KB-NATIVE** | DORMANT-TRACKED, Android 16+ BLOCKER (htapplib + Baidu Map .so 4KB-aligned, 发布前必处理) |
| **ESC-WATCH-1** | downgraded LOW (60h tokenExpiry + 401→clearLogin strategy preempt legacy no-refresh) |
| **ESC-WATCH-2** | TRACKING (terminal status enum O-2 WS 真值集 + Fault 派生 O-1 D-3 回执) |
| Vendor inquiry (O-4) | HTIntf ondemand/voice **int return-code** semantics |

## 5. ICD registry (`aeroradio-workflow/references/icd-contracts.md`) — current LIVE

**11 ICD bumps landed across PA-14/15/Phase C/NEXT-2** (1066 lines total now):
- §2 NetworkModule-v1 (doc-only Constant.serveraddress truth-model + R-ADDR-SLOT CLOSED, no version bump)
- §3 **AuthState-v2.2** (L1/L2 + tokenExpiry + rememberMe + 4-path logout + L2 atomic invariant)
- §3A LoginAuthenticator-v1
- **§3B StartupAuthDecider-v1 NEW** (synchronous atomic check seam, NEXT-2)
- §4 **TerminalDto-v2.1** (28 wire fields + ★ terminal.zone NOT membership)
- §5 **ZoneDto-v2.1** (terzone SOLE source + 多对多 + envelope-meta)
- §6 BroadcastWS-v1 (Plan A 不动)
- §7 IPCSocket-v2 + §8 VoiceAAR-v2
- §9 **DesignTokens-v1.3** (16 alias + Noto Sans SC + JetBrains Mono subset)
- §10 **Endpoints-v1.1** (+sechetaskinfo + sechetask CRUD + authorizations refresh/delete)
- §12 **SchemeDto-v2** (2-shape split + ★ name not taskname)
- §13 **TaskRepository-v2.1** (two-step refresh + multi-active + 4 status int 派生)
- §14 ServerStateRepository-v1 + §15 MediaRepository-v1 + §16 OnDemandCast-v1

## 6. git

- Branch `claude/v4-screens-on-refactor` HEAD `7b0d4f0`。**全部 24 commits push 到 origin 完成** (2026-06-01 restart 前)。
- **commit history** (most recent first)：
  ```
  7b0d4f0 workflow: NEXT-2 D-16 3 ICD bump + ledger + R-ADDR-SLOT CLOSED
  ff55ed2 ui: PA-NEXT2-FE rememberMe wiring
  dd3c0ad data: NEXT-2 D-16 fix — AuthStore v2.2 + StartupAuthDecider + A2 backup rules
  c6ea974 workflow: 8 ICD v1.3 land + Critic skill RTM-ERR-003/4/5/6
  05f4a56 ui: Phase C app
  d07c1a5 ui: Phase C base (Color v1.3 + Noto Sans SC)
  2532c2e data: PA-15 V3TaskRepo two-step
  865b426 ui: Phase B Login + PA-14 C-1 TabBar
  2549b1e data: PA-14 V3TerminalRepo terzone
  9b14303 fix: PA-12 cleartext + PA-13 foreground.register
  5ed351f workflow (PA-10 correction + baseline pre-this-burst)
  ```
- `.gitattributes` 已 fix CRLF root cause; 9 PA + 5-Tab + ICD batch 全 LF-clean。

## 7. Teammates (all idle/STANDBY at this milestone)

- **data-integration-2** (pink): NEXT-2 audit + fix + ICD proposals done。Next: per CTO direction (Logout impl, NEXT-3, etc).
- **fe-business-2** (red): PA-NEXT2-FE rememberMe wiring done。Next: Logout UI affordance (small PA) when CTO directs。
- **fe-platform-2** (blue): Phase C base (token + font) done。Next: BL-TOKEN-RENAME (Phase 3 breaking) when CTO scheduled。
- **legacy-native-2** (green): AR-104 voice + PA-08 cast done。Next: R-001 device-verify (CTO arm64 hardware) or NEXT-3。
- **critic-2** (cyan): 4 consecutive 5-leg reviews PASSED (PA-14/15 + Phase C + NEXT-2)。`smoke-render-required` family standing pattern。

## 8. Operating discipline (unchanged, critical)

- **Build-slot serial**: ONE `--no-daemon` build at a time; slot-holder builds, others HOLD edit+build。JBR=`/home/it1234/android-studio/jbr`; NEVER `--stop` (memory `std-build-serial`)。
- **idle ≠ deliverable**: teammates sometimes finish + idle without sending the deliverable/verdict; FS-verify (test XML mtime / files) then nudge (don't passively wait)。fe-platform 17min finalize-doc tail 是教科书例子。
- **STD-CANONICAL-STATE**: `.state/tasks.yaml` + Critic PASS 是 authoritative; harness `TaskList` "completed" 不用。
- **STD-ICD-WRITE**: PM serializes `icd-contracts.md`; domains propose diffs。
- **STD-SHAREDFILE**: shared config 改时 deliverable 明示 line range + rationale。
- **5-leg gate (Critic 标准)**: L1 Spec / L2 Code (token-application-trace 3-leg) / L3 Test pass / L4 Data (wire snapshot + reconciliation table 4 cells filled) / L5 Smoke (emulator real-run + screencap, never device-pending fallback unless tool-specific failure reason)。
- **6 sibling memories (family)**: `constraint-vs-artifact-rule` / `sweep-evidentiary-bar` / `visual-fidelity-review-gate` / `token-application-trace` / `data-snapshot-verification` / `smoke-render-required` — all exercised this session burst, standing pattern for future audits。
- Per-Tab Critic review units; carry near-unreachable one-liners as backlog。

---

*Resume: read this + tasks.yaml (`next2_d16_2026_06_01` + `commit_batch_2026_06_01` + `d16_2026_06_01`) + decision-log D-13/D-14/D-15/D-16 → re-spawn 5 teammates (`-3` suffix likely) → await CTO endgame direction (§2 task list / NEXT-3 candidates).*
