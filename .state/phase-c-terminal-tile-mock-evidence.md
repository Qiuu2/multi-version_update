# Phase C — Terminal Tile 5-State Mock-Evidence Doc

> PA-14 Phase C app layer / fe-business / 2026-05-30.
> Critic Leg-5 evidence path for the `TerminalTile` per-state rendering, since the
> Huawei real-device host has **all-online data** (4/4 操场, 2/2 英语角, 1/1 航天)
> — the 5 tile states can't organically render from the current host data alone.
> Lead ruling 2026-05-30: this doc + Compose preview screencap is accepted as L5
> evidence in lieu of an organically-mixed real-device screencap.

---

## Code traces (Leg 1-3 closed)

### Spec → Token (Leg 1)
Per `design-system-spec.md` §1.2 + Handoff.html:512-516, 626, 640, 647, 653:

| State | Spec hex (fg / soft) |
|---|---|
| Online  | #16A34A / #E6F4F2 |
| Offline | #8A929F / #EEF0F3 |
| Fault   | #DC2626 / #FDECEC |
| Playing | #2563EB / #E8EFFD |
| Paging  | #EA580C / #FDEEE2 |

### Token → Code (Leg 2)
v1.3 `c.tile*` aliases bind the 10 hexes (Color.kt:118-130) to role-named fields on
`AeroColors`. Consumed in `TerminalTile.kt`:

| State | IconBadge (bg / fg) line | CornerBadge dot line |
|---|---|---|
| Online  | `colors.tileOnline to colors.tileOnlineSoft`   (TerminalTile.kt:109) | `colors.tileOnline`  (TerminalTile.kt:152) |
| Offline | `colors.tileOffline to colors.tileOfflineSoft` (TerminalTile.kt:110) | `colors.tileOffline` (TerminalTile.kt:153) |
| Fault   | `colors.tileFault to colors.tileFaultSoft`     (TerminalTile.kt:111) | `colors.tileFault`   (TerminalTile.kt:147 — 18dp dot + PriorityHigh per spec) |
| Playing | `colors.tilePlaying to colors.tilePlayingSoft` (TerminalTile.kt:112) | `colors.tilePlaying` (TerminalTile.kt:154) |
| Paging  | `colors.tilePaging to colors.tilePagingSoft`   (TerminalTile.kt:113) | `colors.tilePaging`  (TerminalTile.kt:155) |

(Line numbers indicative — re-resolve with `grep -n` after any future re-edit.)

The 5-way `when` in `IconBadge` is exhaustive over `TerminalStatus` (Online/Offline/
Fault/Playing/Paging — `StatusPill.kt:24`); a new state added upstream is a compile
error here, forcing an explicit mapping decision rather than a silent misrender.

### Code → Render (Leg 3 — the data-gated leg)
On the Huawei host today, the data layer (`V3TerminalRepository`) returns 7
terminals × `TerminalStatus.Online`. Hence the screencap `01-terminal-tab-huawei-2026-05-30.jpg`
shows 7 identical green-dot / primarySoft-bg tiles — that is the **correct** render
for that data, not a token-binding bug. The 4 other states (Offline/Fault/Playing/
Paging) take their code paths only when the SSOT emits a Terminal with the matching
status. Per the lead's GO ruling, this evidence is sufficient for Critic Leg 3
because the bindings above are static and exhaustive — there is no missing branch.

---

## Mocked-state demo scenarios (Leg 5 alternative)

Two paths Critic can take; either closes Leg 5.

### Path A — Compose `@Preview`
A multi-state preview can be added to `TerminalTile.kt` (or a sibling preview file)
that calls `TerminalTile(name="N1", status=TerminalStatus.Online)` … through all 5
+ a selected variant. The Compose preview pane in Android Studio renders all 6
without any device or repository. The screencap of that preview pane is L5 evidence.

Suggested preview block (add inside `TerminalTile.kt` or a new
`TerminalTilePreview.kt` under `app/src/main` — `@Preview` is preview-only, no
runtime cost):

```kotlin
@Preview(name = "All 5 states + selected", showBackground = true)
@Composable
private fun TerminalTilePreview() {
    AeroTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TerminalTile(name = "右一终端",   status = TerminalStatus.Online)
                TerminalTile(name = "财务室",     status = TerminalStatus.Offline)
                TerminalTile(name = "网络功放2", status = TerminalStatus.Fault)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TerminalTile(name = "操场扬声器", status = TerminalStatus.Playing)
                TerminalTile(name = "航天广播",   status = TerminalStatus.Paging)
                TerminalTile(name = "海口主控",   status = TerminalStatus.Online, selected = true)
            }
        }
    }
}
```

Critic action: open `TerminalTile.kt` in Android Studio (or generate the preview
via `./gradlew :app:packageDebugAndroidTest` + a Paparazzi-style snapshot test if
available), screencap the rendered preview, attach to the L5 evidence bundle.

### Path B — Fake repository state injection
For an emulator run, a temporary mocked `TerminalRepository` could be wired via
Hilt @TestInstallIn, returning 7 terminals across all 5 states + 1 selected. This
takes ~10 minutes of fixture work but produces a live, scrollable APK that renders
the mixed-state hub end-to-end. Recommended only if Critic specifically wants
emulator (not preview) evidence.

### Path C — Server-side injection (CTO)
If/when the CTO's v3 host can be set up with mixed states (one terminal offlined
via the legacy admin UI, another in fault, etc.), the existing real-device flow
(install APK → screencap) renders all states organically. This is the cleanest
evidence but depends on CTO host availability and is NOT a fe-business action.

---

## Recommendation

Adopt **Path A** as the L5-closing artifact for this Phase C delivery:
- zero additional production code (preview blocks don't ship in release builds);
- Critic can render + screencap without leaving the IDE;
- the 5-state x 1-selected preview demonstrates every binding from the §"Token →
  Code" table above in one image.

Future Phase: when the v3 host data carries mixed states organically (or CTO
chooses Path C), Critic re-runs the smoke-render with a real-device screencap and
the doc retires.

---

*PA-14 Phase C / fe-business / 2026-05-30*
