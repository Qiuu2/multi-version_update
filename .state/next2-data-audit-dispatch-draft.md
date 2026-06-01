# NEXT-2 data audit dispatch — v2 D-16 升级（PM 待 CTO GO 后 SendMessage）

> v2 反映 CTO 2026-06-01 D-16 决策 + kill-app 新路径。替代 v1（uninstall 单路径）。

---

## Dispatch message body

To: data-integration-2  
Summary: NEXT-2 audit auth split-brain D-16 (kill-app + uninstall + 方案 C)

```
## NEXT-2 dispatch — 鉴权 split-brain audit (read-only)

### ⚠ 紧急升级 2026-06-01：路径 B kill-app 普通日常触发

之前以为 BLOCKER-3 只 uninstall 重装路径触发（已严重）。
今天 CTO 复现 **kill-app 路径**（更严重，日常即触发）：
  1. 第一次登录成功 → 进入主界面 ✓
  2. 后台滑掉杀 app
  3. 重新打开 → 直接进内部页 + "加载失败 serveraddress must not be null"

这是 P0_BLOCKER 升级到 P0_CRITICAL。

### CTO D-16 决策（2026-06-01）— 方案 C "记住我" toggle 式

详见 `.state/auth-split-brain-framing.md` v2 §3。核心：

**两层存储分清**：
- L1 凭据（rememberMe 控制，明文）：`account / serverHost / serverPort / rememberMe(bool)`
- L2 鉴权（token 控制，加密）：`token / refreshToken? / tokenExpiry`

**启动两步判断**（V4Activity.onCreate 或 SplashActivity）：
1. 读 L2 + 检 token + serverAddress + account 三件齐全 + tokenExpiry 未过 → 进 MainScreen
2. 否则 → atomic clear L2 残留 + 读 L1 → 跳 LoginScreen 带 prefill

**原子不变量**：L2 三件全有或全无。任一缺失 = clearL2Atomically + 回 LoginScreen。

**退出语义**：
- 主动登出 / token 过期 / 401 → 清 L2，留 L1
- rememberMe 切关 → 清 L1 account 字段，不动 L2
- 登录成功 → 写 L2 + (rememberMe 开则) 写 L1
- 各自独立 path，绝不混淆

### 你这一轮 — L2 Code audit (read-only)

read-only：不改代码 / 不出 patch / 不动 git / 不 build。这一轮只是定位。

### 输入

- **必读**：`.state/auth-split-brain-framing.md` v2（9 节，D-16 全部）
- 上下文 memory：[[serveraddress-owned-by-data-integration]] / [[loginauthenticator-impl-owned-by-data]] / [[login-authenticator-seam]] / [[consumer-seam-binding-rule]] / [[ar010-permission-gate-on-v4]] / [[constraint-vs-artifact-rule]]
- 代码侧重点：
  - `app/.../data/repository/V3LoginAuthenticator.kt` — 登录 impl
  - `app/.../data/auth/AuthStore.kt`（或 ServerConfig / similar）— DataStore 持久化
  - `app/.../data/auth/AuthInterceptor.kt` — 自动加 token 拦截
  - `app/.../data/network/DynamicBaseUrlInterceptor.kt` — base URL 注入
  - `app/.../ServerConfig.kt` — serverAddress 持久化路径
  - `app/.../ui/V4Activity.kt` (`.ui.V4Activity`) — nav 启动入口 — **重点查 onCreate / Composable 哪个 check 决定进 MainScreen vs LoginScreen**
  - 可能有 SplashActivity / EntryActivity / 类似启动屏
  - `app/.../utils/PreferencesUtil.java` — 旧栈 SharedPreferences 通用读写
  - `app/.../utils/ReFreshTokenUtil.java` — 旧栈 token 读取入口
  - `app/.../utils/TaskManageUtils.java` — 旧栈 serverAddress 读取入口
  - `app/.../utils/Constant.java` — 端点 + key 常量
  - Logout 流程：grep 退出登录代码
  - DataStore 写盘点：grep `dataStore.edit` / `.commit()` / `.first()` — 看每个 saveLogin 是否 await commit 完成

### 输出 — `.state/api-snapshots/auth-split-brain-rootcause.md`

**强制 8 段（v2 D-16 扩展）**：

1. **根因 A audit（allowBackup uninstall 路径）**:
   - AndroidManifest.xml 当前 line:N
   - data_extraction_rules.xml + backup_rules.xml 设计草案（exclude L2 加密区，可包含 L1 明文区是 UX 友好的；让 CTO 选）
   - 推荐 fix 方案 A1 (`allowBackup="false"` 最简) vs A2 (xml exclude 精细) 利弊 + 推荐

2. **根因 B audit（双源持久化）**:
   - 新栈 AuthStore 的存档 key + DataStore 文件路径
   - 旧栈 PreferencesUtil 实际写入 / 读取 token + serverAddress 的全部 call sites
   - 验证 V3LoginAuthenticator 是否 mirror-write PreferencesUtil — 实际是 / 否
   - 验证 Logout 是否清几条源 — 实际清几条
   - in-memory cache 形态 (AuthStore StateFlow vs DataStore vs ViewModel scope)

3. **★★ 根因 C audit（kill-app 状态丢失 — NEW, 路径 B 实证）**：
   - DataStore commit 路径：grep `dataStore.edit { ... }` 每个 saveLogin 写入点；DataStore 是 fire-and-forget? saveLogin 是否 await write completion（用 `.collect { }` first emit 或 `dataStore.data.first()` rerun）？
   - V4Activity / SplashActivity nav 启动入口：grep 哪个 Composable / Activity / Fragment 决定首屏；它 check 哪几个 AuthStore 字段；是否 atomic（所有 L2 三件都齐）；过期 check 有无
   - StateFlow scope：AuthStore singleton 是 ApplicationScope 还是 ProcessLifecycle scope？kill app 后重启 ApplicationScope 新创建，StateFlow 是空（必须从 DataStore 重读）；如果 nav 在 DataStore 读完前 evaluate → 命中
   - serverAddress 落到 DataStore 还是仅在 in-memory ServerConfig singleton？grep ServerConfig setter / getter
   - **生死判断 split-brain 真锤**：grep 启动入口 nav 决策代码 + 列出 file:line + 写"实际 check 了 X 字段 / 应 check 三件 + expiry"

4. **(c-1) vs (c-2) 决断 — 旧栈死路 / 活路**:
   - v4 sole-launcher 下旧栈 activity 调用图（同 v1）
   - ReFreshTokenUtil / TaskManageUtils / PreferencesUtil token 读取 caller chain
   - 列每个 caller "live / dead" + file:line
   - **(c-1) 死路** OR **(c-2) 活路** OR **(c-mixed)**
   - 如 c-2 → 升 PM "需 CTO 确认 D-13 变更"

5. **10 候选清单（v1 7 个 + v2 3 个）每条 CONFIRMED / RULED_OUT / IRRELEVANT + file:line**：
   - #1-7 同 v1 framing
   - #8 DataStore async 异步写盘未完成 + kill — 路径 B 实证
   - #9 Nav 启动入口 check 不全 — 路径 B 实证
   - #10 StateFlow in-memory cache 在 kill 时全丢 — 路径 B 推论

6. **fix design**:
   - 根因 A：Manifest allowBackup=false 或 backup_rules.xml exclude
   - 根因 B：Logout-side mirror-clear 旧栈 PreferencesUtil（新栈代码加调用）
   - 根因 C：
     * AuthStore 分层重写：L1 明文 DataStore "prefs_remember" + L2 加密 EncryptedSharedPreferences (现有 androidx.security:security-crypto:1.1.0-alpha06 已在依赖)
     * ServerAddress 落 L1，从 ServerConfig in-memory cache 升级到 L1 DataStore 持久化（grep `ServerConfig` 当前持久化是否仅 in-memory）
     * V4Activity / SplashActivity startup nav 改写：阻塞同步读 L2（不要在 Composable 内异步 collect — 先 await），三件 atomic check + expiry check，fork MainScreen / LoginScreen
     * LoginScreen 加 rememberMe prefill 行为
     * Logout 分两路径：L2-only (主动登出 + 401) vs L1-only (toggle 关)
     * 登录成功：写 L2 + (rememberMe 开则) 写 L1
   - 测试设计：
     * KillAppStateRecoveryTest（NEW）：模拟"写 L1+L2 → 模拟进程死（清 in-memory StateFlow，但 DataStore 残留）→ 重读 → assert nav 决策"
     * UninstallReinstallTest（concept）：用文件 system clear 模拟 uninstall + 验 allowBackup fix
     * LogoutPartialClearTest：assert L2 清 + L1 留 / L1 清 + L2 留 各自独立
     * RememberMeToggleTest：开/关切换 + 预填行为 + 登录成功后写入

7. **5-leg gate 标定**：
   - L1+L2 本 audit + fix 后写代码（你 audit + fix 都做）
   - L3 fix 之后补 tests
   - L4 由 CTO 实测 5 cycle（install/kill/restart/uninstall/install/toggle）补 ground truth
   - L5 critic emulator install/launch/login/kill/launch automation + screencap
   - 列 CTO ground truth 操作清单（adb logcat / adb shell ps / curl /authorizations 看 token state）

8. **ICD 影响范围**:
   - ICD-AuthState v2.1 → v2.2 bump 提议（写 `.state/api-snapshots/icd-proposals-next2.md`）
     * L1/L2 分层
     * StartupNavDecider 接口（启动两步判断契约化）
     * 退出语义 4 path
     * rememberMe toggle 契约
   - 不直接落 icd-contracts.md（PM 串行落）

### ICD 处理

- 4-5 条 ICD bump proposal 写 `.state/api-snapshots/icd-proposals-next2.md`
- 不直接落 icd-contracts.md

### 时限（v2 D-16 范围扩大）

- P0_CRITICAL（升级，日常触发）
- 估时 audit code ~3h + 10-候选 triage ~0.5h + fix design ~1.5h = 5h raw
- calibrated 8h（data domain 1.6）
- ETA 8h
- 完成后立刻接 fix（如 c-1）或 escalate（如 c-2）

### 约束

- read-only：不改代码、不出 patch、不动 git、不 build（audit 只）
- 不持 build-slot
- 不 escalate (c-2) 决策，**只是报 PM**，PM 升 CTO

### 交回流程

audit 完写 `.state/api-snapshots/auth-split-brain-rootcause.md` → SendMessage team-lead + verdict (c-1 / c-2 / c-mixed) + 10-候选 triage + 三根因实证总结。

明白请回 ACK + ETA + 第一步动作（建议从 V4Activity.onCreate 启动入口 grep 开始 — 因为 C-3 决断在 nav 决策一行字）。GO。

— team-lead
```

---

## PM checklist 在 SendMessage 之前

1. CTO 确认 D-16 决策已写入本文档 + framing v2 — DONE
2. CTO 给 GO NEXT-2 立刻 dispatch
3. PM SendMessage data-integration-2 以上消息
4. ledger 开新 next2 节段（同 pa15 模板，含 D-16 reference）

— Drafted by PM v2 2026-06-01，待 CTO GO 后激活
