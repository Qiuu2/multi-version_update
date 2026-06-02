# Task Tab — scheme timeline blank — root cause (PA-15)

> Producer: data-integration-2 · 2026-05-30 · L2 code audit (read-only, no edits)
> Symptom (CTO demo): Task Tab Hero shows the active scheme "海王作息" but the
> timeline below renders no usable rows.
> Sister to PA-14 (RTM-ERR-005): wire shape misread; the symptom is "container
> resolves, contained items missing/wrong" — same family, different endpoint.

---

## One-line verdict

**Hit (a) — wire bug. `V3TaskRepository.refresh()` only calls `GET /task/sechinfo`,
which is the scheme-SUMMARY endpoint (one row per scheme; carries `taskcount` as an
integer count, NOT a task array; per-task fields like real `starttime`, real
`medianame`, real per-task `taskid` are not in the response).** The per-scheme
task list — the timeline — must come from a SEPARATE `POST /task/sechetaskinfo`
body `{name: <sechename>}`, one POST per scheme (the demoably-working v3 path
in `TaskZuoxiListActivity.getDataFormSever`). PA-10 wired only the summary call
and treated the summary rows as if they were timeline tasks; the fields the
timeline needs (`starttime`, `medianame`) come back null/blank → the UI renders
rows with empty time/title.

Recommended fix path (NOT for this audit — for the next dispatch):
`V3TaskRepository.fetchAndPublish` should
(1) GET `/task/sechinfo` → list of schemes with their summary + projectstate;
(2) for each scheme name, POST `/task/sechetaskinfo` with body `{"name": sechename}`
in parallel → real per-task rows; (3) attach those tasks to the corresponding
`Scheme.tasks`. The SchemeRowDto field set already matches /sechetaskinfo's row
shape (TaskGuangboModel — same model class v3 deserializes into in both contexts),
so no DTO change required for the per-task data. The Scheme domain shape stays.

Confidence the root cause is (a): **HIGH** (swagger + v3 ground-truth path
TaskZuoxiListActivity + v3 ground-truth path TaskZuoxiActivity all converge
on the two-endpoint pattern). One curl request can close it definitively
(§5 below).

---

## §1 — Endpoint selection reconciliation

### What V3TaskRepository calls today

| Call | Endpoint | File:line |
|---|---|---|
| LIST | `GET /task/sechinfo` | `V3TaskRepository.kt:95` (`adapter.get(url(PATH_SCHEMES))`) + `:159` (`PATH_SCHEMES = "/task/sechinfo"`) |
| ENABLE/DISABLE | `POST /task/sechenableordisable` | `V3TaskRepository.kt:123` + `:160` (`PATH_ENABLE_DISABLE`) |

NO per-scheme task-list call. NO follow-up POST. The timeline is sourced
exclusively from /sechinfo's row shape.

### Swagger candidates for /task/* (verbatim from swagger-v3-vendor.json)

I enumerated every /task/* path. The relevant subset for "scheme + its tasks":

| Method | Path | Description (from swagger) | Body |
|---|---|---|---|
| **GET** | **/task/sechinfo** | 获得方案信息. taskid=(任务id), taskstate=(任务状态), **taskcount=(任务数)**, startdate, enddate, sechename, projectstate=(0启用，1停用) | none |
| POST | /task/sechinfo | 复制方案 | `copysche` |
| PUT | /task/sechinfo | 更新方案名称 | `copysche` |
| DELETE | /task/sechinfo | 删除方案 | `singlename` |
| GET | /task/sechinfoall | 获得方案信息 (richer/all variant) | none |
| **POST** | **/task/sechetaskinfo** | **获得方案任务信息** | `singlename` = `{name: string}` |
| POST | /task/sechetask | 设置方案任务 (CREATE) | `sechetaskinfo` |
| PUT | /task/sechetask | 更新方案任务 | `sechetaskinfo` |
| DELETE | /task/sechetask | 删除方案任务 | `singleid` |
| POST | /task/sechenableordisable | 启用或停用方案. 0启用, 1停用 | `operateinfo` `{sechename, state}` |
| GET | /task/taskinfo/{id} | 获得任务信息 (id 1=作息 2=文件 3=采播 ...) | path |
| GET | /task/taskinfotwo/{id} | 获得任务信息 (同上, 不含某些类型) | path |

Two critical signals from the swagger:

1. **`sechinfo` description literally says `taskcount=(任务数)`** — an integer count, not a list. The same description also lists `taskid` as a singular id (likely a representative, not an array). This is a scheme-summary endpoint.
2. **`sechetaskinfo` (POST + `singlename` body)** is named "获得方案任务信息" — "get a scheme's task info". The body schema `singlename` is `{name: string}` — pass the scheme name. The response is implied by the request-body schema for the matching write endpoint (`sechetask` POST) which uses `sechetaskinfo` definition: 25 fields — `taskid, prepower, level, volume, priority, datasendmodel, startdate, enddate, execmode, tasktype, taskname, starttime, timelength, timelengthtype, israndomplay, medianame, sechename, cmd, cmdargs, bandrate, liveterminalid, liveterminalname, samplerate, caiboprepower, mediaid`. Same shape as v3 TaskGuangboModel (clean projectstate — the PA-10 sister model).

### Old-stack ground-truth path (v3 demoably works)

The v3 Task Tab has TWO screens that together render scheme + tasks:

- **`TaskZuoxiActivity`** (scheme-list screen):
  - File: `app/.../activity/TaskZuoxiActivity.java:288`
  - Call: `RequestManger.getInstance().get(Constant.serveraddress + Constant.getsecheList, ...)` → `GET /task/sechinfo` (`Constant.java:38` `getsecheList = "/task/sechinfo"`).
  - Renders: `holder.setText(R.id.sche_name, model.getSechename())` (:144); `R.id.task_start_time, model.getStartdate()` (:145); `R.id.task_end_time, model.getEnddate()` (:146); `R.id.task_order, model.getTaskcount() + ""` (:147). NOTE: `startdate/enddate` are SCHEME dates (validity range), not `starttime` (time-of-day) — and `taskcount` is what it renders for "how many tasks", confirming it's the count integer.
  - It does NOT render per-task `starttime`, `medianame`, or `taskname`. The /sechinfo response simply doesn't drive a timeline at v3 — it drives a scheme LIST.

- **`TaskZuoxiListActivity`** (scheme-DETAIL screen — opened by tapping a scheme):
  - File: `app/.../activity/TaskZuoxiListActivity.java:256-285` (`getDataFormSever`):
    ```java
    MyRequestBuilder request = new MyRequestBuilder(mContexts);
    request.setNeedToken(true);
    request.setUrl(Constant.postSchemeInfo);   // = "/task/sechetaskinfo" (Constant.java:59)
    request.setBodyMap(new HashMap<String, String>() {{
        put("name", taskZuoxiModel.getSechename());   // singlename body
    }});
    RequestManger.getInstance().postHashMap(request, new onRequestLister() {
        @Override public void onSucess(int code, String response) {
            TaskGuangboListRsp reponseData = JsonUtil.deSerializeString(response, TaskGuangboListRsp.class);
            // ... iterates reponseData.getData() as per-task rows
        }
    });
    ```
  - Renders per row: `R.id.name, model.getName()` (:141); `R.id.task_start_time, model.getStartdate()` (:142); `R.id.task_end_time, model.getEnddate()` (:143); `R.id.starttime_inhour, model.getStarttime()` (:144); `R.id.timelength_type, TaskMainMethod.setWeekDay(model.getExecmode())` (:145); `R.id.task_music, model.getMedianame()` (:153). **This IS the per-task timeline** — it renders the real `starttime` (time-of-day), the real per-task `name`, the real `medianame`.

Constants confirming:
- `Constant.java:59` `public static final String postSchemeInfo = "/task/sechetaskinfo";`
- `Constant.java:79` `public static final String postTaskListInfo = "/task/sechetaskinfo";` (alias; same endpoint)

### Conclusion §1

v3 uses BOTH endpoints in series: GET /sechinfo to list schemes, then per-tap
POST /sechetaskinfo to populate that scheme's timeline. **PA-10 only wired the
first**. That is the wire-side bug. /task/sechetaskinfo is the correct endpoint
for the timeline.

---

## §2 — DTO field reconciliation (Leg-4 data table)

### Current new-stack DTOs (V3TaskRepository's read path)

`SchemeEnvelopeDto` + `SchemeRowDto` at `app/src/main/java/.../data/dto/SchemeDto.kt`:

| @SerializedName | DTO Kotlin field | File:line |
|---|---|---|
| `data` | `data: List<SchemeRowDto>?` | SchemeDto.kt:24 |
| `taskid` | `taskId: String?` | :29 |
| `taskname` | `taskName: String?` | :30 |
| `sechename` | `schemeName: String?` | :31 |
| `starttime` | `startTime: String?` | :32 |
| `medianame` | `mediaName: String?` | :33 |
| `volume` | `volume: Int?` | :34 |
| `taskstate` | `taskState: Int?` | :35 |
| `projectstate` | `projectState: Int?` | :36 |
| `enablestate` | `enableState: Int?` | :37 |
| `projectstatetate` | `projectStateTate: String?` | :40 (defensive — wrong-model typo, kept verbatim) |

### Wire row shape per endpoint (from swagger + v3 code)

**`/task/sechinfo`** rows (per swagger description + v3 TaskZuoxiActivity render):

| wire field | type | meaning (swagger) | rendered in v3 sechinfo screen? |
|---|---|---|---|
| `taskid` | string | (任务id) — likely scheme's representative task id, not the array | NO (rendered for click-through after) |
| `taskstate` | int | (任务状态 0-准备 1-执行 2-暂停 3-立即执行) | NO |
| `taskcount` | int | **(任务数)** — the number of tasks in this scheme | **YES** (`R.id.task_order`) |
| `startdate` | string | (开始日期) — SCHEME validity start | YES (`R.id.task_start_time`) |
| `enddate` | string | (结束日期) — SCHEME validity end | YES (`R.id.task_end_time`) |
| `sechename` | string | (方案名称) | YES (`R.id.sche_name`) |
| `projectstate` | int | (0启用，1停用) | YES (switch state) |
| `starttime` | (absent) | — | NO (no field rendered) |
| `medianame` | (absent) | — | NO |
| `taskname` | (likely absent / scheme representative) | — | NO |

**`/task/sechetaskinfo`** rows (per swagger `sechetaskinfo` definition + v3 TaskZuoxiListActivity render):

| wire field | type (swagger) | rendered in v3 sechetaskinfo screen? |
|---|---|---|
| `taskid` | string | NO (used for delete/edit click target via taskDataList:233) |
| `taskname` | string | NO directly; `name` is rendered (see below) |
| `name` | string | YES (`R.id.name`) — per-task name |
| `startdate` | string-date | YES (`R.id.task_start_time`) — per-task validity start |
| `enddate` | string-date | YES (`R.id.task_end_time`) |
| `starttime` | string (default "11:20:20") | YES (`R.id.starttime_inhour`) — per-task time-of-day ★ THE TIMELINE TIME |
| `execmode` | int (weekday mask) | YES (rendered via setWeekDay()) |
| `medianame` | string | YES (`R.id.task_music`) ★ THE TIMELINE MEDIA |
| `sechename` | string | NO (set by caller / hidden) |
| `volume` | int | NO (in detail) |
| `priority/level/prepower/mediaid/timelength/timelengthtype/israndomplay/cmd/cmdargs/bandrate/liveterminalid/liveterminalname/samplerate/caiboprepower/datasendmodel/tasktype` | various | NO at list screen — likely used at detail/edit |

★ The two render-blocking fields the v4 timeline needs are `starttime` and `medianame` — both are populated by /sechetaskinfo and absent from /sechinfo.

### Per-cell reconciliation (PA-14 Leg-4 format)

| wire (where it lives) | DTO @SerializedName (file:line) | Domain field (file:line) | UI consumer (file:line) | status |
|---|---|---|---|---|
| sechinfo.sechename | `sechename` → schemeName SchemeDto.kt:31 | Scheme.id (=name) SchemeMapper.kt:41-42 | scheme.name TaskScreen.kt:213 | OK |
| sechinfo.projectstate | `projectstate` → projectState SchemeDto.kt:36 | Scheme.active SchemeMapper.kt:43,59 | scheme (no direct render — only active filter via TaskHomeViewModel.activeScheme:89-90) | OK |
| sechinfo.taskcount | (NOT captured on DTO) | (no domain mapping) | scheme.tasks.size TaskScreen.kt:213 | ★ NOT WIRED — taskcount field would tell us the count even without /sechetaskinfo; we throw it away |
| **sechetaskinfo.starttime** (per-task time-of-day) | **NOT FETCHED** (V3TaskRepository never calls /sechetaskinfo) | SchemeTask.startTime SchemeMapper.kt:78 (sources from sechinfo's `starttime` which is absent) | TaskItem.time TaskUiMappers.kt:40 → TimelineRow Text(task.time) TaskScreen.kt:288 | ★★★ BROKEN — empty string downstream |
| **sechetaskinfo.medianame** | **NOT FETCHED** | SchemeTask.mediaName SchemeMapper.kt:79 (from sechinfo's `medianame` which is absent) | TaskItem.title — actually mapped from `name` not mediaName! TaskUiMappers.kt:41 maps name | ★★ partially broken — title sources from SchemeTask.name which derives from sechinfo's `taskname` which may also be summary not real |
| **sechetaskinfo.name** (per-task name) | NOT FETCHED (DTO has `taskname` not `name`) | — | — | ★★ POTENTIAL FIELD-NAME MISMATCH: swagger sechetaskinfo definition uses `taskname`, but TaskZuoxiListActivity renders `R.id.name, model.getName()` — model.getName() on TaskGuangboModel reads the `name` field. So the actual wire likely has BOTH `name` and `taskname` (TaskGuangboModel has both: `public String taskname` line 22 AND `public String name` line 63). Need curl to confirm which one carries the per-task display name on real sechetaskinfo responses. |

Empty cells in the table = broken trace. There are 3 stars: starttime (the time-of-day), medianame (the media file), and the `name` vs `taskname` ambiguity for per-task display name.

---

## §3 — Data-flow trace (server → screen)

Step-by-step with file:line; the break point is at step 1.

1. **Network fetch**: `V3TaskRepository.fetchAndPublish` at `V3TaskRepository.kt:93-99`:
   ```kotlin
   val json = adapter.get(url(PATH_SCHEMES)).getOrThrow()    // GET /task/sechinfo
   val rows = gson.fromJson(json, SchemeEnvelopeDto::class.java)?.data.orEmpty()
   schemesFlow.value = rows.groupRowsIntoSchemes()
   ```
   **★ Break point**: only /sechinfo is called. The timeline data is never fetched.

2. **Group rows into schemes**: `SchemeMapper.groupRowsIntoSchemes()` at `SchemeMapper.kt:32-47`:
   - Groups SchemeRowDto rows by `schemeName` (sechename).
   - For each scheme, `tasks = rows.map { it.toSchemeTask(projectState) }` — every /sechinfo row for this sechename becomes one SchemeTask. **Each SchemeTask's startTime/mediaName comes from THAT ROW's `starttime`/`medianame` wire fields — which are NOT in the /sechinfo response.** Result: SchemeTask objects with non-null `name` (the row's `taskname` field, if present) but null `startTime`/`mediaName`.

3. **SSOT publish**: `schemesFlow` (`V3TaskRepository.kt:64,99`) emits the new list.

4. **VM combine**: `TaskHomeViewModel.uiState` at `TaskHomeViewModel.kt:58-65`:
   ```kotlin
   combine(repository.observeSchemes(), refreshResult) { schemes, refresh ->
       deriveState(schemes, refresh)
   }
   ```
   `deriveState` at `:92-104`: picks active scheme (first `active=true` else first scheme — line 89-90), wraps as `TaskHomeUiState.Success(scheme.toSchemeUi())`. If no scheme, depends on refresh result: null → Loading, failure → Error, else Empty.

5. **Domain → UI mapping**: `TaskUiMappers.Scheme.toSchemeUi()` at `TaskUiMappers.kt:48-53`:
   ```kotlin
   tasks = tasks.map { it.toTaskItem() }
   ```
   `SchemeTask.toTaskItem()` at `:38-46`:
   ```kotlin
   TaskItem(
       id = id,
       time = startTime.orEmpty(),   // <-- null/blank from step 2
       title = name,                  // <-- name from SchemeTask, derived from sechinfo's taskname
       zone = "",                     // deliberately blank (PA-10 pinned: no zone on wire)
       state = status.toCardState(),
   )
   ```

6. **Screen render**: `TaskScreen.SchemeHome` at `:201`:
   - Hero kicker: `"当前作息方案 · ${scheme.tasks.size} 项任务"` (:213). The count is the number of rows /sechinfo returned for the active scheme.
   - Empty-state branch: `if (scheme.tasks.isEmpty()) { EmptyState(...) }` (:222-231).
   - Timeline rendering at `:233-244`:
     ```kotlin
     listOf("上午", "下午", "晚上").forEach { period ->
         val inPeriod = scheme.tasks.filter { periodOf(it.time) == period }
         ...
         items(inPeriod, key = { it.id }) { task -> TimelineRow(task = task) }
     }
     ```
     `periodOf` at `:397-403`: empty string → toIntOrNull() returns null → 0 → hour < 12 → **"上午"**. So all tasks with empty time fall into the 上午 bucket. They DO render, but each TimelineRow shows blank `task.time` (empty Text), blank `task.title` (if name is null/blank), blank `task.zone` (deliberately).

### Where the visual symptom comes from

If /sechinfo returns 1 row per scheme (most likely for a summary endpoint), `scheme.tasks.size = 1`. The Hero shows "...· 1 项任务", and ONE TimelineRow renders in 上午 with empty content (empty time, possibly empty title, blank zone). Visually this looks "empty" — the user reads it as no real tasks.

If /sechinfo returns N rows per scheme (less likely, but TaskGuangboModel is the same model used by /sechetaskinfo so the server CAN return N rows), then `scheme.tasks.size = N`, the Hero shows the right count, and N empty rows render. Still visually blank because the per-task fields are summary-shaped, not task-shaped.

**Either way the fix is the same**: fetch /sechetaskinfo per scheme to populate real per-task fields.

---

## §4 — Root cause hypothesis (3 candidates with probability + evidence)

### (a) Wire bug — V3TaskRepository missing /sechetaskinfo fetch

**P ≈ 0.85** (HIGH).

Evidence:
- Swagger description on /sechinfo says `taskcount=(任务数)` — an integer count, not an array. Counter to PA-10's mental model where I treated /sechinfo rows as the tasks.
- v3 ground-truth pattern: `TaskZuoxiListActivity:259-274` POSTs /sechetaskinfo body `{name: sechename}` to get the per-task list. This is the demonstrably-working path for the timeline.
- v3 list screen `TaskZuoxiActivity` renders SCHEME-LEVEL fields only from /sechinfo (sechename, startdate, enddate, taskcount, projectstate) — no `starttime`, `medianame`, or per-task `name`. v3 itself acknowledges /sechinfo doesn't have per-task data.
- TaskGuangboModel is REUSED across both endpoints, which is what fooled PA-10 — same Java class, different field-population per endpoint. /sechinfo populates the scheme-summary subset; /sechetaskinfo populates the per-task subset. Same DTO works for both; the bug is the missing endpoint call.

What this DOESN'T explain on its own: if /sechinfo returns 1 row per scheme, why doesn't the Hero kicker just say "1 项任务" (visible to the user)? Probably it does, and CTO's "blank timeline" description is shorthand for "Hero shows N=1 with empty content rows". OR the server returns 0 rows under the active scheme in /sechinfo's response (some servers don't return rows for an active-but-no-tasks scheme). Curl will settle this.

### (b) Mapper bug — DTO/domain field-name mismatch

**P ≈ 0.10** (LOW).

Evidence FOR: the v3 TaskGuangboModel has TWO name-ish fields (`public String taskname` line 22 AND `public String name` line 63) and v3 TaskZuoxiListActivity renders `model.getName()` not `model.getTaskname()` at the list screen. My SchemeRowDto maps `@SerializedName("taskname")` to `taskName` — if the real per-task wire emits `name` (not `taskname`) for the display name, my DTO would miss it. This would matter as soon as we add /sechetaskinfo.

Evidence AGAINST: this is conditional on candidate (a) being fixed first. Even with field-name mismatch, /sechinfo is the wrong endpoint regardless, so (a) is the primary fault.

Action: when (a) is fixed and we issue /sechetaskinfo, the curl will tell us whether the per-task display name comes back as `name` or `taskname`. Add `@SerializedName("name")` defensively.

### (c) VM bug — wrong active-scheme pick

**P ≈ 0.03** (very LOW).

Evidence: `TaskHomeViewModel.activeScheme:89-90` picks `schemes.firstOrNull { it.active } ?: schemes.firstOrNull()`. If MULTIPLE schemes are active simultaneously (which TaskZuoxiActivity:301-348 shows is possible — the v3 grouping/dedupe logic deals with "same sechename appearing multiple times"), we pick the first by insertion order, which may not be "海王作息". But the Hero is clearly showing "海王作息" per CTO, so this picker is fine.

(c.2) — partial: if SchemeMapper.groupRowsIntoSchemes() puts ALL /sechinfo rows under each scheme via the `for (row in this) { ... byScheme.getOrPut(name) { ... }.add(row) }` loop — looking again, the loop is correct: each row goes to ONE bucket keyed by its sechename. Not (c.2).

### Combined probability summary

Most likely: pure (a). Possibly (a) + (b) once (a) is fixed and we read the real
sechetaskinfo response. (c) is essentially ruled out by CTO's observation.

---

## §5 — Action recommendation

### Curl IS required, but only ONE call to close (a) definitively

The fix path (call /sechetaskinfo per scheme) is well-supported by code evidence
alone. But the per-task wire response shape (whether name comes back as `name`
or `taskname`, whether there are extra fields we'd want, whether the envelope
is `{data:[...]}` like every other endpoint) is the kind of "field reality vs
field hope" thing PA-14 burned us on. One curl to /sechetaskinfo + paste here
= zero guesswork in the eventual fix.

### Curl command (for CTO / PM to run, replace `$TOKEN` and `$SCHEME`):

```bash
# Required: SCHEME = a real scheme name from /sechinfo. From CTO demo, try "海王作息".
TOKEN="<bearer token from CTO's prior session>"
SCHEME="海王作息"

# Step 1 — confirm /sechinfo shape (settles (a) on its own):
curl -sS -H "Authorization: Bearer $TOKEN" \
  "http://183.216.51.204:99/api/task/sechinfo" \
  | python3 -m json.tool > /tmp/sechinfo.json
echo "===== sechinfo response (first 50 lines):"
head -50 /tmp/sechinfo.json

# Step 2 — fetch the per-scheme tasks via the v3-ground-truth endpoint:
curl -sS -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "name=$SCHEME" \
  "http://183.216.51.204:99/api/task/sechetaskinfo" \
  | python3 -m json.tool > /tmp/sechetaskinfo.json
echo "===== sechetaskinfo response for '$SCHEME' (first 100 lines):"
head -100 /tmp/sechetaskinfo.json
```

### What to look for in the responses

**In `/tmp/sechinfo.json`** (settles candidate (a)):
- Is it `{"data": [...]}` envelope? (Should be, every other v3 endpoint is.)
- How many rows for sechename="海王作息"? **If 1 → confirms /sechinfo is one-row-per-scheme summary**; if N → /sechinfo can return N rows but per-task fields will still be summary-shape (no starttime/medianame).
- Does each row have a non-null `starttime` (time-of-day) and `medianame`? **If null/absent → confirms /sechinfo doesn't carry timeline data, (a) is proven.**
- What's the `taskcount` value for "海王作息"? Tells us how many tasks /sechetaskinfo SHOULD return.

**In `/tmp/sechetaskinfo.json`** (settles candidate (b) — DTO names):
- Envelope: `{"data": [...]}` ?
- Does each row have `starttime` populated with a time-of-day like "06:30" ?
- Does the display name come back as `name` or `taskname` (or both)? → answers (b).
- Are there fields we don't have in SchemeRowDto we should capture? (compare against the 25 fields swagger lists in the `sechetaskinfo` write-schema).

### If CTO doesn't have time to curl

Code-side decision rule: still proceed with the fix as designed in §1
"Recommended fix path" (refresh = sechinfo enum → per-scheme sechetaskinfo POST).
Add `@SerializedName("name")` defensively (treating it as an alternate display
name field), and the SchemeRowDto field list otherwise unchanged. Worst case
without curl: per-task display name is wrong → fe shows blank titles still.
But the timeline TIME (`starttime`) and MEDIA (`medianame`) WILL populate
correctly, which is 80% of the visible fix.

### NO code changes in this audit

Per dispatch: read-only. No edits, no patch, no git, no build slot consumed.

### ICD bumps proposed (separate file)

The ICD updates (ICD-Endpoints + ICD-TaskRepository §13 + ICD-SchemeDto §12)
are written to `.state/api-snapshots/icd-proposals-pa15.md` — STD-ICD-WRITE,
PM serializes after Critic.

---

## Cross-references

- Sister bug pattern: PA-14 `[[terminal-zone-field-is-not-membership]]` — wire
  shape misread; data was in a different place than I expected. Same family:
  "endpoint name suggests one thing, wire reality is another."
- Sister fact: PA-10 `[[scheme-dto-misspelled-field]]` — already corrected
  TaskZuoxiModel→TaskGuangboModel as the /sechinfo wire model. That correction
  STILL HOLDS; nothing about the model class is wrong, only the endpoint
  selection. Same Java class (`TaskGuangboModel`) is used by both /sechinfo
  (scheme summary mode) and /sechetaskinfo (per-task timeline mode) — the v3
  team reused the model class across two contexts, which is exactly the kind of
  trap PA-15 hit.
- R-ADDR-SLOT: not in scope; V3TaskRepository uses ServerConfig.baseUrl() +
  MyRequestBuilder's two-arg `setUrl(url, "")` overload to avoid the
  PreferencesUtil fallback, same pattern from PA-10. Dormant.

---

## §6 — Post-fix reconciliation table (PA-15 build dispatch)

> Appended after the fix landed (V3TaskRepository two-step refresh + SchemeDto v2
> split + SchemeMapper rewrite + 16/16 green tests on CTO ground-truth fixtures
> + assembleDebug → fresh APK 2026-05-30 16:57).
>
> Below: the canonical wire→DTO→domain→UI table for the TIMELINE path, and the
> 5 capture findings with their final disposition.

### Reconciliation table (full pipeline, file:line)

| wire field (capture) | endpoint | DTO @SerializedName (file:line) | Domain field (file:line) | UI consumer (file:line) | status |
|---|---|---|---|---|---|
| `sechename` | /sechinfo | `schemeName` SchemeDto.kt:75 | `Scheme.id` + `Scheme.name` SchemeMapper.kt:55-59 | scheme.name TaskScreen.kt:213 | ✓ |
| `projectstate` (0=running) | /sechinfo | `projectState` SchemeDto.kt:79 | `Scheme.active` SchemeMapper.kt:58 (via isSchemeActive:30) | active filter TaskHomeViewModel.kt:89-90 | ✓ multi-active aware |
| `taskcount` (int count) | /sechinfo | `taskCount` SchemeDto.kt:73 | (not surfaced — sanity check vs tasks.size) | (test-only sanity) | ✓ kept on DTO |
| `name` ★★★ | /sechetaskinfo | `name` SchemeDto.kt:127 | `SchemeTask.name` SchemeMapper.kt:85 | `TaskItem.title` TaskUiMappers.kt:41 → TaskCard title | ✓ defensive @SerializedName("name"), NOT taskname |
| `starttime` ★★★ HH:MM:SS | /sechetaskinfo | `startTime` SchemeDto.kt:130 | `SchemeTask.startTime` SchemeMapper.kt:88 | `TaskItem.time` TaskUiMappers.kt:40 → TimelineRow Text TaskScreen.kt:288 | ✓ populated |
| `medianame` ★★★ | /sechetaskinfo | `mediaName` SchemeDto.kt:140 | `SchemeTask.mediaName` SchemeMapper.kt:89 | (not surfaced today — TaskItem maps title from name; medianame kept on domain for future) | ✓ populated, MVP-deferred render |
| `taskid` (per-task) | /sechetaskinfo | `taskId` SchemeDto.kt:125 | `SchemeTask.id` SchemeMapper.kt:84 | `TaskItem.id` (lazy key) | ✓ |
| `state/taskstate/enablestate/offlinestate` (4 ints) | /sechetaskinfo | `state/taskState/enableState/offlineState` SchemeDto.kt:142-145 | `SchemeTask.status` (via deriveTaskStatus) SchemeMapper.kt:86 | `TaskItem.state` via toCardState() TaskUiMappers.kt:43 | ✓ Unknown-tolerant rule documented |
| `info` (back-pointer) | /sechetaskinfo | `info` SchemeDto.kt:128 | (NOT domain) | (NOT UI) | ✓ kept on DTO only; not used (NOT an FK) |
| `execmode` (weekday bitmask) | /sechetaskinfo | `execMode` SchemeDto.kt:131 | (NOT domain yet) | (NOT UI yet) | ✓ DTO-only; future "Mon-Fri" render TODO |
| `volume / priority / prepower / length / lengthtype / mediaid / israndomplay / datasendmodel` | /sechetaskinfo | … SchemeDto.kt:146-150 | volume → SchemeTask.volume; rest DTO-only | TaskCard.volume label (volume only) | ✓ DTO complete |
| `startdate / enddate` (per-task) | /sechetaskinfo | `startDate / endDate` SchemeDto.kt:133-134 | (NOT domain) | (NOT UI) | ✓ DTO-only; task-level validity, future |
| `all / count / start / state` (envelope-meta) | both | … | (NOT domain) | (NOT UI) | ✓ DTO-only |

No empty cells = no trace break. fe TaskUiMappers + TaskScreen unchanged.

### 5 capture findings — final disposition

| # | Finding | Disposition |
|---|---|---|
| 1 | `name` is per-task display name, NOT `taskname` | `@SerializedName("name")` on SchemeTaskRowDto.name (SchemeDto.kt:127). Test asserts named tasks ("早读开始铃" etc) — load-bearing regression guard. |
| 2 | Multi-active (海王 + 日本 both projectstate=0) | Per-scheme `active` flag preserved. TaskHomeViewModel picks first-active (Option A); documented-assumption fe can upgrade. Test `refresh_success_multiActive_bothHaiwangAndRibenAreActive` asserts both. |
| 3 | 4 status fields (state / taskstate / enablestate / offlinestate) | `deriveTaskStatus(schemeIsActive, state, taskState, enableState, offlineState)` SchemeMapper.kt:121 with documented-assumption rules; Unknown fallback (R-003). Test `derivedStatus_haiwangActiveTasks_areIdle_underActiveScheme` asserts CTO-capture rows derive to Idle. |
| 4 | `taskcount=14` matches sechetaskinfo row count | Test `refresh_success_taskCountMatchesTasksSize_forHaiwang` asserts the integer reconciles with the array length. |
| 5 | `execmode=62 = Mon-Fri bitmask`; 23-field DTO completeness | All 23 fields on SchemeTaskRowDto (SchemeDto.kt:122-152). execmode + DTO-only fields documented "MVP-deferred UI" in KDoc. Future fe upgrade path open. |

### Build outcome

- compile `:app:compileDebugKotlin` — BUILD SUCCESSFUL (20s, EXECUTED + kapt processed)
- scoped `:app:testDebugUnitTest --tests V3TaskRepositoryTest` — 16/16 green (XML mtime 2026-05-30 16:56, failures=0, errors=0, skipped=0)
- full scoped sweep (`ui.screens.task.*` + `data.repository.*`) — 76 total tests across 10 classes, all 0 failures (no regression in fe-side consumers or sibling V3*Repository tests)
- `:app:assembleDebug` — BUILD SUCCESSFUL (17s)
- APK `app/build/outputs/apk/debug/app-debug.apk` — mtime 2026-05-30 16:57, NetworkSecurityConfig from PA-13 still in (regression sanity passed via `unzip -l`)

### What Critic should verify (Leg 5 smoke render)

Install the fresh APK, login, navigate to Task Tab. Expected:
- Hero card shows the active scheme (海王作息 or 日本作息 — first-active wins).
- Below the Hero, the timeline shows 14 named tasks for 海王 (with times 07:50, 08:20, 09:00, 09:10, 09:50, 10:10, 10:50, 11:00, 11:40, 13:50, 14:25, 15:05, 15:15, 16:15).
- 上午/下午/晚上 sections all populated; no empty timeline.
- Toggling enable/disable on a scheme updates the active flag after a re-fetch.

If 海王 timeline is empty → fix has regressed. If only one scheme shows active when two should → multi-active handling regressed.

### Cross-reference

- PA-14 sibling memory: `[[terminal-zone-field-is-not-membership]]` — same family of "don't trust a wire field's apparent semantics."
- PA-10 sibling memory: `[[scheme-dto-misspelled-field]]` — projectstate vs projectstatetate; TaskGuangboModel vs TaskZuoxiModel; counter-intuitive 0==running sign.
- R-ADDR-SLOT (PA-14): ZoneMethod's mixed-slot pattern; V3TaskRepository continues to use ServerConfig.baseUrl() + MyRequestBuilder two-arg setUrl(url, "") to keep R-ADDR-SLOT dormant.
