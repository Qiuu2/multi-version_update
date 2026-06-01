# Zone mapping root cause — 2026-05-30

## One-line root cause

**`V3TerminalRepository.fetchAndPublish` fetches both `/terminal/terzone` AND `/terminal/terminalinfo`, then groups the flat terminal list by `terminal.zone` field and re-attaches by `zone.id` — but `terminal.zone` is NOT the zone-membership key. The real membership is the nested `zone.terminal[]` array already present on the `/terminal/terzone` response. The all-terminals + groupBy path produces wrong results (and the wrong cardinality) for every zone.**

Fix = use ONLY `/terminal/terzone` and read `zone.terminal[]` directly; delete the second fetch and the client-side join.

## Evidence — code

`app/src/main/java/com/htgd/radiocontrol/aeroradiocontrol/data/repository/V3TerminalRepository.kt:84-101`:

```kotlin
private suspend fun fetchAndPublish(): Result<Unit> = runCatching {
    val zonesJson = adapter.get(url(PATH_ZONES)).getOrThrow()           // /terminal/terzone
    val terminalsJson = adapter.get(url(PATH_TERMINALS)).getOrThrow()   // /terminal/terminalinfo

    val terminals = gson.fromJson(terminalsJson, TerminalEnvelopeDto::class.java)
        ?.data?.mapNotNull { it.toTerminalOrNull() }.orEmpty()
    val byZone = terminals.groupBy { it.zoneId }                        // ← BUG: by terminal.zone field

    val zones = gson.fromJson(zonesJson, ZoneEnvelopeDto::class.java)
        ?.data?.mapNotNull { it.toZoneOrNull() }
        ?.map { zone -> zone.copy(terminals = byZone[zone.id].orEmpty()) }  // ← BUG: re-attach
        .orEmpty()
    zonesFlow.value = zones
}
```

`TerminalDto.kt:42` captures `@SerializedName("zone") val zone: Int? = null` (becomes `zoneId` in domain via `TerminalMapper.kt:29`); that field is what `groupBy` keys on. **It does NOT represent zone membership.**

`ZoneDto.kt:59-67` has NO `terminal[]` field — so the new stack throws away the nested data already in the terzone response and tries to reconstruct it (wrongly) from a different endpoint.

## Evidence — ground truth (CTO capture `terzone-cto-capture-2026-05-30.json`)

Zone "操场" id=1 carries 4 nested terminals: 14 右一终端, 15 右二终端, 16 营销大厅-2, 18 网络功放2.

Their `terminal.zone` field values: **0, 0, 0, 8** — none equal 1.

So `byZone[1]` (operated-on by the current code) contains the wrong terminals (whatever has `terminal.zone==1`), and the right terminals are buried under `byZone[0]` (which then incorrectly gets attached to a zone with `zone.id==0`, if one exists, or simply gets dropped).

Also: terminal id=14 appears in 4 zones (操场, 英语角分区, 航天, 会议室). Membership is **many-to-many**, which a single-field flat groupBy can never represent. The nested `zone.terminal[]` is the only correct source.

## Evidence — old stack (v3) is consistent with the truth

v3 does NOT do the groupBy. `httptask/ZoneMethod.getZoneTerminal(zoneid)` (line 42-66) calls **per zone**:

```
GET /terminal/zoneterminal/{zoneid}   →   MachineListRsp{data:[MachineInfo]}
```

i.e. the dedicated association endpoint, one call per zone. Different shape than terzone, but same intent: read membership from the server's join, not derive it from `terminal.zone`.

## R-ADDR-SLOT check (PM asked)

ZoneMethod.java mixes both base-URL slots IN THE SAME CLASS:
- `:44` — `Constant.serveraddress + Constant.getGroupTerminal + "/" + zoneid` (uses Constant slot)
- `:85` — `PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getGroupTerminal + "/" + i` (uses PreferencesUtil slot)

If ZoneMethod were ever pulled into a V4 path, R-ADDR-SLOT would wake along the line-85 code path (V3LoginAuthenticator writes Constant.serveraddress only). **It is not pulled in by anything in `data/` today**, so dormant — but it IS used by v3 Activities (ZoneManageActivity, AddZoneActivity, ZoneDetailActivity, TempTTSActivity). If V4 ever launches one of those Activities, R-ADDR-SLOT wakes. Watch item.

## (a) / (b) / (c) verdict

**Hit (b) AND (c):**
- (b) Mapper / Repository logic wrong: joining by `terminal.zone` instead of using `zone.terminal[]`.
- (c) DTO incomplete: `ZoneDto` is missing the nested `terminal: List<TerminalDto>` field (and 24 - currently-captured TerminalDto fields).

Not (a): the backend data is fine; v4's read of it is wrong.
