# NEXT-2 data audit dispatch — DRAFT (PM 待 CTO GO 后 SendMessage)

> Pre-staged dispatch message for data-integration-2 once CTO confirms PA-15 manual verify
> + GO on NEXT-2 auth split-brain audit. Copy verbatim into SendMessage.

---

## Dispatch message body

To: data-integration-2  
Summary: NEXT-2 audit auth split-brain (uninstall residual + dual-source)

```
## NEXT-2 dispatch — 鉴权 split-brain audit (read-only, no edits)

### 症状（CTO 实测 2026-05-30 demo）

adb uninstall com.htgd.radiocontrol.aeroradiocontrol + adb install app-debug.apk 后，
App 启动跳过登录页直接进主页，token 仍生效。本应回到"请登录"。

### PM 5-min grep 已实证的两根因

**根因 A (allowBackup auto-restore)**: 
- AndroidManifest.xml:57 `android:allowBackup="true"`
- res/xml/ 无 backup_rules.xml / data_extraction_rules.xml (只有 accessibility + network_security_config)
- → Android Auto Backup 把 shared_prefs + DataStore 全量备份+restore，uninstall 后 install 自动回填 token

**根因 B (双源持久化 split-brain)**:
- ReFreshTokenUtil.java:43 旧栈用 PreferencesUtil.getInstance().getEntity(Constant.key_tokenModel, GetTokenModel.class)
- TaskManageUtils.java:201 旧栈用 PreferencesUtil.getInstance().getField("serverAddress", mContext)
- 新栈 AuthStore (DataStore) 与旧栈 PreferencesUtil (SharedPreferences) 是两条独立通路

### CTO 决策的 fix scope（CTO 已 AskUserQuestion 选 "双修 a+b+c"）

(a) Manifest `allowBackup="false"` OR `backup_rules.xml` exclude — 不动旧栈 ✓
(b) Logout 流程加清 PreferencesUtil 的 token + serverAddress — 不动旧栈代码 ✓ (新栈侧 API 调用)
(c-1) 如旧栈死路 → 证明并文档化 → 不动旧栈 ✓
(c-2) 如旧栈活路 → 改旧栈读取 PreferencesUtil 改为读 AuthStore → 变更 D-13 ⚠

(c) 走 c-1 还是 c-2 由你的 audit 死路/活路结果决定，不预判。

### 你这一轮的任务 — L2 Code audit (read-only)

read-only：不改代码、不出 patch、不动 git、不 build。这一轮只是定位。

### 输入

- 框架文档：`.state/auth-split-brain-framing.md` (9 节，PM 已落)
- 上下文 memory：[[serveraddress-owned-by-data-integration]] / [[loginauthenticator-impl-owned-by-data]] / [[login-authenticator-seam]] / [[consumer-seam-binding-rule]] / [[ar010-permission-gate-on-v4]]
- 代码侧重点：
  - `app/.../data/repository/V3LoginAuthenticator.kt` — 登录 impl
  - `app/.../data/auth/AuthStore.kt` (或类似名) — DataStore 持久化
  - `app/.../data/auth/AuthInterceptor.kt` — 自动加 token 拦截
  - `app/.../data/network/DynamicBaseUrlInterceptor.kt` — base URL 注入
  - `app/.../utils/PreferencesUtil.java` — 旧栈 SharedPreferences 通用读写 (line 28-29 weird-but-not-bug: key 即 prefs file name)
  - `app/.../utils/ReFreshTokenUtil.java` — 旧栈 token model 读取入口
  - `app/.../utils/TaskManageUtils.java` — 旧栈 serverAddress 读取入口
  - `app/.../utils/Constant.java` — 端点 + key 常量 (key_tokenModel)
  - Logout 流程：grep 找退出登录代码（新栈 fe + 任何旧栈调用方）
  - v4 sole-launcher 后旧栈 activity 进入图：grep 找 startActivity / 反查 ReFreshTokenUtil / TaskManageUtils / 同 PreferencesUtil token reader 的 caller chain

### 输出 — `.state/api-snapshots/auth-split-brain-rootcause.md`

强制 6 段:

1. **根因 A (allowBackup) audit**:
   - AndroidManifest.xml 当前配置摘要 + line:N
   - data_extraction_rules.xml + backup_rules.xml 应有内容草案（exclude AuthStore preferences_pb 文件名 + 含 token 的 SharedPreferences files）
   - 推荐 fix 方案 A1 (allowBackup="false" 最简) vs A2 (xml exclude 精细) 利弊 + 推荐 + 提议

2. **根因 B audit — 双源真实情况**:
   - 列出新栈 AuthStore 的存档 key + DataStore 文件路径
   - 列出旧栈 PreferencesUtil 实际写入 / 读取 token + serverAddress 的全部 call sites (grep file:line)
   - 验证 V3LoginAuthenticator 登录成功后是否 mirror-write PreferencesUtil — 实际是 / 否
   - 验证 Logout 流程清几条源 — 实际清几条
   - 列出 in-memory cache 形态 (AuthStore StateFlow vs DataStore)

3. **(c-1) vs (c-2) 决断 — 旧栈死路 / 活路证据**:
   - 把 v4 sole-launcher 下旧栈 activity 调用图扫一遍（Manifest grep + 新栈 fe screen 的所有跳转 grep + intent target 反查）
   - ReFreshTokenUtil / TaskManageUtils / PreferencesUtil token 读取 caller chain
   - 列每个 caller "live / dead"，给具体引用 file:line
   - 总裁判 **(c-1) 旧栈死路** 或 **(c-2) 旧栈活路** 或 **(c-mixed) 部分活部分死**
   - **如 c-2 → 升级 PM "需 CTO 确认是否变更 D-13"，audit 不动手**

4. **配合 7 候选清单** (per framing.md §4):
   - 7 候选每条标 CONFIRMED / RULED_OUT / IRRELEVANT，给 file:line 证据
   - #2 in-memory cache 不同步 / #4 server-address race / #5 Logout 不全清 / #7 JWT TTL 60h refresh 错触发

5. **fix design**:
   - 根因 A fix 设计（含 data_extraction_rules.xml 草案如选 A2 路径）
   - 根因 B fix 设计 (b) Logout-side mirror-clear (新栈代码改) + (c) 死路证明 OR 活路改旧栈
   - 测试设计：reinstall-cycle test (uninstall → install → check token state)，Logout-clear test

6. **5-leg gate 标定**:
   - 本 audit 是 L1+L2，L3 fix 之后补，L4 由 CTO 实测 install/uninstall cycle 补，L5 critic emulator install cycle smoke
   - 列出 CTO ground truth 操作清单（adb logcat / curl /authorizations 看 token state）

### ICD 处理

- 如 fix 落地后需要 ICD bump（AuthState 持久化语义、Logout 清单契约）→ 写 proposed diff 到 `.state/api-snapshots/icd-proposals-next2.md`
- 不直接落 icd-contracts.md（同 PA-14/15 staging 纪律）

### 时限

- P0_BLOCKER (重装行为 = 鉴权回归)
- 估时 audit code ~2h + 7-candidate triage ~0.5h + fix design ~0.5h = 3h raw
- calibrated 4.8h
- ETA 4.8h

### 约束

- read-only：不改代码、不出 patch、不动 git、不 build
- 不持 build-slot（audit 不需 build）
- 不 escalate (c-2) 决策，**只是报 PM**，PM 升 CTO

### 交回流程

audit 完写 `.state/api-snapshots/auth-split-brain-rootcause.md` → SendMessage team-lead + verdict (c-1 / c-2 / c-mixed) + 7-candidate triage 总结。PM 看完后：
- (c-1) → 直接 dispatch fix
- (c-2) → 升 CTO 决策变更 D-13
- (c-mixed) → 升 CTO 决策范围

明白请回 ACK + ETA + 第一步动作。GO。

— team-lead
```

---

## PM checklist 在 SendMessage 之前

1. CTO 实测 PA-15 任务 Tab → confirm 14 task render + Hero "海王作息"
2. CTO GO NEXT-2
3. (可选) PM 先落 8 条 ICD bump 到 icd-contracts.md（PA-14 4 + PA-15 4），让 NEXT-2 audit 时有权威版可引
4. PM SendMessage data-integration-2 以上消息
5. ledger 开新 next2 节段（同 pa15 模板）

— Drafted by PM 2026-05-30，待 CTO GO 后激活
