# ICD: Interface Control Documents

跨 Domain Agent 共享的接口契约。所有 agent 在涉及共享接口时必须查阅本文档；变更接口时必须发起 ICD_UPDATE 广播。

ICD 命名规范：`ICD-{InterfaceName}-v{version}`

---

## 1. 核心契约清单（AeroRadio）

| ICD ID | Interface | Producer | Consumers | Status |
|--------|-----------|----------|-----------|--------|
| ICD-NetworkModule-v1 | 动态 baseUrl 拦截器接口 | data-integration | 所有 frontend agent | LIVE |
| ICD-AuthState-v2.1 | JWT 60h ★ TTL 实测 + 登录响应 data:[{token,priority,userid}] array shape + refresh/delete /authorizations/current endpoints（PA-14 实读校正） | data-integration | frontend-business, frontend-platform | LIVE |
| ICD-LoginAuthenticator-v1 | 登录鉴权 seam（fe 定义接口、data 实现 /authorizations） | frontend-business（consumer-defined） | data-integration（impl） | LIVE |
| ICD-TerminalDto-v2.1 | 终端 DTO（28 wire 字段，+14 PA-14）+ Domain TerminalStatus（sealed+Unknown）+ Repository（observe/refresh）+ ★ `terminal.zone` 字段 NOT membership FK | data-integration | frontend-business | LIVE |
| ICD-ZoneDto-v2.1 | Domain Zone（嵌套 terminals SSOT，`/terminal/terzone` 唯一权威，多对多保留，envelope-meta） | data-integration | frontend-business | LIVE |
| ICD-TaskRepository-v2.1 | 作息/任务 Repository（observe/refresh **二步 sechinfo→并发 sechetaskinfo→原子 publish**/setSchemeActive/getExecutionLog）+ Domain Scheme（嵌套 tasks）/SchemeTask/SchemeTaskStatus（sealed+Unknown）/TaskLog + 多 active option-A + 4 status int 派生规则 | data-integration（领域 owner） | frontend-business | LIVE（real impl V3TaskRepository，PA-15 Critic PASS_HIGH） |
| ICD-TaskDto-v1 | 任务 DTO（逆推自 TaskGuangboModel + TaskIdModel） | data-integration | frontend-business | LIVE (code-fact, 源见 §12) |
| ICD-SchemeDto-v2 | 作息 DTO **2 形分裂**（SchemeRowDto `/task/sechinfo` SUMMARY + SchemeTaskRowDto `/task/sechetaskinfo` TIMELINE 23 字段 + ★ per-task `name` 字段非 `taskname`） | data-integration | frontend-business | LIVE（PA-15 split; 源见 §12） |
| ICD-TtsTaskDto-v1 | TTS 任务 DTO（逆推自 TtsTaskContentModel） | data-integration | frontend-business | LIVE (源见 §12) |
| ICD-MediaDto-v1 | 媒体/文件夹 DTO（逆推自 MusicInfoModel + MusicFolderInfoModel） | data-integration | frontend-business | LIVE (源见 §12) |
| ICD-ServerStateDto-v1 | 系统健康度 DTO（逆推自 SeverStateModel） | data-integration | frontend-business | LIVE (源见 §12) |
| ICD-ServerStateRepository-v1 | 系统健康度 Repository(observeServerState/refresh) + Domain ServerState + ServerHealth(sealed+Unknown) | data-integration | frontend-business（Service Tab） | LIVE (real V3 impl) |
| ICD-MediaRepository-v1 | 点播媒体库 Repository(observeFolders/observeMedia/refresh) + Domain Media/MediaFolder(嵌套); CAST 不含(→legacy OnDemandCastAdapter) | data-integration | frontend-business（广播 Tab 点播） | LIVE (real V3 impl, LIST 半) |
| ICD-OnDemandCast-v1 | 点播 cast 推送 seam(HTIntf newondemandlist→setondemand*→startondemand; 逆推 orderMusic/startplay + javap) | legacy-native | frontend-business（广播 Tab 点播） | **LIVE**(控制结构纯Java) / **DRAFT-pending-device**(真机执行 R-001) / **DRAFT-pending-vendor**(int 码语义) |
| ICD-BroadcastWS-v1 | 终端状态 WebSocket 推送格式 | frontend-platform | frontend-business, data-integration | DRAFT |
| ICD-RealtimeFallback-v1 | WS 断线时的 10s 轮询协议 | frontend-platform | frontend-business | DRAFT |
| ICD-IPCSocket-v2 | 本机 127.0.0.1:4521 TCP（逆推自 SocketClient） | legacy-native | frontend-business | **LIVE**(连接语义+协程封装) / **DRAFT-pending-vendor**(命令词表, O-4) |
| ICD-VoiceAAR-v2 | htapplib.aar Kotlin 适配（逆推自 AAR javap + v3） | legacy-native | frontend-business | **LIVE**(HTIntf控制+CallBackIntf 21回调+MP3结构) / **DRAFT-pending-device**(native执行, R-001) + **OPEN**(init参语义) |
| ICD-MapLocation-v1 | 百度地图定位回调 + 经纬度回写 | legacy-native | frontend-business, data-integration | PLANNED (Phase 2 百度地图; 无 section, 落地时定义) |
| ICD-DesignTokens-v1.3 | 设计 token（颜色/圆角/阴影/字体/动效；+broadcast 3-mode + tile 5-state + task-card 3-state 16 alias 字段 + Noto Sans SC + JetBrains Mono subset 落 res/font） | frontend-platform（从 Handoff 提炼） | 所有 frontend agent | LIVE |
| ICD-Endpoints-v1.1 | REST 端点权威清单（路径×方法）+ 全局约定；+`/task/sechetaskinfo` POST + `/task/sechetask` CRUD + `/authorizations/current` refresh/delete；swagger 9079 行权威（PA-14/PA-15 实读校正） | data-integration | frontend-business, legacy-native | DRAFT-FROZEN（含 OPEN, 待 INQ-O-1） |

---

> **注（ICD-AuthState-v2 已落地 2026-05-27）**：§3 已更新为 v2 正文（= 已落地 AuthStore.kt，Critic 一致性快审 PASSED）。v1 模板作废（无实现无消费者，hard cutover 零迁移）。ICD_UPDATE 已由 PM 广播 frontend-business + frontend-platform。

## 2. ICD-NetworkModule-v1（动态 baseUrl 拦截器）

**Producer**: Data-Integration Agent
**Consumers**: 所有需要发 HTTP 请求的 agent

### 接口定义

```kotlin
// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideOkHttpClient(
        @ApplicationContext ctx: Context,
        authInterceptor: AuthInterceptor,
        baseUrlInterceptor: DynamicBaseUrlInterceptor  // ← 关键
    ): OkHttpClient
}

// data/network/DynamicBaseUrlInterceptor.kt
class DynamicBaseUrlInterceptor @Inject constructor(
    private val authStore: AuthStore  // 从 SharedPreferences 读 serverAddress
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // 若 url 仍为 placeholder.invalid，替换为真实 serverAddress
        val newUrl = if (request.url.host == "placeholder.invalid") {
            val serverAddr = authStore.serverAddressFlow.value
                ?: error("Server address not configured")
            request.url.newBuilder()
                .host(serverAddr.host)
                .port(serverAddr.port)
                .scheme("http")  // 明文 HTTP，符合现状
                .build()
        } else request.url
        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
```

### 使用约定

- **所有 ApiService 接口**用 `http://placeholder.invalid/` 作为 Retrofit baseUrl
- **所有方法**使用相对路径：`@GET("/terminal/terminalinfo")`，不使用 `@Url`
- **登录页**特殊处理：登录请求用 `@Url` 直接传完整 URL，因为此时 serverAddress 尚未存入 store

### 错误处理

| 场景 | 处理 |
|------|------|
| serverAddress 未配置 | 抛 `ServerAddressNotConfiguredException`，UI 路由到登录页 |
| serverAddress 格式错误 | 登录时校验，禁止存入 |
| 网络层 IOException | 由上层 Repository 转换为 `Result.Failure(NetworkError)` |

### 变更历史

- v1.0 (2026-05-27): 初始版本

---

## 3. ICD-AuthState-v2（鉴权状态）

**Producer**: Data-Integration Agent
**Consumers**: Frontend-Business（AR-005 LoginScreen / 导航）, Frontend-Platform（WS 鉴权读 jwt）, 间接 AR-002 拦截器
**Status**: LIVE（v2，2026-05-27 起；v1 模板作废。Critic 一致性快审 PASSED，= 已落地 AuthStore.kt 逐字一致）

### 接口定义（= 已落地 AuthStore.kt + ServerAddress.kt，逐字一致）

```kotlin
// data/auth/AuthStore.kt
interface AuthStore {
    val serverAddress: StateFlow<ServerAddress?>   // 当前服务器端点；首登前/reset 后为 null
    val jwt: StateFlow<String?>                    // 当前 JWT（不含 "Bearer " 前缀）；登出为 null
    val refreshToken: StateFlow<String?>           // 当前 refresh token；登出/后端不发为 null
    val account: StateFlow<String?>                // 末次登录账号（登出保留以预填）
    val isLoggedIn: StateFlow<Boolean>             // 是否持有有效 JWT（导航便利量）

    suspend fun saveLogin(
        address: ServerAddress,
        account: String,
        jwt: String,
        refreshToken: String?,                     // 可空：后端不发 refresh token 时传 null (OPEN D-1)
    )
    suspend fun clearLogin()                        // 清 jwt+refresh，保留 serverAddress+account（预填）
    suspend fun reset()                             // 清空一切（含 serverAddress+account）
    suspend fun refresh(knownStaleJwt: String?): Result<String>  // 见「刷新去重契约」
}

// data/auth/ServerAddress.kt
data class ServerAddress(val host: String, val port: Int) {
    companion object {
        const val DEFAULT_PORT = 80
        fun parse(input: String): Result<ServerAddress>   // "host" 或 "host:port"；缺省端口 80
        // 注：toUrl() 未实现（拦截器只需 host/port 分离）；如 fe 真需要再补，届时走 ICD_UPDATE
    }
}
```

### 与 v1 模板的差异（Δ）

| v1 模板 | v2（实落地） | 说明 |
|---|---|---|
| `*Flow` 后缀（serverAddressFlow…） | 去 `Flow` 后缀（serverAddress…） | 皆 StateFlow，命名更简洁 |
| —（无） | `isLoggedIn: StateFlow<Boolean>` | 新增，导航直接订阅 |
| `saveLogin(…, refresh: String)` | `saveLogin(…, refreshToken: String?)` | refresh **可空**（OPEN D-1：后端可能不发） |
| `refreshJwt(): Result<String>` | `refresh(knownStaleJwt: String?): Result<String>` | 改名+入参；去重键=调用方上报的过期 token；null=强制刷新 |
| —（无） | `reset()` | 新增，整库擦除（区别于 clearLogin 保留 host/account） |
| `ServerAddress.toUrl()` | （未实现） | 拦截器只需 host/port 分离 |

### 存储约定（soul: Security First）

- `jwt` + `refreshToken` → **EncryptedSharedPreferences**（AES256，文件 `auth_secure`）
- `serverAddress`(host+port) + `account` → 普通 SharedPreferences（非密，文件 `auth_plain`，登出保留以预填）
- **密码永不持久化**（ESC-WATCH-1 铁律）
- 依赖：`androidx.security:security-crypto:1.1.0-alpha06`（minSdk21 下唯一支持 EncryptedSharedPreferences 的版本；Google 已 deprecated 无继任者→未来 tech-debt）

### 线程契约（RISK-AR-001）

- 拦截器在 OkHttp 后台线程阻塞读 token；每次写入**先 commit 加密存储、再发布 StateFlow**，故并发读永远拿到一致快照（old-complete 或 new-complete，无半写）。

### 刷新去重契约（refresh）

- Mutex 串行化：命中**同一过期 token** 的并发 401 共享一次网络刷新。
- 去重键 = `knownStaleJwt`（调用方 401 时所用 jwt）：取锁后若 live jwt 已不等于它 → 别的调用已刷过 → 直接返回 live token，不再发网络。**时序无关**（不依赖取锁前快照，无 flake；Critic 真并发测 VERIFIED）。
- `knownStaleJwt == null` → 无条件强制刷新。刷新失败 → clearLogin（调用方路由登录）。
- 网络机制经 `TokenRefresher` 抽象注入；当前默认 `UnsupportedTokenRefresher`（401→失败→重登），impl 待 **OPEN(INQ-O-1 D-1)** 回执；届时仅换 TokenRefresher 一个绑定，AuthStore 主体零返工。

### 消费者用法提示

- 登录：`ServerAddress.parse(input).fold(...)` 校验 → `saveLogin(addr, account, jwt, refreshToken)`。
- 导航：订阅 `isLoggedIn`（或 `jwt`）。登出：`clearLogin()`（保留预填）或 `reset()`（彻底清）。
- 切服务器/换账号：先 `reset()` 再登录，避免旧凭据串台。
- AR-002 AuthInterceptor：401 时调 `refresh(knownStaleJwt = 本次请求所附 jwt)`。
- 登录网络调用经 `LoginAuthenticator` seam（fe 定义、data 实现，见 ICD-LoginAuthenticator）。

### 变更历史

- v2.1 (2026-05-30, PA-14 实读校正): JWT TTL **~60h**（CTO 实测；旧文档 24h 作废）。登录响应实读为 array shape `{"data":[{"token":"...","priority":<int>,"userid":<int>}]}` — 当前 `TokenEnvelopeDto.data.firstOrNull().token` 形状正确；建议增 `priority/userid` 为 wire-only 字段以备 v4 admin gate（domain 暂不暴露）。Refresh 端点 `POST /authorizations/current` + Logout 端点 `DELETE /authorizations/current` 已 swagger 确认存在（当前 UnsupportedTokenRefresher 未接线；接 OPEN INQ-O-1 D-1）。
- v2.0 (2026-05-27): 首次真实定义（TASK-AR-003 落地，Critic PASSED）。去 Flow 后缀 / +isLoggedIn / refreshToken 可空 / refresh(knownStaleJwt) / +reset()。v1 模板作废（无实现无消费者，hard cutover 零迁移）。
- v1.0 (2026-05-27): 模板草案（从未实现）。

---

## 3A. ICD-LoginAuthenticator-v1（登录鉴权 seam）

**Producer（接口定义）**: Frontend-Business（consumer-defined seam，在 `ui/screens/auth/LoginAuthenticator.kt`）
**Implementor**: Data-Integration（`RetrofitLoginAuthenticator`，DataModule 唯一 `@Binds` 绑定；POST `/api/authorizations` form→TokenModel→AuthResult）
**Status**: LIVE（2026-05-27；fe 接口 + data 真实 impl 均已落地编译绿；Critic 经 AR-005 消费侧 + AR-002 实现侧评审验证）

```kotlin
fun interface LoginAuthenticator {
    suspend fun authenticate(
        address: ServerAddress,   // 沿用 ICD-AuthState 契约类型(非 String)
        account: String,
        password: String,         // 传入用于鉴权；实现方禁止持久化(soul/安全 ESC-WATCH-1)
    ): Result<AuthResult>
}
data class AuthResult(
    val jwt: String,
    val refreshToken: String? = null,   // 后端可能不发(OPEN D-1)
    val account: String? = null,        // 服务器规范化账号；null=用用户输入
)
```

- **归属铁律**：`@Binds` 单点归 data（DataModule）；fe 不得自建 binding（Missing↔Duplicate 竞态教训，STD-COMPILABLE/Hilt 图单点）。fe 侧 `UnconfiguredLoginAuthenticator` 仅作 fallback/测试替身，不绑定。
- AuthResult 字段 1:1 映射 `AuthStore.saveLogin(address,account,jwt,refreshToken?)`（address 由 ViewModel 从表单出，不在 AuthResult 回显）。
- 真实刷新/重登语义仍受 OPEN(D-1) 影响（见 §3 刷新去重契约 + TokenRefresher）。

---

## 4. ICD-TerminalDto-v2（终端 DTO + Repository 接口）

**Producer**: Data-Integration Agent
**Consumers**: Frontend-Business（AR-102 Hub / AR-105 ZoneDetail / AR-106 广播目标）
**Status**: LIVE（v2，2026-05-27；AR-101 落地，Critic --no-daemon 复核 proposed==landed 放行。v1 单 state 假设作废）

### ⚠ 关键实读（v1 假设被推翻）

服务器**不返单一 `state` 字段**——MachineInfo 实读证返 **4 个独立 int 状态**：`taskstate / devicestate / netstate / speechstate`。故采 **wire/domain 两层**：Wire DTO 镜像 4 个 int（无损保真）；Domain 单一 `TerminalStatus` 由 `TerminalMapper.deriveStatus()` **派生**（有损 UI 折叠隔离在一个可换函数）。

### ⚠ ★ PA-14 关键警告：`terminal.zone` 字段 **不是** zone-membership FK

CTO ground truth (`.state/api-snapshots/terzone-cto-capture-2026-05-30.json`) 实证：zone "操场" (id=1) 下挂的 4 个终端，其 `terminal.zone` 值为 `0,0,0,8` — **没有一个等于父 zone id 1**。该字段语义 v3 文档未定义；v3 自身也**不**用它做分组（v3 调 `/terminal/zoneterminal/{id}` per zone）。**zone 归属的唯一权威来源 = `/terminal/terzone` 的嵌套 `ZoneDto.terminal[]` 数组**（见 §5）。任何按 `terminal.zone` 分组的实现 = 同 2026-05-30 操场 BLOCKER 复发。同族经验 memory `[[terminal-zone-field-is-not-membership]]` / `[[data-snapshot-verification]]` (RTM-ERR-005)。

**v2.1 wire 全集** (PA-14 实读，24+envelope-meta，全 nullable per R-003)：`id, type, taskstate, devicestate, netstate, speechstate, volume, isinstancy, zone (★ NOT membership)`, `name, ip, latitude, longitude, isrecord, issponsor, shortcircuit, lopencircuit, ropencircuit, temperature, humidity, isdecode, isencode, switchcount` + envelope `all, count, start, state`。v1 的 14 字段仍 active；v2.1 新增 14 字段（DTO-only，未 domain promote）：`isrecord/issponsor/shortcircuit/lopencircuit/ropencircuit/temperature/humidity/isdecode/isencode/switchcount` + envelope-meta 4。其中 `shortcircuit / lopencircuit / ropencircuit` 可作未来 `TerminalStatus.Fault` 派生输入（当前 deriveStatus 未消费，候 O-1 D-3 回执）。

### TerminalRepository 接口（fe 消费契约 · @Binds 可注入）

```kotlin
interface TerminalRepository {
    fun observeZones(): Flow<List<Zone>>          // Zone 嵌套 terminals；refresh 成功后重发
    fun observeTerminals(): Flow<List<Terminal>>  // 扁平终端流（同一内存 SSOT）
    suspend fun refresh(): Result<Unit>           // 拉网络入 SSOT；失败保留上一快照（驱动 error/retry）
}
// Impl: 内存 MutableStateFlow SSOT (Room 后续 drop-in); refresh 拉 /terminalinfo+/terzone → 按 zoneId join → 两 fetch 都成功才发布(无半更新)
```

### Domain 模型（fe 消费）

```kotlin
data class Terminal(id, name, zoneId, status: TerminalStatus, volume:Int?, longitude:String?, latitude:String?)

sealed interface TerminalStatus {              // Unknown-tolerant (ESC-WATCH-2)
    Online; Offline; Fault; Playing; Paging     // 已知 case 对齐 fe StatusPill 集
    data class Unknown(raw: String)             // R-003 兜底; 未识别值不崩, raw=源 token 便于诊断
}
// fe 的 when 必须保留 Unknown/else 分支(sealed 强制穷尽); 新增已知 case 走 ICD_UPDATE, fe when 仍穷尽
```

### Wire DTO（data 内部, fe 不消费）

```kotlin
// data/dto/TerminalDto.kt — Gson @SerializedName, 全 nullable, 包络 {data:[]}
TerminalDto(id, name, ip, zone, groupid, type,
    taskstate:Int?, devicestate:Int?, netstate:Int?, speechstate:Int?,   // ← 4 个独立 int
    isinstancy:Int?, volume:Int?, longitude:String?, latitude:String?)
```

### status 派生（⚠ documented-assumption · 候 OPEN INQ-O-1 D-3 + O-2）

`deriveStatus` 占位序：无状态→Unknown / netState==0→Offline / isUrgent‖speechState≠0→Paging / taskState≠0→Playing / else→Online。**Fault 暂不可从已知 int 派生（无确认的 fault 字段——已并入 O-1 D-3 问后端）；v4 的 talking/casting/urgent/alarm 不在此映射**。回执后仅换 deriveStatus，domain 类型 + fe 映射零变。

### 端点对应
- GET `/terminal/terminalinfo` → List<TerminalDto>；GET `/terminal/terzone` → List<ZoneDto>；refresh 内部 join 二者。

### 变更历史
- v2.1 (2026-05-30, PA-14 实读校正): ★ `terminal.zone` NOT membership FK 警告 pinned（CTO terzone capture 反证；2026-05-30 操场 BLOCKER 根因）。wire 字段集 14→24+envelope-meta（+isrecord/issponsor/shortcircuit/lopencircuit/ropencircuit/temperature/humidity/isdecode/isencode/switchcount 全 DTO-only）。Repository 改写 `refresh()` 只调 `/terminal/terzone` + `mapper.toTerminalOrNull(containingZoneId)`，废弃 `/terminal/terminalinfo + groupBy(terminal.zone)` 路径。Critic PA-14 PASS_W_MINOR HIGH（5-leg + emulator smoke 操场 4/4 命中 ground truth）。
- v2.0 (2026-05-27, AR-101): observe+refresh 接口；wire/domain 两层(4 int→派生)；status=sealed+Unknown；Zone 嵌套；Partial 不暴露(无缓存)。v1 单 state 作废。派生/真值集/Fault 候 D-3+O-2。
- v1.0 (2026-05-27): 模板（7 状态单 state，从未实现，假设错）。

---

## 5. ICD-ZoneDto-v2（分区 DTO + Domain Zone）

**Producer**: Data-Integration Agent
**Consumers**: Frontend-Business
**Status**: LIVE（v2，2026-05-27，随 AR-101）

### Domain Zone（fe 消费，**嵌套 terminals** — 决策 1，采纳 fe Q1 倾向）

```kotlin
data class Zone(id, name, description:String?, terminals: List<Terminal> = emptyList())
```
- **嵌套来源 = join 非单端点内嵌**：repo refresh 拉 `/terminalinfo`(全终端) + `/terzone`(分区) → 按 zoneId 把 terminals 分组进 zones。非"单个 zone 端点返内嵌列表"。
- 派生计数（每区 online/fault、全局 fault）**不在 repo / 不在 Zone**——fe UI 层自算（决策 1）。

### Wire ZoneDto（data 内部）
```kotlin
ZoneDto(id:Int?, name:String?, description:String?, count:Int?, online:Int?, offline:Int?, busyline:Int?)
```

### 关键约定
- **分区 = 终端集合**（v4 PRD）；一个终端可属多个分区；分区不嵌套（无父子分区）。
- 端点：`GET /terminal/terzone`→分区；`GET /terminal/zoneterminal/{id}`→分区下终端（当前 ZoneDetail 从 observeZones 嵌套 terminals 取；若需懒拉单独加 getZoneTerminals 走 ICD_UPDATE）。

### 变更历史
- v2.1 (2026-05-30, PA-14 实读校正): **嵌套来源 = `/terminal/terzone` 内的 `ZoneDto.terminal[]` 数组**（非 join 派生）；Zone view SOLE source = terzone（废弃 `/terminal/terminalinfo + groupBy`）。Wire 字段补 envelope-meta `all (String, 注：terzone 返 String 非 Int)/count (Int)/start (Int)/state (Int)` + 兼容旧 ZoneModel `online/offline/busyline` 防御保留。**多对多保留**：CTO ground truth 终端 id=14 出现在 4 个 zone 的 `terminal[]` 中，`Zone.terminals` 直接保留多次出现；扁平 `observeTerminals()` 返回 4×，UI 自决去重。`Terminal.zoneId = parentZone.id`（NOT wire `terminal.zone`，见 §4 ★ 警告）。Critic PA-14 PASS_W_MINOR HIGH（5-leg + emulator smoke 操场 4/4 + 5 named zones 全验）。
- v2.0 (2026-05-27, AR-101): Domain Zone 嵌套 terminals(join by zoneId)；计数 fe 自算。
- v1.0 (2026-05-27): 模板(terminalIds 扁平 + 计数字段，未实现)。

---

## 6. ICD-BroadcastWS-v1（终端状态 WebSocket）

**Producer**: Frontend-Platform Agent
**Consumers**: Frontend-Business, Data-Integration

> 状态：DRAFT — Handoff 文档说"WebSocket push，回退 10s 轮询"，但具体消息格式厂商后端未提供。本 ICD 定义客户端预期格式，待对接验证。

### 消息格式（客户端预期）

```kotlin
sealed class BroadcastWSMessage {
    @Serializable
    data class TerminalStateChange(
        @SerialName("type") val type: String = "terminal_state",
        @SerialName("terminalId") val terminalId: String,
        @SerialName("state") val state: TerminalState,
        @SerialName("playing") val playing: PlayingInfo?,
        @SerialName("timestamp") val timestamp: Long
    ) : BroadcastWSMessage()

    @Serializable
    data class TaskProgress(
        @SerialName("type") val type: String = "task_progress",
        @SerialName("taskId") val taskId: String,
        @SerialName("progress") val progressPct: Int,
        @SerialName("status") val status: String
    ) : BroadcastWSMessage()

    @Serializable
    data class Heartbeat(
        @SerialName("type") val type: String = "ping",
        @SerialName("timestamp") val timestamp: Long
    ) : BroadcastWSMessage()
}
```

### 回退策略（与 Handoff 一致）

- **WS 主通道**：连上后持续推
- **断线检测**：30s 未收到心跳 → 标记断线
- **断线 UI**：顶部黄 banner "实时已断开"
- **回退轮询**：每 10s 轮询 `/terminal/terminalinfo` 获取所有终端状态
- **重连**：指数退避（2s, 4s, 8s, 16s, max 60s）

### 待验证项（提交后端对接后）

- [ ] 实际 WS 端点 URL（`ws://<server>/ws/?token=<jwt>` ?）
- [ ] 心跳间隔
- [ ] 鉴权方式（query token vs header）
- [ ] 消息实际字段名

### 变更历史

- v1.0-DRAFT (2026-05-27): 客户端预期协议，待后端对接

---

## 7. ICD-IPCSocket-v2（本机 TCP socket · 逆推自 SocketClient）

**Producer**: Legacy-Native Agent
**Consumers**: Frontend-Business（间接，通过 Repository）
**Status（混合，CTO D-12）**: **LIVE** — 连接语义（逆推自 `utils/SocketClient.java`: HOST:17 / 4521:22 / connect+3s:32 / SO_TIMEOUT:34 / println发:38 / readLine收:69-70 / `###ShellRunError:`47）+ 协程封装(AR-007 LocalSocketClient, Critic PASSED)。 **DRAFT-pending-vendor** — 命令词表（v3 发任意 shell 字符串无类型化枚举可逆推，候厂商 O-4；ShellCommand=Raw 透传，O-4 回执后 additive 类型化）。
> 完整方法/字段×来源：`.state/icd-ipcsocket-v2-proposed.md`（不复制）。下方 v1 模板内容为历史，以本 v2 状态为准。

### 接口定义（v1 模板，历史）

```kotlin
// data/ipc/LocalSocketClient.kt
interface LocalSocketClient {
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun sendCommand(cmd: ShellCommand): Result<String>
    val connectionStateFlow: StateFlow<ConnectionState>
}

sealed class ShellCommand(val raw: String) {
    object StartPaging : ShellCommand("paging start")
    data class StopPaging(val sessionId: String) : ShellCommand("paging stop $sessionId")
    data class SetVolume(val vol: Int) : ShellCommand("volume set $vol")
    // ...更多命令待补充（需 Legacy-Native agent 从历史代码提炼）
}

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }
```

### 物理参数

- **地址**: `127.0.0.1:4521`（设备本机）
- **协议**: TCP，类 shell 命令文本协议
- **编码**: UTF-8
- **命令分隔**: `\n`（待确认）
- **超时**: 5s 默认

### 使用约定

- 仅在前台 Activity 时建立连接（节省资源）
- 命令幂等性：同一命令重发不应产生副作用
- 错误恢复：连接断开后自动重连 3 次

### 变更历史

- v1.0 (2026-05-27): 初始版本，待 Legacy-Native agent 补全命令清单

---

## 8. ICD-VoiceAAR-v2（语音对讲 AAR · 逆推自 AAR javap + v3）

**Producer**: Legacy-Native Agent
**Consumers**: Frontend-Business（广播 Tab · 对讲模式）
**Status（混合，CTO D-12）**:
- **LIVE** — HTIntf 控制面（纯 Java，SPIKE 证 native 数=0；调用点 MainMethod/CallOtherActivity 等）+ **CallBackIntf 21 回调**（双源：AAR javap 恰 21 abstract + v3 `service/HTIntfHandler.java:45 implements`，DP-4 钉死 CallBackIntf 非 NativeTalkListener）+ MP3 native **结构**（4 个 Mp3Encode* 签名 ↔ libaudioplay.so JNI 符号一一对应 + 链接自洽，SPIKE-001 已复现）。
- **DRAFT-pending-device** — MP3 native **执行**（真机 loadLibrary 成功/编码产出/无 SIGSEGV，无设备源，候 R-001 §7.2）。「符号对得上 ≠ 真机跑通」。
- **OPEN** — HTIntf.init 7~8 参语义（无确定源，不影响 adapter 结构）。
> ABI 实况见 §SPIKE / R-001（arm64-v8a + armeabi-v7a 双 ABI，非"只有 32 位"——旧描述已废）。完整方法×来源：`.state/icd-voiceaar-v2-proposed.md`。

### 适配层接口（建议）

```kotlin
// data/voice/VoiceTalkAdapter.kt
interface VoiceTalkAdapter {
    fun isAvailable(): Boolean                          // 设备是否支持 32 位 ABI
    suspend fun startTalk(targetIds: List<String>): Result<TalkSession>
    suspend fun endTalk(session: TalkSession)
    fun observeTalkState(session: TalkSession): Flow<TalkState>
}

data class TalkSession(val sessionId: String, val startedAt: Long)

sealed class TalkState {
    object Connecting : TalkState()
    object Talking : TalkState()
    data class Error(val cause: Throwable) : TalkState()
    object Ended : TalkState()
}
```

### 32 位 ABI 约束

- **限制**: AAR 只有 `armeabi-v7a` + `x86`
- **影响**: app 必须支持 32 位才能运行，会限制 64 位-only 设备
- **缓解**: 在 `build.gradle.kts` 强制 abiFilters 包含 32 位变体；UI 检测到不支持时降级到"对讲模式不可用"

### 变更历史

- v1.0-DRAFT (2026-05-27): 待 Legacy-Native agent 完成 AAR 接口逆向

---

## 9. ICD-DesignTokens-v1.2（设计 token）

**Producer**: Frontend-Platform Agent（从 `references/design-system-spec.md` 落地，spec 从 Handoff.html / AeroRadio v4.html 提炼）
**Consumers**: 所有 frontend agent
**Status**: LIVE（v1.2 — +gold/goldSoft 任务迁移 accent，BL-GOLD-TOKEN）
**实际落地包**: `app/src/main/java/com/htgd/radiocontrol/aeroradiocontrol/ui/theme/`（8 文件：Color/Shape/Spacing/Type/Elevation/Motion/Gradients/AeroTheme）
**注入方式**: CompositionLocal（`LocalAeroColors` 等）+ `AeroTheme.xxx` 访问器；token 为 data-class 字段（非 object 常量），可被 AeroTheme override。

> v1.1 修订（DEL-TASK-AR-009-v2，PM R-1~R-5 裁定）：① 速查子集升级为完整清单；② 落地命名与 spec 权威名不一致 → 见 §9.1 **别名映射**（R-1 非破坏路径）；③ 补录 bgBeige（R-2）+ 派生 token（R-3）；④ Elevation 对齐 spec 2/8（R-4）；⑤ 字体占位状态登记（R-5）。
> v1.2 修订（BL-GOLD-TOKEN，fe-platform，Critic PASSED HIGH 2026-05-29）：+`gold`(#A8780A) + `goldSoft`(#FAF0CC) 两个 AeroColors 字段（任务 迁移/对调 accent：gold=border+tag fg / goldSoft=tag pill bg）。**ADDITIVE 非破坏**（defaulted 字段，仅 no-arg `AeroColors()` 构造点，无消费方迁移，无 CTO gate）。去硬编码 `TaskScreen.kt:300-301`（DSN-ERR-001 缓解）；消费方 fe-business 待 ③ 任务 VM slot 接 `c.gold`/`c.goldSoft`。

### 9.1 命名别名映射（R-1 — 非破坏；落地名 ↔ spec 权威名）

> 落地代码用左列名（已被业务屏消费，本期**不改**以免 breaking）；spec/Handoff 权威名见右列。Critic 校验设计偏离时按本表对位，**两列视为同一 token**。
> **Phase 3 待 CTO 批**（backlog `BL-TOKEN-RENAME`）：将左列真正改名为右列（breaking change，需 ICD_UPDATE + 迁移窗 + frontend-business 协调）。现阶段不执行。

| 落地字段（code） | spec 权威名 | Hex | 备注 |
|------------------|------------|-----|------|
| `AeroColors.bg` | `AeroColors.Background` | `#F4F5F7` | — |
| `AeroColors.surface` | `AeroColors.Surface` | `#FFFFFF` | — |
| `AeroColors.primary` | `AeroColors.Primary` / `TabTerminal` / `ModeCast` | `#0E7C70` | **终端 Tab 色 + 点播 mode 色复用 primary**（无独立 tabTerminal/modeCast 字段；R-1） |
| `AeroColors.pageWarm` | `AeroColors.TabBroadcast` / `ModePage` / `StatusPaging` | `#EA580C` | 广播 Tab + 寻呼 mode + 寻呼状态同色 |
| `AeroColors.talkBlue` | `AeroColors.ModeTalk` / `StatusPlaying` | `#2563EB` | 对讲 mode + 播放状态同色 |
| `AeroColors.serviceBlue` | `AeroColors.TabService` | `#2563EB` | 与 talkBlue 同 hex，语义分列 |
| `AeroColors.taskPurple` | `AeroColors.TabTask` | `#7C3AED` | — |
| `AeroColors.aiTeal` | `AeroColors.TabAI` | `#14B8A6` | — |
| `AeroTypography.numeric` | `AeroType.MetricNum` | 22sp/700/mono/**tnum** | C-1 已补 tnum |
| `AeroTypography.kicker` | `AeroType.Label` | 11sp/400/mono/0.6/UPPERCASE | C-2 weight 已改 400；**大写需调用方 `.uppercase()`**（TextStyle 不强制） |
| `AeroTypography.topBar` | `AeroType.TopBarTitle` | 22sp/600/-0.2 | — |
| `AeroTypography.sectionTitle` | `AeroType.SectionTitle` | 18sp/600 | — |
| `AeroTypography.bodyLarge` | `AeroType.BodyMedium` | 15sp/500 | — |
| `AeroShapes.rTile` | `AeroShapes.Tile` | 16.dp | — |
| `AeroShapes.rChip` | `AeroShapes.Chip` | 999.dp | — |
| `AeroElevation.e1/e2/e3/fab` | `AeroElevation.Card/CardHover/Modal/FAB` | 1/2/8/6.dp | R-4：e2/e3 已对齐 spec 2/8 |

> 其余字段（ink/ink2-4、surface2-3、primaryInk/Soft、status*、shape rCard/rInput/rSheet、type display/body/bodySmall/label/button、spacing/motion 全部）落地名与 spec 引用名仅大小写/前缀差异，逐一不列；按语义对位。

### 9.2 完整 token 清单（落地 1:1，详值见 design-system-spec.md）

- **Color（33）**：中性 bg/bgBeige/surface(1-3)/ink(1-4) + 品牌 primary/primaryInk/primarySoft + tab/mode pageWarm/talkBlue/taskPurple/aiTeal/serviceBlue + status 5 + statusSoft 5 + 派生 line/lineStrong/divider + **gold/goldSoft（任务 迁移/对调 accent，v1.2）**。
- **Shape（5）**：rCard 12 / rTile 16 / rChip 999 / rInput 12 / rSheet 24(顶角)。
- **Spacing（16）**：pageH16 / sectionV12 / cardPad14 / tileGap10 / btnPadV10 / btnPadH18 / topBarH56 / tabBarH80 / tabRaise16 + 8px 栅格 xs4/sm8/md12/lg16/xl24/xxl32。
- **Type（9）**：display/topBar/sectionTitle/bodyLarge/body/bodySmall/kicker/numeric/label/button（mono = numeric+kicker）。
- **Elevation（4）**：e1 1 / e2 2 / e3 8 / fab 6（R-4 对齐 spec §5 M3 等价值）。
- **Motion（6+2）**：fabIn220/tap130/pulse1500/tabBadgePulse2200/wave900/skel1600 + emphasized(.2,.7,.3,1)/standard(.2,0,0,1) easing。
- **Gradients（3）**：Primary / Warm / Night。

### 9.3 字体状态（v1.3 — BL-FONT-ASSETS 关闭）

✅ **2026-05-30 Phase C v1.3 落地**：`AeroSans` / `AeroMono` 从占位 (`FontFamily.Default` / `FontFamily.Monospace`) 升级为真接 `res/font/`：
- Noto Sans SC **subset** Regular/Medium/Bold @ wght=400/500/700（GB2312 L1+L2 6763 字 ∪ 项目实读 963 字 ∪ ASCII printable ∪ CJK 标点 ∪ 全角符号 = 7173 chars cmap，**100% 项目串覆盖**），3 weight × 2.25 MB = 6.75 MB
- JetBrains Mono **subset** Regular/Medium（ASCII + Latin-1 + dash punct），2 weight × 67 KB = 0.13 MB
- 共 5 ttf 落 `res/font/`；OFL §3 license texts 落 `app/licenses/{NotoSansSC,JetBrainsMono}-OFL.txt`（aapt2 拒非字体资源进 res/font/，license 必须分离）
- Type.kt `AeroSans = FontFamily(Font(R.font.noto_sans_sc_regular, Normal), Font(..._medium, Medium), Font(..._bold, Bold))`；`AeroMono` 同 pattern
- grep `FontFamily.Default` / `FontFamily.Monospace` → 0 hits 全 purge
- APK 增量 **+5.0 MB**（51.77 MB→56.78 MB；CTO Q2=(ii) subset 路径，避开 full vendor +80MB OTA 负担）
- 子集漏字 fix workflow：append corpus → `pyftsubset` re-run → re-commit
- `fontFeatureSettings="tnum"` 在 numeric 仍生效，subset 兼容
- Subset 工具：pyftsubset 4.63.0；变量主字体 `NotoSansSC[wght].ttf` 经 `fontTools.varLib.mutator.instantiateVariableFont` 实例化后 subset

### 9.4 v1.3 语义角色 alias（16 fields，additive，零新 hex，AR-009 R-1 sealed pattern）

**Phase C-residual 2026-05-30 — 不破坏现有 consumer**。每个 alias = 现有 internal val 同一 hex；新增 alias 给消费屏一个"按语义命名"的入口，与未来 `BL-TOKEN-RENAME` breaking rename 解耦。

#### 9.4.1 Broadcast 3-mode (3 alias)

| Alias | Hex | Backs onto | Spec ref | Consumer |
|---|---|---|---|---|
| `modePaging` | #EA580C | `PageWarm` | Handoff.html:883 §s-broadcast 三档差异 | `BroadcastScreen` mode=page tint |
| `modeIntercom` | #2563EB | `TalkBlue` | Handoff.html:884 | `BroadcastScreen` mode=talk tint |
| `modeCast` | #0E7C70 | `Primary` | Handoff.html:885 | `BroadcastScreen` mode=cast tint (与 brand teal 同色) |

`BroadcastMode.identityColor(colors)` 中心化映射函数（mirrors PA-14 C-1 `AeroTab.identityColor()`），3 mode 在 ModeSegmented / VoicePanel status-line / Idle CTA / CastPanel CTA 全 binding 经此函数。Resting chip label 也带 mode-identity 色（"carry identity even before selection"，Critic Phase C 真机验证）。

#### 9.4.2 Terminal tile 5-state (10 alias = 5 fg + 5 soft)

| Alias | Hex | Backs onto | Spec ref | Consumer |
|---|---|---|---|---|
| `tileOnline` | #16A34A | `StatusOnline` | Handoff.html:626 §components | `TerminalTile` IconBadge tint + CornerBadge dot — online |
| `tileOffline` | #8A929F | `StatusOffline` | Handoff.html:640 | offline |
| `tileFault` | #DC2626 | `StatusFault` | Handoff.html:647 | fault |
| `tilePlaying` | #2563EB | `StatusPlaying` | Handoff.html:653 | playing |
| `tilePaging` | #EA580C | `StatusPaging` | Handoff.html:512-516 derived | paging |
| `tileOnlineSoft` | #E6F4F2 | `PrimarySoft` | Handoff.html:625 | online icon bg |
| `tileOfflineSoft` | #EEF0F3 | `Surface3` | Handoff.html:639 | offline icon bg |
| `tileFaultSoft` | #FDECEC | `StatusFaultSoft` (v1.1) | Handoff.html:646 | fault icon bg |
| `tilePlayingSoft` | #E8EFFD | `StatusPlayingSoft` (v1.1) | Handoff.html:653 | playing icon bg |
| `tilePagingSoft` | #FDEEE2 | `StatusPagingSoft` (v1.1) | Handoff.html derived | paging icon bg |

`TerminalTile.kt` IconBadge + CornerBadge 用 `c.tile*` 替代 `c.status*`（semantic-rename 同 hex，零视觉变更）。fe-business sharp-trace 教训：5 状态差异化 **data-gated 不是 code-gated** — `when`-swap binding 已正确，渲染需服务端推混合状态终端（Critic Path A Compose @Preview 5-state 接受作 Leg 5 evidence）。

#### 9.4.3 Task-card 3-state pill (3 alias，DOCUMENTED ASSUMPTION)

| Alias | Hex | Backs onto | Spec ref | Consumer |
|---|---|---|---|---|
| `taskCardStateDone` | #8A929F | `Ink3` | Handoff.html:930-931 §s-task lists states; NO hex pin (assumption) | TaskCard 状态 pill — 已完成 |
| `taskCardStateRunning` | #EA580C | `StatusPaging` | matches existing TaskScreen.kt:205 in-code usage for "进行中" — code-fact consistency | TaskCard 状态 pill — 进行中 |
| `taskCardStatePending` | #4A5260 | `Ink2` | preliminary — awaiting CTO real-device review | TaskCard 状态 pill — 待执行 |

★ ASSUMPTION TAG：Handoff.html 不固定 task-pill hex。PM Q1=(a) ruling 2026-05-30：LIVE-documented-assumption；CTO 真机回退 → 改 alias hex 不动 consumer（sealed AR-009 R-1 path）。`temporalStateOf(taskTime, now)` 推导（±60s window for Running，past=Done，future=Pending，parse-fail=Pending R-003 safe default）— TemporalStateTest 8 case 验。

**复用 scheme-list 启用/停用**（语义一致 hex 同源 — PM 2026-05-30 ruling "P2 命名 TaskCardState* + KDoc 复用注释"）。

### 9.5 增量变更（v1.1/v1.2/v1.3 累计）

> v1.1 修订（DEL-TASK-AR-009-v2，PM R-1~R-5 裁定）：① 速查子集升级为完整清单；② 落地命名与 spec 权威名不一致 → 见 §9.1 **别名映射**（R-1 非破坏路径）；③ 补录 bgBeige（R-2）+ 派生 token（R-3）；④ Elevation 对齐 spec 2/8（R-4）；⑤ 字体占位状态登记（R-5）。
> v1.2 修订（BL-GOLD-TOKEN，fe-platform，Critic PASSED HIGH 2026-05-29）：+`gold`(#A8780A) + `goldSoft`(#FAF0CC) 两个 AeroColors 字段（任务 迁移/对调 accent：gold=border+tag fg / goldSoft=tag pill bg）。**ADDITIVE 非破坏**（defaulted 字段，仅 no-arg `AeroColors()` 构造点，无消费方迁移，无 CTO gate）。
> v1.3 修订（Phase C-residual, fe-platform + fe-business 合发，Critic PASSED_W_MINOR HIGH 2026-05-30）：(a) 16 alias 字段（broadcast 3 + tile 5 fg + tile 5 soft + task-card 3）— 见 §9.4；(b) BL-FONT-ASSETS 关闭：Noto Sans SC + JetBrains Mono subset 真接 res/font/，AeroSans/AeroMono FontFamily.Default/Monospace placeholder 全 purge — 见 §9.3。**ADDITIVE 非破坏 + 零新 hex**（alias 全引用现有 internal val），fe-business 屏消费 token 不破坏其他 consumer。APK +5.0 MB（CTO Q2=(ii) subset 路径）。OFL §3 license vendor 合规。

---

## 10. ICD-Endpoints-v1（REST 端点权威清单 + 全局约定）

**Producer**: Data-Integration Agent
**Consumers**: Frontend-Business（经 Repository/ViewModel 间接消费）, Legacy-Native（旧栈迁移协调）
**Status**: DRAFT-FROZEN — 契约文档已冻结为 Phase 1 全部 Repository 的迁移基线；含 5 个 OPEN，待 INQ-O-1 后端回执出 v2 增量。

> **本 ICD 是什么 / 不是什么**：是「端点契约文档冻结」——钉死端点口径、URL 组成、全局成功/包络约定、Phase 0 auth DTO 骨架。**不是**全量 DTO 实现（各业务 DTO 随 Phase 1 各 Repository 落地，届时新增 ICD-TerminalDto / ICD-TaskDto 等的 v2）。权威端点明细表见 `.state/endpoint-inventory-draft.md`（本 ICD 引用之，不复制以免双源漂移）。

### 11.1 端点口径（O-4 已钉死）

- **最小单元 = 路径 × 方法**。"21 端点"是路径口径；按路径×方法展开为 ~40+ 操作（如 `/task/taskinfo` 承担 GET/POST/PUT/DELETE 四个操作，各为独立契约单元）。
- Retrofit 接口按「路径×方法」逐一声明，各自的 Request / Response DTO 独立定义（即便共用同一路径）。
- 权威明细（方法/路径/常量/调用方/v4 状态）以 `.state/endpoint-inventory-draft.md §1` 为准；其 P0..P3 接入优先级见该文 §4。

### 11.2 URL 组成（实读旧栈确认）

- **Base = `http://{host}:{port}/api`** —— `/api` 是所有端点的基路径前缀。证据：`LoginActivity.prelogin` 拼 `serverAddress = "http://"+ip+":"+port+"/api"`，`MyRequestBuilder.setUrl` 拼 `serveraddress + 端点路径常量`，故 `/authorizations` 实际命中 `http://host:port/api/authorizations`。
- 明文 HTTP（校园 LAN 既定约束）。
- **对 AR-002 DynamicBaseUrlInterceptor 的约束**：占位 `http://placeholder.invalid/` 必须被替换为 `http://{host}:{port}/api/`（含 `/api`！），且各 ApiService 的相对路径不带 `/api`（如 `@POST("/authorizations")`）。或等价地：base 不含 `/api`、每个相对路径补 `/api`——二选一，ICD 推荐前者（`/api` 进 base，路径保持与 `Constant.java` 常量一致）。
- 鉴权头：key = `Authorization`，值前缀 = `Bearer `（含尾随空格），由 AuthInterceptor 统一注入（登录请求除外）。

### 11.3 全局成功 / 响应包络约定（documented-assumption，OPEN 待 O-1 确认）

> 以下为旧栈实读得出的 documented-assumption；**INQ-O-1 回执后以后端答复为准并触发 ICD_UPDATE**。

| 约定 | documented-assumption（旧栈实读） | OPEN 引用 |
|------|-----------------------------------|-----------|
| 成功判定 | **HTTP 状态码 = 200** 即成功（`RequestManger` 用 `response.code()==EorroCode.SUCESS(=200)`），非读 body 业务码 | OPEN(INQ-O-1 Q3) ★解 AR-002 错误映射硬阻塞 |
| 响应包络 | `{ "data": [ ... ] }` —— **只有 `data`，无 `code`/`message`**；`data` 恒为数组（单条也包成单元素数组） | OPEN(INQ-O-1 Q2) |
| 业务子状态 | HTTP 200 后部分端点在 `data[0].state` 带业务码（如 `state="15"`=任务名重复），含义按端点而异，**不在包络层** | OPEN(INQ-O-1 Q3) |
| 请求体 | POST/PUT/DELETE 一律**扁平 form 表单**（非 JSON）；DELETE 的 `id` 走 form body | OPEN(INQ-O-1 Q1) |
| 路径参数 | 带参端点旧栈手拼**路径段**（`/task/taskterminal/{id}`、`/terminal/mediainfo/{type}` 等）；DELETE 的 id 反走 body（需确认后端是否真支持 DELETE-with-body） | OPEN(INQ-O-1 Q4) |
| multipart | 文件 part 名 = `mediafile`；伴随字段（taskid/speed/volume…）传法待确认 | OPEN(INQ-O-1 Q5) |

**新栈 DTO 防御姿态（O-1 未答前强制）**：Gson `setLenient()` + 反序列化容忍未知字段；可空字段标 nullable；枚举留 unknown 兜底；数字解析失败给 fallback。对应 R-003 owner 侧 fallback。

### 11.4 Phase 0 auth 端点 DTO 骨架（本 ICD 冻结的唯一具体 DTO）

> 仅覆盖 Phase 0 登录闭环触及的 auth 端点。字段名来自实读 `GetTokenModel` / `TokenModel`，**OPEN(INQ-O-1 D-1/D-2)** 待确认登录是 form 还是 JSON、有无独立刷新端点。

```
# POST /api/authorizations  (登录换 token)
# 请求：form 表单 (documented-assumption, OPEN D-2)
#   username : String   # 账号
#   userpwd  : String   # 密码 (注意是 userpwd 不是 password)
# 响应：{ "data": [ TokenModel ] }  (单元素数组)
#   TokenModel:
#     token              : String   # JWT 本体 (附 "Bearer " 前缀后入 Authorization 头)
#     expired_at         : String   # JWT 过期时间
#     refresh_expired_at : String   # 刷新过期时间 (暗示存在 refresh token, OPEN D-1)
#     priority           : String   # 用户权限级别

# 刷新机制：OPEN(INQ-O-1 D-1)
#   旧栈无独立 /authorizations/refresh 端点——"刷新"是用存储凭据重 POST /api/authorizations。
#   登录响应含 refresh_expired_at 暗示本应有 refresh_token + 独立端点（旧栈未用）。
#   待后端确认；AuthStore(AR-003) 已用 TokenRefresher 抽象留好两种结局的接口。
#   ⚠ 若后端确认只能"重存密码、过期重登"，与 soul「密码永不持久化」冲突 → 需 CTO 决策。
```

Retrofit 形态待 AR-002 拦截器定稿后落入 `data/api/AuthApi.kt`（本 ICD 只冻结契约字段，不冻结 Retrofit 注解写法）。

### 11.5 OPEN 项汇总（出 v2 的触发条件）

| OPEN | 内容 | 影响 | 解除条件 |
|------|------|------|---------|
| O-1 Q1 | 多方法 Req/Resp DTO 是否各异 | Phase1 全 Repository DTO | INQ-O-1 回执 |
| O-1 Q2 | 是否有 code/message 包络 | 错误信息提取 + 成功判定 | INQ-O-1 回执 |
| O-1 Q3 | 成功判定=HTTP 2xx vs 业务码 | **AR-002 错误映射硬阻塞** | INQ-O-1 回执 |
| O-1 Q4 | {id}/{type} 路径参数 vs query | 每端点 @Path/@Query 选择 | INQ-O-1 回执 |
| O-1 Q5 | multipart 字段名 | 上传端点 @Part | INQ-O-1 回执 |
| O-1 D-1 | 有无独立刷新端点 / 是否被迫存密码 | AR-003 存储 + AR-002 401 链 | INQ-O-1 回执 (+可能 CTO 决策) |
| O-1 D-2 | 登录 form vs JSON、头 key/前缀 | AR-002 头注入 + AR-005 请求构造 | INQ-O-1 回执 |

### 11.6 v1.1 端点增量（PA-14 + PA-15 实读 + swagger 9079 行核对）

| Verb | Path | Body / Auth | PA / Status | Notes |
|---|---|---|---|---|
| POST | `/authorizations/current` | Bearer | PA-14 swagger 确认 | refresh token endpoint（当前 UnsupportedTokenRefresher 未接线；OPEN INQ-O-1 D-1） |
| DELETE | `/authorizations/current` | Bearer | PA-14 swagger 确认 | logout / delete token endpoint（当前未接线） |
| GET | `/terminal/terzone` | Bearer | PA-14 LIVE | ★ zones 嵌套 terminals — Zone view 的 SOLE source；多对多保留 |
| GET | `/terminal/terminalinfo` | Bearer | PA-14 LIVE | 扁平全终端；**Zone view 不再使用**（PA-14 废弃 groupBy 路径）；其他 consumer 不受影响 |
| POST | `/terminal/zoneterminal` | Bearer | swagger 确认 | SET zone↔terminal binding — 是 POST 不是 GET。`GET /terminal/zoneterminal` 返 405 |
| GET | `/terminal/zoneterminal/{id}` | Bearer | swagger 确认 | 单 zone 终端列表（v3 ZoneMethod 使用；v4 不需要，terzone 已嵌套） |
| POST | `/task/sechetaskinfo` | Bearer + form `name=<sechename>` | PA-15 LIVE | ★ AUTHORITATIVE timeline source — 单作息内的 task 列表，per-task 23 字段（starttime / `name` ★ NOT `taskname` / medianame / state/taskstate/enablestate/offlinestate 4 status 等） |
| POST | `/task/sechetask` | Bearer | swagger 确认 | CRUD: create scheme task（body `sechetaskinfo` def）；future increment |
| PUT | `/task/sechetask` | Bearer | swagger 确认 | CRUD: update scheme task；future increment |
| DELETE | `/task/sechetask` | Bearer | swagger 确认 | CRUD: delete scheme task（body `singleid`）；future increment |
| GET | `/task/sechinfo` | Bearer | PA-15 校正 | scheme **SUMMARY** list (taskid/taskstate/taskcount/sechename/projectstate/startdate/enddate)；**NOT timeline source** — `taskcount` 是整数计数非 array；timeline 必走 sechetaskinfo |

**参考权威**：swagger 完整 9079 行在 `.state/api-snapshots/swagger-v3-vendor.json`。Phase 0 auth DTO 骨架 (§11.4) 仍 LIVE；PA-14 v2.1 中标注的 `priority/userid` wire-only 字段对应 §3 的 v2.1 entry。

### 变更历史

- v1.1 (2026-05-30, PA-14 + PA-15 实读校正): +`POST /task/sechetaskinfo` 标 AUTHORITATIVE timeline + sechetask CRUD（POST/PUT/DELETE 都 swagger 确认）+ `POST /authorizations/current` refresh + `DELETE /authorizations/current` logout。`GET /task/sechinfo` 重标为 SCHEME SUMMARY (NOT timeline source)。`GET /terminal/terzone` ★ Zone view SOLE source。Critic PA-14 + PA-15 PASS_HIGH 5-leg + emulator smoke。
- v1.0-DRAFT-FROZEN (2026-05-27): 草案冻结。口径(O-4)、URL 组成(`/api` 前缀)、全局约定(documented-assumption)、Phase0 auth DTO 骨架已定；7 个 OPEN 待 INQ-O-1。明细表引用 `.state/endpoint-inventory-draft.md`。

---

## 11. ICD 变更流程

```
Domain Agent A 想改 ICD
    │
    ▼
向 PM 提 ICD_UPDATE 草案
    │
    ▼
PM 召集所有 affected_agents 评估影响
    │
    ▼
Critic 审核（破坏性变更必审）
    │
    ▼
    ┌────────────┐
    │            │
PASSED       FAILED
    │            │
    ▼            ▼
广播            修改后重提
ICD_UPDATE
    │
    ▼
本文档 + agent memory 同步更新
```

### 破坏性变更（breaking change）规则

- 必须有明确的 migration window
- 必须提供向后兼容方案或 hard cutover 时间表
- 必须通知 Human CTO

---

## 12. 逆推 DTO ICD（任务域/媒体/系统健康度 · AR-110 · CTO D-12 可逆推类）

> Producer: Data-Integration · LIVE（code-fact，逆推自旧栈 model，Critic AR-110 抽验 file:line 真命中）
> **完整字段 × 每字段来源（逆推自 v3 file:line）见 `.state/icd-reverse-engineered-proposed.md`**（不复制以免双源漂移，同 §10 引 endpoint-inventory-draft 的原则）。
> 注（Critic F-1）：file:line 是「该字段确在该文件」级溯源，非 byte-exact 锚点（旧 model 字段行序与表略漂移，字段均真实存在）。

| ICD | 逆推自（旧栈 model） | 关键点 |
|-----|------|------|
| ICD-TaskDto-v1 | TaskGuangboModel(:12-49) + 写响应 TaskIdModel | state="15"=任务名重复(TaskMainMethod:313) |
| ICD-SchemeDto-v2 (**SchemeRowDto**) | **TaskGuangboModel** (作息 list /task/sechinfo, PA-10 校正) — **SUMMARY shape** | ⚠ /task/sechinfo→TaskGuangboListRsp，**clean `projectstate`(0=running)**。拼写 `projectstatetate` 是 **TaskZuoxiModel**(String，另一 detail/CRUD 流，非 list wire)——SchemeRowDto 仅防御保留。**taskcount 是整数计数非 array**；timeline 必走 sechetaskinfo（见下行）。 |
| ICD-SchemeDto-v2 (**SchemeTaskRowDto**) | **TaskGuangboModel** (per-task /task/sechetaskinfo, PA-15 新增) — **TIMELINE shape，23 字段全捕获** | ★ PA-15 wire 实读 (CTO 海王作息 capture)：per-task 显示名字段是 **`name`** ★★★ NOT `taskname`（旧 dispatch 假设错）；`info` 是 sechename 反向 back-pointer，不是 FK；4 status ints (state/taskstate/enablestate/offlinestate) 折叠到 Domain.SchemeTaskStatus 经 SchemeMapper.deriveTaskStatus；execmode=62=0b111110=Mon-Fri bitmask（MVP-deferred UI）；同族 [[terminal-zone-field-is-not-membership]]。 |
| ICD-TtsTaskDto-v1 | TtsTaskContentModel | state/taskid/speed/male/contents |
| ICD-MediaDto-v1 | MusicInfoModel + MusicFolderInfoModel | 媒体 + 文件夹 |
| ICD-ServerStateDto-v1 | SeverStateModel(:10-19) | state/connection/taskcount/bandwidth/maxconnection/ctrl·dataport/name/ip/gate |

**DRAFT 残留（无源/候回执，不在上表 LIVE）**：TerminalStatus 4-int→单一状态派生规则(候 O-1 D-3/O-2)、/authorizations/refresh 端点存在性(候 D-1，旧栈 grep 零)、urgentplay/gettempttstask/deltemptts(旧栈定义零调用无源)、统一 code/message 包络(候 O-1 Q2)。

> 跨域：这批 LIVE DTO 是后续 fe Task/Service Tab Repository/ViewModel 的 seam 契约源；fe 接入时 Critic 按"seam 两侧一致"核（同 AR-101/102）。

---

## 13. ICD-TaskRepository-v1（作息/任务 Repository 接口 + Domain · 方案A）

> Producer: Data-Integration（领域 owner 拍板）· LIVE（接口契约 + **real impl V3TaskRepository, PA-10 Critic PASSED HIGH big-review**）· wire 经 PA-10 校正为 TaskGuangboModel（见下 RESOLVED）
> 触发: fe-business 从任务 5 屏渲染反推消费需求（AR-101 接口先行纪律）。契约源 = 已 LIVE 逆推 DTO（ICD-SchemeDto-v1/TaskDto-v1/TtsTaskDto-v1，§12）。
> 全文 proposal: `.state/icd-taskrepository-proposed.md`

### 接口（PA-03b 范围 = 只读 + 启停；CRUD 后续增量）
```kotlin
interface TaskRepository {
    fun observeSchemes(): Flow<List<Scheme>>                    // 每 Scheme 含嵌套 tasks；refresh 成功后重发
    suspend fun refresh(): Result<Unit>                         // 拉 v3 入 SSOT；失败保留上一快照（同 V3TerminalRepository 契约）
    suspend fun setSchemeActive(schemeId: String, active: Boolean): Result<Unit>  // 启停方案（/task/sechenableordisable）；成功后 active 翻转经 observeSchemes 重发（SSOT 单源）
    suspend fun getExecutionLog(): Result<List<TaskLog>>        // 一次性（v3 无日志流端点；轮询模型）。真有流端点再升 observe（ICD_UPDATE）
}
```

### Domain 模型（fe 消费）
```kotlin
data class Scheme(id:String, name:String, active:Boolean, tasks:List<SchemeTask> = emptyList())  // active=projectstate 派生
data class SchemeTask(id:String, name:String, status:SchemeTaskStatus, startTime:String?=null, mediaName:String?=null, volume:Int?=null)
sealed interface SchemeTaskStatus {   // Unknown-tolerant（同 TerminalStatus，ESC-WATCH-2）
    data object Idle; data object Running; data object Disabled
    data class Unknown(val raw:String)   // R-003 兜底；fe when 须保 Unknown 分支即穷尽
}
data class TaskLog(id:String, taskName:String, timestamp:String, message:String)
```

### RESOLVED by PA-10 real impl (was OPEN) + remaining documented-assumption
- ✅ **wire = TaskGuangboModel** (NOT TaskZuoxiModel): /task/sechinfo→TaskGuangboListRsp, FLAT rows (each=1 task carrying parent sechename+projectstate), grouped by sechename → nested Schemes. Scheme.id=name=sechename (v3 唯一方案标识 + POST key).
- ✅ **Scheme.active = (projectstate == 0)** — 0==running（反直觉；pinned TaskZuoxiActivity:248-254）。setSchemeActive(active) POST {sechename, state} 0=enable/1=disable → /task/sechenableordisable；reply 0(success)/15(already-same) OK 否则 fail；then refresh()（I-3 单源，无本地翻转）。
- ✅ **SchemeTaskStatus 派生 PINNED**：both-null→Unknown；!active(projectstate!=0)→Disabled；active&taskstate!=0→Running；active&taskstate==0→Idle。*余 doc-assumption*：taskstate 比 0/非0 更细的语义无 v3 doc → Unknown-tolerant 兜底未分类。
- ✅ **getExecutionLog = empty (RESOLVED)**：grep 证 v3 无 log REST 端点（仅 on-device LOCAL_DIR_LOG）。返空列——诚实, 非派生/伪造。日后有端点 → ICD_UPDATE 升 real。
- ✅ **`projectstatetate` 校正**：是 **TaskZuoxiModel**(detail/CRUD 另一流, String) 的字段, **非** list wire。list 用 clean `projectstate`。SchemeRowDto 仅 cost-free 防御 @SerializedName 保留。
- ✅ **UI 映射 (fe re-touch, post-PA-10)**：`TaskItem.zone` → **空白**（确认 TaskGuangboModel 无 zone 字段；原 mediaName 占位现真数据下=visible bug, 已修）。`LogEntry.success` moot（无 log feed → ExecutionLog Empty 态）。SchemeTaskStatus→UI 映射对齐 pinned 派生。

### v2.1 RESOLVED additions (PA-15 fix, 接口签名仍 v1 不变)

- ✅ **PA-15 BLOCKER FIX**：PA-10 误把 `/task/sechinfo` SUMMARY rows 当 timeline tasks → 14 task timeline 渲染空白（CTO 2026-05-30 demo blocker）。**根因**：sechinfo SUMMARY-only (taskcount=int 14 非 array)；timeline 必须 fetch SEPARATE endpoint `POST /task/sechetaskinfo` body `name=<sechename>`（v3 ListActivity demoably-working 同路径）。同族 RTM-ERR-005 [[data-snapshot-verification]]。
- ✅ **refresh() 升级为 TWO-step + atomic publish**：
  1. `GET /task/sechinfo` → enumerate schemes via `SchemeRowDto.toSchemeSummary()`（每行 → Scheme skeleton，`active=(projectstate==0)`）。
  2. 每 scheme name parallel `POST /task/sechetaskinfo` body `{name: sechename}`（`coroutineScope { async + awaitAll }`）。每 per-task response 经 `SchemeTaskRowDto.toSchemeTask`，scheme.active 透传到 status 派生。
  3. **Atomic publish**：全 per-scheme task fetch 成功才 publish 全 snapshot；ANY child failure → 整个 publish 跳过，PRIOR snapshot 保留（同 V3TerminalRepository 契约）。
  4. Concurrent refresh() 共享 ONE in-flight fetch（Mutex + Deferred）。
- ✅ **per-task `name` 字段实读 (PA-15 ★★★)**：sechetaskinfo 每行 display name 字段 = `name`（"早读开始铃" / "第一节课上课铃" etc），**NOT `taskname`**。SchemeTaskRowDto `@SerializedName("name")` 钉死，CTO ground truth 14 named task 真机渲染验证（Critic Path A + emulator smoke）。
- ✅ **多 active scheme = Option A first-active-wins**：CTO 2026-05-30 capture：海王作息 + 日本作息 都 projectstate=0 RUNNING；TaskHomeViewModel.activeScheme 取 first-active（按 sechinfo `start` ordering）。Data SSOT 保留所有 active scheme 信息；fe 升 Option B (user pick) / C (merge timelines) 是 fe-only change，data 层无 repo 改动。documented-assumption pinned in SchemeMapper KDoc。
- ✅ **4 status ints 派生 (sealed-with-Unknown)**：`deriveTaskStatus(schemeIsActive, state, taskState, enableState, offlineState)` 规则（top→bottom, first match wins）：
  1. scheme NOT active → **Disabled**（per-task row under stopped scheme）
  2. enableState == 0 → **Disabled**（per-task disable flag；analogue /task/taskdoorno "0=enable, 1=disable"）
  3. taskState != 0 → **Running**（任意 per-task running 信号）
  4. all four ints null → **Unknown("no-state")**（defensive R-003）
  5. 否则 → **Idle**（已排但未触发 — CTO capture 正常态）
  
  单次 capture 不能完全枚举真值集；documented-assumption pinned。真值集确认后**只**改 deriveTaskStatus，sealed `SchemeTaskStatus` 类型 + fe boundary 不变。
- ✅ **getExecutionLog = empty (PA-15 confirms PA-10)**：swagger sweep 确认 NO `/task/log` / `/task/journal` / `/task/history` 端点；getExecutionLog 返空。日后真有端点 → ICD_UPDATE（signature 不变）。
- ✅ **SchemeDto 分裂 v1 → v2**：DTO shape change（见 §12）但 Domain Scheme/SchemeTask shape 不变，fe consumer **零 API 变更**。

### 变更历史
- v2.1 (2026-05-30, PA-15 real fix + Critic PASS_HIGH): refresh() 单步 → TWO-step (sechinfo→并发 sechetaskinfo)，atomic publish；per-task `name` ★★★ 字段实读；多 active Option A 文档化；4 status int 派生规则 pinned；SchemeDto v1→v2 (DTO split)。**接口签名不变（仍 v1）**。16/16 V3TaskRepositoryTest green + Critic 5-leg + emulator 13/14 named tasks 真机渲染验证。同族 [[data-snapshot-verification]] RTM-ERR-005。
- v2 (2026-05-29, PA-10 real impl): V3TaskRepository STUB→REAL。Critic PASSED HIGH big-review（独立读 v3 源）。**接口签名不变（仍 v1）**；解全部 OPEN（见 RESOLVED 节）：wire=TaskGuangboModel/clean projectstate(0=running)/setSchemeActive POST {sechename,state}/getExecutionLog 无端点返空/SchemeTaskStatus 派生 pinned/projectstatetate 校正到 TaskZuoxiModel。fe mapper re-touch（zone→blank）post-PA-10。**PA-15 推翻 PA-10 单 fetch 假设**：sechinfo only=SUMMARY not timeline，必须配 sechetaskinfo（见 v2.1）。
- v1-doc (2026-05-29, PA-03③ UI 接入): fe-business 落 TaskHome/SchemeDetail/ExecutionLog VM + TaskUiMappers（对接口编程，跑 V3TaskRepository stub[空]，21 测绿）。**无接口签名变更（仍 v1）**；仅追加 UI 边界映射 documented-assumption（zone/success 见上）。impl=V3TaskRepository（真 v3 wire）仍待排（大审 pin zone/success/status/projectstatetate）。
- v1 (2026-05-28, 方案A PA-03b 接口先行): observe/refresh/setSchemeActive/getExecutionLog；Scheme 嵌套 tasks（同 Zone）；SchemeTaskStatus sealed+Unknown（同 TerminalStatus）；getExecutionLog 一次性。Critic 轻审 PASSED（同构核 Terminal）。impl=V3TaskRepository 待 PA-01 收尾后排。

---

## 14. ICD-ServerStateRepository-v1（系统健康度 Repository 接口 + Domain · 方案A）

> Producer: Data-Integration（领域 owner）· LIVE（**real V3 impl** = V3ServerStateRepository，非 stub）· Critic PASSED HIGH 2026-05-29（PA-05；独立复核 8 测 + 终端回归绿）。
> 触发: Service Tab（系统健康度）。契约源 = 已 LIVE 逆推 ServerStateDto（§12，1:1 SeverStateModel）。wire: GET /server/serverstate（Constant.java:97）→ SeverStateRsp{data:[SeverStateModel]} → data[0]（单元素信封，同 terminals）。

### 接口
```kotlin
interface ServerStateRepository {
    fun observeServerState(): Flow<ServerState?>   // 单对象；null 直到首次 refresh 成功；每次成功重发
    suspend fun refresh(): Result<Unit>            // 拉 /server/serverstate 入 SSOT；失败(network/parse/empty)保留上一快照（同 V3TerminalRepository 契约）
}
```

### Domain 模型
```kotlin
data class ServerState(
    health: ServerHealth, name: String?, ip: String?, gate: String?,
    connection: Int?, maxConnection: Long?, taskCount: Int?, bandwidth: Int?, ctrlPort: Int?, dataPort: Int?
)
sealed interface ServerHealth {            // R-003；fe when 须保 Unknown 分支
    data object Online; data object Offline; data class Unknown(val raw: String)
}
```

### OPEN / documented-assumption
- **deriveHealth 保守占位**：`state` int → null/0→Offline，else→Online（候 v3 适配实测）。当前 `state` 为 Int? 全覆盖 when，**ServerHealth.Unknown 暂不可达**（前向兼容 affordance；真值集变富时加 case 路由 Unknown，fe when 不变）。
- **dual-slot（R-ADDR-SLOT, cross-cutting watch）**：repo 用 ServerConfig.baseUrl()(=Constant.serveraddress)，**不**读 v3 PreferencesUtil('serverAddress')。V4 login 只写前者；15 个保留 v3 GET 路径读后者但 V4 nav 不可达 → dormant。广播 Tab/运行时 demo re-check（见 tasks.yaml risks_added R-ADDR-SLOT）。
- 10/10 字段 verbatim（无 typo trap，不同于 SchemeDto 的 projectstatetate）。

### 变更历史
- v1 (2026-05-29, PA-05): real-direct（镜像 V3TerminalRepository）。observeServerState/refresh + ServerState + ServerHealth sealed。V3ServerStateRepository real impl（V3CallbackAdapter path1 raw JSON）。Critic PASSED HIGH（8 测 + 终端回归绿；SSOT 3 失败模式留快照实测）。

---

## 15. ICD-MediaRepository-v1（点播媒体库 Repository 接口 + Domain · 方案A · LIST 半）

> Producer: Data-Integration（领域 owner）· LIVE（real V3 impl=V3MediaRepository，LIST 半）· Critic PASSED HIGH 2026-05-29（PA-07；独立复核 7 测 + 全 V3 回归绿）。
> 触发: 广播 Tab 点播媒体选择器。契约源 = 已 LIVE 逆推 MediaDto（§12，1:1 MusicInfoModel/MusicFolderInfoModel）。wire: GET /terminal/mediafolderinfo + GET /terminal/mediainfo(all-media)，{data:[...]} 信封；client 端 groupBy folderId join（同 Terminal 两取+join，非 per-folder 递归）。

### 接口（LIST 半；CAST 不在此契约）
```kotlin
interface MediaRepository {
    fun observeFolders(): Flow<List<MediaFolder>>   // 每 folder 嵌套其 media；start empty；refresh 成功重发
    fun observeMedia(): Flow<List<Media>>            // 扁平 media 列；同 SSOT
    suspend fun refresh(): Result<Unit>             // 两取入 SSOT；失败(network/parse)留上一快照（同 V3TerminalRepository）；空列=合法空库(非失败, 区别 ServerState 单对象)
}
```

### Domain 模型
```kotlin
data class Media(id:String, name:String, folderId:String, format:String?=null, durationSeconds:Int?=null, sizeBytes:Int?=null)
data class MediaFolder(id:String, name:String, parentId:String?=null, media:List<Media> = emptyList())  // parentId null at root(FALG_MUSIC=3)/0
```

### 范围 / OPEN
- **CAST(点播 推送到终端) 不在此契约** = legacy-native 的 OnDemandCastAdapter（HTIntf AAR，§16 待 impl）。fe 点播屏组合: MediaRepository(选) + OnDemandCastAdapter(推) + BroadcastTargetsViewModel(目标)。urgentplay REST = 死端点(v3 从未接线)。
- count/all（folder 媒体计数）现 DTO-only 未入 domain；fe 若要库头计数 badge → ICD_UPDATE 提升（非 wire 变更）。
- per-folder /terminal/mediainfo/{folderid} lazy-load 端点保留备用（现 all-media+client group）。start（分页 offset）wire 有但 fetch-all 无分页消费, 未 carry。

### 变更历史
- v1 (2026-05-29, PA-07 Option A): LIST 半 real-direct。observeFolders/observeMedia/refresh + Media/MediaFolder(嵌套)。castMedia 按 Option A 剔除(→legacy OnDemandCastAdapter)。Critic PASSED HIGH（7 测 + 回归绿）。

---

## 16. ICD-OnDemandCast-v1（点播 cast 推送 seam · AAR/HTIntf · 方案A）

> Producer: Legacy-Native（领域 owner）· Critic PASSED HIGH 2026-05-29（PA-08；javap 复核 6 签名 + v3 序列 verbatim + 回归绿）。
> 触发: 广播 Tab 点播模式 推送动作（区别 MediaRepository §15 的"选"）。源: v3 ActivityMusicOrder.orderMusic():241-260 + MainMethod.startplay():34-49 + AAR javap。

**Status 分裂（同 §8 VoiceAAR）**：
- **LIVE**（控制结构, 纯 Java，javap 证）：HTIntf 6 静态方法 newondemandlist():void / setondemandterminal(int):int / setondemandmedia(int):int / setondemandvolume(int):int / startondemand():int / stopondemand():int。接口 = OnDemandCastAdapter。
- **DRAFT-pending-device**（R-001）：startondemand/stopondemand 真机执行（MP3/audio native 路径，§7.2 真机清单）。
- **DRAFT-pending-vendor**：startondemand()/stopondemand() int 返回码成功语义（v3 两站点均忽略该 int=无源；isOk(state)=true 单点假设；vendor-inquiry 候选，并入 O-4 厂商包）。

### 接口（data/voice/OnDemandCastAdapter.kt）
```kotlin
interface OnDemandCastAdapter {
    fun isAvailable(): Boolean   // AAR/ABI 探针(复用 VoiceNativeProbe). 非 RECORD_AUDIO(playback 非 capture)
    suspend fun castMedia(mediaIds: List<Int>, targetTerminalIds: List<Int>): Result<Unit>  // 一次性(非 Flow); newondemandlist→setondemandterminal*→setondemandmedia*→startondemand
    suspend fun stopCast(): Result<Unit>           // stopondemand; best-effort 幂等
    suspend fun setCastVolume(volume: Int): Result<Unit>  // setondemandvolume
}
class OnDemandCastException(val state: Int) : RuntimeException
```

### 决策 / 边界
- 独立 adapter（非并入 VoiceTalkAdapter）：一次性 Result vs voice 长流 Flow。
- isAvailable 复用 VoiceNativeProbe（无第 2 探针）。**无 RECORD_AUDIO**（fe 点播屏不弹麦权限；gate=isAvailable only）。
- **不注册 CallBackIntf**（castMedia 返回于 startondemand 同步 int，不夺 voice 全局回调）。fail-closed 复用 VoiceUnavailableException。
- 屏组合（fe）: MediaRepository §15(选) + OnDemandCastAdapter(推) + BroadcastTargetsViewModel(目标)。urgentplay REST=死端点。

### 变更历史
- v1 (2026-05-29, PA-08): impl 落地。javap 证 6 签名; 序列 verbatim v3; gate JVM 测 + happy-path device-pending(同 AR-104 边界). Critic PASSED HIGH(5 测 + Voice 回归 7 绿).

---

*ICD Contracts v1.0.0 — AeroRadioControl Cross-Domain Interface Registry*
