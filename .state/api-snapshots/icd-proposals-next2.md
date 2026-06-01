# ICD proposals — NEXT-2 (2026-06-01, audit phase)

> Producer: data-integration-2. Anchored to:
>  - `.state/auth-split-brain-framing.md` v2 (D-16 / 方案 C)
>  - `.state/api-snapshots/auth-split-brain-rootcause.md` (this audit's findings)
>  - Code: AuthStoreImpl.kt / V4Activity.kt / AppNavGraph.kt / LoginRoute.kt /
>    ConstantServerConfig (ServerConfig.kt) / DynamicBaseUrlInterceptor.kt
>
> STD-ICD-WRITE: do NOT edit `icd-contracts.md` directly. PM serializes after
> Critic. These proposals are aligned with the fix design (§6 of rootcause.md);
> the fix dispatch will implement them.

---

## ★ Top-of-§ICD-AuthState pinned annotation

> **AuthStore is now formally TWO LAYERS** (D-16):
>  - **L1 凭据** (rememberMe-controlled, plain): account / serverHost /
>    serverPort / rememberMe(bool). Survives logout iff rememberMe=true. Used
>    for LoginScreen prefill UX.
>  - **L2 鉴权** (encrypted): jwt / refreshToken? / tokenExpiry / serverAddress.
>    The L2 four-tuple {jwt, serverAddress, account, tokenExpiry} is an ATOMIC
>    invariant — all four valid → resume session, ANY missing/expired →
>    `clearL2Atomically()` + route to LoginScreen.
>
> **Constant.serveraddress is a derived cache, not a source of truth.** v4 reads
> the truth from `AuthStore.serverAddress`. The static field is REHYDRATED from
> AuthStore by `StartupAuthDecider.resumeSessionIfValid()` on app start, and
> WRITTEN by `V3LoginAuthenticator.authenticate()` after a successful login.
> Any Plan A code that reads `serverConfig.baseUrl()` is reading from this
> cache; the cache may be stale only during the brief window between
> `MyApplication.onCreate` and `V4Activity.onCreate` — both happen on the main
> thread before any composable mounts, so no repo can race in.

---

## (A) ICD-AuthState v2.1 → v2.2 (additive, no break)

### v2.2 interface additions

```kotlin
interface AuthStore {
    // ── EXISTING (v2.1) ────────────────────────────────────────────────
    val serverAddress: StateFlow<ServerAddress?>
    val jwt: StateFlow<String?>
    val refreshToken: StateFlow<String?>
    val account: StateFlow<String?>
    val isLoggedIn: StateFlow<Boolean>
    suspend fun saveLogin(address: ServerAddress, account: String, jwt: String, refreshToken: String?)
    suspend fun clearLogin()
    suspend fun reset()
    suspend fun refresh(knownStaleJwt: String?): Result<String>

    // ── NEW (v2.2) ─────────────────────────────────────────────────────
    /** Epoch-millis when [jwt] becomes invalid. Null = no expiry known (legacy
     *  60h-from-issue is the documented-assumption default; explicit value
     *  takes precedence when the server provides one). [StartupAuthDecider]
     *  uses this for the atomic L2 four-tuple check. */
    val tokenExpiry: StateFlow<Long?>

    /** Whether the user opted to remember the L1 prefill (account + host:port).
     *  Independent of [isLoggedIn] — survives logout. */
    val rememberMe: StateFlow<Boolean>

    /** Extended saveLogin: tokenExpiry + rememberMe are L2/L1 metadata
     *  respectively. Default values preserve v2.1 callers (no break). */
    suspend fun saveLogin(
        address: ServerAddress,
        account: String,
        jwt: String,
        refreshToken: String?,
        tokenExpiry: Long? = null,
        rememberMe: Boolean = true,
    )

    /** Atomically clears the L2 four-tuple in a single SharedPreferences
     *  transaction. Used by [StartupAuthDecider] when the four-tuple is
     *  partially populated (residue from a crashed write or a partial
     *  backup-restore). Idempotent. */
    suspend fun clearL2Atomically()

    /** Clears L1 account+host+port and sets rememberMe=false; L2 untouched.
     *  Triggered by the LoginScreen rememberMe toggle going OFF. */
    suspend fun clearL1Account()

    /** Sets the rememberMe flag without touching credentials. Persists L1. */
    suspend fun setRememberMe(enabled: Boolean)
}
```

### Logout semantics (verbatim from framing v2 §3.3)

| Trigger | Clears L2 | Clears L1 | Notes |
|---|---|---|---|
| User active logout | ✓ | ✗ | Preserve rememberMe for re-login prefill |
| Token expiry detected | ✓ | ✗ | Same as active logout |
| 401 from network layer | ✓ | ✗ | AuthInterceptor calls clearLogin() |
| User toggles rememberMe = OFF | ✗ | ✓ (account/host/port + rememberMe=false) | L1-only |
| User toggles rememberMe = ON + login success | Writes L2 | Writes L1 (account/host/port + rememberMe=true) | Both layers |

**Discipline**: L1 / L2 paths are INDEPENDENT — never mixed.

### L2 atomic invariant

> **L2 four-tuple atomic group**: `jwt` + `serverAddress` + `account` +
> `tokenExpiry` (when present). All-or-none. Any field missing →
> `clearL2Atomically()` + route to LoginScreen. The clear MUST happen in a
> single `SharedPreferences.commit()` transaction to avoid a window where a
> second kill-app interrupts a partial clear and re-creates the same problem.

---

## (B) NEW ICD-StartupAuthDecider-v1 (LIVE)

Producer: data-integration. Consumer: `V4Activity`.

```kotlin
// data/auth/StartupAuthDecider.kt

/**
 * Atomic startup auth check, called from V4Activity.onCreate before the Compose
 * tree is set. Synchronous, side-effecting:
 *  - On valid L2: rehydrates Constant.serveraddress (via ServerConfig.setBaseUrl)
 *    from AuthStore.serverAddress before returning true.
 *  - On invalid L2: calls AuthStore.clearL2Atomically() and returns false.
 *
 * Idempotent: safe to call multiple times. Thread-safe: takes Constant
 * .serveraddress lock implicitly via the ServerConfig seam.
 *
 * The contract is INTENTIONALLY synchronous — V4Activity needs the answer
 * before setContent so AppNavGraph receives the right startDestination. The
 * implementation must read AuthStore's StateFlows via .value (initialized
 * synchronously in AuthStoreImpl's constructor from persisted SharedPreferences).
 */
interface StartupAuthDecider {
    fun resumeSessionIfValid(): Boolean
}

@Singleton
class DefaultStartupAuthDecider @Inject constructor(
    private val authStore: AuthStore,
    private val serverConfig: ServerConfig,
    private val clock: () -> Long = System::currentTimeMillis,
) : StartupAuthDecider {
    override fun resumeSessionIfValid(): Boolean {
        val jwt = authStore.jwt.value
        val addr = authStore.serverAddress.value
        val account = authStore.account.value
        val expiry = authStore.tokenExpiry.value

        val valid = !jwt.isNullOrBlank()
            && addr != null
            && !account.isNullOrBlank()
            && (expiry == null || expiry > clock())

        if (!valid) {
            // Suspend wrapper acceptable here — kotlinx.coroutines.runBlocking
            // is allowed in this synchronous startup decision (main thread,
            // pre-Compose, microsecond-scale clear). Matches the existing
            // AuthInterceptor/DynamicBaseUrlInterceptor runBlocking pattern.
            runBlocking { authStore.clearL2Atomically() }
            return false
        }
        serverConfig.setBaseUrl("http://${addr.host}:${addr.port}/api")
        return true
    }
}
```

### Single @Binds

In `DataModule.kt` (consumer-seam-binding-rule):
```kotlin
@Binds @Singleton
abstract fun bindStartupAuthDecider(impl: DefaultStartupAuthDecider): StartupAuthDecider
```

### V4Activity integration

```kotlin
@AndroidEntryPoint
class V4Activity : ComponentActivity(), CancelAdapt {
    @Inject lateinit var startupAuth: StartupAuthDecider
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val start = if (startupAuth.resumeSessionIfValid()) AppRoutes.Main else AppRoutes.Login
        setContent { AeroTheme { AppNavGraph(startDestination = start) } }
    }
}
```

`AppNavGraph` already accepts `startDestination: String = AppRoutes.Splash` (line 33). The Splash composable becomes effectively unreachable — can be removed or kept as a brief brand flash on the cold-login path (fe-business's call).

---

## (C) ICD-NetworkModule-v1 §X clarification (no version bump — doc only)

Add to the description:

> **`Constant.serveraddress` truth model (PA-15 / NEXT-2 D-16)**:
>  - SOURCE OF TRUTH: `AuthStore.serverAddress` (persisted in plain
>    SharedPreferences "auth_plain.xml" via `KeyValueStore.commit()`).
>  - DERIVED CACHE: `Constant.serveraddress` (Java static String). The
>    cache is REHYDRATED from AuthStore on app start by
>    `StartupAuthDecider.resumeSessionIfValid()`; WRITTEN by
>    `V3LoginAuthenticator.authenticate()` on successful login; CLEARED
>    on 401 (AuthInterceptor) and on user logout (TBD UI).
>  - INVARIANT (post-NEXT-2): for any Plan A repo call,
>    `Constant.serveraddress == AuthStore.serverAddress.value.let { "http://${it.host}:${it.port}/api" }`
>    when AuthStore reports `isLoggedIn=true`. Violation = NEXT-2 regression.
>
> **`DynamicBaseUrlInterceptor`** (Retrofit/OkHttp interceptor chain) reads
> directly from `AuthStore.serverAddress` and is **dormant** under Plan A
> because V3CallbackAdapter bypasses Retrofit. Documented for the eventual
> Plan B migration (when Retrofit becomes the active path, this interceptor
> picks up the truth source directly without needing
> `Constant.serveraddress` at all).
>
> **`PreferencesUtil("serverAddress")`** (legacy SharedPreferences slot read
> by `MyRequestBuilder.setUrl(String)` single-arg + several legacy
> Activities) is **R-ADDR-SLOT DORMANT** — none of the readers are reachable
> from V4Activity (sole launcher; legacy Activities demoted by AR-006).
> NEXT-2 fix does NOT write this slot (Plan A red line — zero legacy
> changes). Will close R-ADDR-SLOT post-fix.

---

## (D) Manifest / backup-rules artifacts (root cause A)

These aren't strictly ICD entries, but list them here so PM can sequence the
Manifest STD-SHAREDFILE diff alongside the AuthStore + Decider bumps.

- **AndroidManifest.xml** — line range to modify: the `<application>` tag,
  inside the existing attribute list (current attrs at lines 55-64). ADD two
  attributes, alphabetically:
  - `android:dataExtractionRules="@xml/data_extraction_rules"`
  - `android:fullBackupContent="@xml/backup_rules"`

  Both go in the same `<application>` block; declare exact line numbers in the
  fix-phase deliverable per STD-SHAREDFILE.

- **NEW `res/xml/data_extraction_rules.xml`** (API 31+, ~10 lines):
  ```xml
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

- **NEW `res/xml/backup_rules.xml`** (API <31, ~6 lines):
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <full-backup-content>
    <exclude domain="sharedpref" path="auth_secure.xml" />
  </full-backup-content>
  ```

Cross-link: same family as PA-13's `network_security_config.xml` — the
canonical Manifest-config artifact pattern (`[[constraint-vs-artifact-rule]]`,
`[[manifest-config-artifact-required]]`).

---

## How to land (PM, after Critic PASS on the eventual fix)

1. Patch `aeroradio-workflow/references/icd-contracts.md`:
   - ICD-AuthState v2.1 → v2.2 (additive — interface adds + 4-path logout table + L2 atomic invariant).
   - NEW section ICD-StartupAuthDecider-v1 (LIVE).
   - ICD-NetworkModule-v1 doc-only `Constant.serveraddress` truth-model section (no version bump).
   - Pin the ★ top-of-§ICD-AuthState annotation prominently.
2. Cross-link to `[[serveraddress-owned-by-data-integration]]` and
   `[[loginauthenticator-impl-owned-by-data]]` (data-integration owns both
   ServerConfig rehydration and StartupAuthDecider).
3. After fix lands: close R-ADDR-SLOT in `aeroradio-workflow/references/risk-log.md` (or wherever it tracks) — `ServerConfig.setBaseUrl` is now the canonical write path, the PreferencesUtil slot stays dormant by routing, all legacy callers documented as v4-unreachable.
4. Sequence: fe-business adds rememberMe toggle in LoginScreen + Logout overflow in main scaffold (small UI work, no data layer change beyond what NEXT-2 lands).
