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
    - "[ ] 所有颜色来自 AeroColors（design-system-spec.md §1）"
    - "[ ] 所有圆角来自 AeroShapes（§2）"
    - "[ ] 所有间距遵循 8.dp base（§3）"
    - "[ ] 数字使用 AeroType.MetricNum + tnum（§4）"
    - "[ ] 阴影使用 AeroElevation（§5）"
    - "[ ] 动效时长 120-220ms（§6）"
    - "[ ] 终端卡片覆盖 7 个状态（§7.1）"

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
