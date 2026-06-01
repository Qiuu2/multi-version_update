---
name: critic-skill
description: >
  Critic Agent 的技能配置文件。
  包含对抗式提问技术、AeroRadio 专属错误模式库（Compose/Kotlin/Retrofit/ABI/Design）、
  4 个 Domain 的评审 checklist、理想化假设检测框架、
  与 PM 和 Domain Agent 的交互协议、评审反馈循环机制。
version: 1.0.0
author: AeroRadio Architecture Team
derived_from: itc-enterprise-workflow/agents/critic v1.0.0
---

# Critic Agent — Skill

## 1. Adversarial Questioning Framework

### 1.1 The Socratic Method — 4 Phase

Critic 用苏格拉底方法系统化挑战每个产出：

```yaml
phase_1_clarification:
  goal: "确保对产出的理解是准确的"
  questions:
    - "你声称解决了什么问题？"
    - "这个改动的目标是什么？"
    - "成功的衡量标准是什么？"
    - "这是 5 Tab 中哪个 Tab 的功能？哪个二级页？"
    - "这是新栈还是旧栈代码？涉及迁移吗？"

phase_2_challenge:
  goal: "挑战核心假设"
  questions:
    - "为什么用 X 方案而不是 Y？"
    - "这个假设的依据是什么？"
    - "如果反例存在，你的结论还成立吗？"
    - "为什么 Compose 而不是 View？为什么 Retrofit 而不是直接 OkHttp？"

phase_3_evidence:
  goal: "要求证据支撑"
  questions:
    - "数据从哪来？经过验证吗？"
    - "测试覆盖了哪些场景？"
    - "性能声明的测量条件是什么？"
    - "测过 64 位-only 设备吗？测过弱网吗？"

phase_4_implication:
  goal: "探索后果和边界"
  questions:
    - "这个决策对其他 Tab / 其他 Agent 有什么影响？"
    - "如果服务器返回意外格式，会怎样？"
    - "并发场景、配置变更、低端设备下的行为？"
    - "新功能是否破坏了既有功能（回归风险）？"
```

### 1.2 Six Devil's Advocate Patterns

```yaml
patterns:
  - id: "DA-001"
    name: "What If Best Case Fails"
    description: "假设产出的最佳情况不发生"
    application:
      - "如果服务器返回 5xx，UI 会怎样？"
      - "如果用户在 WS 连接前快速点击，会触发什么？"
      - "如果 AAR 在 64 位设备上加载失败，对讲模式怎么办？"

  - id: "DA-002"
    name: "Question the Assumption"
    description: "质疑核心假设"
    application:
      - "假设 1：用户始终有网络 → 验证：离线场景测过吗？"
      - "假设 2：服务器 WS 协议格式与 DRAFT 一致 → 验证：和厂商确认了吗？"
      - "假设 3：所有终端都在线 → 验证：离线终端的 UI 是？"

  - id: "DA-003"
    name: "Boundary Conditions"
    description: "边界条件极限测试"
    application:
      - "1 个终端 vs 1000 个终端的列表渲染"
      - "JWT 还剩 1 秒过期时的请求"
      - "网络在请求中断的精确时刻"
      - "终端名超长（100+ 字符）的卡片显示"

  - id: "DA-004"
    name: "Devil in the Details"
    description: "细节中的魔鬼"
    application:
      - "时区处理：lastSeenAt 是 UTC 还是本地？显示时怎么转？"
      - "字符编码：终端名包含 emoji 或繁体字时？"
      - "并发：两个 ViewModel 同时观察同一个 Flow 时的副作用？"

  - id: "DA-005"
    name: "Hidden Dependencies"
    description: "隐藏依赖"
    application:
      - "这段代码隐式依赖什么 SharedPreferences key？"
      - "Compose 屏依赖什么 LocalProvider？"
      - "Hilt 注入链中是否有循环？"
      - "Native 库是否依赖未文档化的系统服务？"

  - id: "DA-006"
    name: "Reversibility / Rollback"
    description: "可逆性 / 回滚"
    application:
      - "如果新栈拦截器有 bug，怎么回滚到旧栈？"
      - "Room migration 失败如何恢复？"
      - "ICD breaking change 后如何兼容老客户端？"
```

### 1.3 Idealized Assumption Detection

```yaml
idealized_assumption_patterns:
  - pattern: "All-or-Nothing Thinking"
    indicators:
      - "声明 'always works' 'never fails' 'perfect'"
      - "未说明的边界条件"
    challenge: "What conditions must hold for this to be true?"

  - pattern: "Happy Path Only"
    indicators:
      - "只测试 success case"
      - "Error handling 是 TODO"
      - "Compose 屏只画了 default state，没画 empty/error/loading"
    challenge: "What happens when the assumed conditions fail?"

  - pattern: "Implicit Trust"
    indicators:
      - "假设外部数据格式永远正确"
      - "假设服务器永远在线"
      - "未验证 native AAR 返回值"
    challenge: "What if the input is corrupted, hostile, or missing?"

  - pattern: "Convenience Sampling"
    indicators:
      - "性能测试只在 Pixel 6 上跑"
      - "兼容性测试只在 Android 13 上跑"
      - "WS 测试只在 WiFi 下"
    challenge: "Is the sample representative? What about excluded cases?"

  - pattern: "Static World Assumption"
    indicators:
      - "假设服务器列表不变"
      - "假设设备时间正确"
      - "假设网络一直可用"
    challenge: "How does this handle change over time?"

  - pattern: "Backend Trust"
    indicators:
      - "假设 REST 返回字段一定存在"
      - "假设 WS 消息字段格式与 DRAFT ICD 完全一致"
    challenge: "What if backend returns unexpected format?"
```

---

## 2. AeroRadio Error Pattern Database

### 2.1 Compose Error Patterns (CMP-ERR)

```yaml
compose_errors:
  - id: "CMP-ERR-001"
    name: "Excessive Recomposition"
    severity_default: "MAJOR"
    indicators:
      - "Composable receives unstable types (List<T>, lambda) as parameter"
      - "remember() block uses non-deterministic value"
      - "No @Stable / @Immutable on data class"
    detection:
      - "Look for List<T> / Set<T> as parameters without @Immutable wrapper"
      - "Look for lambda parameters without remember"
    fix_pattern: |
      // BAD
      @Composable fun TerminalList(terminals: List<TerminalDto>) { ... }

      // GOOD
      @Immutable
      data class TerminalListUiState(val terminals: List<TerminalDto>)
      @Composable fun TerminalList(state: TerminalListUiState) { ... }

  - id: "CMP-ERR-002"
    name: "State Hoisting Violation"
    severity_default: "MAJOR"
    indicators:
      - "Composable 内部有 mutableStateOf 但同时接收回调"
      - "State 既在内部管理又通过参数传入"
    fix_pattern: "Single source of truth — state 在外部，事件向上"

  - id: "CMP-ERR-003"
    name: "Side Effect in Composition"
    severity_default: "BLOCKER"
    indicators:
      - "API 调用直接写在 @Composable 函数体里"
      - "LaunchedEffect 缺失"
      - "rememberCoroutineScope 误用作 LaunchedEffect"
    fix_pattern: "Use LaunchedEffect(key) for side effects tied to composition"

  - id: "CMP-ERR-004"
    name: "Configuration Change State Loss"
    severity_default: "MAJOR"
    indicators:
      - "remember 用于应该 survive rotation 的状态"
      - "ViewModel 缺失或未注入"
    fix_pattern: "Use rememberSaveable for UI state; ViewModel for screen state"

  - id: "CMP-ERR-005"
    name: "Hardcoded Design Token"
    severity_default: "MAJOR"
    aeroradio_specific: true
    indicators:
      - "Color(0xFF...) literal"
      - "dp(12) literal instead of AeroSpacing"
      - "Direct Material color reference instead of AeroColors"
    fix_pattern: "Use AeroColors / AeroSpacing / AeroShapes from theme package"
    detection_regex:
      - "Color\\(0x[A-F0-9]{8}\\)"
      - "\\.dp\\b" # 后续人工判断是否在 AeroSpacing 之外硬编码

  - id: "CMP-ERR-006"
    name: "Missing Accessibility Semantics"
    severity_default: "MINOR"
    indicators:
      - "Icon 无 contentDescription"
      - "可点击元素无 semantics"
    fix_pattern: "Add contentDescription / Modifier.semantics { ... }"
```

### 2.2 Kotlin / Coroutine Error Patterns (KT-ERR)

```yaml
kotlin_errors:
  - id: "KT-ERR-001"
    name: "GlobalScope Usage"
    severity_default: "BLOCKER"
    indicators:
      - "GlobalScope.launch"
      - "GlobalScope.async"
    fix_pattern: "Use viewModelScope / lifecycleScope / repository's CoroutineScope"

  - id: "KT-ERR-002"
    name: "Unstructured Concurrency"
    severity_default: "MAJOR"
    indicators:
      - "Job 未存到 ViewModel，不会随屏幕销毁取消"
      - "Flow.launchIn 用错 scope"
    fix_pattern: "Tie all coroutines to a structured scope"

  - id: "KT-ERR-003"
    name: "Blocking Call in Main Dispatcher"
    severity_default: "BLOCKER"
    indicators:
      - "runBlocking on Main"
      - "I/O without withContext(Dispatchers.IO)"
    fix_pattern: "Use withContext(Dispatchers.IO) for blocking work"

  - id: "KT-ERR-004"
    name: "Flow Hot vs Cold Confusion"
    severity_default: "MAJOR"
    indicators:
      - "ViewModel 中暴露 Flow（应该是 StateFlow）"
      - "StateFlow 未 set initialValue"
    fix_pattern: "Use StateFlow for UI state; SharedFlow for events"

  - id: "KT-ERR-005"
    name: "Exception Swallowed in Coroutine"
    severity_default: "MAJOR"
    indicators:
      - "try { ... } catch(e: Exception) { /* empty */ }"
      - "缺失 CoroutineExceptionHandler"
    fix_pattern: "Log + propagate via Result / sealed class"

  - id: "KT-ERR-006"
    name: "Mutex Missing for Critical Section"
    severity_default: "MAJOR"
    indicators:
      - "AuthStore.refreshJwt 并发场景未加 Mutex"
      - "共享可变状态无保护"
    fix_pattern: "Use Mutex.withLock for critical sections"
```

### 2.3 Retrofit / Networking Error Patterns (NET-ERR)

```yaml
networking_errors:
  - id: "NET-ERR-001"
    name: "Dynamic baseUrl Misconfiguration"
    severity_default: "BLOCKER"
    aeroradio_specific: true
    indicators:
      - "Retrofit baseUrl 不是 'http://placeholder.invalid/'"
      - "ApiService 使用 @Url 而不是相对路径（除登录外）"
      - "DynamicBaseUrlInterceptor 不在 OkHttpClient 中"
    fix_pattern: |
      // 见 ICD-NetworkModule-v1
      Retrofit.Builder().baseUrl("http://placeholder.invalid/").build()
      // ApiService: @GET("/terminal/terminalinfo")
      // Interceptor: replace placeholder.invalid with authStore.serverAddress

  - id: "NET-ERR-002"
    name: "JWT in Insecure Storage"
    severity_default: "BLOCKER"
    aeroradio_specific: true
    indicators:
      - "JWT 存到普通 SharedPreferences"
      - "JWT 存到明文文件"
    fix_pattern: "Use EncryptedSharedPreferences"

  - id: "NET-ERR-003"
    name: "Missing Auth Interceptor"
    severity_default: "MAJOR"
    indicators:
      - "OkHttpClient 无 AuthInterceptor"
      - "AuthInterceptor 未处理 401"
    fix_pattern: "Add AuthInterceptor that auto-refreshes JWT on 401"

  - id: "NET-ERR-004"
    name: "Token Refresh Storm"
    severity_default: "MAJOR"
    indicators:
      - "并发请求都收到 401 时同时刷新 token"
      - "缺失 synchronized / Mutex 保护"
    fix_pattern: "Use Mutex.withLock + check if token already refreshed"

  - id: "NET-ERR-005"
    name: "Old Stack vs New Stack Coexistence"
    severity_default: "MAJOR"
    aeroradio_specific: true
    indicators:
      - "新代码引用 httptask/*Method.java"
      - "同一 endpoint 在新旧栈都被实现"
      - "多个 OkHttpClient 实例（应共享）"
    fix_pattern: "Single OkHttpClient via Hilt; migrate one module at a time"

  - id: "NET-ERR-006"
    name: "DTO Field Mismatch"
    severity_default: "MAJOR"
    indicators:
      - "DTO 字段名与后端返回不一致（缺少 @SerialName）"
      - "可空字段未标记 nullable"
      - "枚举值未处理 unknown"
    fix_pattern: "Verify against actual server response; use sealed class for enums"
```

### 2.4 ABI / Native Error Patterns (ABI-ERR)

```yaml
abi_native_errors:
  - id: "ABI-ERR-001"
    name: "32-bit Only AAR Without abiFilters"
    severity_default: "BLOCKER"
    aeroradio_specific: true
    indicators:
      - "libs/htapplib.aar 引入但 build.gradle.kts 无 abiFilters"
      - "ndk.abiFilters 缺失 armeabi-v7a"
    fix_pattern: |
      android {
        defaultConfig {
          ndk {
            abiFilters += listOf("armeabi-v7a", "x86")
          }
        }
      }

  - id: "ABI-ERR-002"
    name: "Missing 64-bit Fallback"
    severity_default: "MAJOR"
    aeroradio_specific: true
    indicators:
      - "对讲功能未检测设备 ABI"
      - "64 位-only 设备上无降级 UI"
    fix_pattern: |
      if (Build.SUPPORTED_32_BIT_ABIS.isEmpty()) {
        // 显示 "对讲功能在此设备不可用"
      }

  - id: "ABI-ERR-003"
    name: "Native Crash Recovery Missing"
    severity_default: "MAJOR"
    indicators:
      - "Native 调用未包 try-catch（虽然 catch 不到 SIGSEGV，但能处理 UnsatisfiedLinkError）"
      - "无 native crash reporter"
    fix_pattern: "Wrap native calls; add Firebase Crashlytics NDK"

  - id: "ABI-ERR-004"
    name: "JNI Reference Leak"
    severity_default: "MAJOR"
    indicators:
      - "Native 持有 Java 对象未释放"
      - "Global ref 未 DeleteGlobalRef"
    fix_pattern: "Audit AAR's JNI bindings; ensure cleanup on lifecycle end"
```

### 2.5 Design Deviation Error Patterns (DSN-ERR)

```yaml
design_deviation_errors:
  - id: "DSN-ERR-001"
    name: "Hardcoded Color"
    severity_default: "MAJOR"
    aeroradio_specific: true
    detection_regex: "Color\\(0x[A-F0-9]{8}\\)"
    exclusion: "ui/theme/AeroColors.kt（定义处除外）"
    fix_pattern: "Use AeroColors.XXX"

  - id: "DSN-ERR-002"
    name: "Wrong Card Radius"
    severity_default: "MINOR"
    aeroradio_specific: true
    indicators:
      - "Card 使用 RoundedCornerShape(8.dp) 或其他非 12.dp"
      - "应使用 AeroShapes.Card"
    fix_pattern: "Use AeroShapes.Card"

  - id: "DSN-ERR-003"
    name: "Number Not Using Mono"
    severity_default: "MINOR"
    aeroradio_specific: true
    indicators:
      - "统计数字、计数、时间未使用 AeroType.MetricNum"
      - "未启用 fontFeatureSettings = 'tnum'"
    fix_pattern: "Use AeroType.MetricNum with tnum"

  - id: "DSN-ERR-004"
    name: "Tab Color Mismatch"
    severity_default: "MAJOR"
    aeroradio_specific: true
    indicators:
      - "终端 Tab 高亮不是 #0E7C70"
      - "广播 Tab 高亮不是 #EA580C"
      - "广播内三档色不对（寻呼 #EA580C / 对讲 #2563EB / 点播 #0E7C70）"
    fix_pattern: "Use AeroColors.TabXXX / AeroColors.ModeXXX"

  - id: "DSN-ERR-005"
    name: "Missing Terminal State Variant"
    severity_default: "MAJOR"
    aeroradio_specific: true
    indicators:
      - "TerminalCard 未覆盖全部 7 个状态"
      - "无离线 / 故障态处理"
    fix_pattern: "Implement all states per design-system-spec.md §7.1"

  - id: "DSN-ERR-006"
    name: "Missing Realtime Banner"
    severity_default: "MAJOR"
    aeroradio_specific: true
    indicators:
      - "WS 断线无黄 banner"
      - "回退轮询无 UI 提示"
    fix_pattern: "Implement ConnectionBanner per design-system-spec.md §8.1"

  - id: "DSN-ERR-007"
    name: "Wrong Motion Duration"
    severity_default: "MINOR"
    aeroradio_specific: true
    indicators:
      - "Tab 切换动画 > 220ms"
      - "未使用 cubic-bezier(.2, .7, .3, 1)"
    fix_pattern: "Use AeroMotion.XXX from theme"
```

### 2.6 Architectural Pattern Violations (ARC-ERR)

```yaml
architectural_errors:
  - id: "ARC-ERR-001"
    name: "Layer Violation"
    severity_default: "MAJOR"
    indicators:
      - "Composable 直接调用 Retrofit"
      - "Repository 持有 Compose State"
      - "ViewModel 持有 Context"
    fix_pattern: "Strict: UI → ViewModel → Repository → DataSource"

  - id: "ARC-ERR-002"
    name: "Missing Hilt Scope"
    severity_default: "MAJOR"
    indicators:
      - "Repository 未 @Singleton"
      - "ApiService 未 @Provides"
    fix_pattern: "Use Hilt @Singleton / @ViewModelScoped properly"

  - id: "ARC-ERR-003"
    name: "Cross-Domain Direct Call"
    severity_default: "MAJOR"
    aeroradio_specific: true
    indicators:
      - "Frontend-Business agent 文件直接 import Legacy-Native 内部类"
      - "未通过定义好的 ICD 接口"
    fix_pattern: "Use ICD-defined interface; route via Repository pattern"
```

### 2.7 Runtime-Artifact / Sweep-Discipline Patterns (RTM-ERR)

> Added 2026-05-30 after the PA-12 NetworkSecurityConfig cleartext miss and the
> PA-13 ActivityLifecycleForegroundState.register sweep follow-up. The constraint
> was stated in spec/KDoc/ICD; the runtime artifact (Manifest attr, res/xml,
> Application init) was never produced. Unit tests use fakes and can't surface
> a missing artifact. Critic-soul DA-002 ("Question the Assumption") must apply
> to project constraints, not only to code claims.

```yaml
runtime_artifact_errors:
  - id: "RTM-ERR-001"
    name: "Documented Constraint Without Shipped Artifact"
    severity_default: "BLOCKER"
    aeroradio_specific: true
    description: >
      A project constraint stated in spec/KDoc/ICD/CLAUDE.md has a runtime
      activation switch in AndroidManifest.xml / res/xml/* / build.gradle /
      Application.onCreate. Verifying the constraint is documented is NOT
      equivalent to verifying the switch is flipped. Fakes in unit tests
      satisfy the seam but never exercise the OS-level read path.
    detection_pairs:
      - constraint: "HTTP cleartext (Constant.serveraddress = http://…, ICD-NetworkModule §1 '明文 HTTP')"
        artifact: "AndroidManifest android:networkSecurityConfig=@xml/network_security_config + res/xml/network_security_config.xml with cleartextTrafficPermitted=true"
        verification: "grep `networkSecurityConfig` and `cleartextTrafficPermitted`; read merged Manifest at build/intermediates; aapt2 dump xmltree on the APK shows the attr"
      - constraint: "Runtime permission gate (e.g. VoiceTalkAdapter.startTalk → RECORD_AUDIO)"
        artifact: "AndroidManifest <uses-permission android:name=…/> for the matching name"
        verification: "aapt2 dump permissions on the APK lists the name"
      - constraint: "@HiltViewModel / @Inject anywhere"
        artifact: "AndroidManifest <application android:name=… points at an @HiltAndroidApp Application>"
        verification: "annotation literally on the class declaration (not only in KDoc); Manifest android:name resolves to that class"
      - constraint: "Polling gated on isForeground / WS reconnect-on-foreground"
        artifact: "Application.onCreate calls ActivityLifecycleForegroundState.register(this) (the `by lazy` MutableStateFlow defaults to false; without registerActivityLifecycleCallbacks no onActivityStarted fires)"
        verification: "grep for the register call in MyApplication.onCreate; PollingRefreshScheduler test stays green AND the prod call site exists"
      - constraint: "Native AAR ABI (htapplib.aar arm64 + v7a)"
        artifact: "app/build.gradle defaultConfig.ndk.abiFilters covers shipped .so set"
        verification: "abiFilters literal in build.gradle"
      - constraint: "Gson reflection over reverse-engineered DTOs (Phase 3 release)"
        artifact: "ProGuard/R8 -keep rules for DTO classes + @SerializedName field names"
        verification: "proguard-rules.pro has the keep entries; assembleRelease runs clean"
    fix_pattern: |
      For any deliverable exercising a documented-constraint surface
      (network / permissions / native libs / DI root / Application init),
      run the artifact-pair sweep BEFORE signing off. ~90 seconds, grep-able.
      Example (PA-12 miss): the cleartext constraint was in NetworkModule
      KDoc + ICD §1 + CLAUDE.md from day 1, but the Manifest attribute +
      res/xml file never existed. PA-01/02/05/07/10 each had real network
      calls and each PASS was issued without checking the activation switch
      — the runtime demo on a 华为 arm64 device surfaced the BLOCKER.

  - id: "RTM-ERR-002"
    name: "Sweep Absence Claimed on Truncated Output"
    severity_default: "MAJOR"
    description: >
      A sweep finding ("artifact X is MISSING") that relied on truncated
      output — `head -N`, "first N hits", a scan-mode listing — is NOT
      evidence of absence. Truncated output is a scan AID; an absence claim
      requires the same evidentiary bar as a primary review.
    indicators:
      - "Sweep used grep | head -N or any bounded slice, then concluded 'not present'"
      - "No artifact-level check (aapt2 dump / merged Manifest / APK unzip) backing the absence claim"
      - "Severity assigned (BLOCKER/MAJOR) without re-verification at full rigor"
    fix_pattern: |
      A sweep finding that surfaces a NEW issue must be re-verified before
      it leaves the desk:
        1. Unbounded grep (no `head`/`tail`) OR explicit `grep -c` / `wc -l`
           to know the listing isn't lying.
        2. Positive-confirmation read of the source file at the expected
           location (`sed -n` the line range, not just rely on grep).
        3. Artifact-level check at the same axis as the original constraint:
           merged Manifest for Manifest claims, `aapt2 dump permissions`
           for uses-permission claims, `dexdump` for compiled-code claims,
           `unzip -l` for packaged-resource claims.
      Only after all three does the finding leave my desk. The receiving
      teammate's verification is the safety net; unverified sweeps can
      manufacture work as easily as catch it.

      Example (PA-12 RECORD_AUDIO false positive): I ran
      `grep -i "RECORD_AUDIO\|uses-permission" Manifest | head -8`. The
      Manifest had >8 uses-permission lines; line 32's RECORD_AUDIO was
      truncated out of the listing. I read absence into a `head`-bounded
      result. fe-platform-2 caught it with three-way verification
      (source / grep / `aapt2 dump permissions`). Had I followed the
      rule above, the false positive would not have shipped to PM.

  - id: "RTM-ERR-003"
    name: "Compose UI Reviewed Without Design-Spec Compliance Pass"
    severity_default: "BLOCKER"
    aeroradio_specific: true
    description: >
      Reviewing a UI deliverable's code-fidelity (state machine, Hilt, SSOT,
      mapping, tests) without opening `aeroradio-workflow/references/design-
      system-spec.md` and walking the rendered surface is NOT a visual review.
      Unit tests cannot assert token consumption / named-element presence /
      hardcoded color literals / Material3 default fallthrough — the review
      must positively assert each with grep evidence + spec citation. The
      rendered surface IS the deliverable.
    indicators:
      - "REVIEW_RESULT carries no 'Visual gate' subsection"
      - "Reviewer never opened design-system-spec.md for the surface"
      - "Spec names elements (hero gradient / decorative circle / footer / etc.)
        that no grep of the screen.kt cites by line"
      - "Material3-default fallthroughs (Button/Card/Text without colors=/shape=/
        style= → MaterialTheme.* not AeroTheme.*) unaudited"
    fix_pattern: |
      Visual gate procedure for any PA that adds/changes Screen.kt /
      *Content.kt / *Panel.kt / *Card.kt — BEFORE issuing the verdict:

      Step A — Spec surface citation. Open `aeroradio-workflow/references/
        design-system-spec.md`, find the section describing the rendered
        surface. If absent → flag DSN-ERR-007 "Spec-Gap on Rendered Surface"
        and surface to PM. DO NOT pass on assumed parity.

      Step B — Read the full screen.kt (not a grep slice).

      Step C — Token-consumption sweep (grep-able, positive-evidence-bound):
        grep -nE 'Color\(0x[0-9A-Fa-f]{8}\)' <files>     # MUST be empty (DSN-ERR-001)
        grep -nE '\.(padding|size|width|height)\([0-9]+\.dp\)' <files>  # raw magic numbers
        grep -c 'AeroTheme\.(colors|spacing|shapes|typography)\.' <files>  # MUST be > 0
        Zero token consumption on a rendered file = BLOCKER.

      Step D — Named-element walk. For each element the spec names for this
        surface, produce one of:
          `<element_name>: <screen.kt:line>` OR `<element_name>: ABSENT`
        Every ABSENT is a DSN-ERR-005-class finding (Missing Spec Element).

      Step E — Material3 default fallthrough audit. Grep:
          grep -nE 'Card\(\s*\{|Button\(\s*onClick|Text\("[^"]*"\s*\)' <files>
        For each hit, verify it carries an explicit `colors=` / `shape=` /
        `style=` bound to AeroTheme.*, not the M3 default. Flag each
        unbound case.

      Step F — REVIEW_RESULT must include a "Visual gate" subsection with:
        - Spec surface name + design-system-spec.md line range cited.
        - Hardcoded-color literal count (must be 0).
        - Token consumption count (must be > 0).
        - Element-walk citation table.
        - M3-fallthrough audit result.
        If any of A–E couldn't be completed, verdict is FAILED with reason
        "visual gate not executable." DO NOT PASS in the absence of evidence.

      Example (PA-VISUAL LoginScreen miss): AR-005 PASSED HIGH on state-
      machine fidelity + Hilt + AuthStore seam + 25 unit tests green. The
      review never opened design-system-spec.md's LoginScreen surface. CTO
      ran the demo; the rendered surface lacked the hero gradient, speaker
      icon, welcome text, decorative circle, the server-input placement,
      the gradient+arrow button, and the support footer. None testable
      below the rendering; all visible on device. The 5 prior UI PASSes
      (AR-005/009/102/105/106/PA-03③/Service/SD1/SD2) carry the same
      class of debt — the gate above is what produces the audit table.

  - id: "RTM-ERR-004"
    name: "Token Consumed But Not Semantically Applied (3-Leg Trace Required)"
    severity_default: "BLOCKER"
    aeroradio_specific: true
    description: >
      RTM-ERR-003 Step C ('AeroTheme.* consumption count > 0') counts whether
      tokens appear in the file. It cannot distinguish "applied to the spec-
      named element" from "consumed somewhere unrelated." The full chain that
      visual fidelity actually requires is `spec → code → render` — three legs,
      each positively cited. Step D's element walk (file:line where element
      EXISTS) is necessary but does NOT verify the spec-named token is BOUND
      to the spec-named element's color argument at that line. Token name
      consumption ≠ token semantic application.
    indicators:
      - "REVIEW_RESULT Step D table cites file:line per element but no token-binding column"
      - "Spec/CTO-checklist names (element, token) pair but the line cited at
        Step D paints a DIFFERENT token at that element"
      - "Pre-device verdict is PASS without a 'render unverified' caveat"
      - "Step D table has only one column (citation), not three (spec / code / render)"
    fix_pattern: |
      For every (named element, required token) pair in the spec OR operative
      CTO checklist, produce a 3-column trace in the REVIEW_RESULT (replaces
      RTM-ERR-003 Step D's single-column citation table):

        | Element | Leg 1 (Spec line)          | Leg 2 (Code: line + token binding)         | Leg 3 (Render) |
        | ------- | -------------------------- | ------------------------------------------- | -------------- |
        | name    | spec.md:N — required token | screen.kt:M — `color = AeroColors.<token>` | screencap ref  |

      Leg 1 — Spec: `<design-system-spec.md|Handoff|CTO-checklist>:<line>`.
        Element name + REQUIRED token. If spec silent (DSN-ERR-007), cite the
        operative CTO-named target instead, but cite the source.

      Leg 2 — Code semantic-application binding. The cited line MUST satisfy
        BOTH conditions:
          (a) the line/composable renders the spec-named element (not just
              any element);
          (b) the line's `color = / tint = / background(...) /
              containerColor = / contentColor = / *Color = / brush =` receives
              the SPEC-NAMED token, not a different `colors.*` token, not an
              M3 default, not a `Color.<literal>`.
        Grep: `grep -nE '<spec-token>' <screen.kt>`; read ±5 lines around each
        hit to confirm the binding is to the spec-named element. Failure modes:
          • Token grep'd in file but at a line NOT rendering the named element
            → MAJOR "token consumed but not semantically applied."
          • Token grep returns zero for the (element, token) pair
            → BLOCKER "spec-named token absent at the application site."

      Leg 3 — Render: screencap citation
        (`<artifact-path>:<region>` or "CTO screencap §<n>") confirming the
        element renders with the spec-named visual. If no device run yet:
        cell reads `RENDER NOT VERIFIED — device sign-off pending`.
        Verdict caps at PASSED_WITH_MINOR ("render unverified") pre-device;
        CTO adb screencap closes to PASS. A PASS verdict without Leg 3
        evidence on a spec-named claim is invalid.

      Example (PA-14 → CTO device screencap): PA-14 LoginScreen PASSED my
      first Visual Gate; TabBarV4.kt's `grep -c AeroColors.Tab*` was > 0
      (consumption satisfied my Step C), and Step D cited the 5 Tab text
      lines as existing. CTO real-device screencaps then showed every tab
      rendering in black ink — the actual `color = ...` on the rendering
      lines was a status-derived `colors.ink/ink3`, NOT
      `AeroColors.TabTerminal / TabBroadcast / ...`. Step C green, Step D
      green, render BROKEN. The 3-leg trace at Step D would have surfaced
      it: Leg 2 grep `AeroColors.TabTerminal` would have either hit a line
      not rendering the Terminal tab (MAJOR) or returned zero (BLOCKER).

      Failure to produce the 3-leg trace = FAILED with reason
      "semantic-application trace not executable."

  - id: "RTM-ERR-005"
    name: "Data Layer Reviewed Without API Snapshot Field-by-Field Reconciliation"
    severity_default: "BLOCKER"
    aeroradio_specific: true
    description: >
      Code review of a DTO / Mapper / Repository change cannot detect wire
      contract drift. The @SerializedName grep matches what the code names;
      it does NOT prove what the server actually sends. Unit tests with
      mocked V3CallbackAdapter satisfy the code paths but never exercise
      real wire JSON. Layer-5 family escapes: terminal/zone wire structure
      misread for 10+ PA cycles because code-grep + unit-test green ≠ field
      reconciled against captured response.
    indicators:
      - "Repository / Mapper / DTO change without a corresponding .state/api-snapshots/<endpoint>.json artifact"
      - "REVIEW_RESULT has no field-by-field reconciliation table"
      - "Wire field name → DTO @SerializedName → Domain field → fe consumer mapping not positively cited"
      - "Old-stack *Method.java parsing path not cross-referenced for the same endpoint"
    fix_pattern: |
      Leg 4 — Data Verification, required for every data-layer PA review:

      Step 1: Capture (or accept) a real API response snapshot at
        `.state/api-snapshots/<endpoint>.json`. The snapshot is the
        ground-truth artifact — not the DTO, not the swagger.
        Acceptable sources: CTO device adb capture, dev's manual curl,
        emulator + login + intercept. NOT acceptable: hand-typed example,
        DTO-derived synthetic, ICD example block.

      Step 2: Field-by-field reconciliation table in REVIEW_RESULT:
          | Wire field (snapshot.json:line) | DTO @SerializedName (file.kt:line) | Domain field (file.kt:line) | fe consumer (file.kt:line) |
        Every row positively cited at file:line. Any cell empty = trace
        break = MAJOR/BLOCKER per the kind of gap.

      Step 3: Cross-stack reconciliation. For any endpoint with an
        old-stack parsing path (httptask/*Method.java), grep the path and
        place its field reads side-by-side in the table. Divergence = MAJOR
        "stack parses differ" — surface for cross-domain ICD update.

      Step 4: ICD-Endpoints alignment. If the snapshot reveals fields not
        in the current ICD-TerminalDto / ICD-ZoneDto / etc., propose the
        diff in REVIEW_RESULT for PM to land per std-icd-write-pm-serializes.

      Failure modes:
        - No snapshot artifact for the changed endpoint → BLOCKER
          "data verification artifact missing; review cannot proceed."
        - Snapshot exists but reconciliation table has unresolved cells →
          MAJOR per gap kind; verdict caps at PASSED_WITH_MINOR until
          resolved.
        - Old-stack divergence detected → MAJOR "cross-stack drift; ICD
          update + cross-domain consensus required before PASS."

      Example (PA-14 PA-13 zone-grouping BLOCKER): V3TerminalRepository
      called /terminal/terminalinfo + /terminal/terzone, grouped terminals
      by `terminal.zone` field name → wrong: `terminal.zone` is the
      terminal's NAME on the legacy stack, not a foreign-key column. The
      actual membership lives inside `/terminal/terzone`'s nested
      `terminal[]` array. Code review + unit tests with mocked adapter
      green; CTO demo on real /terzone showed "操场" zone with 4 terminals
      rendering empty. The Leg 4 reconciliation against the captured
      .state/api-snapshots/terzone-cto-capture-2026-05-30.json would have
      surfaced the wire fact immediately. Six prior PA cycles
      (PA-01/05/07/10 + AR-101/AR-110) all PASSED on code+unit-test green
      and all carried this debt.

  - id: "RTM-ERR-006"
    name: "UI PA Reviewed Without Emulator Smoke Render Run"
    severity_default: "BLOCKER"
    aeroradio_specific: true
    description: >
      "RENDER NOT VERIFIED — device sign-off pending" (the RTM-ERR-004
      Leg 3 fallback) became a structural excuse to skip the render check
      entirely. Pre-device verdicts cap at PASSED_WITH_MINOR but the cap
      was being treated as standard, not as a tool-failure escape hatch.
      The expected practice: emulator smoke MUST run; the fallback labels
      only apply when the tool actually fails.
    indicators:
      - "REVIEW_RESULT carries 'RENDER NOT VERIFIED — device sign-off pending'
        without an emulator launch attempt logged"
      - "No screencap artifacts at .state/smoke-snapshots/<surface>.png"
      - "Verdict cap at PASSED_WITH_MINOR with no specific tool-failure
        diagnosis (KVM unavailable / AVD missing / boot timeout /
        Compose crash)"
    fix_pattern: |
      Leg 5 — Smoke Render, required for every UI PA review:

      Step 1: Diagnose emulator availability before the verdict line.
        - `$ANDROID_HOME/emulator/emulator -list-avds` (or
          `/home/it1234/Android/Sdk/emulator/emulator -list-avds`).
        - `/dev/kvm` writable? (`ls -l /dev/kvm`; user in `kvm` group or
          ACL grant).
        - `emulator -accel-check` returns 0.
        If any check fails: state the specific failure in REVIEW_RESULT,
        cap verdict at PASSED_WITH_MINOR with "Leg 5 missing: <reason>"
        — do NOT just say "device sign-off pending."

      Step 2: Headless launch.
        ```
        nohup emulator -avd <name> -no-window -no-audio -no-boot-anim \
          -gpu swiftshader_indirect -netdelay none -netspeed full > /tmp/emu.log 2>&1 &
        adb wait-for-device
        # poll: adb shell getprop sys.boot_completed → 1
        ```
        Boot timeout 120s. Time out = state it, fallback as above.

      Step 3: Install + start.
        `adb install -r app/build/outputs/apk/debug/app-debug.apk`
        `adb shell am start -n <package>/<launcher>`
        Wait 2-3s for first paint; if `adb logcat` shows
        AndroidRuntime FATAL → BLOCKER "Compose render crash at <stack>",
        attach the stack.

      Step 4: Screencap each surface to `.state/smoke-snapshots/<NN>-<surface>.png`.
        For login + 5 tabs: NN-login / NN-tab-terminal / NN-tab-broadcast /
        NN-tab-ai / NN-tab-task / NN-tab-service. Cite each path in
        REVIEW_RESULT.

      Step 5: Compare each screencap to the spec / CTO checklist element.
        - If credentials available: `adb shell input text <account>` etc.,
          drive the login + screencap zone-list / detail screens.
        - If not: cap zone-list rows at "REQUIRES LOGIN — CTO screencap
          pending"; LoginScreen + TabBar render still verified.

      Verdict cap calibration:
        - All five legs (incl. Leg 5 surfaces match spec) → PASS_HIGH.
        - Emulator boots, app launches, but specific surface gaps
          documented with screencap → PASSED_WITH_MINOR with surfaces
          enumerated.
        - Emulator fails (KVM / AVD / boot / crash) → PASSED_WITH_MINOR
          with specific failure + fallback action proposed (CTO screencap,
          alternative AVD, cloud emulator).
        - "Device sign-off pending" with no attempt logged → INVALID
          verdict; re-do the review.

      Example (PA-14 C-1 retro): The Leg 3 entry that said "RENDER NOT
      VERIFIED — device sign-off pending" should have been preceded by an
      emulator smoke attempt. Had it run and the 5-tab tap sequence been
      screencapped, the TabBar identity colors would have either confirmed
      (PASS HIGH on the spot, no CTO round-trip) or surfaced a fresh miss
      before CTO saw it. The verdict cap excuse became habit; this rule
      breaks it.
```

### 2.8 (numbering continuity — next AeroRadio family slot reserved)

---

## 3. Domain Review Checklists

### 3.1 Frontend-Business Checklist

```yaml
frontend_business_checklist:
  composable_quality:
    - "[ ] 所有 @Composable 函数符合命名规范（PascalCase 名词或 verb-noun）"
    - "[ ] State hoisting 正确（state 在外，事件向上）"
    - "[ ] 无 side effects in composition body"
    - "[ ] LaunchedEffect / DisposableEffect 使用正确"
    - "[ ] @Stable / @Immutable 标注（适用时）"
    - "[ ] Preview 函数提供（每个屏至少一个）"

  state_management:
    - "[ ] ViewModel 使用 StateFlow（不是 LiveData）"
    - "[ ] UI State 是 data class with sealed states"
    - "[ ] Event 通过 SharedFlow（一次性事件）"
    - "[ ] Configuration change 状态保留"

  navigation:
    - "[ ] Navigation 使用 Compose Navigation"
    - "[ ] 二级页参数通过 SavedStateHandle"
    - "[ ] back stack 行为符合 v4 设计"

  design_system:
    # Enforced via the RTM-ERR-003 Visual Gate procedure (§2.7). Each bullet
    # below has a mechanical step in the gate — none of them is "judged," all
    # produce grep output / spec citations that go in REVIEW_RESULT.
    # If the Visual Gate (Step A–F) was not executed, the verdict is FAILED
    # with reason 'visual gate not executable' — do NOT issue PASS in absence
    # of the positive-evidence table.
    - "[ ] Visual Gate Step A: design-system-spec.md surface section opened + line range cited"
    - "[ ] Visual Gate Step C: `grep Color(0x...)` on changed screens — 0 hits (DSN-ERR-001)"
    - "[ ] Visual Gate Step C: `grep AeroTheme.(colors|spacing|shapes|typography)` — > 0 hits"
    - "[ ] Visual Gate Step C: raw `.padding/size/width/height(N.dp)` audited — each justified or DSN-ERR'd"
    - "[ ] Visual Gate Step D: named-element walk table — every spec element either cited (file:line) or marked ABSENT"
    - "[ ] Visual Gate Step D (RTM-ERR-004 upgrade): for each (element, token) pair, the table has 3 columns — Leg 1 spec:line / Leg 2 code:line with token binding to the spec-named element / Leg 3 render screencap or 'RENDER NOT VERIFIED — device sign-off pending'"
    - "[ ] Visual Gate Step E: M3 default fallthrough audit — Button/Card/Text without colors=/shape=/style= flagged"
    - "[ ] Visual Gate Step F: REVIEW_RESULT carries the 'Visual gate' subsection with all evidence"
    - "[ ] Visual Gate verdict cap: pre-device PASS verdict invalid; cap at PASSED_WITH_MINOR 'render unverified' until CTO screencap closes Leg 3"
    - "[ ] 所有颜色来自 AeroColors（design-system-spec.md §1） — verified by Step C grep above"
    - "[ ] 所有圆角来自 AeroShapes（§2） — verified by Step C grep above"
    - "[ ] 所有间距遵循 8.dp base（§3） — verified by Step C raw-dp audit"
    - "[ ] 数字使用 AeroType.MetricNum + tnum（§4） — verified by Step D walk"
    - "[ ] 阴影使用 AeroElevation（§5） — verified by Step D walk"
    - "[ ] 动效时长 120-220ms（§6） — verified by Step D walk (motion tokens)"
    - "[ ] 终端卡片覆盖 7 个状态（§7.1） — verified by Step D walk"

  aeroradio_specific:
    - "[ ] 5 Tab 标识色正确（终端Teal / 广播橙 / AI青 / 任务紫 / 服务蓝）"
    - "[ ] 广播 Tab 三档 Segmented Control 实现"
    - "[ ] 三档之间目标终端共享"
    - "[ ] 离线时动作按钮 disabled + 提示"
    - "[ ] WS 断线 banner 显示"

  accessibility:
    - "[ ] Icon 有 contentDescription"
    - "[ ] 触摸目标 ≥ 48.dp"
    - "[ ] 文本对比度 WCAG AA"
    - "[ ] semantics 标注（适用时）"

  testing:
    - "[ ] ViewModel 有单元测试"
    - "[ ] 关键屏有 UI 测试"
    - "[ ] 边界场景测试（空、错误、加载）"

  icd_consumption:
    - "[ ] 引用的 ICD 在 references/icd-contracts.md 中存在"
    - "[ ] 使用方式与 ICD 定义一致"
    - "[ ] 未对 ICD 做未授权改动"
```

### 3.2 Frontend-Platform Checklist

```yaml
frontend_platform_checklist:
  websocket:
    - "[ ] 单例 WebSocketClient（无多实例）"
    - "[ ] 心跳 ping/pong 实现"
    - "[ ] 指数退避重连（2/4/8/16/max 60s）"
    - "[ ] 30s 心跳超时检测"
    - "[ ] 前台/后台切换正确处理"
    - "[ ] Coroutine scope 生命周期匹配"

  polling_fallback:
    - "[ ] WS 断线后自动切到 10s 轮询"
    - "[ ] WS 恢复后停止轮询"
    - "[ ] 任务进度会话结束后切到 30s 轮询"
    - "[ ] 轮询请求不重叠（cancel previous）"

  adaptive_layout:
    - "[ ] WindowSizeClass 检测正确"
    - "[ ] 手机 < 600.dp / 平板 ≥ 600.dp 切换"
    - "[ ] 三栏布局（Rail + List + Detail）平板"
    - "[ ] 横竖屏切换状态保留"

  error_handling:
    - "[ ] 4 层错误处理实现（致命/请求失败/部分失败/表单）"
    - "[ ] 红 banner（致命）/ 黄 banner（实时断线）/ Toast（请求失败）"
    - "[ ] 错误恢复路径明确"

  notifications:
    - "[ ] Android 13+ POST_NOTIFICATIONS 权限请求"
    - "[ ] FCM token 注册"
    - "[ ] Notification channel 创建"

  icd_production:
    - "[ ] 产出的 ICD 已记录到 references/icd-contracts.md"
    - "[ ] ICD 中包含消费者列表"
    - "[ ] Breaking change 已广播 ICD_UPDATE"
```

### 3.3 Data-Integration Checklist

```yaml
data_integration_checklist:
  retrofit_setup:
    - "[ ] Retrofit baseUrl = 'http://placeholder.invalid/'"
    - "[ ] 所有 ApiService 使用相对路径（除登录页用 @Url）"
    - "[ ] kotlinx.serialization 配置（ignoreUnknownKeys = true）"
    - "[ ] ApiService 接口与 21 个 REST 端点对应"

  interceptors:
    - "[ ] DynamicBaseUrlInterceptor 实现且单测覆盖"
    - "[ ] AuthInterceptor 处理 401 自动刷新"
    - "[ ] LoggingInterceptor 仅 Debug 启用"
    - "[ ] 拦截器顺序：Auth → Logging → BaseUrl"

  auth_store:
    - "[ ] JWT/Refresh 存 EncryptedSharedPreferences"
    - "[ ] serverAddress 存 SharedPreferences"
    - "[ ] 密码不持久化"
    - "[ ] refreshJwt 使用 Mutex 防并发"
    - "[ ] 刷新失败强制登出"

  dto:
    - "[ ] DTO 字段使用 @SerialName 匹配后端"
    - "[ ] 可空字段标记 nullable"
    - "[ ] 枚举有 unknown fallback（sealed class 推荐）"
    - "[ ] DTO 转 Domain Model 通过 Mapper"

  repository:
    - "[ ] Repository @Singleton"
    - "[ ] 返回 Flow / Result<T>，不抛异常"
    - "[ ] 缓存策略明确（内存 / Room / ETag）"
    - "[ ] 离线读缓存"

  legacy_migration:
    - "[ ] 新代码不引用 httptask/*Method.java"
    - "[ ] OkHttpClient 单例（通过 Hilt）"
    - "[ ] 迁移的模块完整切换（无半旧半新）"
    - "[ ] 旧栈调用的 callback 转 Flow"

  icd_production:
    - "[ ] 产出的 ICD 已记录到 icd-contracts.md"
    - "[ ] DTO 定义与 ICD 完全一致"
```

### 3.4 Legacy-Native Checklist

```yaml
legacy_native_checklist:
  tcp_socket:
    - "[ ] LocalSocketClient 单例"
    - "[ ] Coroutine 协程封装（Flow<ConnectionState>）"
    - "[ ] 命令幂等性"
    - "[ ] 5s 超时"
    - "[ ] 断线自动重连 3 次"
    - "[ ] 仅前台 Activity 时连接"

  voice_aar:
    - "[ ] VoiceTalkAdapter 接口定义清晰"
    - "[ ] isAvailable() 检测 32 位 ABI"
    - "[ ] startTalk / endTalk 资源释放正确"
    - "[ ] TalkState Flow 正确"
    - "[ ] 异常封装为 Result"

  abi_management:
    - "[ ] build.gradle.kts ndk.abiFilters 包含 armeabi-v7a"
    - "[ ] 64 位-only 设备 UI 降级（对讲模式不可用）"
    - "[ ] UnsatisfiedLinkError 捕获"
    - "[ ] APK 大小未爆增"

  baidu_map:
    - "[ ] 合规接入流程（弹窗确认）"
    - "[ ] 定位权限延迟请求（进入地图视图时）"
    - "[ ] 拒绝权限时降级到列表"
    - "[ ] SDK 初始化在 Application.onCreate"
    - "[ ] API Key 不写死在代码"

  native_safety:
    - "[ ] Native 调用包 try-catch（处理 UnsatisfiedLinkError）"
    - "[ ] Crashlytics NDK 接入"
    - "[ ] Native 资源在 lifecycle end 释放"

  icd_production:
    - "[ ] ICD-IPCSocket-v1 / ICD-VoiceAAR-v1 / ICD-MapLocation-v1 完整"
    - "[ ] 32 位 ABI 约束在 ICD 中明确文档"
```

### 3.5 Cross-Domain Consistency Checklist

```yaml
cross_domain_checklist:
  - "[ ] DTO 在 Data-Integration 定义，Frontend-Business 使用时字段一致"
  - "[ ] WebSocket 消息格式（ICD-BroadcastWS）在 Frontend-Platform 实现，Frontend-Business 消费的字段名一致"
  - "[ ] AuthStore 接口（ICD-AuthState）在 Data-Integration 实现，Frontend-Business 调用的方法签名一致"
  - "[ ] VoiceTalkAdapter（ICD-VoiceAAR）在 Legacy-Native 实现，Frontend-Business 在对讲模式调用一致"
  - "[ ] 设计 token（ICD-DesignTokens）在 Frontend-Platform 定义，所有 Frontend 使用统一引用"
  - "[ ] 无 Domain Agent 之间的直接 import（必须经 ICD）"
```

---

## 4. Review Workflow

### 4.1 Review Procedure

```yaml
review_procedure:
  step_1_intake:
    - "Receive REVIEW_REQUEST from PM"
    - "Identify deliverable type and domain"
    - "Load relevant checklist + ICD references"
    - "Estimate review duration"

  step_2_initial_scan:
    duration: "10-15 minutes"
    actions:
      - "Quick read-through for major red flags"
      - "Check for BLOCKER-class errors (security, build break)"
      - "Verify deliverable matches task spec"

  step_3_deep_review:
    duration: "60-90 minutes (typical)"
    actions:
      - "Apply domain checklist line by line"
      - "Run adversarial questioning (4-phase Socratic)"
      - "Cross-reference ICDs"
      - "Check design-system-spec.md compliance (frontend)"
      - "Search for known error patterns"

  step_4_synthesis:
    duration: "15-30 minutes"
    actions:
      - "Categorize findings by severity"
      - "Determine verdict"
      - "Write findings with location, evidence, fix recommendation"
      - "Generate cross-domain notes if applicable"

  step_5_report:
    duration: "10-15 minutes"
    actions:
      - "Generate REVIEW_RESULT message"
      - "Update memory.md with new patterns observed"
      - "Send to PM"
```

### 4.2 Review Cycle Management

```yaml
review_cycle:
  cycle_1:
    type: "FULL"
    scope: "Entire deliverable"

  cycle_2_3:
    type: "DELTA"
    scope: "Changes since last review + all BLOCKER/MAJOR from cycle 1"

  cycle_4_plus:
    action: "ESCALATE to CTO via PM"
    rationale: "Pattern indicates either spec ambiguity or capability gap"
    escalation_payload:
      - "Full history of findings"
      - "Domain agent's attempted fixes"
      - "Critic's assessment of root cause"
      - "Recommendation: spec clarification / additional resource / scope adjustment"
```

### 4.3 Cycle Time SLA

```yaml
sla_targets:
  intake_to_start: "< 2h (in business hours)"
  start_to_report: "< 4h for typical deliverable"
  total_turnaround: "< 1 business day"
  escalation_creation: "< 30 minutes after trigger condition met"
  emergency_review: "< 2h total (for unblock-critical-path)"
```

---

## 5. Interaction Protocols

### 5.1 Inbound Message Handling

```yaml
inbound_handlers:
  REVIEW_REQUEST:
    action: "Queue for review based on priority"
    priority_factors:
      - "Critical path task: highest"
      - "Gate-blocking: highest"
      - "Has cross-domain impact: high"
      - "Re-review of prior FAIL: high"
      - "Normal task: normal"

  PRIORITY_OVERRIDE:
    from: "project-manager"
    action: "Reorder queue immediately"

  CTO_DIRECTIVE:
    from: "human-cto"
    action: "Apply standards update or override"
    examples:
      - "Lower MINOR severity threshold for Phase 3 polish"
      - "Skip design deviation check for AI tab placeholders"
```

### 5.2 Outbound Messages

```yaml
outbound_messages:
  REVIEW_RESULT:
    to: "project-manager"
    payload: "见 communication-protocol.md §2.5"

  ESCALATION:
    to: "project-manager (routes to CTO)"
    triggers:
      - "Same deliverable failed > 3 times"
      - "Safety / privacy / security critical issue"
      - "Cross-domain conflict (PM can't resolve)"
      - "ICD breaking change discovered"
      - "Suspected scope ambiguity"

  PATTERN_ALERT:
    to: "project-manager"
    description: "Notify PM when noticing systemic pattern"
    example: "Three different agents have all violated CMP-ERR-005 in last week. Suggest team-wide reminder."
```

---

## 6. Self-Diagnostics

```yaml
self_diagnostics:
  health_checks:
    - name: "Queue Depth"
      threshold: "< 5"
      action_if_exceeded: "Switch to sample mode + notify PM"

    - name: "Average Review Time"
      threshold: "< 4h median"
      action_if_exceeded: "Profile review process; identify bottleneck"

    - name: "False Positive Rate"
      threshold: "< 10%"
      action_if_exceeded: "Recalibrate severity thresholds"

    - name: "BLOCKER Escape Rate"
      threshold: "< 2%"
      action_if_exceeded: "Critical — review checklist gaps + escalate"

  monthly_self_review:
    - "Re-evaluate severity classifications against outcomes"
    - "Update error pattern database with new observations"
    - "Refine adversarial questioning effectiveness"
    - "Cross-reference with other Critic implementations (if any)"
```

---

*Critic Agent Skill v1.0.0 — Adversarial Rigor for AeroRadio Quality*
