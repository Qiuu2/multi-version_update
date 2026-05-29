# V3 Codebase Audit Report — AeroRadioControl

> 作者：Project Manager Agent · 日期：2026-05-27 · 分支：`claude/v4-screens-on-refactor`
> 目的：为 Gate 1（Phase 0 计划批准）提供事实基线。所有数字来自本次实扫，未做估算。

---

## 0. Executive Summary（给 CTO 的一页纸）

| 维度 | 现状 | 信号 |
|------|------|------|
| 代码规模 | 250 个 `.java` + 46 个 `.kt`（其中 ui/ 下约 41 个 .kt） | 旧栈仍是绝对主体 |
| UI 双轨 | 28 个 Activity + 6 个 Fragment（旧 XML）↔ 30 个 `@Composable` 文件（新 v4） | 新旧 UI 并存，入口未收敛 |
| 新栈深度 | Hilt/Retrofit/Room/Compose 依赖**已就位**，但业务端点迁移 = **0** | 地基浇了，墙没砌 |
| Mock→Real | **0/5 Tab** 接真数据（全部跑 MockData） | Phase 1 尚未真正开始 |
| **32 位 ABI（头号风险）** | **`htapplib.aar` 实际已含 `arm64-v8a` 原生库** | ⚠ 与项目假设矛盾，可能反转 R-001 |
| 安全债 | 明文 HTTP（已知）+ 双 LAUNCHER + `sharedUserId` + 硬编码签名口令 | 多项需决策 |

**最重要的一句话**：项目档案（CLAUDE.md / SKILL / PM-memory）把"AAR 只有 32 位"列为头号 CRITICAL 风险（R-001，对讲模式可能完全不可用）。**实扫结果与此矛盾——AAR 里有 64 位（arm64-v8a）的 `libaudioplay.so` 和 `libmp3lame.so`。** 若验证通过，最高危风险大幅下降。详见 §4 与 §7。

---

## 1. 旧栈文件清单

### 1.1 网络旧栈（核心迁移对象）

旧栈网络层不止 `httptask/*Method.java`，实际由 6 个文件构成（约 1233 LOC）：

| 文件 | LOC | 角色 | 迁移定性 |
|------|-----|------|---------|
| `httptask/RequestManger.java` | 298 | OkHttp 3 封装 + 回调分发核心 | 全栈替换为 Retrofit + 拦截器 |
| `httptask/TaskMainMethod.java` | 592 | `/task/*` 全部端点的手拼 URL 调用 | 迁移到 `TaskRepository.kt` |
| `httptask/ZoneMethod.java` | 218 | `/terminal/zone*`、`/terminal/terzone` 调用 | 迁移到 `TerminalRepository.kt` |
| `httptask/MyRequestBuilder.java` | 55 | 请求构造辅助 | 随 RequestManger 一并删 |
| `httptask/onRequestLister.java` | 9 | 回调接口 | 协程 `suspend` 取代 |

> 命名提示：CTO 任务书写"`httptask/*Method.java` 全部列出"。命名带 `Method` 的共 3 个——`httptask/TaskMainMethod.java`、`httptask/ZoneMethod.java`、`method/MainMethod.java`。**但 `method/MainMethod.java` 经核实不是 REST 调用方**（详见 §3.1 更正）。故旧栈网络层实为上表 5 个文件 ~982 LOC。

> ⚠ **更正（2026-05-27，端点溯源时发现）**：本报告初稿将 `method/MainMethod.java`（61 LOC）列为"网络旧栈"并标注调用 `/authorizations` 等端点，**有误**。实读其源码：它封装的是**原生 AAR 语音指令**（`HTIntf.startpaging / startondemand / startspeech`，即寻呼/点播/对讲的 native 入口），与 REST 无关，应归入 Legacy-Native 域而非 Data-Integration 迁移范围。此修正同时是 `TASK-AR-SPIKE-001` 的相关上下文（spike 要测的 native 方法即 `startspeech/startpaging` 一族）。详见 `.state/endpoint-inventory-draft.md` §0.2。

### 1.2 旧 UI 栈（28 Activity + 6 Fragment）

`activity/`（28）：`ActivityMain, ActivityMusicOrder, AddCaiboActivity, AddFileBroadActivity, AddSchemeActivity, AddSchemeTaskActivity, AddTerminalAmplifierActivity, AddWenziyuyinActivity, AddZoneActivity, BeCallActivity, BeSpeechByOtherActivity, CallOtherActivity, CaptureActivity, ConnectActivity, LocationInMapActivity, LoginActivity, SelectMusicActivity, SettingActivity, SignActivity, TaskGuangboActivity, TaskGuangboDetailActivity, TaskRuningActivity, TaskZuoxiActivity, TaskZuoxiDetailActivity, TaskZuoxiListActivity, TempTTSActivity, ZoneDetailActivity, ZoneManageActivity`

`fragment/`（6）：`FragmentDianBo（点播）, FragmentDuiJiang（对讲）, FragmentRenwu（任务）, FragmentShortcutTask, FragmentXunHu（寻呼）, TabFragment`

> `FragmentXunHu/DuiJiang/DianBo` = 旧版"寻呼/对讲/点播"三档，正是 v4 `BroadcastScreen` 要聚合替代的对象。

### 1.3 其他旧栈包（概览，非本次重点）

`adapter/ base/ component/ datautil/ model/{event,request,response} receiver/ service/ utils/{map,...} widget/{dialog,popwindow,view,viewpager}` —— 仍全 Java，依赖 RxJava2 / EventBus / Glide / LRecyclerView 等旧库。`service/` 含 `TalkService`、`UpFileService`、`ReUpFileService`（旧广播/上传服务）。

---

## 2. 21 个 REST 端点：已实现 vs 缺失（对照 `constant/Constant.java`）

### 2.1 关键结论

- **新栈（Retrofit）实现的业务端点 = 0。** `data/api/` 下唯一的 `HealthApiService.ping(@Url)` 是连通性探测，**不对应** `Constant.java` 任何业务端点。
- 全部业务端点目前**仅由旧栈**（RequestManger + *Method.java）调用。
- `Constant.java` 实际定义的**不同路径约 30 个**（部分路径按 HTTP 方法复用，如 `/task/taskinfo` 同时承担 GET/PUT/POST/DELETE）。"21 个端点"应理解为**规范化后的资源动作集**——此口径需在 ICD 中钉死，否则 Data-Integration 与 Critic 会对不齐。**建议 Gate 1 顺带确认端点权威清单。**

### 2.2 端点盘点（按资源分组，状态对照新栈）

| 端点路径 | 用途 | 旧栈 | 新栈(Retrofit) |
|---------|------|:----:|:----:|
| `/authorizations` | 登录换 JWT | ✅ | ❌ |
| `/terminal/terminalinfo` | 全部终端 | ✅ | ❌ |
| `/terminal/terminaldo/` | 终端操作 | ✅ | ❌ |
| `/terminal/gitude/` · `/terminal/savegitude` | 终端经纬度 读/存 | ✅ | ❌ |
| `/terminal/zoneterminal` | 分区↔终端 (GET/POST) | ✅ | ❌ |
| `/terminal/terzone` | 分区 增/查/删 (POST/GET/DELETE) | ✅ | ❌ |
| `/terminal/mediainfo` · `/terminal/mediainfo/` | 媒体 列表/按类型/上传 | ✅ | ❌ |
| `/terminal/mediafolderinfo` | 媒体文件夹 | ✅ | ❌ |
| `/terminal/urgentplay` | 紧急播放 | ✅ | ❌ |
| `/terminal/terminalquicktask` · `/terminal/shortcutkey` | 快捷任务/快捷键 | ✅ | ❌ |
| `/task/taskinfo` | 任务 增删改查 | ✅ | ❌ |
| `/task/sechinfo` · `/task/sechetask` · `/task/sechetaskinfo` | 作息方案 列表/任务/方案 | ✅ | ❌ |
| `/task/sechenableordisable` | 作息启停 | ✅ | ❌ |
| `/task/ttstaskinfo` · `/task/ttstaskcontent` | TTS 任务 信息/内容 | ✅ | ❌ |
| `/task/taskterminal` · `/task/taskmusic` | 任务↔终端 / 任务↔媒体 | ✅ | ❌ |
| `/task/taskenordis` · `/task/taskdoorno` · `/task/taskvolume` | 方案 运行/启停/音量 | ✅ | ❌ |
| `/task/gettempttstask` · `/task/deltemptts` · `/task/addtempttstask` · `/task/addtempttstaskmedia` | 临时(文件)任务 增删查+媒体 | ✅ | ❌ |
| `/server/serverstate` | 系统健康度 | ✅ | ❌ |

**Mock→Real 迁移度：0%。** 这是 Phase 1 的全部工作量所在。

---

## 3. `libs/htapplib.aar` 与 ABI 检查

### 3.1 文件存在性

`app/libs/htapplib.aar` 存在（539 KB，2026-05-14 更新）。注意：实际位置是 **`app/libs/`** 而非 CLAUDE.md 写的根 `libs/`。

### 3.2 ⚠ ABI 实扫（与项目假设直接冲突）

`unzip -l app/libs/htapplib.aar` 的 `jni/` 内容：

| ABI 目录 | `libaudioplay.so` | `libmp3lame.so` | 完整性 |
|----------|:----:|:----:|------|
| `jni/arm64-v8a/` | ✅ 7.3 KB | ✅ 336 KB | **完整（64 位！）** |
| `jni/armeabi-v7a/` | ✅ 5.3 KB | ✅ 428 KB | 完整（32 位） |
| `jni/armeabi/` | ❌ 缺 | ✅ 501 KB | 不完整 |
| `x86 / x86_64` | — | — | **不存在** |

**结论：AAR 含 64 位（arm64-v8a）原生库，且语音两个 .so 齐全。** 这与 CLAUDE.md「AAR 只有 armeabi-v7a + x86」、PM-memory `RISK-AR-003`、以及当前风险登记册 `R-001` 的前提**全部矛盾**。

### 3.3 与 `build.gradle` 配置的二次矛盾

`app/build.gradle` 现配置 `abiFilters = ["armeabi-v7a", "arm64-v8a"]`，但其**注释仍声称**"armeabi-v7a is the htapplib.aar's only ABI … Voice intercom will not work on arm64 until the aar vendor ships 64-bit native libs"。即：**注释滞后于产物**——AAR 已被更新（5/14）补入 64 位库，但代码注释与项目档案未同步。

> 注意：`x86/x86_64` 仍缺失 → 64 位**模拟器**（多为 x86_64）跑语音仍会 `UnsatisfiedLinkError`，但真机（arm64）不受影响。

---

## 4. 现有 Compose / View 屏占比

### 4.1 数量

- **新（Compose）**：`ui/` 下约 41 个 `.kt`，含 30 个带 `@Composable` 的文件。
- **旧（View/XML）**：28 Activity + 6 Fragment。
- 粗略屏级占比：新 ~13 个一/二级屏 vs 旧 34 个 Activity/Fragment ≈ **新 28% : 旧 72%**（按屏数，非 LOC）。

### 4.2 v4 Compose 资产（已落地）

| 分组 | 文件 |
|------|------|
| theme（设计 Token，**已建**） | `AeroTheme, Color, Type, Spacing, Shape, Elevation, Gradients, Motion`（8） |
| scaffold | `MainScaffold, AppNavGraph, AppRoutes` |
| components/atoms | `MButton, MChip, MInput, MSwitch, StatusPill` |
| components/molecules | `BackTopBar, EmptyState, FabBar, HeroStrip, NotificationBanner, Skeleton, TabBarV4, TerminalTile, TopBarV4` |
| screens/auth | `LoginScreen, SplashScreen` |
| screens/terminal | `TerminalHubScreen, ZoneDetailScreen` (+`TerminalMockData`) |
| screens/broadcast | `BroadcastScreen` |
| screens/task | `TaskScreen, SchemeDetailScreen, SchemeEditScreen, ExecutionLogScreen, TempFileBroadcastScreen` (+`TaskMockData`) |
| screens/service / ai / placeholder | `ServiceScreen, AiScreen, TabPlaceholder` |
| 入口 | `ui/V4Activity.kt`（Compose 宿主） |

### 4.3 关键缺口

- **全部 v4 屏跑 Mock**：`TerminalMockData.kt` / `TaskMockData.kt` + 各屏内联 mock，共 10 个屏文件引用 mock。**没有任何屏接 ViewModel→Repository→真数据。**
- **无 ViewModel 层**：`ui/` 下未见 `*ViewModel.kt`（仅 health 样例在 `data/repository`）。屏与数据之间是空的。

---

## 5. `build.gradle` 依赖盘点

### 5.1 工程配置

| 项 | 值 | 备注 |
|----|----|------|
| AGP / 插件 | application + kotlin-android + kapt + hilt | — |
| compileSdk / target | 35 / 35 | 现代 |
| minSdk | 21 | 兼容老设备 |
| Java / Kotlin JVM | 17 / 17 | Phase 0 已升 |
| Compose | `buildFeatures.compose=true`，compiler `1.5.15`，BOM `2024.10.01`，Kotlin `1.9.25` | 版本自洽 |
| `abiFilters` | `armeabi-v7a, arm64-v8a` | 见 §3.3 |
| versionCode / Name | 3 / "3.0" | **仍是 v3 版本号**，v4 发布前需升 |
| 签名 | `debug{}` 内**硬编码** keyAlias/口令 + `signkey/a9006sign.jks` | 安全债（§7） |
| `sharedUserId` | `com.example.htgd`（在 Manifest） | 已废弃，移除需数据迁移 |

### 5.2 依赖分层

**新栈（Phase 0 已引入）**：Hilt `hilt_version`、Retrofit 2.11.0 + converter-gson、OkHttp 4.12.0 + logging、Room 2.6.1、coroutines 1.8.1、Lifecycle/ViewModel 2.8.7、Navigation-Compose 2.8.4、hilt-navigation-compose 1.2.0、Gson 2.10.1；测试 mockk 1.13.13 + turbine 1.2.0 + coroutines-test。

**旧栈（仍在用）**：RxJava2 + rxandroid、EventBus 3.0（jar）、Glide 4.16、`base-rvadapter`、`XTabLayout`、`LRecyclerView`、AVLoadingIndicator、simplezxing、Android-PickerView、customactivityoncrash、multidex。

**本地产物**：`htapplib.aar`、`BaiduLBS_Android.aar`（含两 ABI 的百度 .so 在 `app/libs/{arm64-v8a,armeabi-v7a}/`）；旧 `okhttp-3.2.0.jar`/`okio-1.13.0.jar`/`gson-2.7.jar` 已从 classpath **排除**（仍躺盘上）。

> 双栈共存已是事实（RISK-AR-002）：OkHttp 4（新）与被排除的 OkHttp 3 jar（旧 RequestManger 编译期靠 OkHttp4 向下兼容）。需确保**全局单例 OkHttpClient**，避免连接池浪费。

---

## 6. 横切现状速查（Phase 0/1 关键能力）

| 能力 | 期望（WBS） | 现状 | 缺口 |
|------|------------|------|------|
| 动态 baseUrl 拦截器 | `DynamicBaseUrlInterceptor` | ❌ 未实现，靠 `@Url` 逐调用注入 + `placeholder.invalid` | Phase 0 P0 |
| AuthStore（JWT/Refresh/地址） | `AuthStore` + EncryptedSharedPreferences | ❌ 无；token 仍走旧 `Constring`/`ServerToken`/SharedPreferences | Phase 0 P0 |
| 安全存储 | DataStore / EncryptedSharedPrefs | ❌ 均未引入 | — |
| WebSocket 实时 | `RealtimeClient` + 心跳/重连 | ❌ 代码库零 WebSocket 引用 | Phase 1 P0 |
| 10s 轮询回退 | `PollingFallback` | ❌ 无 | Phase 1 |
| 本机 IPC 4521 | 协程封装 | ⚠ 旧 `utils/SocketClient.java` 存在（被 `SignActivity` 用），未协程化 | Phase 1 P2 |
| OkHttp 单例 | Hilt provide | ✅ `AppModule.provideOkHttpClient`（debug 全 body 日志） | 注意 release 不泄露 token |

---

## 7. 已识别风险点

> 按 PM 规则：高危带缓解项，需 CTO 决策的明确标注。

### 🔴 需 CTO 关注 / 决策

**RISK-AUDIT-01 ｜ 头号风险前提被推翻：AAR 已含 64 位**
- 事实：`htapplib.aar` 有 `jni/arm64-v8a/{libaudioplay,libmp3lame}.so`（§3.2）。项目档案（R-001，概率 0.40 / CRITICAL）假设其只有 32 位。
- 影响：若 64 位库验证可用，**对讲模式不再是发布阻塞项**，关键路径与 Phase 1 风险显著下降；同时无需为"上架 Google Play 64 位强制"发愁（且本项目为校园 LAN 内部分发，未必走 Play）。
- 待办（建议派 Legacy-Native）：① 在 arm64 真机 `System.loadLibrary` 验证符号导出；② 同步修正 build.gradle 注释、CLAUDE.md、SKILL `RISK-AR-003`、PM-memory `R-001`。
- ⚠ 残留：`x86_64` 缺失 → 64 位**模拟器**语音不可用（开发期影响，非发布阻塞）。
- **决策点**：是否将 R-001 从 CRITICAL 降级为 LOW（待真机验证回执后）。

**RISK-AUDIT-02 ｜ 双 LAUNCHER 入口**
- 事实：Manifest 中 `SignActivity`（旧）与 `ui.V4Activity`（新）**都带 `MAIN/LAUNCHER`**，桌面会出现两个图标 / 启动入口不确定。
- 影响：用户困惑；v4 切换策略不清。
- 选项：A. V4Activity 设唯一 LAUNCHER，旧入口降级为内部跳转；B. 保留双入口仅供迁移期对照（需明确退出时机）。
- **决策点**：v4 是否已可作为默认入口。

**RISK-AUDIT-03 ｜ 端点权威清单口径（"21" vs 实际 ~30 路径）**
- 事实：`Constant.java` 不同路径约 30 个，"21 端点"为规范化口径，未在任何 ICD 钉死。
- 影响：Data-Integration 迁移与 Critic 验收会对不齐，ICD churn 风险。
- 缓解：Gate 1 一并产出权威端点表（资源×方法），写入 `references/icd-contracts.md`。

**RISK-AUDIT-04 ｜ 安全债集合（明文 HTTP 之外的新增项）**
- 硬编码签名口令于 `build.gradle`（`keyPassword 'wzq@1993'` 等）；`sharedUserId="com.example.htgd"`（废弃，移除需数据迁移）；`MANAGE_EXTERNAL_STORAGE` + `INSTALL_PACKAGES` + 无障碍服务 + `SYSTEM_ALERT_WINDOW` 等重权限；百度 API Key 明文在 Manifest。
- 缓解：签名移到 `signingConfigs` + `local.properties`/环境变量；权限做合规审查（百度合规弹窗 RISK-AR-007）；release 关闭 OkHttp body 日志（当前仅 debug，✅）。

### 🟡 监控

| ID | 风险 | 现状证据 | 缓解 |
|----|------|---------|------|
| RISK-AUDIT-05 | 双网络栈共存连接池浪费 | OkHttp4(新) + 旧 RequestManger | 强制单例 OkHttpClient（已 Hilt 提供，需让旧栈也复用） |
| RISK-AUDIT-06 | v4 屏全 mock，无 ViewModel 层 | §4.3 | Phase 1 自下而上：Repository→ViewModel→屏 |
| RISK-AUDIT-07 | WS 协议未知（沿用 RISK-AR-006） | 代码零 WS 引用 | Phase 0 即向厂商索要 WS 文档 |
| RISK-AUDIT-08 | IPC SocketClient 未协程化且耦合 SignActivity | `utils/SocketClient.java` | Phase 1 `LocalSocketClient.kt` 封装 |
| RISK-AUDIT-09 | 版本号仍 3.0/code 3 | build.gradle | v4 发布前升版本 |

---

## 8. PM 建议（进 Phase 0 的前置动作）

1. **立即派 Legacy-Native 验证 AAR 64 位可用性**（RISK-AUDIT-01）——这会直接改写关键路径与风险登记册，应在 Gate 1 计划定稿前拿到结论。
2. **Gate 1 计划顺带交付权威端点清单**（RISK-AUDIT-03），作为 `ICD-Endpoints-v1` 草案。
3. **确认 v4 入口策略**（RISK-AUDIT-02）——影响 Phase 1 是否需要"旧屏→新屏"过渡跳转任务。
4. Phase 0 WBS 的两个 P0（`DynamicBaseUrlInterceptor` + `AuthStore`）确认仍是真空地带（§6），是接真数据的唯一前置——优先级正确，无需调整。

> 本报告仅陈述事实与风险，**不替 CTO 决策**。等待 CTO 评审后，PM 将据此出 Phase 0 WBS + DAG 提交 Gate 1。

---

*v3-audit-report.md — Generated by PM Agent for AeroRadio Multi-Agent Workflow*
