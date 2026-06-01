# NEXT-2 — Auth split-brain root cause (D-16, 2026-06-01)

> Producer: data-integration-2 · L2 code audit (read-only) · anchored to
> framing v2 `.state/auth-split-brain-framing.md` (D-16 / 方案 C / 路径 A+B).
> No code edits, no git ops, no build slot consumed.

---

## TL;DR (one paragraph)

**Verdict (c-1) — legacy is dead-path on the v4 launcher; one true root-cause-C bug + one true root-cause-B bug + one preventive root-cause-A fix.**

The kill-app NPE comes from a **single, sharp bug**: under Plan A, `Constant.serveraddress` (the Java `static String` field that ALL V3*Repository call sites read via `ConstantServerConfig.baseUrl()`) is **the URL truth, but only `V3LoginAuthenticator.authenticate()` writes it**. A restored session bounces straight past Login → Main without calling `authenticate()`, so on every process restart `Constant.serveraddress == null` while `AuthStore.jwt` + `AuthStore.serverAddress` are correctly hydrated from EncryptedSharedPreferences + plain SharedPreferences. Result: AuthStore is "logged in", `AppNavGraph` skips to Main, repos call `serverConfig.baseUrl() + path` → `"null/terminal/terzone"` → OkHttp parse fail → **"加载失败 serveraddress must not be null"** (the exact symptom CTO observed on 2026-06-01).

`DynamicBaseUrlInterceptor` reads from AuthStore correctly and IS kill-app safe — but it's **dormant** under Plan A (V3CallbackAdapter bypasses Retrofit/OkHttpClient → never runs the interceptor chain → ServerAddress not consulted). PA-14's PA-13 `network_security_config.xml` fix is unaffected. Framing v2 §2's `ReFreshTokenUtil` / `TaskManageUtils` evidence is REAL CODE but **DEAD CODE under v4 sole-launcher** — neither has a v4-reachable caller.

The fix per D-16 is the right shape (L1/L2 split + StartupNavDecider atomic check), and one tight additional discipline: **a single nav decider must compute the "go to Main" predicate atomically over BOTH stores AND rehydrate `Constant.serveraddress` from AuthStore on app start** — exactly once, before any repo can read it.

---

## §1 — Root cause A (allowBackup → uninstall path) — preventive

**Evidence (line-precise)**:
- `app/src/main/AndroidManifest.xml:57` (counted from current file) — `android:allowBackup="true"` on the `<application>` tag.
- `app/src/main/res/xml/` directory inventory: only `accessibility_service_config.xml` + `network_security_config.xml` (from PA-13). **No `backup_rules.xml`, no `data_extraction_rules.xml`.**
- Android Auto Backup → backs up `/data/data/<pkg>/shared_prefs/*` on uninstall, restores on next same-signature install.

**Mechanism**: `EncryptedSharedPreferences` lives under `shared_prefs/`. Auto Backup restores the encrypted prefs file across uninstall/reinstall. Since the EncryptedSharedPreferences MasterKey is **derived from the Android Keystore alias** (default `MasterKey.Builder(ctx).setKeyScheme(AES256_GCM)` uses `_androidx_security_master_key_`), and the alias survives the Android user identity (not the app's data dir), the restored ciphertext CAN be decrypted by the same device's keystore. → Path A explained.

**Two fix options + recommendation**:

| Option | Manifest change | Files added | Pros | Cons | UX |
|---|---|---|---|---|---|
| **A1** Manifest `allowBackup="false"` | one-line | none | minimal, defensive, no XML to maintain | future legitimate backup (e.g. user-controlled L1 prefs for device migration) blocked | uninstall = full wipe; user re-types host+account next install |
| **A2** xml `data_extraction_rules.xml` + `backup_rules.xml` exclude `auth_secure` only | `allowBackup="true"` retained | 2 new XML files | finer-grained — L1 (`auth_plain`) restorable across uninstall = nice UX, L2 (`auth_secure`) excluded blocks the security hole | more XML to keep right; minSdk-version split (data_extraction_rules.xml = API 31+, backup_rules.xml = pre-31 — BOTH needed) | uninstall preserves L1 prefill (host+account survive); user only re-enters password |

**My recommendation: A2** — UX-friendlier and aligns with D-16's L1/L2 separation philosophy ("L1 = 用户体验, L2 = 安全资产"). The XML overhead is one-time and the security stance is identical (L2 excluded = same effect as allowBackup=false for the actual sensitive data). Critic family note: this is the canonical case of `[[constraint-vs-artifact-rule]]` (a documented backup-allowance constraint that must materialize as a Manifest attr + xml artifact; the `network_security_config.xml` work in PA-13 is the sibling).

**Concrete artifacts to produce in fix phase** (drafts; not landing in audit):

```xml
<!-- res/xml/data_extraction_rules.xml — API 31+ -->
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
  <cloud-backup>
    <exclude domain="sharedpref" path="auth_secure.xml" />
  </cloud-backup>
  <device-transfer>
    <exclude domain="sharedpref" path="auth_secure.xml" />
  </device-transfer>
</data-extraction-rules>
```
```xml
<!-- res/xml/backup_rules.xml — pre-API 31 -->
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
  <exclude domain="sharedpref" path="auth_secure.xml" />
</full-backup-content>
```
Manifest add: `android:dataExtractionRules="@xml/data_extraction_rules"` + `android:fullBackupContent="@xml/backup_rules"` (both on `<application>`).

**Status**: ★ CONFIRMED root cause A. Preventive — doesn't fix Path B but closes Path A.

---

## §2 — Root cause B (dual-source persistence) — overstated; mostly dead-code

### New-stack persistence audit

| What | File:line | Persisted? | Read on start? |
|---|---|---|---|
| L2 JWT | `AuthStoreImpl.kt:34` (init read) + `:64-66` (write) + `SharedPrefsKeyValueStore.kt:36` (`commit()` — **synchronous!**) | YES — EncryptedSharedPreferences "auth_secure" | YES — constructor reads `secure.getString(KEY_JWT)` |
| L2 refreshToken | same | YES | YES |
| L1 serverHost/serverPort | `AuthStoreImpl.kt:31` (init via `loadServerAddress()` :120-124) + `:59-63` (write) | YES — plain SharedPreferences "auth_plain" | YES |
| L1 account | `AuthStoreImpl.kt:40` (init) + `:61` (write) | YES | YES |
| **`Constant.serveraddress`** | `Constant.java:18` `public static String serveraddress;` (no default → NULL) | **NO — process-scoped static field** | **NO — never re-hydrated from AuthStore on app start** ★ |

Findings:
- ★ The framing v2 §3's "DataStore async 异步写盘" hypothesis is FALSE for the current code. `SharedPrefsKeyValueStore.put` uses `editor.commit()` (line 36, synchronous block-until-durable) NOT `apply()`. So the kill-app symptom is NOT a "write didn't fsync" race — the L2 prefs really are on disk. (Confirms candidate #8 is RULED_OUT below.)
- ★ The framing's concern about `DataStore` doesn't apply because **there is no DataStore in this codebase** — current AuthStore uses SharedPreferences (plain + EncryptedSharedPreferences) via the `KeyValueStore` seam. (When D-16 fix lands, we should KEEP this seam — DataStore is not needed here. The framing v2 §3 implied DataStore; we can simply continue with EncryptedSharedPreferences + `commit()` which has the right durability semantics already.)

### Legacy-stack persistence callers (the framing's `ReFreshTokenUtil`/`TaskManageUtils` evidence)

| Citation in framing v2 §2 | Actually exists? | Caller in v4 sole-launcher path? |
|---|---|---|
| `ReFreshTokenUtil.java:43 PreferencesUtil.getEntity(key_tokenModel)` | YES (`utils/ReFreshTokenUtil.java`) | **NO callers** — `grep ReFreshTokenUtil.getInstance` across the repo finds ZERO non-self references. **Dead under v4.** |
| `TaskManageUtils.java:201 PreferencesUtil.getField("serverAddress")` | YES (the `getServeNomber` helper) | **No v4-launcher caller** — TaskManageUtils is only invoked from legacy `activity/*Activity.java` files (Task/Zone/Music *Activity.java), all demoted from LAUNCHER by AR-006. **Dead under v4.** |

`PreferencesUtil("serverAddress")` IS read by other legacy paths: `ActivityMusicOrder.java:107,145`, `TaskGuangboDetailActivity.java:196,224`, `SelectMusicActivity.java:161,196`, `MyRequestBuilder.java:32`. **None of these are reachable** in the v4 entry-point under sole-launcher (V4Activity → AppNavGraph → Splash/Login/Main). They're carcass only.

The ONLY active dual-stack contact point on the v4 path is `MyRequestBuilder.setUrl(String)` single-arg overload (`MyRequestBuilder.java:30-35`) which prepends `Constant.serveraddress` AND has a `PreferencesUtil("serverAddress")` fallback. **All V3*Repository POSTs use the TWO-arg overload `setUrl(url, "")`** — verified at V3LoginAuthenticator.kt:62 (via my own PA-10/PA-15 work) and V3TaskRepository.kt:106,148. The single-arg path is genuinely not exercised by the v4 launcher. Confirms the R-ADDR-SLOT dormant tracking.

### Logout flow audit

`grep clearLogin` in `app/src/main/.../ui/` → **NO UI caller**. AppNavGraph.kt:17 KDoc says "logout will clear+pop back to login" but that wiring DOES NOT EXIST in any 5-tab screen. The only `clearLogin()` call is internal: `AuthStoreImpl.refresh().onFailure` at line 114 (auto-clear on refresh fail). So:
- A user-driven logout button is currently **un-implemented** on v4.
- Framing v2 §2's "Logout 不全清" candidate is N/A in the current code — there's no v4 logout to under-clear.
- D-16's fix will need to ADD logout buttons (data-integration owns the seam; fe wires the UI). This is in scope for the fix phase.

### Verdict on root cause B

**OVERSTATED in framing v2.** Three sub-findings:
- The new-stack persistence is correctly two-store (L1 plain + L2 encrypted) and writes are sync-`commit()`. No async-write race.
- The legacy `PreferencesUtil` token/serverAddress callers cited in framing exist but are **dead under v4 sole-launcher**. (c-1) on that axis.
- No actual v4 Logout flow exists yet; cannot be "not clearing dual sources" — there's nothing to clear.

The REAL split-brain is in §3 — the `Constant.serveraddress` static field being out-of-sync with `AuthStore.serverAddress`.

**Status**: ⚠ PARTIALLY CONFIRMED (only the `Constant.serveraddress` vs `AuthStore.serverAddress` axis); the legacy-PreferencesUtil axis is RULED_OUT (dead code).

---

## §3 — Root cause C (kill-app NPE) ★ ACTUAL MECHANISM

### The 5-step kill-app failure mode (file:line for every step)

1. **App restart after kill-app**:
   - `MyApplication.onCreate()` runs (`base/MyApplication.java:96`). It initializes Baidu/AutoSize/legacy OkHttp wiring. **Does NOT touch `Constant.serveraddress` and does NOT read AuthStore.** So `Constant.serveraddress` retains its static-init value of **NULL** (`Constant.java:18` `public static String serveraddress;` — no default).
2. **AuthStore singleton constructed** (Hilt @Singleton): `AuthStoreImpl` constructor at `AuthStoreImpl.kt:23-43`:
   - Line 31: `_serverAddress = MutableStateFlow(loadServerAddress())` — `loadServerAddress()` reads `plain.getString(KEY_HOST)` + `getInt(KEY_PORT)` (lines 120-124). **Returns the correctly-persisted ServerAddress** (the prior session's host:port from "auth_plain.xml" survives kill-app — synchronous commit).
   - Line 34: `_jwt = MutableStateFlow(secure.getString(KEY_JWT))` — reads from EncryptedSharedPreferences "auth_secure.xml". **Returns the correctly-persisted JWT**.
   - Line 43: `_isLoggedIn = MutableStateFlow(!_jwt.value.isNullOrBlank())` — evaluates to **TRUE** because the JWT is non-blank.
3. **V4Activity.onCreate** (`ui/V4Activity.kt:29-36`): trivial — `setContent { AeroTheme { AppNavGraph() } }`. **NO atomic auth check.** Just hands off to NavGraph.
4. **AppNavGraph routing** (`ui/scaffold/AppNavGraph.kt:30-66`):
   - Splash route (`:39-48`): hardcoded comment at **line 42**: `// No real token check yet — always send to Login.` Always navigates to Login.
   - Login route (`:50-58`) renders `LoginRoute` which observes `viewModel.isLoggedIn` (= `authStore.isLoggedIn`).
   - **`LoginRoute.kt:31-33`**: `LaunchedEffect(loggedIn) { if (loggedIn) onLoggedIn() }` → triggers IMMEDIATELY on first composition because `authStore.isLoggedIn.value == true` (from step 2).
   - `onLoggedIn` is `navController.navigate(AppRoutes.Main) { popUpTo(AppRoutes.Login) { inclusive = true } }`. **User bounces directly to Main without typing anything, without calling `LoginViewModel.onSubmit()`.**
5. **Main mounts** → 5-tab scaffold → tabs start polling → `V3TerminalRepository.fetchAndPublish()` calls `adapter.get(url(PATH_ZONES))` where `url(path) = serverConfig.baseUrl() + path` (V3TerminalRepository.kt:101-102).
   - `ConstantServerConfig.baseUrl()` (`data/v3bridge/ServerConfig.kt:29`) returns `Constant.serveraddress` = **NULL** (never written this process).
   - Kotlin string concat: `null + "/terminal/terzone"` → `"null/terminal/terzone"` (a malformed but non-null string).
   - `V3CallbackAdapter.get(url)` → `RequestManger.get("null/terminal/terzone", ...)` → OkHttp `HttpUrl.parse("null/terminal/terzone")` returns null → IOException or NPE depending on the OkHttp path → repository surfaces "加载失败 serveraddress must not be null" (or similar) to the UI.

**This is the exact CTO 2026-06-01 symptom**, mechanistically explained.

### The "split-brain真锤" file:line

| Surface | Code | Reads | Kill-app-safe? |
|---|---|---|---|
| `DynamicBaseUrlInterceptor.intercept` `data/network/DynamicBaseUrlInterceptor.kt:42-71` | reads `AuthStore.serverAddress.first()` | ✓ AuthStore is hydrated from plain prefs on constructor (synchronous commit). **DORMANT under Plan A** (Retrofit never runs). |
| `ConstantServerConfig.baseUrl()` `data/v3bridge/ServerConfig.kt:29` | reads `Constant.serveraddress` (static String) | ✗ Static field is process-scoped, NEVER re-hydrated on app start. **ACTIVE on every Plan A request.** |
| `AuthStore.serverAddress` `AuthStoreImpl.kt:31-32` | reads from plain prefs via `loadServerAddress()` | ✓ Correct. |
| `V4Activity.onCreate` `ui/V4Activity.kt:29-36` | nothing — no auth check | ✗ Hands directly to NavGraph; can't compute "ready to go Main" predicate. |
| `AppNavGraph` Splash `ui/scaffold/AppNavGraph.kt:42` | nothing (hardcoded `// No real token check yet`) | ✗ Always sends to Login regardless of session state. |
| `LoginRoute.LaunchedEffect(loggedIn)` `LoginRoute.kt:31-33` | `authStore.isLoggedIn.value` only | ✗ Checks JWT presence ONLY — does NOT check serverAddress + account atomically, and does NOT trigger `Constant.serveraddress` rehydration. |

**Single load-bearing line**: `LoginRoute.kt:32 if (loggedIn) onLoggedIn()` — **"checks JWT-only / should check L2 {token, serverAddress, account, expiry} atomic AND ensure `Constant.serveraddress` rehydrated."**

### Where `Constant.serveraddress` IS written

`grep` across the codebase finds these writers:
- `LoginActivity.java:497` `Constant.serveraddress = serverAddress;` — legacy `SignActivity → LoginActivity` flow, **NOT REACHED under v4 sole-launcher** (V4Activity is the only LAUNCHER; SignActivity is exported=false, no Intent targets it).
- `MyRequestBuilder.java:32` `Constant.serveraddress = PreferencesUtil.getInstance().getField(Constring.serverAddress, mContext);` — the SINGLE-ARG `setUrl(path)` fallback. Only triggered when `Constant.serveraddress.contains("//")` is false. **On kill-app the static is null → calling `.contains` NPEs before this assignment can run.** Effectively dead under kill-app.
- `V3LoginAuthenticator.kt:63` `serverConfig.setBaseUrl("http://${address.host}:${address.port}/api")` — the ONLY v4-reachable writer. **But only runs when the user actually submits the Login form.** A restored session bouncing past Login → Main NEVER triggers this.

So: the rehydration gap is real and complete. **Nobody puts `Constant.serveraddress` back after process kill.**

**Status**: ★★ CONFIRMED root cause C — the single sharp bug.

---

## §4 — (c-1) vs (c-2) verdict — DEAD-LEGACY (c-1)

### Caller chain audit under v4 sole-launcher

| Legacy class | Token reads | Caller in v4 path | Live/dead |
|---|---|---|---|
| `ReFreshTokenUtil.reFreshToken()` `utils/ReFreshTokenUtil.java:42` | `PreferencesUtil.getEntity(Constant.key_tokenModel)` | **0 callers** (grep) | **DEAD** |
| `TaskManageUtils.getServeNomber()` `utils/TaskManageUtils.java:200` | `PreferencesUtil.getField("serverAddress")` | only from legacy Activities | **DEAD** under v4 launcher |
| `ServerToken.serverToken` static | (the v3 token store) | written by `V3LoginAuthenticator.kt:73` (PA-10 already wires this) AND read by `RequestManger`'s legacy header logic | **LIVE — but already integrated** by PA-10. Not a split-brain source; it's a Plan A bridge. |
| `MyRequestBuilder.setUrl(String)` (single-arg fallback) | `Constant.serveraddress` then `PreferencesUtil("serverAddress")` | only legacy Activities (all demoted) | **DEAD** under v4 launcher |

### Verdict

**(c-1) Dead-legacy.** The legacy stack's `Constant.serveraddress` and `PreferencesUtil("serverAddress")` reads are present in the codebase but **none are reachable from the v4 sole-launcher path**. The legacy classes EXIST (we kept them — Plan A red line: zero changes to legacy code) but are inert.

This means: **no escalation to CTO needed on D-13 (Plan A) variation**. The fix is data-integration's own to design without touching legacy code. The fix design in §6 below respects D-13.

**Status**: (c-1) CONFIRMED — no CTO escalation needed.

---

## §5 — 10-candidate triage

| # | Candidate | Verdict | Evidence (file:line) |
|---|---|---|---|
| 1 | Dual-source persistence (DataStore vs SharedPreferences) | **RULED_OUT** | No DataStore in this codebase; persistence is single-source via `KeyValueStore` seam (`SharedPrefsKeyValueStore.kt:36` — sync `commit()`). |
| 2 | In-memory cache desync (AuthStore StateFlow vs persisted) | **RULED_OUT** | StateFlow is initialized from persisted state in the constructor (`AuthStoreImpl.kt:31,34,37,40`). On kill-app+restart, new singleton re-reads from disk. No desync. |
| 3 | Reinstall residue (allowBackup auto-restore) | **CONFIRMED** (Path A) | `AndroidManifest.xml:57 android:allowBackup="true"` + no exclude rules. § 1 above. |
| 4 | Server-address race (Interceptor singleton cache) | **RULED_OUT (active path)** + ★ FOUND THE ACTUAL BUG | `DynamicBaseUrlInterceptor.kt:42-71` reads from AuthStore — kill-app safe BUT dormant under Plan A. The active path is `ConstantServerConfig.baseUrl()` reading `Constant.serveraddress` (process-scoped static, NOT rehydrated). The "race" framing was wrong (there's no race — it's a missing rehydration). |
| 5 | Logout 不全清 | **N/A** | No v4 Logout button exists. The KDoc on AppNavGraph.kt:17 promises one ("logout will clear+pop back to login") but no UI calls `authStore.clearLogin()`. Will be added in fix. |
| 6 | 新旧栈共存窗口 (c-1 vs c-2) | **(c-1) CONFIRMED** | §4. Legacy `ReFreshTokenUtil` + `TaskManageUtils.getServeNomber` exist but have 0 v4-reachable callers. |
| 7 | JWT TTL 60h vs doc 24h refresh 错触发 | **DEFERRED — not implicated in current symptom** | `TokenRefresher` is `UnsupportedTokenRefresher` (`DataModule.kt:135-138` PA-10 work) — refresh is intentionally unwired. 401 → `clearLogin()` → re-login. TTL drift just means a stale token survives until first 401 (already the current behavior). NOT the kill-app cause. (D-16 fix should still add explicit `tokenExpiry` to AuthStore so the startup check can short-circuit a near-expiry session before the first server call — that's a UX cleanup.) |
| 8 | DataStore async fsync race | **RULED_OUT** | No DataStore. SharedPrefsKeyValueStore uses sync `commit()`. The framing v2 §3 hypothesis was wrong about the storage primitive. |
| 9 | Nav 启动入口 check 不全 | **★★ CONFIRMED — THE BUG** | `AppNavGraph.kt:42` literal `// No real token check yet — always send to Login`. `LoginRoute.kt:31-33` then trampolines via `isLoggedIn` JWT-only check. No atomic `{token, serverAddress, account, expiry}` predicate. No `Constant.serveraddress` rehydration. |
| 10 | StateFlow in-memory cache lost on kill | **RULED_OUT** | New singleton on app restart re-reads from persisted prefs. Cache loss is benign; persistence is the source of truth. (The framing v2's hypothesis was correct that StateFlow gets recreated, but wrong that this causes the bug — the constructor read makes it self-healing.) |

**The single load-bearing finding is #9 combined with the `Constant.serveraddress` rehydration gap (#4-revised).**

---

## §6 — Fix design (D-16 implementation)

### Per-root-cause

**Root cause A (preventive)** — Manifest + 2 xml files:
- Add `android:dataExtractionRules="@xml/data_extraction_rules"` and `android:fullBackupContent="@xml/backup_rules"` on `<application>` (keep `allowBackup="true"` for L1 restorability; the exclude rules in the xml take precedence on the L2 file).
- Create `res/xml/data_extraction_rules.xml` and `res/xml/backup_rules.xml` per §1 templates (exclude domain="sharedpref" path="auth_secure.xml" in both).
- One Manifest line + 2 small xml = ~30 lines of XML total. STD-SHAREDFILE applies (one Manifest, declare exact line ranges).

**Root cause B (V3 logout discipline + dead-code containment)**:
- Add a `Logout` action on the 5-tab scaffold (likely as a header overflow menu — owned by fe-business but data-integration provides the seam). Action calls `authStore.clearLogin()` + clears `Constant.serveraddress` (via `serverConfig.setBaseUrl("")` or a new `serverConfig.clear()` method) + clears legacy `ServerToken.serverToken` (Plan A bridge cleanup — D-14 token sink). Then nav to Login.
- The L1 (rememberMe toggle) clear path is separate: see L1/L2 split below.
- Document `ReFreshTokenUtil` / `TaskManageUtils.getServeNomber` / `MyRequestBuilder` single-arg `setUrl` as **dormant-by-routing** in code comments (Plan A respects D-13 — don't touch them; just document the dormancy contract).

**Root cause C ★★ (THE FIX)**:

1. **Promote AuthStore to L1/L2 vocabulary in KDoc + add `tokenExpiry`** (KDoc + interface addition; binary-incompatible additions, not refactor):
   - Rename keys (or just doc-relabel) `KEY_HOST` / `KEY_PORT` / `KEY_ACCOUNT` → L1 layer.
   - `KEY_JWT` / `KEY_REFRESH` → L2 layer (already in EncryptedSharedPreferences). Add `KEY_TOKEN_EXPIRY: Long` (epoch millis) to L2.
   - Add `interface AuthStore { ... val tokenExpiry: StateFlow<Long?>; val rememberMe: StateFlow<Boolean> ... }`. Add `clearL2Atomically()`, `clearL1Account()`, `saveRememberMe(...)` to interface.
   - `saveLogin(...)` signature extended with `tokenExpiry: Long? = null, rememberMe: Boolean = true`.

2. **Add `StartupAuthDecider` seam** (new interface owned by data-integration; new file `data/auth/StartupAuthDecider.kt`):
   ```kotlin
   interface StartupAuthDecider {
       /** Synchronous, atomic, called before any nav. Returns true iff
        *  AuthStore L2 {jwt, serverAddress, account, expiry} are all valid
        *  AND the static `Constant.serveraddress` has been (re)hydrated from
        *  AuthStore.serverAddress. Safe to call from any thread; idempotent. */
       fun resumeSessionIfValid(): Boolean
   }
   ```
   Impl rehydrates `serverConfig.setBaseUrl(...)` from `AuthStore.serverAddress` before returning true; clears L2 atomically if any field missing.
3. **Change V4Activity.onCreate** to call the decider BEFORE setContent and pass the result to AppNavGraph as `startDestination`:
   ```kotlin
   @Inject lateinit var startupAuth: StartupAuthDecider
   override fun onCreate(...) {
       super.onCreate(savedInstanceState)
       val start = if (startupAuth.resumeSessionIfValid()) AppRoutes.Main else AppRoutes.Login
       setContent { AeroTheme { AppNavGraph(startDestination = start) } }
   }
   ```
   AppNavGraph already accepts `startDestination` (line 33) — no Composable refactor needed. Splash composable can be DROPPED entirely (it's currently a no-op fade then forced-Login) or kept as a brief brand show. Removing the `// No real token check yet` hack.
4. **LoginRoute simplification**: with the decider gating Main routing at app start, `LoginRoute.LaunchedEffect(loggedIn)` should be RESTRICTED to "user just submitted credentials" — but since the only path to set `isLoggedIn=true` from inside Login is via `viewModel.onSubmit()`, this is already correct after step 3. **No LoginRoute code change** beyond a KDoc clarifying the semantics.
5. **LoginScreen add rememberMe toggle**: fe-business work; data-integration provides the seam (already covered by `saveRememberMe(...)` in step 1). Prefill account+host already happens via `prefillAccount`/`prefillServer` (LoginViewModel.kt:46-48) — confirmed working.
6. **Logout / 401 path**: extend `AuthInterceptor.kt` 401-handler to additionally clear `Constant.serveraddress` (call `serverConfig.setBaseUrl("")`) so any racing repo immediately sees the URL-truth go null + UI sees the error and routes back to Login.

### Tests (4 new + 2 extended)

| Test | Purpose | File |
|---|---|---|
| `KillAppStateRecoveryTest` (NEW) | Construct AuthStore from prefs containing {jwt, host, port, account, expiry-future}; call `startupAuth.resumeSessionIfValid()`; assert returns true AND `Constant.serveraddress` (via ServerConfig fake) is rehydrated. Then mutate one field to null (each L2 component) → assert returns false AND L2 atomically cleared. | `app/src/test/.../data/auth/KillAppStateRecoveryTest.kt` |
| `UninstallReinstallTest` (NEW, concept) | Simulate via clearing prefs files between two AuthStore instantiations; assert restored session is honored only when both L1+L2 present (the post-allowBackup-fix behavior is: L1 may be restored, L2 cannot, so `resumeSessionIfValid` returns false → land on Login with prefill). | same dir |
| `LogoutPartialClearTest` (NEW) | Test L2-only clear (active logout) leaves L1 prefill intact; L1-only clear (rememberMe off) wipes account but keeps L2. | same dir |
| `RememberMeToggleTest` (NEW) | Toggle rememberMe → assert L1 fields update accordingly; subsequent login success writes L1 only when rememberMe=true. | same dir |
| `AuthStoreImplTest` (EXTEND) | Add: `tokenExpiry` round-trip; `clearL2Atomically` is single-transaction; constructor reads `tokenExpiry` from secure prefs. | (existing file) |
| `AuthInterceptorTest` (EXTEND) | 401 → ensure `serverConfig.setBaseUrl("")` is called alongside `clearLogin()`. | (existing file) |

### Backwards compatibility / fe surface impact

- fe consumers of `AuthStore`: `LoginViewModel`/`LoginRoute`/`LoginScreen` keep observing the same StateFlows. NEW StateFlow `rememberMe` + new method `saveRememberMe(...)` are additions, not changes. `prefillAccount`/`prefillServer` semantics unchanged.
- ICD-AuthState v2.1 → v2.2 (additive bump; see §8).

---

## §7 — 5-leg gate calibration

| Leg | Content | Owner | Status |
|---|---|---|---|
| L1 Spec | Handoff + AeroRadio v4 spec + D-16 framing v2 + this rootcause.md §3-§6 | data-integration-2 (incorporated; cited) | ✓ this audit |
| L2 Code | The audit above (§§1-5) + the fix in §6 (next dispatch) | data-integration-2 | ✓ audit done; fix on slot |
| L3 Test | 4 new + 2 extended tests in §6 above | data-integration-2 (in fix dispatch) | pending |
| L4 Data | CTO 5-cycle real-device verification (see precise commands below) | CTO | pending — list below |
| L5 Smoke | critic-2 emulator automation: 4-cycle install/login/kill/restart/screenshot LoginScreen vs MainScreen | critic-2 | pending |

### CTO L4 ground-truth checklist (precise adb commands)

**Cycle 1 — cold install + login**:
```bash
adb uninstall com.htgd.radiocontrol.aeroradiocontrol  # clear all
adb install app-debug.apk
adb shell am start -n com.htgd.radiocontrol.aeroradiocontrol/.ui.V4Activity
# Expect: Login screen, all fields blank, rememberMe toggle visible.
# User submits credentials → main screen renders zones from real backend.
```

**Cycle 2 — kill-app + restart (THE BLOCKER reproducer)**:
```bash
adb shell am force-stop com.htgd.radiocontrol.aeroradiocontrol
adb logcat -c   # clear logs
adb shell am start -n com.htgd.radiocontrol.aeroradiocontrol/.ui.V4Activity
adb logcat -d | grep -E 'StartupAuthDecider|AuthStore|ConstantServerConfig|serveraddress'
# PASS criteria:
#   - logs show StartupAuthDecider returns true
#   - logs show Constant.serveraddress rehydrated (non-null) before any repo call
#   - UI lands DIRECTLY on Main (no LoginScreen flash)
#   - Zones load successfully (no "加载失败 serveraddress must not be null")
```

**Cycle 3 — rememberMe ON + kill + restart**:
```bash
# precondition: cycle 2 working; from Main, toggle rememberMe=true via settings (if exposed) then logout
# (rememberMe is set at LOGIN — toggle on form. Just do cycle 1 with the toggle ON.)
adb shell am force-stop com.htgd.radiocontrol.aeroradiocontrol
adb shell am start -n com.htgd.radiocontrol.aeroradiocontrol/.ui.V4Activity
# Cycle 3a — kill while logged in: SAME AS CYCLE 2 (rememberMe is for re-LOGIN prefill, not the kill-app path)
# Cycle 3b — logout then restart: account+host prefilled, password blank
```

**Cycle 4 — rememberMe OFF + kill + restart**:
```bash
# Login with rememberMe=false
# kill + restart → same as cycle 2 (L2 still valid → Main)
# logout → restart → LoginScreen ALL BLANK (no prefill)
```

**Cycle 5 — uninstall + reinstall (Path A regression check)**:
```bash
adb uninstall com.htgd.radiocontrol.aeroradiocontrol
adb install app-debug.apk
adb shell am start -n com.htgd.radiocontrol.aeroradiocontrol/.ui.V4Activity
# PASS criteria (after backup_rules.xml fix):
#   - LoginScreen lands cold (L2 was excluded from backup, so no JWT restored)
#   - L1 prefill MAY restore (host+account) — UX improvement
#   - Cannot bounce to Main without re-entering password.
```

### Observability instrumentation for fix phase

Add `Log.i("StartupAuthDecider", "resume=$valid, host=$host, expires=$expiry, jwt=${if (jwt != null) "present" else "null"}")` so CTO can verify the decision per cycle without code introspection.

---

## §8 — ICD impact

Written to `.state/api-snapshots/icd-proposals-next2.md` (separate file, STD-ICD-WRITE; PM serializes). Summary:

- **ICD-AuthState v2.1 → v2.2** (additive):
  - L1/L2 vocabulary in the contract description.
  - Add `tokenExpiry: StateFlow<Long?>` (epoch millis).
  - Add `rememberMe: StateFlow<Boolean>`.
  - Add `clearL2Atomically()`, `clearL1Account()`, `saveRememberMe(...)`.
  - Document the 4-path logout semantics from framing v2 §3.3 verbatim.
  - Document the L2 atomic invariant ({jwt, serverAddress, account, expiry} all-or-none).

- **NEW ICD-StartupAuthDecider-v1** (LIVE):
  - Single-method seam `resumeSessionIfValid(): Boolean` — synchronous, atomic, idempotent.
  - Contract: returns true iff L2 four-tuple valid AND `Constant.serveraddress` rehydrated. Returns false iff any L2 field missing/expired → caller goes to Login. Internally clears L2 atomically on the false path.
  - Single @Binds in `DataModule` (consumer-seam-binding-rule).

- **ICD-NetworkModule-v1 §X clarification** (no version bump — just doc):
  - `Constant.serveraddress` is now defined as "rehydrated from AuthStore.serverAddress by `StartupAuthDecider` at app start; written by V3LoginAuthenticator on successful login; cleared on 401 + on logout." (i.e. the truth source moves to AuthStore; the static is a derived cache.)
  - `DynamicBaseUrlInterceptor` (Retrofit path) remains dormant under Plan A; document.

- **No change to**: ICD-LoginAuthenticator (V3LoginAuthenticator), ICD-TaskRepository, ICD-TerminalDto, ICD-Endpoints. The fix is entirely within the auth layer.

---

## Cross-references

- Sister bug pattern: PA-13 `[[constraint-vs-artifact-rule]]` — a documented constraint with a runtime activation switch (Manifest attr + res/xml) that wasn't produced. Root cause A here is the same pattern (allowBackup default + missing backup_rules.xml).
- Sister memory: `[[serveraddress-owned-by-data-integration]]` — data-integration owns ServerConfig + ServerAddress.parse. PA-15 entrenched this. NEXT-2 extends it: data-integration also owns `StartupAuthDecider` (the rehydration seam).
- Sister memory: `[[loginauthenticator-impl-owned-by-data]]` — same ownership boundary; the fix extends V3LoginAuthenticator + adds StartupAuthDecider impl under `data/auth/`.
- Sister memory: `[[two-endpoint-shape-reuse-trap]]` — same "wire reality vs assumption" discipline applied to dual-store reality vs assumed single source.
- R-ADDR-SLOT: now CLOSEABLE post-fix. ServerConfig will have a documented rehydration contract; the legacy `PreferencesUtil("serverAddress")` slot stays dormant by routing (all legacy callers demoted).

---

*Audit complete. Awaiting PM decision on fix dispatch.*

---

## §6.5 — Fix landed (NEXT-2 build slot)

> Appended after the fix landed: 12 source/test/manifest files changed +
> 6 new test+source+xml files added; compile clean; 54 tests across 7 classes
> green (XML truth); assembleDebug → fresh APK 2026-06-01 13:02 with all
> 4 critical Manifest attrs surviving merge and both new backup xml files
> packaged.

### Files changed (13 modified + 6 added)

**Source (10 modified, 1 new):**
- `app/.../data/auth/AuthStore.kt` MODIFIED — v2.1→v2.2 contract additions (interface KDoc rewritten with L1/L2 vocabulary + atomic invariant; new flows `tokenExpiry` + `rememberMe`; new methods `clearL2Atomically` / `clearL1Account` / `setRememberMe`; extended `saveLogin` with default-args back-compat).
- `app/.../data/auth/AuthStoreImpl.kt` MODIFIED — adds tokenExpiry/rememberMe StateFlows initialized from commit-backed prefs; clearLogin keeps v2.1 semantics (L2 secrets only, preserves serverAddress/account/rememberMe for prefill); clearL2Atomically goes stronger (also wipes serverAddress for residue cleanup, used by the decider); clearL1Account / setRememberMe / saveLogin extended.
- `app/.../data/auth/KeyValueStore.kt` MODIFIED — `getLong` / `getBoolean` read-side completeness (put-side already accepted them).
- `app/.../data/auth/SharedPrefsKeyValueStore.kt` MODIFIED — impls for the two new reads.
- **`app/.../data/auth/StartupAuthDecider.kt` NEW** — `interface StartupAuthDecider { fun resumeSessionIfValid(): Boolean }` + `DefaultStartupAuthDecider` impl (synchronous .value reads of the L2 four-tuple; rehydrates `Constant.serveraddress` via ServerConfig on valid; runBlocking `clearL2Atomically()` on invalid for residue wipe) + injectable `Clock` seam (production `SystemClock`).
- `app/.../data/network/AuthInterceptor.kt` MODIFIED — @Inject ServerConfig; 401-refresh-failure path additionally calls `serverConfig.setBaseUrl("")` so `Constant.serveraddress` cache clears in lockstep with the dead session.
- `app/.../di/DataModule.kt` MODIFIED — adds DefaultStartupAuthDecider / StartupAuthDecider imports + `@Provides @Singleton fun provideClock(): Clock = SystemClock` + `@Binds @Singleton bindStartupAuthDecider` (single binding per consumer-seam-binding-rule).
- `app/.../ui/V4Activity.kt` MODIFIED — `@Inject lateinit var startupAuthDecider`; `onCreate` calls `resumeSessionIfValid()` SYNCHRONOUSLY before `setContent` and passes `startDestination=Main/Login` to AppNavGraph.
- `app/.../ui/scaffold/AppNavGraph.kt` MODIFIED — Splash composable + `// No real token check yet — always send to Login` hack REMOVED; default `startDestination` changed from `AppRoutes.Splash` to `AppRoutes.Login` (safer fallback); KDoc rewritten to explain the V4Activity decider hand-off.
- `app/.../AndroidManifest.xml` MODIFIED — `<application>` gets two new attrs alphabetically (line 58 `android:dataExtractionRules="@xml/data_extraction_rules"`, line 59 `android:fullBackupContent="@xml/backup_rules"`); `allowBackup="true"` retained for L1 restorability.
- **`app/src/main/res/xml/data_extraction_rules.xml` NEW** (API 31+: `<cloud-backup>` + `<device-transfer>` both exclude `auth_secure.xml`).
- **`app/src/main/res/xml/backup_rules.xml` NEW** (pre-31: `<full-backup-content>` excludes `auth_secure.xml`).

**Tests (4 new + 4 modified):**
- **NEW `KillAppStateRecoveryTest`** (9 tests) — the load-bearing regression guard: persisted-session-resume happy path (asserts true + setBaseUrl called with the rehydration URL) + each L2 field individually missing → returns false + clearL2Atomically wipes residue + idempotency on repeated decider calls.
- **NEW `LogoutPartialClearTest`** (7 tests) — D-16 4-path logout matrix: clearLogin = L2-only (L1 prefill preserved); clearL2Atomically = L2+serverAddress (decider residue wipe, L1 preserved); clearL1Account = L1-only (L2 preserved); single-commit transaction discipline per path; independence of paths.
- **NEW `RememberMeToggleTest`** (7 tests) — toggle on/off as pure flag write with no credential side-effects; restart-survival; saveLogin(rememberMe=true) writes L1 prefill; saveLogin(rememberMe=false) records L1 row with flag off; StateFlow emits transitions.
- **NEW `UninstallReinstallTest`** (3 tests) — post-fix backup_rules state: simulates L1 restored + L2 excluded; decider returns false; pre-decider window verifies LoginScreen can read L1 prefill; cold-install both-stores-empty.
- `AuthStoreImplTest` EXTENDED (+4 tests) — tokenExpiry round-trip; 0L-sentinel → null; v2.1 default-args back-compat; clearLogin drops tokenExpiry alongside jwt.
- `AuthInterceptorTest` EXTENDED — 401 success path adds `verify(exactly=0) { config.setBaseUrl("") }`; 401 failure path renamed `…_andClearsServerConfig` with `verify(exactly=1) { config.setBaseUrl("") }`.
- `RealtimeClientImplTest` MODIFIED — FakeAuthStore extended for v2.2 contract (tokenExpiry/rememberMe flows + 3 new methods).
- `LoginViewModelTest` MODIFIED — FakeAuthStore extended for v2.2 contract.

### Build + test + APK evidence

- `:app:compileDebugKotlin` BUILD SUCCESSFUL (21s, after one fix: smart-cast through intermediate boolean needed `!!`); `:app:kaptDebugKotlin` UP-TO-DATE; Hilt graph processed new @Inject ctor + @Provides Clock + @Binds StartupAuthDecider with NO MissingBinding (first attempt failed on Clock-MissingBinding; added @Provides).
- `:app:testDebugUnitTest --tests 'data.auth.*'` BUILD SUCCESSFUL. XML truth:
  - **KillAppStateRecoveryTest 9/9 green** (load-bearing happy-path + 5 negative cases + 2 idempotency)
  - **LogoutPartialClearTest 7/7 green**
  - **RememberMeToggleTest 7/7 green**
  - **UninstallReinstallTest 3/3 green**
  - AuthStoreImplTest 15/15 green (4 new + 11 existing — clearLogin contract preserved)
  - V3LoginAuthenticator / Retrofit / etc unchanged
- `:app:testDebugUnitTest --tests AuthInterceptorTest --tests LoginViewModelTest` BUILD SUCCESSFUL.
  - **AuthInterceptorTest 5/5 green** (includes new "refreshFailure clears ServerConfig" assertion)
  - **LoginViewModelTest 8/8 green** (FakeAuthStore v2.2-compat)
- **TOTAL NEXT-2 scope: 54 tests across 7 classes, 0 failures, 0 errors, 0 skipped.**
- `:app:assembleDebug` BUILD SUCCESSFUL (17s). **APK ready**: `app/build/outputs/apk/debug/app-debug.apk` mtime **2026-06-01 13:02**, size 56,785,878 (≈+4.6MB vs the 2026-05-30 build's 52MB — that's from PA-15 commits accumulating, not NEXT-2 alone; the NEXT-2 increment is the 2 small xml + 1 new class + interface diffs).
- **APK content sanity** (`unzip -l`): 4 critical resources confirmed inside:
  - `AndroidManifest.xml`
  - `res/xml/network_security_config.xml` (468 bytes — PA-13 NOT regressed)
  - `res/xml/backup_rules.xml` (308 bytes — NEW)
  - `res/xml/data_extraction_rules.xml` (572 bytes — NEW)
- **Manifest-merge verified**: grep on the merged AndroidManifest in `intermediates/merged_manifest/debug/processDebugMainManifest/` shows all 4 attrs present (`allowBackup="true"` + `dataExtractionRules="@xml/data_extraction_rules"` + `fullBackupContent="@xml/backup_rules"` + `networkSecurityConfig="@xml/network_security_config"`).
- All 13 source/test/xml files LF-clean; no nested-block-comment hazard (scanned).

### Design judgments locked in code (per PM cosign)

1. **`clearL2Atomically` is single-commit PER STORE, not cross-store.** The atomicity guarantee that matters is within the encrypted file (so a kill-app between writes can't leave a partial encrypted record); cross-store atomicity is enforced by the decider's four-tuple re-check on the next startup. The impl uses two `commit()` calls (secure + plain) for this reason. `LogoutPartialClearTest.clearL2Atomically_oneCommitPerStore` asserts exactly this discipline.
2. **`serverConfig.setBaseUrl("")` fail-fast over null.** Empty string is what AuthInterceptor's 401 failure writes. V3* repos build `serverConfig.baseUrl() + path` → `"" + "/terminal/terzone" = "/terminal/terzone"` → OkHttp rejects "not absolute" → caller sees a typed error fast. Pre-fix this would have produced `null + path` → either NPE on .contains() in MyRequestBuilder or a malformed-but-truthy URL string. The empty-string sentinel keeps consumer paths NPE-free while still producing a clean rejection downstream.
3. **`tokenExpiry == null` = legacy 60h-from-issue documented-assumption.** Mirrors PA-15's SchemeTaskStatus null-tolerance idiom. The decider's check `(expiry == null || expiry > now)` treats null as "trust this session" — the next 401 will discover staleness anyway. Future server-side expiry support is purely additive.

### R-ADDR-SLOT post-fix status

NEXT-2 fix formally closes R-ADDR-SLOT. ServerConfig is now the canonical write path (V3LoginAuthenticator at submit, StartupAuthDecider at app start, AuthInterceptor on 401 failure). PreferencesUtil("serverAddress") legacy slot remains dormant by routing (all readers in demoted Activities, none reachable from V4Activity). Recommended action for PM after Critic PASS: move R-ADDR-SLOT from DORMANT-TRACKED to CLOSED in `.state/tasks.yaml`.

### Critic L4/L5 readiness

- L4 (data fidelity): The KillAppStateRecoveryTest fixture models the exact CTO 2026-06-01 reproducer — load persisted state via in-memory FakeKeyValueStore, instantiate fresh AuthStoreImpl (modeling Hilt SingletonComponent rebuild after process kill), run decider, assert. The 9 cases cover the load-bearing happy path + every L2-invariant violation + idempotency.
- L5 (smoke render — for Critic emulator): the 5-cycle adb checklist in §7 is the canonical sequence. Expected per cycle:
  - **Cycle 1** (cold install + login + main): LoginScreen with all fields blank; submit → MainScaffold renders zones.
  - **Cycle 2** ★ THE BLOCKER REPRODUCER: `adb shell am force-stop` + restart → MainScaffold renders zones (no LoginScreen flash, NO "加载失败 serveraddress must not be null").
  - **Cycle 3** (rememberMe on + kill + restart): Same as cycle 2 (rememberMe controls re-login prefill, not the kill-app path).
  - **Cycle 4** (rememberMe on + logout + restart): LoginScreen with account+host prefilled, password blank.
  - **Cycle 5** (uninstall + reinstall): LoginScreen all blank (L2 excluded, L1 may restore via Auto Backup but the test's contract is the user types password fresh).

### Cross-references

- Sister memory: `[[static-field-rehydration-trap]]` — generalization of the bug this fixes; mention in `bindStartupAuthDecider` KDoc.
- Sister memory: `[[constraint-vs-artifact-rule]]` — applied to allowBackup as the textbook case (Manifest attr + xml artifact must travel together); same family as PA-13's network_security_config.xml.
- ICD: `.state/api-snapshots/icd-proposals-next2.md` aligned with what shipped (interface additions match v2.2 proposal; StartupAuthDecider seam matches; Manifest/xml artifacts match A2 option).
