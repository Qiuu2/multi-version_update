# ICD Proposals — Phase C-residual

> Owner: fe-platform. PM serializes into `aeroradio-workflow/references/icd-contracts.md` after Critic PASS (STD-ICD-WRITE — I do not edit the registry directly).

## Proposal #1 — ICD-DesignTokens v1.2 → v1.3 (additive aliases)

**Status**: PROPOSED — pending Critic review of Color.kt diff at `app/src/main/java/com/htgd/radiocontrol/aeroradiocontrol/ui/theme/Color.kt:104-145` (post-edit numbering).

**Breaking change?** NO. Strictly additive defaulted fields on `AeroColors` data class. All existing consumers unchanged. Old token names remain valid forever (sealed AR-009 R-1 alias pattern, BL-TOKEN-RENAME backlog).

**New hex literals**? ZERO. Every new field references an existing backing literal already declared in `Color.kt`.

### v1.3 additions to §15 DesignTokens (16 aliases)

#### v1.3.1 — Broadcast 3-mode semantic aliases

| Token | Hex | Backs onto | Spec ref | Consumer |
|---|---|---|---|---|
| `modePaging` | #EA580C | `PageWarm` | Handoff.html:883 §s-broadcast 三档差异 | `BroadcastScreen` mode=page tint |
| `modeIntercom` | #2563EB | `TalkBlue` | Handoff.html:884 | `BroadcastScreen` mode=talk tint |
| `modeCast` | #0E7C70 | `Primary` | Handoff.html:885 | `BroadcastScreen` mode=cast tint (shares brand teal) |

Rationale: spec defines 3 mode tints with verbatim hex. Existing `pageWarm`/`talkBlue`/`primary` carry the same hex but under non-mode roles, so consumers currently either hard-code or borrow a confusingly-named token. The alias publishes the role explicitly without churning consumers.

#### v1.3.2 — Terminal tile 5-state semantic aliases

| Token | Hex | Backs onto | Spec ref | Consumer |
|---|---|---|---|---|
| `tileOnline` | #16A34A | `StatusOnline` | Handoff.html:626 §components | `TerminalTile` status dot — online |
| `tileOffline` | #8A929F | `StatusOffline` | Handoff.html:640 | offline |
| `tileFault` | #DC2626 | `StatusFault` | Handoff.html:647 | fault |
| `tilePlaying` | #2563EB | `StatusPlaying` | Handoff.html:653 | playing |
| `tilePaging` | #EA580C | `StatusPaging` | Handoff.html:512-516 derived (paging is in the status palette but not explicitly redrawn at tile level) | paging |
| `tileOnlineSoft` | #E6F4F2 | `PrimarySoft` | Handoff.html:625 | online icon bg |
| `tileOfflineSoft` | #EEF0F3 | `Surface3` | Handoff.html:639 | offline icon bg |
| `tileFaultSoft` | #FDECEC | `StatusFaultSoft` (v1.1) | Handoff.html:646 | fault icon bg |
| `tilePlayingSoft` | #E8EFFD | `StatusPlayingSoft` (v1.1) | Handoff.html:653 | playing icon bg |
| `tilePagingSoft` | #FDEEE2 | `StatusPagingSoft` (v1.1) | Handoff.html derived | paging icon bg |

Rationale: spec's terminal tile section pulls colors from `--status-*` and surface tokens. The `tileXxx` aliases let `TerminalHubScreen` switch on domain `TerminalState` and pick a token whose name reflects the rendering role, decoupling from any future divergence between status-pill and tile-icon palettes.

#### v1.3.3 — Task-card state pill (PM-confirmed P2 naming; DOCUMENTED ASSUMPTION)

| Token | Hex | Backs onto | Spec ref | Consumer |
|---|---|---|---|---|
| `taskCardStateDone` | #8A929F | `Ink3` | Handoff.html:930-931 §s-task lists states; NO hex pin (assumption) | Task card pill — 已完成 |
| `taskCardStateRunning` | #EA580C | `StatusPaging` | matches existing TaskScreen.kt:205 in-code usage for "进行中" — code-fact consistency | Task card pill — 进行中 |
| `taskCardStatePending` | #4A5260 | `Ink2` | preliminary — awaiting CTO real-device review | Task card pill — 待执行 |

**ASSUMPTION TAG**: Handoff.html does NOT pin task-pill hex (line 930 names states 已完成/进行中/待执行/已取消/已迁移/对调; line 974 mentions scheme 启用 toggle without color rule). PM authorized landing these aliases as `LIVE-documented-assumption` (Q1=(a)) so fe-business is unblocked. If CTO real-device review later pushes back, the hex can be re-pointed in Color.kt with zero consumer-code churn (alias level absorbs).

Also reused for scheme-list 启用 / 停用 (single hex source per PM brief — "语义一致 hex 同源").

### Registry row diff for `aeroradio-workflow/references/icd-contracts.md`

Append to §15 DesignTokens version history table:

```
| v1.3   | 2026-05-30 | Phase C-residual semantic-role aliases (additive, 16 new fields, ZERO new hex). Modes broadcast 3-mode (modePaging/modeIntercom/modeCast), terminal tile 5-state (tileOnline/Offline/Fault/Playing/Paging + matching *Soft), task-card 3-state (taskCardStateDone/Running/Pending — DOCUMENTED ASSUMPTION pending CTO real-device review). Sealed AR-009 R-1 alias pattern. NON-BREAKING. |
```

Bump active version line in §1 registry: `ICD-DesignTokens-v1.2` → `ICD-DesignTokens-v1.3`.

---

## Proposal #2 — ICD-DesignTokens v1.3 font row (LIVE — Q2 = (ii) Subset GO)

**Status**: PROPOSED. CTO Q2 decision = (ii) Subset GO, 2026-05-30. Pending Critic review of `Type.kt:25-39` (post-edit) + `app/src/main/res/font/` + `app/licenses/`.

### v1.3.4 — Font assets

| Asset | Source | Path | Size | License |
|---|---|---|---|---|
| Noto Sans SC Regular (subset) | Google Fonts variable-master `NotoSansSC[wght].ttf` instanced at wght=400, then subset | `res/font/noto_sans_sc_regular.ttf` | 2.25 MB | SIL OFL 1.1 |
| Noto Sans SC Medium (subset) | Same master @ wght=500 | `res/font/noto_sans_sc_medium.ttf` | 2.25 MB | SIL OFL 1.1 |
| Noto Sans SC Bold (subset) | Same master @ wght=700 | `res/font/noto_sans_sc_bold.ttf` | 2.25 MB | SIL OFL 1.1 |
| JetBrains Mono Regular (subset) | Upstream `JetBrainsMono-Regular.ttf` subset to ASCII + Latin-1 | `res/font/jetbrains_mono_regular.ttf` | 67 KB | SIL OFL 1.1 |
| JetBrains Mono Medium (subset) | Upstream `JetBrainsMono-Medium.ttf` subset to ASCII + Latin-1 | `res/font/jetbrains_mono_medium.ttf` | 67 KB | SIL OFL 1.1 |

**Subset corpus** (for Noto Sans SC):
- GB2312 L1 (3755) + L2 (3008) = 6763 chars
- Project chars extracted from Handoff.html + AeroRadio v4.html + all `app/src/main/java/*.{kt,java}` = 963 chars (deduped, includes terminal/广播/任务/服务/AI/海王作息/14 task name/zone names)
- ASCII printable U+0020-007E
- CJK punctuation U+3000-303F
- Fullwidth forms U+FF00-FFEF
- **Total: 7173 chars in subset cmap. 100% project-string coverage verified.**

**APK growth**: +5.0 MB measured (51.77 MB baseline → 56.78 MB with fonts). Uncompressed font payload is 6.7 MB; APK ZIP compression brings net to +5.0 MB. Slight overshoot of CTO's stated upper bound (+3-5MB) — accepted as production-correct subset (cannot cut further without dropping GB2312 L2, which would risk demo中无字).

**Subset-miss fix workflow**: append missing char to source corpus → re-run `pyftsubset` → re-commit. Expected workflow per CTO.

### Wiring

`Type.kt` (lines 30-39 post-edit):
```kotlin
internal val AeroSans: FontFamily = FontFamily(
    Font(R.font.noto_sans_sc_regular, FontWeight.Normal),
    Font(R.font.noto_sans_sc_medium,  FontWeight.Medium),
    Font(R.font.noto_sans_sc_bold,    FontWeight.Bold),
)
internal val AeroMono: FontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium,  FontWeight.Medium),
)
```

Consumer-facing API unchanged — all `AeroTypography` `TextStyle`s already reference `AeroSans` / `AeroMono`; fe-business sees zero API change.

**Grep verification**: `FontFamily.Default` / `FontFamily.Monospace` → 0 hits across the codebase.

### License compliance

OFL §3 requires the license text accompany the Font Software. Vendored at:
- `app/licenses/NotoSansSC-OFL.txt` (Adobe Source Han Sans → Google Noto Sans SC re-brand provenance)
- `app/licenses/JetBrainsMono-OFL.txt`

Kept OUT of `res/font/` because Android resource validation only accepts `.ttf` / `.otf` / `.xml` in `res/font/`.

### Extended registry row for §15

Append the v1.3 entry to also cover fonts:

```
| v1.3   | 2026-05-30 | Phase C-residual: (a) 16 semantic-role alias fields on AeroColors (broadcast 3-mode / tile 5-state fg / tile 5-state soft / task-card 3-state — last is DOCUMENTED ASSUMPTION pending CTO real-device review). Sealed AR-009 R-1, additive non-breaking, ZERO new hex. (b) BL-FONT-ASSETS closed: Noto Sans SC (3 weights, GB2312 L1+L2 + project chars subset, +6.75MB raw) + JetBrains Mono (2 weights, ASCII subset, +135KB raw) vendored to res/font/; AeroSans/AeroMono in Type.kt now reference R.font.* (FontFamily.Default/Monospace placeholders removed). Net APK growth +5.0MB measured (CTO Q2=(ii) Subset GO 2026-05-30). OFL §3 license texts under app/licenses/. |
```

— fe-platform slot, 2026-05-30

---

## Backlog addendum — SchemeTask.targetZone field (PA-14 Phase C fe-business INFO)

> Source: PA-14 Phase C deliverable INFO flag #1 + lead 2026-05-30 routing.

### Title
`SchemeTask.targetZone` field — propose for v1.4 (cross-domain).

### Status
**BACKLOG / unscheduled** — fe-business renders `"目标分区 · —"` placeholder chip on every task row so the visual real-estate is reserved; non-blocking for PA-14 demo.

### Impact surface (consumer side)
- `ui/screens/task/TaskScreen.kt` — `TargetZoneTag(label)` currently renders `task.zone.ifBlank { "—" }`. Drop the `ifBlank("—")` once `TaskItem.zone` is populated from a real source.
- `ui/screens/task/TaskUiMappers.kt::toTaskItem` — currently hard-codes `zone = ""` because the domain `SchemeTask` (PA-10) has no zone column. Replace with `zone = task.targetZone.orEmpty()` (or whatever name lands).
- Task-execution decision UX downstream — knowing **which zone(s) a bell rings on** is operationally useful for the school operator (currently invisible).

### Dependencies (cross-domain, NOT fe-business actionable)
- **data-integration audit**: confirm whether the v3 wire (`sechetaskinfo` response payload) carries a zone field at all. The PA-10 capture suggested it does not — `info` is `sechename`, not zone-list. If absent on the existing endpoint:
- **swagger 4358 `/task/taskterminal/{taskid}` exists** (per fe-platform note in the施工图 / lead 2026-05-30) — may give the per-task terminal list (from which a zone can be derived by join against terminal→zone membership). Verifying whether this endpoint is reachable + what it returns is data-integration's call.
- If neither path yields zone info, **fe + data + backend** coordination needed to add a `target_zone` field server-side; this would be a v3 wire change (out of Plan A "zero protocol change" scope, so likely deferred to a post-PA cycle).

### Proposed v1.4 ICD addition (when the field lands)
```
data class SchemeTask(
    val id: String,
    val name: String,
    val status: SchemeTaskStatus,
    val startTime: String? = null,
    val mediaName: String? = null,
    val volume: Int? = null,
    val targetZone: String? = null,      // ← NEW v1.4; nullable for backward compat
)
```
And `TaskItem.zone` becomes `targetZone.orEmpty()` instead of `""`.

### Owner
**PM serializes** per STD-ICD-WRITE. fe-business does NOT add the domain field unilaterally (lead 2026-05-30 ruling: "不主动加 domain 字段").

— fe-business slot, 2026-05-30
