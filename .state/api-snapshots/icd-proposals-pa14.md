# ICD proposals — PA-14 (2026-05-30)

> Producer: data-integration-2. Anchored to CTO's Bearer-token capture
> (`terzone-cto-capture-2026-05-30.json`) + vendor swagger
> (`swagger-v3-vendor.json`). STD-ICD-WRITE: do NOT edit
> `icd-contracts.md` directly; PM serializes after Critic.

## (A) ICD-AuthState — corrections + bumps

**Login request (POST /authorizations)** — form body fields:
- `username` (string) ★ verified against swagger `authorations` definition + V3LoginAuthenticator code (already correct).
- `userpwd` (string) ★ same.
- NOT `name`/`password` — those names appear nowhere in the wire.

**Login response** — `data` is an **ARRAY of token objects**, not a single object:
```
{"data": [{"token": "...", "priority": <int>, "userid": <int>}]}
```
The current `TokenEnvelopeDto` reads `data.firstOrNull().token` — correct shape, but the
`priority`/`userid` fields are not surfaced. Recommend adding `priority: Int?` and
`userid: Int?` to the wire DTO and consider exposing `priority` on the domain `AuthResult`
(권限 level — could gate v4 admin actions later). Keep as wire-only for now if domain
exposure not yet wanted.

**JWT lifetime**: **~60h** (实测 by CTO). The previous doc said 24h. Bump
ICD-AuthState's TTL note; refresh-vs-relogin policy unchanged (current
UnsupportedTokenRefresher binding still right — re-login on 401).

**Refresh endpoint**: `POST /authorizations/current` with `Authorization: Bearer <token>`
(swagger confirms). Not currently implemented (UnsupportedTokenRefresher).

**Logout endpoint**: `DELETE /authorizations/current` with `Authorization: Bearer <token>`
(swagger confirms). Not currently implemented.

## (B) ICD-TerminalDto — full 24-field truth (+ envelope-meta)

**Wire fields captured by CTO terzone capture**, in nesting order under each
`zone.terminal[]` element:

```
id, type, taskstate, devicestate, netstate, speechstate, volume, isinstancy,
zone, name, ip, latitude, longitude, isrecord, issponsor, shortcircuit,
lopencircuit, ropencircuit, temperature, humidity, isdecode, isencode,
switchcount
```
Plus envelope-meta echoed on each row: `all, count, start, state`.

Already in TerminalDto v1 (the original 14): id, name, type, ip, taskstate, devicestate,
netstate, speechstate, volume, isinstancy, zone, longitude, latitude, groupid (the last
is NOT in the CTO terzone capture — kept defensively).

**Newly added (PA-14, all wire-side, all Int?)**: isrecord, issponsor, shortcircuit,
lopencircuit, ropencircuit, temperature, humidity, isdecode, isencode, switchcount, all,
count, start, state.

**Domain promotion** (what `Terminal` exposes today):
- id (String), name (String), zoneId (String), status (TerminalStatus), volume (Int?),
  longitude (String?), latitude (String?).

**Recommend deferred-promotion** (DTO-only for now, easy to lift later):
- temperature, humidity → useful if a health-detail screen surfaces.
- isrecord, isencode/isdecode, switchcount → for a future advanced-state view.
- shortcircuit/lopencircuit/ropencircuit → fault indicators; could feed a richer
  TerminalStatus.Fault derivation (currently NOT derived — see TerminalMapper KDoc).

### ★ Doc-level annotation REQUIRED in the ICD (blocks PA-14-bug recurrence)

> **`terminal.zone` field is NOT the zone-membership key.** CTO ground truth shows zone
> "操场" (id=1) carries 4 terminals whose `terminal.zone` values are 0,0,0,8 — none equal
> the parent zone id 1. The field's semantics are not documented; v3 itself does NOT use
> it for grouping (v3 calls `/terminal/zoneterminal/{id}` per zone). The ONLY authoritative
> source for zone membership is the nested `ZoneDto.terminal[]` array on
> `/terminal/terzone`. Any future consumer that groups by `terminal.zone` to derive
> membership is wrong and will reproduce the 2026-05-30 操场 BLOCKER.

## (C) ICD-ZoneDto — nested terminals + envelope-meta

**Wire fields (PA-14 ground truth)**:
```
id (int), datetime (string), name (string), description (string),
terminal (array of TerminalDto — the membership source ★),
all (string — note: string, not int, per CTO capture),
count (int), start (int), state (int),
online?, offline?, busyline? (legacy ZoneModel fields; absent on terzone, kept defensively
in case other endpoints emit them)
```

**Domain `Zone` mapping** (unchanged shape, semantics now correct):
- id (String), name (String), description (String?), terminals (List<Terminal>).

**Authoritative SSOT rule**: Zone view reads ONLY `/terminal/terzone`; each
`ZoneDto.terminal[]` becomes `Zone.terminals` with `Terminal.zoneId = zone.id` (NOT the
wire field). Many-to-many preserved (terminal id=14 nested under 4 zones in CTO capture →
appears in `Zone.terminals` of all 4 domain Zones; flat `observeTerminals()` returns it
4×, the UI dedupes if it wants).

**Do NOT** reconstruct membership from `/terminal/terminalinfo` + any `groupBy`.

## (D) ICD-Endpoints — adds, corrections, do-not-uses

| Verb   | Path                              | Body / Auth                | Notes |
|--------|-----------------------------------|----------------------------|-------|
| POST   | `/authorizations`                 | form `username`+`userpwd`  | login; returns `data: [{token, priority, userid}]` |
| POST   | `/authorizations/current`         | Bearer                     | refresh token (swagger; not yet wired) |
| DELETE | `/authorizations/current`         | Bearer                     | logout / delete token (swagger; not yet wired) |
| GET    | `/server/serverstate`             | Bearer                     | already used (V3ServerStateRepository) |
| GET    | `/terminal/terzone`               | Bearer                     | ★ zones with nested terminals — Zone view's SOLE source |
| GET    | `/terminal/terminalinfo`          | Bearer                     | flat all-terminals; **NOT used by Zone view** (PA-14); other consumers OK |
| POST   | `/terminal/zoneterminal`          | Bearer                     | SET zone↔terminal binding — NOT a GET. `GET /terminal/zoneterminal` returns 405. |
| GET    | `/terminal/zoneterminal/{id}`     | Bearer                     | per-zone terminal list (swagger lists it; v3 ZoneMethod uses it; v4 does NOT need it under PA-14 because terzone already nests) |
| GET    | `/task/sechinfo`                  | Bearer                     | already used (V3TaskRepository) |
| POST   | `/task/sechenableordisable`       | Bearer + form              | already used |
| GET    | `/terminal/mediainfo`             | Bearer                     | already used (V3MediaRepository) |
| GET    | `/terminal/mediafolderinfo`       | Bearer                     | already used |

**Reference**: swagger lives at `.state/api-snapshots/swagger-v3-vendor.json`. Before adding
any future endpoint, check it there first — do not guess.

## How to land (PM)

After Critic PASS:
1. Patch `aeroradio-workflow/references/icd-contracts.md` sections AuthState / TerminalDto
   / ZoneDto / Endpoints with the above. Bump each version.
2. Pin the ★ `terminal.zone is not membership` annotation prominently in TerminalDto and
   ZoneDto sections — that's the most important sentence.
3. Sequence fe's mapper re-touch (zone→blank kept, log empty kept, projectstate-sign
   already in PA-10 deliverable — same family of corrections).
