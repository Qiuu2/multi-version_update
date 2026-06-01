# Phase C — Spec ↔ Actual Diff (施工图 for fe-business)

> Anchors: Handoff.html (spec source-of-truth) + `.state/cto-verify/01-03-*.jpg` (CTO Huawei 真机 2026-05-30 17:28).
> Tokens listed below have ALL been added to `app/src/main/java/.../ui/theme/Color.kt` v1.3 (fe-platform slot 2026-05-30).
> fe-business consumes via `AeroTheme.colors.<role>` — zero new hex on consumer side.

## Token mapping table — what fe-business should swap

| Surface | Current code | New token (v1.3) | Spec ref | Real-device evidence | Acceptance |
|---|---|---|---|---|---|
| Broadcast 3-mode "寻呼" tint | `c.statusPaging` / `c.pageWarm` (whichever was used) | `c.modePaging` | Handoff.html:883 | `02-broadcast.jpg`: selected mode = teal-green (wrong), non-selected = grey (no mode tint at all) | All 3 mode chips tinted with their respective mode color in BOTH selected and resting states |
| Broadcast 3-mode "对讲" tint | (nothing currently — same teal as paging) | `c.modeIntercom` | Handoff.html:884 | same: 对讲 chip = same teal as 寻呼 selected | 对讲 chip resting state = #2563EB blue accent (text or underline) |
| Broadcast 3-mode "点播" tint | (nothing) | `c.modeCast` | Handoff.html:885 | same: 点播 chip = grey | 点播 chip resting state = #0E7C70 teal (shares brand) |
| Broadcast CTA "🎤 开始寻呼" button bg | hard-coded green / brand | `c.modePaging` (when mode=page) — bg = `c.modePaging`, fg = white | Handoff.html:883 + `cto-verify/02-broadcast.jpg` shows wrong color #16A34A | CTA bg = `c.modePaging` orange (#EA580C) when in 寻呼 mode; should be `c.modeIntercom` blue when in 对讲 mode; `c.modeCast` teal when in 点播 mode |
| Terminal filter chip set | `listOf(全部, 在线, 离线, 故障)` (4 items) | EXTEND list to include "播放中" / "寻呼中" (5 spec states) | Handoff.html:512-516 + Handoff.html:807 `state(online/offline/fault/playing/paging)` | `01-terminal.jpg` shows only 4 chips, paging absent | filter chip set = 5 items: 全部 + 在线 + 离线 + 故障 + 播放中 + 寻呼中 (could be 6 incl 全部, or "全部 + 5 spec states") |
| Terminal tile icon bg | hardcoded `c.primarySoft` for all | per-state: `c.tileOnlineSoft` / `c.tileOfflineSoft` / `c.tileFaultSoft` / `c.tilePlayingSoft` / `c.tilePagingSoft` | Handoff.html:625-657 | `01-terminal.jpg`: all 4 tiles render identical light-teal icon bg regardless of state | each tile icon bg follows its state's `*Soft` variant — visible differentiation in screenshot |
| Terminal tile state dot (top-right) | hardcoded `c.statusOnline` for all | `c.tileOnline` / `c.tileOffline` / `c.tileFault` / `c.tilePlaying` / `c.tilePaging` per state | Handoff.html:626,640,647,653 + (paging by extension) | `01-terminal.jpg`: all tiles show same tiny green dot | dot color follows state; for fault state per spec also bumps to 18dp + priority_high glyph; for playing per spec replaces dot with ▮▮▮▮ waveform |
| Terminal tile status pill | hardcoded `StatusPillState.Online` for all | follow domain `TerminalState`; pill consumes existing `c.statusXxxSoft` (no change there — already correct via v1.1) | Handoff.html:628,642,649,656 | `01-terminal.jpg`: all 4 tiles show "在线" pill | pill text + color matches domain state, all 5 states rendered correctly |
| Task card state pill | NOT rendered today | `c.taskCardStateDone` (#8A929F) / `c.taskCardStateRunning` (#EA580C) / `c.taskCardStatePending` (#4A5260) — apply to pill text + (optional) tinted soft bg via `c.statusXxxSoft` reuse | Handoff.html:930-931 (states listed, hex NOT pinned — see ICD documented-assumption) | `03-task.jpg`: task rows have NO state pill at all | pill rendered on each task row with one of 3 states; copy = 已完成/进行中/待执行 |
| Task card 目标分区 tag | NOT rendered today | (no new token — use existing `c.ink3` for label color, surface chip bg) | Handoff.html:930 "任务卡含时间/状态/目标分区" | `03-task.jpg`: no zone tag on any task row | tag rendered showing the task's target zone name (e.g. "操场" / "教学楼") |
| Task card timestamp | format = `HH:mm:ss` (e.g. "07:50:00") | format = `HH:mm` (truncate seconds) | Handoff.html:930 not explicit; v4.html shows HH:mm; ground truth: AeroRadio bell-schedule UX = minute precision, schools don't pick seconds | `03-task.jpg`: shows "07:50:00", "08:20:00", etc | timestamp = `HH:mm`, no `:ss` — pure DateTimeFormatter change, no token |
| Task migrated / swapped border | already correct (v1.2 `c.gold`) | no change | Handoff.html:946-948 | (not in screenshots — no migrated tasks in 海王作息) | no work |
| Tab identity colors (5 Tab) | already correct (v1.2 + PA-14 C-1) | no change | Handoff.html:783-957 spec card src lines + v4.html:110 | C-1 visible on all 3 screenshots (任务 purple, 广播 orange when selected) | no work — PA-14 closed this |
| Sans / Mono fonts | `FontFamily.Default` / `FontFamily.Monospace` | **`AeroSans` / `AeroMono` now real** (Type.kt:30-39) — Noto Sans SC 3-weight subset + JetBrains Mono 2-weight subset vendored to `res/font/` per CTO Q2=(ii) Subset GO 2026-05-30 | Handoff.html:547 §字号 "Noto Sans SC · JetBrains Mono" | `01-03.jpg` pre-fix: HarmonyOS Sans renders; post-fix needs real-device verify via CTO screencap | DONE on fe-platform side — fe-business sees no API change (continue using `AeroTheme.typography.*`); real-device check confirms Chinese chars render in Noto Sans SC not HarmonyOS |
| Quick-action 4 chip (方案详情/编辑/执行日志/临时广播) tint | all neutral grey | NO CHANGE — INFO not BLOCKER | Handoff.html spec has NO chip color rule | `03-task.jpg`: all 4 chips identical grey | INFO: spec allows neutral; if visual fidelity demands later, would need new spec ruling — DO NOT add tokens now |

## File-level施工 list for fe-business

1. **`ui/screens/broadcast/BroadcastScreen.kt`** (and any mode segmented control / CTA composable) — swap hardcoded brand/status colors for `c.modePaging` / `c.modeIntercom` / `c.modeCast` based on selected mode; bind CTA bg to the active mode color.
2. **`ui/screens/terminal/TerminalHubScreen.kt`** — extend filter chip list to 5 spec states (add 播放中 + 寻呼中); per-tile rendering switch on domain `TerminalState` to pick `c.tileXxx` fg + `c.tileXxxSoft` icon bg; dot color likewise.
3. **`ui/screens/terminal/TerminalUiModels.kt`** (or whatever model maps domain→UI) — extend the state mapping to drive the new tile token selection; pull from existing `TerminalState` sealed class (no domain change).
4. **`ui/screens/task/TaskScreen.kt`** — add 状态 pill composable (3-state, copy 已完成/进行中/待执行); add 目标分区 tag (text on `c.ink3`); change timestamp formatter to `HH:mm`.
5. **No changes** to AeroTheme.kt / Color.kt / Type.kt (fe-platform's lane).

## Token summary

| Token (v1.3 new) | Hex | Backing literal | KDoc tag |
|---|---|---|---|
| modePaging | #EA580C | PageWarm | LIVE — Handoff.html:883 |
| modeIntercom | #2563EB | TalkBlue | LIVE — Handoff.html:884 |
| modeCast | #0E7C70 | Primary | LIVE — Handoff.html:885 |
| tileOnline | #16A34A | StatusOnline | LIVE — Handoff.html:626 |
| tileOffline | #8A929F | StatusOffline | LIVE — Handoff.html:640 |
| tileFault | #DC2626 | StatusFault | LIVE — Handoff.html:647 |
| tilePlaying | #2563EB | StatusPlaying | LIVE — Handoff.html:653 |
| tilePaging | #EA580C | StatusPaging | LIVE — Handoff.html:512-516 (state list) |
| tileOnlineSoft | #E6F4F2 | PrimarySoft | LIVE — Handoff.html:625 |
| tileOfflineSoft | #EEF0F3 | Surface3 | LIVE — Handoff.html:639 |
| tileFaultSoft | #FDECEC | StatusFaultSoft (v1.1) | LIVE — Handoff.html:646 |
| tilePlayingSoft | #E8EFFD | StatusPlayingSoft (v1.1) | LIVE — Handoff.html:653 |
| tilePagingSoft | #FDEEE2 | StatusPagingSoft (v1.1) | LIVE — Handoff.html:512-516 derived |
| taskCardStateDone | #8A929F | Ink3 | LIVE — documented assumption (spec line 930 names states, no hex pin) |
| taskCardStateRunning | #EA580C | StatusPaging | LIVE — matches TaskScreen.kt:205 existing usage |
| taskCardStatePending | #4A5260 | Ink2 | LIVE — preliminary, awaiting CTO real-device review |

16 aliases total. Zero new hex. All v1.3-tagged, additive, non-breaking.

## INFO findings (not blocking)

- **I-1 — quick-action chip tinting**: spec is silent on color rule for 方案详情/编辑/执行日志/临时广播 chips. Current neutral grey is defensible. Re-raise if CTO real-device review pushes back.
- **I-2 — terminal "5 chip" vs "6 chip"**: spec lists 5 states (online/offline/fault/playing/paging) plus the implicit "全部" all-pass chip = 6 chips total. May need horizontal scroll on narrow phones (Huawei screenshot shows full-width chips already filling 4; 6 would overflow). fe-business: consider scrollable LazyRow if widths don't fit.
- **I-3 — terminal tile playing-state visual**: spec line 653-656 replaces the simple dot with a 4-bar waveform (▮▮▮▮). Not just a color change — a glyph change. Out of fe-platform scope (no glyph token); flag for fe-business.
- **I-4 — terminal tile fault-state visual**: spec line 644-650 enlarges the dot to 18dp and embeds a `priority_high` material icon. Same — glyph change for fe-business.

## Open items

- **Status pill final hex** — Q1 = (a) DOCUMENTED ASSUMPTION per PM (2026-05-30). Will not change consumer code if CTO real-device review picks different hex later (alias level absorbs the swap).
- **Subset miss workflow** — if real-device demo shows a 方块字 (missing glyph) for any Chinese character outside the subset, the fix is in `/tmp/fonts-work/noto_subset_chars.txt` corpus + re-run pyftsubset + re-commit. Expected workflow per CTO. Subset currently covers GB2312 L1+L2 (6763) ∪ project chars from Handoff.html / AeroRadio v4.html / .kt/.java sources (963) ∪ ASCII ∪ CJK punctuation ∪ fullwidth forms = 7173 chars total. All UI critical strings (终端/广播/任务/服务/AI/海王作息/14 task names/zone names) verified covered.

— fe-platform slot, 2026-05-30
