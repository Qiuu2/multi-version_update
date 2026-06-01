# ICD proposals — PA-15 (2026-05-30, post-fix)

> Producer: data-integration-2. Anchored to:
>  - CTO captures: `sechinfo-cto-capture-2026-05-30.json`,
>    `sechetaskinfo-haiwang-cto-capture-2026-05-30.json`
>  - Vendor swagger: `swagger-v3-vendor.json`
>  - PA-15 root cause: `task-schedule-rootcause.md`
>  - PA-15 fix (built): SchemeDto.kt / SchemeMapper.kt / V3TaskRepository.kt /
>    V3TaskRepositoryTest.kt (16/16 green)
>
> STD-ICD-WRITE: do NOT edit `icd-contracts.md` directly. PM serializes after Critic.

## ★ Top-of-§12 + top-of-§13 pinned annotation

> **`/task/sechinfo` ≠ `/task/sechetaskinfo`** — they use the SAME Java model
> on the v3 server (TaskGuangboModel) and the SAME `{data:[...]}` envelope, but
> they carry DIFFERENT FIELDS PER ROW. /sechinfo is scheme-SUMMARY (one row per
> scheme: sechename, projectstate, taskcount, startdate/enddate, taskstate —
> NO per-task starttime/medianame). /sechetaskinfo (POST body `{name: sechename}`)
> is per-task timeline data (starttime HH:MM:SS, medianame, the per-task
> display name `name` — **NOT `taskname`** ★★★, execmode weekday bitmask, the
> 4 status ints state/taskstate/enablestate/offlinestate). **Always fetch BOTH**;
> use sechinfo for scheme listing and sechetaskinfo for the per-scheme timeline.
> Sister of PA-14 ("data in different place"); here it's "data in a SEPARATE
> response, fetched separately." See [[terminal-zone-field-is-not-membership]].

## (A) ICD-Endpoints additions / corrections

| Verb   | Path                              | Auth   | Notes |
|--------|-----------------------------------|--------|-------|
| GET    | `/task/sechinfo`                  | Bearer | Scheme SUMMARY list. ★ NOT a timeline source — `taskcount` is integer count, NOT array. |
| POST   | `/task/sechetaskinfo`             | Bearer | body `{name: sechename}` form-urlencoded → per-task timeline for one scheme. ★ AUTHORITATIVE timeline source. |
| POST   | `/task/sechetask`                 | Bearer | CRUD: create scheme task (body `sechetaskinfo` def). Future increment. |
| PUT    | `/task/sechetask`                 | Bearer | CRUD: update scheme task. Future increment. |
| DELETE | `/task/sechetask`                 | Bearer | CRUD: delete (body `singleid`). Future increment. |
| POST   | `/task/sechenableordisable`       | Bearer | body `{sechename, state}` — 0=enable, 1=disable. Already wired. |

(All other /task/* paths from the swagger sweep are out of MVP scope; full
inventory remains in `swagger-v3-vendor.json` and `task-schedule-rootcause.md` §1.)

## (B) ICD-SchemeDto-v2 — TWO DTO shapes (was: one)

### v1 → v2 breaking shape clarification

PA-10's single `SchemeRowDto` conflated the two endpoint shapes. v2 splits:

- **SchemeEnvelopeDto / SchemeRowDto** — scheme summary from `/task/sechinfo`.
- **SchemeTaskEnvelopeDto / SchemeTaskRowDto** — per-task timeline from
  `/task/sechetaskinfo`.

### Field set (`SchemeRowDto` — `/task/sechinfo` summary row)

Per CTO 2026-05-30 capture; all nullable (R-003):

| @SerializedName | Kotlin field | type | meaning |
|---|---|---|---|
| taskid | taskId | Int? | scheme's own taskid (namespace overlaps with per-task ids by coincidence; NOT for FK) |
| taskstate | taskState | Int? | scheme task-state aggregate |
| taskcount | taskCount | Int? | INTEGER count of tasks under this scheme (★ NOT an array) |
| startdate | startDate | String? | scheme validity start (YYYY-MM-DD) |
| enddate | endDate | String? | scheme validity end |
| sechename | schemeName | String? | scheme identity (the domain Scheme.id + name) |
| projectstate | projectState | Int? | 0=RUNNING ★ counter-intuitive sign, 1=stopped |
| all / count / start / state | … | Int? | envelope-meta echoed on each row |
| projectstatetate | projectStateTate | String? | defensive: TaskZuoxiModel:40 typo field; not authoritative |

### Field set (`SchemeTaskRowDto` — `/task/sechetaskinfo` per-task row)

Full 23 wire fields per CTO `field_inventory_full_dto_coverage_required`; all
nullable (R-003):

| @SerializedName | Kotlin field | meaning |
|---|---|---|
| taskid | taskId: Int? | REAL per-task id (e.g. 73657) |
| **name** ★★★ | **name: String?** | **PER-TASK DISPLAY NAME (NOT `taskname`)** |
| info | info: String? | redundant copy of parent sechename (back-pointer, NOT FK) |
| starttime | startTime: String? | HH:MM:SS time-of-day |
| startdate / enddate | startDate / endDate: String? | per-task validity window |
| execmode | execMode: Int? | weekday bitmask (62 = 0b111110 = Mon-Fri); MVP-deferred UI |
| lengthtype / length | lengthType / length: Int? | playback length config |
| mediaid / medianame | mediaId / mediaName: Int? / String? | media reference |
| state / taskstate / enablestate / offlinestate | … : Int? | 4 status fields; collapsed to Domain.SchemeTaskStatus by SchemeMapper.deriveTaskStatus |
| volume / priority / prepower / israndomplay / datasendmodel | … : Int? | playback config; MVP-deferred (kept in DTO for ICD completeness) |
| all / count / start | … : Int? | envelope-meta |

## (C) ICD-TaskRepository-v1 §13 — refresh contract update

Replace the PA-10 single-fetch description with:

> **refresh()** — TWO-step:
> 1. `GET /task/sechinfo` → enumerate schemes via [SchemeRowDto.toSchemeSummary]
>    (one Scheme skeleton per row, with `active = (projectstate == 0)`).
> 2. For each scheme name, `POST /task/sechetaskinfo` body `{name: sechename}`
>    in PARALLEL (appScope.async + awaitAll under coroutineScope). Each per-task
>    response maps via [SchemeTaskRowDto.toSchemeTask], threading the parent
>    scheme's `active` flag into status derivation.
> 3. **Atomic publish**: if ALL per-scheme task fetches succeed, publish the full
>    snapshot. If ANY per-scheme fetch fails (or any parse throws), the entire
>    publish is skipped and the PRIOR snapshot is retained (same SSOT contract
>    as V3TerminalRepository — Critic AC).
> 4. Concurrent refresh() shares ONE in-flight fetch (Mutex + Deferred); a
>    finished refresh clears the slot.

PA-10's single-fetch (sechinfo only) is REMOVED — it returned scheme-summary
rows and rendered the timeline blank (PA-15 root cause).

### §13 status-derivation rule update (documented-assumption, pinned)

> [deriveTaskStatus] takes the parent scheme's `active` flag + the 4 per-task
> status ints (state / taskstate / enablestate / offlinestate). Rules (top→bottom,
> first match wins):
>
> 1. scheme NOT active → **Disabled** (per-task row under a stopped scheme).
> 2. enableState == 0 → **Disabled** (per-task disable flag; analogue
>    /task/taskdoorno "0=enable,1=disable").
> 3. taskState != 0 → **Running** (any non-zero per-task running signal).
> 4. all four ints null → **Unknown("no-state…")** (defensive, R-003).
> 5. otherwise → **Idle** (scheduled but not firing now — CTO capture's normal).
>
> A single CTO capture cannot fully enumerate the value set; this is a
> documented-assumption. When the real value set is confirmed, only this
> function changes; the sealed [SchemeTaskStatus] type + fe boundary stay.

### Log endpoint (PA-15 confirms PA-10 assumption)

> getExecutionLog() returns empty (documented-assumption). The swagger sweep
> (PA-15 §1) confirms NO /task/log /task/journal /task/history endpoint exists.
> If a log source ever surfaces → ICD_UPDATE (signature stays).

### Multi-active note (NEW)

> MULTIPLE schemes can have projectState=0 simultaneously (CTO 2026-05-30
> capture: 海王作息 AND 日本作息 both active). Repository SSOT carries all
> schemes with per-scheme `active`; TaskHomeViewModel.activeScheme picks
> first-active wins (Option A). **documented-assumption**: fe may upgrade to
> Option B (expose full active list) or C (merge all active timelines); the
> data layer SSOT already carries everything needed for either, no repo change
> required.

## How to land (PM, after Critic PASS)

1. Patch `aeroradio-workflow/references/icd-contracts.md`:
   - §12 SchemeDto: replace single-row description with TWO-shape (SchemeRowDto +
     SchemeTaskRowDto); add the ★ pinned annotation at section top.
   - §13 TaskRepository: replace refresh() with TWO-step contract; pin status
     derivation rules; pin multi-active documented-assumption.
   - Endpoints: add POST /task/sechetaskinfo entry; mark /task/sechinfo as
     "scheme-SUMMARY, NOT timeline source".
   - Cross-link to memory `[[terminal-zone-field-is-not-membership]]` family.
2. Bump SchemeDto ICD version v1 → v2 (DTO shape change, fe consumers UNCHANGED
   — they consume domain Scheme/SchemeTask which kept the same shape).
3. Sequence: no fe re-touch required (zone→blank still right, log empty still
   right, projectstate-sign already pinned in PA-10 and still right; only the
   data layer changed).
