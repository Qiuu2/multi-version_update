# ICD-AuthState-v2 — 提交草案（供 Critic 一致性快审 → PM 落 icd-contracts.md §3 + 广播 ICD_UPDATE）

> Producer: Data-Integration · 日期: 2026-05-27 · 触发: TASK-AR-003 首次真实定义 AuthStore（v1 为从未实现/无消费者的模板）
> 一致性核验基准：`app/src/main/java/com/htgd/radiocontrol/aeroradiocontrol/data/auth/AuthStore.kt` + `ServerAddress.kt`（本文逐字镜像之）
> change_type: MODIFY · breaking_change: true（命名+签名变）· 兼容策略: **hard cutover，零迁移成本**（v1 无任何实现/消费者代码）

---

## 拟替换 icd-contracts.md §3 正文如下

### 3. ICD-AuthState-v2（鉴权状态）

**Producer**: Data-Integration Agent
**Consumers**: Frontend-Business（AR-005 LoginScreen / 导航）, Frontend-Platform（WS 鉴权读 jwt）, 间接 AR-002 拦截器
**Status**: LIVE（v2，2026-05-27 起；v1 模板作废）

#### 接口定义（= 已落地 AuthStore.kt，逐字一致）

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
    suspend fun refresh(knownStaleJwt: String?): Result<String>  // 见下「刷新去重契约」
}

// data/auth/ServerAddress.kt
data class ServerAddress(val host: String, val port: Int) {
    companion object {
        const val DEFAULT_PORT = 80
        fun parse(input: String): Result<ServerAddress>   // "host" 或 "host:port"；缺省端口 80
    }
}
```

#### 与 v1 模板的差异（Δ，给消费者明确改点）

| v1 模板 | v2（实落地） | 说明 |
|---|---|---|
| `serverAddressFlow` / `jwtFlow` / `refreshTokenFlow` / `accountFlow` | `serverAddress` / `jwt` / `refreshToken` / `account` | 去 `Flow` 后缀（皆 StateFlow，命名更简洁） |
| —（无） | `isLoggedIn: StateFlow<Boolean>` | 新增，导航直接订阅 |
| `saveLogin(addr, account, jwt, refresh: String)` | `saveLogin(address, account, jwt, refreshToken: String?)` | refresh **可空**（OPEN D-1：后端可能不发 refresh token） |
| `refreshJwt(): Result<String>` | `refresh(knownStaleJwt: String?): Result<String>` | 改名 + 入参；去重键 = 调用方上报的过期 token；null=强制刷新 |
| —（无） | `reset()` | 新增，整库擦除（区别于 clearLogin 保留 host/account） |
| `ServerAddress.toUrl()` | （未实现） | 拦截器只需 host/port 分离；如 fe 真需要再补，届时走 ICD_UPDATE |

#### 存储约定（soul: Security First）

- `jwt` + `refreshToken` → **EncryptedSharedPreferences**（AES256，文件 `auth_secure`）
- `serverAddress`(host+port) + `account` → 普通 SharedPreferences（非密，文件 `auth_plain`，登出保留以预填）
- **密码永不持久化**（ESC-WATCH-1 铁律）
- 依赖：`androidx.security:security-crypto:1.1.0-alpha06`（minSdk21 下唯一支持 EncryptedSharedPreferences 的版本）

#### 线程契约（RISK-AR-001）

- 拦截器在 OkHttp 后台线程以阻塞读取 token（`jwt.value` / `runBlocking { jwt.first() }`）。
- 每次写入**先 commit 加密存储、再发布 StateFlow**，故并发读永远拿到一致快照（old-complete 或 new-complete，无半写）。

#### 刷新去重契约（refresh）

- 用 Mutex 串行化：命中**同一过期 token** 的并发 401 共享一次网络刷新。
- 去重键 = `knownStaleJwt`（调用方 401 时所用的 jwt）：取锁后若 live jwt 已不等于它 → 别的调用已刷过 → 直接返回 live token，不再发网络请求。**时序无关**（不依赖取锁前快照，无 flake）。
- `knownStaleJwt == null` → 无条件强制刷新（如主动续期）。
- 刷新失败 → clearLogin（调用方路由登录）。
- 网络机制经 `TokenRefresher` 抽象注入；当前默认 `UnsupportedTokenRefresher`（401→刷新失败→重登），impl 待 **OPEN(INQ-O-1 D-1)** 回执（独立刷新端点 vs 凭据重登），届时仅换 TokenRefresher 一个绑定，AuthStore 主体零返工。

#### 消费者用法提示（fe-business / fe-platform）

- 登录：`ServerAddress.parse(input).fold(...)` 校验 → `saveLogin(addr, account, jwt, refreshToken)`。
- 导航：订阅 `isLoggedIn`（或 `jwt`）。
- 登出：`clearLogin()`（保留预填）或 `reset()`（彻底清）。
- 切服务器/换账号场景：先 `reset()` 再走登录，避免旧凭据串台。
- AR-002 AuthInterceptor：401 时调 `refresh(knownStaleJwt = 本次请求所附 jwt)`。

#### 变更历史

- v2.0 (2026-05-27): 首次真实定义（TASK-AR-003 落地）。去 Flow 后缀 / +isLoggedIn / refreshToken 可空 / refresh(knownStaleJwt) / +reset() / ServerAddress.parse。v1 模板作废（无实现无消费者，hard cutover 零迁移）。
- v1.0 (2026-05-27): 模板草案（从未实现）。

---

*待 Critic 一致性快审（核本文 = AuthStore.kt 接口）→ PM 落 §3 + 广播 ICD_UPDATE(affected: frontend-business, frontend-platform)。*
