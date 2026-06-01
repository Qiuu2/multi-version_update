# NEXT-2 — 鉴权 split-brain framing 文档（D-16 升级版 2026-06-01）

> PM framing 工件 v2，反映 D-16 CTO 决策 + 2026-06-01 kill-app 路径新发现。
> 替代旧版（基于 uninstall 重装单路径）。data-integration audit dispatch 主体。

---

## 1. 症状（CTO 实测两路径）

### 路径 A — 2026-05-30 demo BLOCKER-3 v1（已知）
`adb uninstall com.htgd.radiocontrol.aeroradiocontrol` + `adb install app-debug.apk` 后，App 启动**跳过登录页直接进主页**，token 仍生效。

### 路径 B — 2026-06-01 新发现（升级到 P0 紧急）
1. 第一次登录成功 → 进入主界面 ✓
2. **后台滑掉杀 app**（普通日常路径）
3. 重新打开
4. **直接进内部页面**（不是回登录页）+ 显示 `"加载失败 serveraddress must not be null"`

**严重性升级**：从 uninstall 影响 → 日常每次后台杀 app 就触发。**普通用户日常使用就撞**。

**期望**：两路径都应该回登录页（按 D-16 方案 C 决策）。

---

## 2. 三根因（PM grep + CTO 实测）

### 根因 A — `allowBackup=true` 全 restore（路径 A）

```
app/src/main/AndroidManifest.xml:57   android:allowBackup="true"
app/src/main/res/xml/                 NO backup_rules.xml / data_extraction_rules.xml
```

Android Auto Backup 在 uninstall 时备份 `/data/data/<pkg>/shared_prefs/*` + `/data/data/<pkg>/files/datastore/*` 到 Google Drive / Backup Transport，同签名 APK install 时自动 restore。

**只解释路径 A**。

### 根因 B — 双源持久化 split-brain (路径 A + B 都有)

```
ReFreshTokenUtil.java:43   PreferencesUtil.getInstance().getEntity(Constant.key_tokenModel, GetTokenModel.class)
TaskManageUtils.java:201   PreferencesUtil.getInstance().getField("serverAddress", mContext)
```

新栈 AuthStore (DataStore) 与 旧栈 PreferencesUtil (SharedPreferences) 是两条独立通路。

### ★ 根因 C — kill-app 状态丢失（路径 B 新发现，最严重）

**假设**（待 audit 锤实）：
- DataStore 异步写入：登录后 `AuthStore.saveLogin(...)` 触发 DataStore commit，但 Flow emit + 实际写盘是异步。短时间内 kill app **可能写盘没完成**，但 ViewModel state 已经 in-memory 写过 → nav 觉得"已登录"已经发生
- StateFlow in-memory cache 在进程 kill 时全丢 → 重启读 DataStore 是空 → 部分字段 null
- v4 nav 启动入口（V4Activity.onCreate）**没有原子检查**所有 L2 字段都齐全才进 MainScreen — 可能只检查 `jwt != null` 就放行
- → MainScreen 内某个 polling refresh 触发 `serverConfig.baseUrl()` 读 serverAddress = null → NPE

**待 audit 确定**：
- DataStore 是否在 kill 前 commit 完成（writeProtoFile + fsync）
- nav 启动入口 check 哪几个字段
- ServerAddress 是写到 DataStore 哪个 key + 读取路径

---

## 3. D-16 CTO 决策（2026-06-01）— 方案 C "记住我" toggle 式

### 3.1 两层存储分清（架构关键）

| 层 | 字段 | 加密 | 持久化目标 |
|---|---|---|---|
| **L1 凭据 (rememberMe 控制)** | `account` / `serverHost` / `serverPort` / `rememberMe: Boolean` | 明文 | DataStore Preferences "prefs_remember" 或 SharedPreferences |
| **L2 鉴权 (token 控制)** | `token` / `refreshToken?` / `tokenExpiry` | **加密**（EncryptedSharedPreferences / Tink-DataStore） | "auth_secure" 文件 |

L1 = 用户体验（让登录页预填）；L2 = 安全资产（绝不能漏到 backup / log / 旧栈 cache）。

### 3.2 启动两步判断（StartupActivity / V4Activity.onCreate）

```kotlin
fun onAppStart() {
    val authState = readL2AuthState()  // 同步阻塞读 EncryptedSharedPreferences
    val authValid = authState != null
                    && authState.token.isNotBlank()
                    && authState.serverAddress != null     // ★ atomic check 三件
                    && authState.account.isNotBlank()
                    && !isExpired(authState.tokenExpiry)   // 60h TTL check
    if (authValid) {
        navigate to MainScreen
    } else {
        clearL2Atomically()                                 // 清残留, 防 split-brain
        val rememberMe = readL1RememberMe()
        navigate to LoginScreen(
            prefillAccount = if (rememberMe.enabled) rememberMe.account else "",
            prefillHost    = if (rememberMe.enabled) rememberMe.serverHost else "",
            prefillPort    = if (rememberMe.enabled) rememberMe.serverPort else "",
            password       = "",                            // ★ 总空（不持久化）
            toggleState    = rememberMe.enabled,
        )
    }
}
```

### 3.3 退出语义

| 触发 | 清 L2 | 清 L1 | 备注 |
|---|---|---|---|
| 用户主动登出 | ✓ | ✗ | 保留 rememberMe 字段以预填 |
| Token 过期检测 | ✓ | ✗ | 同主动登出 |
| 401 网络层强制登出 | ✓ | ✗ | 同主动登出 |
| 用户切 rememberMe = 关（LoginScreen toggle） | ✗ | ✓ (清 account/serverHost/serverPort + 标 rememberMe=false) | 仅清 L1，L2 不动 |
| 用户切 rememberMe = 开 + 登录成功 | 写新 L2 | 写新 L1（account/serverHost/serverPort + rememberMe=true） | 两层都写 |

**纪律**：L1 / L2 path 各自独立，**绝不混淆**。

### 3.4 原子不变量

> **L2 三件原子组**：`token` + `serverAddress` + `account` 要么全有，要么全无。任一缺失 = `clearL2Atomically()` + 回 LoginScreen。

`clearL2Atomically()` 必须在同一事务（DataStore edit{} / SharedPreferences.commit() 等价）中清三个 key，避免 fsync 间窗口被 kill app 又撞同样问题。

---

## 4. 七候选清单（v1 路径 A） + 三新候选（v2 路径 B）

| # | 候选 | grep 实证 | 命中? |
|---|---|---|---|
| 1 | 双源持久化分裂 (DataStore vs SharedPreferences) | ReFreshTokenUtil + TaskManageUtils 旧栈用 PreferencesUtil 命中 | ✅ 待 audit |
| 2 | In-memory cache 不同步 (AuthStore StateFlow vs DataStore) | 未 grep | ⚠ 待 audit |
| 3 | 重装残留 (allowBackup auto-restore) | Manifest:57 allowBackup=true 命中 | ✅ 实证 |
| 4 | Server-address race (Interceptor singleton 缓存) | R-ADDR-SLOT 已 dormant tracked | ⚠ 待 audit |
| 5 | Logout 不全清 | 未 audit Logout 代码 | ⚠ 待 audit |
| 6 | 新旧栈共存窗口 (v4 sole-launcher 后是否真死路) | TaskManageUtils 仍存在 | ⚠ 待 audit (c-1 vs c-2 决断点) |
| 7 | JWT TTL 60h vs doc 24h refresh 错触发 | TokenRefresher 是 TODO impl | ⚠ 待 audit |
| **8** | **DataStore async 异步写盘未完成 + kill** | **CTO 2026-06-01 实测命中** | **✅ 路径 B 实证** |
| **9** | **Nav 启动入口 check 不全（只看 jwt 不看 serverAddress）** | **CTO 实测 "serveraddress must not be null" NPE** | **✅ 路径 B 实证** |
| **10** | **StateFlow in-memory cache 在 kill 时全丢** | **kill app 后 ViewModel scope 释放** | **✅ 路径 B 推论** |

audit 输出 = 给每条 ⚠ 一个 CONFIRMED / RULED_OUT 标签 + file:line 证据。

---

## 5. 5-leg gate 套用

| Leg | 内容 | 谁做 |
|---|---|---|
| L1 Spec | Handoff §登录/鉴权章 + AeroRadio v4 设计稿 + D-16 决策 + 本文件 §3 | data-integration 引证 |
| L2 Code | audit V3LoginAuthenticator + AuthStore + DynamicBaseUrlInterceptor + ReFreshTokenUtil + TaskManageUtils + Logout 链路 + V4Activity / SplashActivity nav 启动入口 + DataStore commit 路径 + EncryptedSharedPreferences 选型 | data-integration-2 |
| L3 Test | rewrite AuthStoreTest + V3LoginAuthenticatorTest + LogoutFlowTest + 新增 KillAppStateRecoveryTest（模拟 kill-app: 写 L1+L2 → 清 in-memory → 重读 持久化 → assert nav 决策） | data-integration-2 |
| L4 Data | CTO 真机循环：login → kill app → 重启 → 看 nav；login → uninstall → install → 看 nav；logout → restart → 看 nav；rememberMe toggle 关 → 看 LoginScreen prefill 行为 | CTO 真机配合 |
| L5 Smoke | critic-2 emulator 自动化 install/launch/login/kill/launch + adb screencap LoginScreen vs MainScreen 4 cycle | critic-2 |

**device-pending 借口禁止** — per `[[smoke-render-required]]`，Critic 真跑 emulator。

---

## 6. dispatch sequencing (Phase C 已 COMPLETE，NEXT-2 立刻可启)

```
Stage 0 (现在)   : PM framing v2 (D-16 升级) — DONE
                   PM 7 commits land in claude/v4-screens-on-refactor — DONE
                   APK 55MB fresh 2026-05-30 18:36 — DONE
Stage 1 (NOW)    : PM dispatch data-integration-2 NEXT-2 audit (per dispatch draft)
                   input: 本文件 §3 D-16 决策 + §4 10 候选清单 + §2 三根因
                   output: .state/api-snapshots/auth-split-brain-rootcause.md
                           + audit verdict on (c-1) vs (c-2) on 旧栈死路
                           + L1/L2 存储具体方案（DataStore vs EncryptedSharedPreferences vs JetSec Crypto）
                           + StartupActivity / V4Activity 启动入口 nav check 改写
                           + fix design + tests + apk
                   ETA: 4-5h calibrated audit + 4-6h fix（升级了，因为 D-16 范围扩大）
Stage 2 (audit done): PM 看 audit 报告
                       - 如全 c-1（旧栈死路）→ 直接 GO fix
                       - 如 c-2（旧栈活路）→ 升 CTO 决策"是否变更 D-13"
Stage 3 (fix GO)    : data 拿 build-slot, 写 fix + tests + APK
                       核心改动：
                       - AuthStore 分层重写（L1 明文 + L2 加密）
                       - ServerAddress 落 L1
                       - V4Activity.onCreate 加 atomic check + nav fork
                       - LoginScreen 加 rememberMe prefill 行为
                       - Logout flow 拆 L2-only 路径
                       - rememberMe toggle 关 → L1 清账号字段
                       - DataStore commit 确保 fsync 完成才 nav（用 Datastore.edit { }.firstOrNull() 同步）
                       - Manifest allowBackup=false 或 data_extraction_rules.xml exclude L2
Stage 4 (fix done) : critic 5-leg + emulator install/launch/login/kill/launch automation
                       + adb screencap LoginScreen vs MainScreen 4 cycle
Stage 5 (critic PASS): CTO 真机 4 cycle 复验：
                       - cycle 1: install + login + 看 MainScreen
                       - cycle 2: kill app + 重启 → 必须 LoginScreen 不是 NPE
                       - cycle 3: rememberMe 开 + login + kill + 重启 → LoginScreen 预填账号 + 空密码
                       - cycle 4: rememberMe 关 + login + kill + 重启 → LoginScreen 全空
                       - cycle 5: uninstall + install → LoginScreen 全空
Stage 6 (CTO PASS) : PM 串行落 ICD-AuthState v2.1 → v2.2 bump + commit batch
                     + 通告 fe-business / fe-platform / legacy（鉴权契约改了）
```

---

## 7. 5 teammate 状态 (NEXT-2 期)

| Teammate | Stage 1-2 | Stage 3 | Stage 4 | Stage 5-6 |
|---|---|---|---|---|
| data-integration-2 | WORKING (audit) | WORKING (fix + build-slot) | STANDBY | STANDBY |
| critic-2 | STANDBY | STANDBY | WORKING (5-leg) | STANDBY |
| fe-business-2 | STANDBY | STANDBY (除非 LoginScreen 改) | STANDBY | STANDBY |
| fe-platform-2 | STANDBY | STANDBY | STANDBY | STANDBY |
| legacy-native-2 | STANDBY | STANDBY (除非 c-2 + 旧栈改) | STANDBY | STANDBY |

Sonnet 优先，PM Opus lead。

---

## 8. 风险关联（v2 更新）

- **R-ALLOW-BACKUP** (新增 dormant)：D-16 fix 顺手解（backup_rules.xml exclude L2 + Manifest 调整）
- **R-ADDR-SLOT** (DORMANT-TRACKED)：本题 L1 改写涵盖。NEXT-2 done 后可 close。
- **ESC-WATCH-1** (旧栈无 refresh 端点)：本题 L2 token expiry 60h check 强制重登策略代替 refresh。NEXT-2 后 ESC-WATCH-1 可降级。
- **D-13 (Plan A 旧栈一行不改)**：仅 c-2 路径触发变更，audit 决断点。
- **D-16 (NEW 2026-06-01)**：方案 C "记住我" toggle 式两层存储 + 启动两步判断 + 退出独立 path + 原子不变量。本题主驱动。

---

## 9. PM 待 CTO GO

framing v2 done。下一步：PM 复制 `.state/next2-data-audit-dispatch-draft.md` (v2 已升级) → SendMessage data-integration-2。等 CTO GO。

— Written by PM (team-lead), v2 2026-06-01（D-16 升级）
