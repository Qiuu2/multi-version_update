# Handover Snapshot — AeroRadioControl v4 (PM)

> Refreshed **2026-05-29** (mid-session, 5-Tab build complete). Supersedes the 2026-05-28 version.
> You are **team-lead, playing Project Manager**. Agent-Teams workflow (`CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`).
> Startup: read `aeroradio-workflow/SKILL.md` + `aeroradio-workflow/agents/project-manager/*.md`. Authority ledger = `.state/tasks.yaml` (STD-CANONICAL-STATE). Decisions = `.state/decision-log.md`.

---

## 0. ⚠ FIRST: team re-spawn after /clear
The team `aeroradio-v4` config + task list persist on disk, but live teammate processes die on /clear. Re-spawn the 5 teammates via the Agent tool (team_name="aeroradio-v4", name=<role>, subagent_type general-purpose, model opus, run_in_background). **Re-spawning appends a numeric suffix** because the dead members still hold the original names — this session's live names are **`data-integration-2 / critic-2 / fe-business-2 / fe-platform-2 / legacy-native-2`** (a future re-spawn likely becomes `-3`). Verify the spawn-result names; SendMessage to those exact names. Each teammate prompt: read its `aeroradio-workflow/agents/<role>/{profile,soul,skill,memory}.md` + MEMORY.md recall + handover + tasks.yaml + decision-log. (See memory [[agent-team-respawn-suffix]].)

## 1. Current progress — 5-Tab BUILD COMPLETE (2026-05-29)
- **Phase 0 + Phase 1 prep + ICD逆推**: all Critic-PASSED (AR-001~010, AR-101~107, AR-110/111).
- **Plan A (D-13)**: UI-only, v3 data layer + httptask/*Method.java ZERO change, no WS, only new net code = callback→StateFlow adapter.
- **PA-01** (V3CallbackAdapter + V3TerminalRepository @Binds-swap + V3LoginAuthenticator) = PASS. **PA-02** (PollingRefreshScheduler, replaces WS) = PASS.
- **This session (2026-05-29), all Critic PASSED HIGH, independently reran:**
  - **PA-04**: F-2 (V3LoginAuthenticator→ServerConfig.setBaseUrl) + TaskRepository stub.
  - **终端 Tab** (PA-03①②): polling wired into Hub/ZoneDetail VMs; VM zero-change proven.
  - **BL-GOLD-TOKEN** + ICD-DesignTokens v1.2.
  - **任务 Tab** (PA-03③): TaskHome/SchemeDetail/ExecutionLog VMs on TaskRepository **stub (empty)**; de-mock; PASSED_WITH_MINOR (carried zone=mediaName→blank).
  - **服务 Tab**: PA-05 V3ServerStateRepository (REAL) + Service VM. Real data.
  - **广播 Tab**: PA-07 MediaRepository list (REAL) + PA-08 OnDemandCastAdapter (AAR cast) + PA-09 SD1(shell+targets+点播) + SD2(寻呼/对讲 voice). All done.
- **5-Tab: 终端✓ 任务✓(stub) 服务✓ 广播✓ ; AI 降级 (not built).**

## 2. ⚠ What REMAINS (the endgame — awaiting CTO direction at this milestone)
1. **Real V3TaskRepository impl** (data) — task Tab runs on the EMPTY STUB; real v3 wire (SchemeDto fidelity incl. `projectstatetate` typo, the carried `zone→blank` decision, SchemeTaskStatus value set, getExecutionLog source) is the pending "big data review" (Critic primed). On landing, fe re-touches task VM/mapper (small).
2. **Runtime demo** — deferred to one unified emulator pass after all Tabs; **needs CTO's emulator + reachable v3 host** (.state/runtime-demo-checklist.md). Also where R-ADDR-SLOT gets its real-world re-check.
3. **PTT design call** (CTO) — fe replaced Handoff's press-hold PTT with tap-to-start/end (honest to the session seam; Critic+PM endorse). Restore PTT = UX-only follow-up, no VM/adapter rework.
4. **Commit/PR** — whole session UNCOMMITTED (only on CTO request). HEAD still `1e5b6b8`.
5. **R-001 arm64 device-verify** (CTO) — voice + cast AAR happy-paths statically sound, NOT device-verified.

## 3. Key architecture facts
- All v3 repos use the **ServerConfig seam** (=Constant.serveraddress), set at login by V3LoginAuthenticator (F-2). Repos: V3TerminalRepository, V3ServerStateRepository, V3MediaRepository (all REAL, path1 raw JSON via V3CallbackAdapter) + V3TaskRepository (STUB).
- **广播 Tab is 3-domain**: 点播 = MediaRepository(list, data §15) + OnDemandCastAdapter(cast, legacy §16, AAR); 寻呼/对讲 = VoiceTalkAdapter(legacy, AR-104/§8); targets = BroadcastTargetsViewModel. **Adapters are terminal-level (List<Int>); targets are zone-level** → fe resolves zone→terminal at the VM boundary via shared **BroadcastTargetResolver** (Option A, TerminalRepository SSOT, dedup + non-numeric drop + empty-guard).
- **Permission asymmetry**: 寻呼/对讲 need RECORD_AUDIO (adapter-owned gate, fe prompts on RecordAudioPermissionException + retries); 点播 does NOT (playback).
- Held cross-domain contracts: `.state/voice-fe-consumption-note.md` + `.state/cast-seam-contract.md`.

## 4. Risks / watch-items
- **R-ADDR-SLOT** (DORMANT-TRACKED): V4 login writes Constant.serveraddress but NOT v3 PreferencesUtil("serverAddress"); 15 retained v3 GET paths read the latter but are unreachable from V4 nav. Re-check at runtime demo. Mitigation if it wakes: ConstantServerConfig also write PreferencesUtil (needs Context). See tasks.yaml risks_added.
- **R-001** (LOW-conditional, GO-pending-device): voice/cast AAR native, arm64 device-verify = CTO.
- Vendor inquiry (alongside O-4): HTIntf ondemand/voice **int return-code** semantics (isOk()=true assumption).

## 5. ICD registry (`aeroradio-workflow/references/icd-contracts.md`) — current LIVE
NetworkModule/AuthState-v2/LoginAuthenticator/TerminalDto-v2/ZoneDto-v2/TaskRepository-v1(§13, impl=stub)/SchemeDto(projectstatetate)/TtsTaskDto/MediaDto/ServerStateDto/ServerStateRepository-v1(§14, real)/MediaRepository-v1(§15, real list)/OnDemandCast-v1(§16, LIVE control/DRAFT device+vendor)/IPCSocket-v2/VoiceAAR-v2/DesignTokens-v1.2. WS ICDs dropped (Plan A). Endpoints-v1=DRAFT-FROZEN.

## 6. git
- Branch `claude/v4-screens-on-refactor` (PR→main). HEAD `1e5b6b8` (5 prior-session checkpoint commits). **This entire session uncommitted** (CTO-gated). When committing: per-task/hunk grouping, exclude .idea/.claude/.messages, .gitattributes already fixes CRLF root cause.

## 7. Teammates (all idle/standby at milestone)
- **data-integration-2** (pink): PA-04/05/07 done. Next: real V3TaskRepository impl (the big one) when CTO directs.
- **fe-business-2** (red): all 4 Tabs done. Next: task VM re-touch after real V3TaskRepository.
- **fe-platform-2** (blue): PA-02 + gold token done. Idle.
- **legacy-native-2** (green): AR-104 voice + PA-08 cast done. Next: R-001 device (CTO).
- **critic-2** (cyan): all reviews done; primed for the real V3TaskRepository "big review".

## 8. Operating discipline (unchanged, critical)
- **Build-slot serial**: ONE `--no-daemon` build at a time; slot-holder builds, others HOLD edit+build. Critic's `--no-daemon --rerun-tasks` rerun is the neutral arbiter (STD-DAEMON-STALE). JBR=`/home/it1234/android-studio/jbr`; NEVER `--stop`.
- **idle ≠ deliverable**: teammates sometimes finish + idle without sending the deliverable/verdict; FS-verify (test XML mtime / files) then nudge (don't passively wait).
- **STD-CANONICAL-STATE**: .state/tasks.yaml + Critic PASS is authoritative; harness TaskList "completed" = teammate "delivered" marker only (harness task IDs have been unreliable this session — rely on .state).
- **STD-ICD-WRITE**: PM serializes icd-contracts.md; domains propose diffs.
- Per-Tab Critic review units; carry near-unreachable one-liners as backlog, don't burn slot cycles.

---
*Resume: read this + tasks.yaml (pa_tasks PA-01~09 + risks_added + session_resume_2026_05_29) + decision-log D-13/D-14 → re-spawn 5 teammates (-N suffix) → await CTO endgame direction (§2).*
