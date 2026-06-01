# NEXT-2 — 鉴权 split-brain framing 文档

> PM framing 工件，PA-15 done 后直接 dispatch data-integration-2 进 audit。
> 写于 2026-05-30，CTO 与 PM 协同 framing 期间；不替 fix scope 决策、只把决策与 sequencing 落字。

---

## 1. 症状（CTO 确认）

**复现路径**：`adb uninstall com.htgd.radiocontrol.aeroradiocontrol` + `adb install app-debug.apk` 后，App 启动**跳过登录页直接进主页**，Token 仍生效。

**期望**：uninstall 后 token / serverAddress / 用户态全清，重装从登录页开始。

**症状级**：BLOCKER（鉴权侧重装回归丢失 + 隐含 split-brain 风险）

---

## 2. 两个根因（PM 5-min grep 实证）

### 根因 A — `allowBackup=true` 直接解释当前症状

```
app/src/main/AndroidManifest.xml:57   android:allowBackup="true"
app/src/main/res/xml/                 NO backup_rules.xml / data_extraction_rules.xml
                                      只有 accessibility + network_security_config
```

**机制**：Android Auto Backup 在 uninstall 时把 `/data/data/<pkg>/shared_prefs/*` + `/data/data/<pkg>/files/datastore/*` 备份到 Google Drive / Backup Transport；同签名 APK install 时自动 restore。AuthStore (DataStore) **+** PreferencesUtil (SharedPreferences) token 全部回填。

**实证**：grep 命中 1 行。无 backup rules 文件存在 = 默认全量备份生效。

### 根因 B — 双源持久化 split-brain (隐性)

新栈 `AuthStore` (DataStore Preferences) 与 旧栈 `PreferencesUtil` (SharedPreferences) 是两条独立持久化通路：

```
ReFreshTokenUtil.java:43   PreferencesUtil.getInstance().getEntity(Constant.key_tokenModel, GetTokenModel.class, mcontext)
TaskManageUtils.java:201   PreferencesUtil.getInstance().getField("serverAddress", mContext) + Constant.getServerState
```

**实证**：旧栈用 `PreferencesUtil` 自己读 token model + serverAddress，**不读 AuthStore**。

**潜在 split-brain（待 audit 确认是否真发生）**：
- 新登录 → 写新 token 到 AuthStore ✓ 但是否 mirror-write 到 PreferencesUtil？**待验证**
- Logout → 清 AuthStore，是否清 PreferencesUtil 同源？**待验证**
- v4 sole-launcher 后 `ReFreshTokenUtil` / `TaskManageUtils` 是否仍被调用？**待验证**（如全死路 = 隐性问题不实际发生；如仍活路 = 真 split-brain）

---

## 3. CTO 决策的 fix scope (2026-05-30 AskUserQuestion)

**双修 a + b + c**：

| 子项 | 内容 | D-13 (Plan A 旧栈一行不改) 影响 |
|---|---|---|
| (a) | Manifest `allowBackup="false"` **或** `backup_rules.xml` exclude AuthStore + Preferences token files | 不动旧栈 ✓ |
| (b) | Logout 流程加清 PreferencesUtil 的 token + serverAddress（新栈侧添加调用清理写到旧栈那个 SharedPreferences file 名） | 不动旧栈代码 ✓（仅新栈新增清理调用 SharedPreferences API） |
| (c-1) | **如果**旧栈死路 → 证明并文档化 ReFreshTokenUtil / TaskManageUtils / 其他 PreferencesUtil 读 token 调用方在 v4 sole-launcher 后无活路 | 不动旧栈 ✓ |
| (c-2) | **如果**旧栈活路 → 改旧栈读取 PreferencesUtil 改为读 AuthStore | **变更 D-13 ⚠** — 需 CTO 再确认 |

(c) 走 c-1 还是 c-2 由 audit 死路/活路结果决定，**不预判**。

---

## 4. PM 假设清单（split-brain 经典 7 候选 + 命中情况）

| # | 候选路径 | grep 实证 | 命中? |
|---|---|---|---|
| 1 | 双源持久化分裂 (DataStore vs SharedPreferences) | ReFreshTokenUtil + TaskManageUtils 旧栈用 PreferencesUtil 命中 | ✅ 待 audit |
| 2 | In-memory cache 不同步 (AuthStore StateFlow vs DataStore) | 未 grep | ⚠ 待 audit |
| 3 | 重装残留 (allowBackup auto-restore) | Manifest:57 allowBackup=true 命中 | ✅ 实证 |
| 4 | Server-address race (Interceptor singleton 缓存) | R-ADDR-SLOT 已 dormant tracked | ⚠ 待 audit |
| 5 | Logout 不全清 | 未 audit Logout 代码 | ⚠ 待 audit |
| 6 | 新旧栈共存窗口 (v4 sole-launcher 后是否真死路) | TaskManageUtils 仍存在 | ⚠ 待 audit (c-1 vs c-2 决断点) |
| 7 | JWT TTL 60h vs doc 24h refresh 错触发 | TokenRefresher 是 TODO impl | ⚠ 待 audit |

audit 输出 = 给每条 ⚠ 一个 CONFIRMED / RULED_OUT 标签 + file:line 证据。

---

## 5. 5-leg gate 套用

| Leg | 内容 | 谁做 |
|---|---|---|
| L1 Spec | Handoff §登录/鉴权章 + AeroRadio v4 设计稿 → 引 line 范围 | PM 已知（再 grep 确认） |
| L2 Code | data audit V3LoginAuthenticator + AuthStore + DynamicBaseUrlInterceptor + ReFreshTokenUtil + TaskManageUtils + Logout 链路 + v4 sole-launcher 下 *Method.java 调用图 → `.state/api-snapshots/auth-split-brain-rootcause.md` | data-integration-2 (post-PA-15) |
| L3 Test | V3LoginAuthenticatorTest + AuthStoreTest + LogoutFlowTest 添 mirror-clear + uninstall-cycle 断言 | data 实施 |
| L4 Data | CTO 测 install → login → 拉数据 → uninstall → install → 看是否跳登录 + 拉数据 — adb logcat / curl 看 token state. capture 到 `.state/auth-uninstall-reinstall-evidence-2026-XX-XX.txt` | CTO 配合（PA-15 之后） |
| L5 Smoke | critic-2 emulator 跑 `install / uninstall / install / boot` 4 段 + adb screencap 每段 LoginScreen vs MainScreen → `.state/smoke-snapshots/0X-auth-cycle-*.png` | critic-2 (post-fix) |

**device-pending 借口禁止** — per `smoke-render-required` memory，Critic 必须真跑 emulator。

---

## 6. dispatch sequencing (PA-15 done 后启动)

```
Stage 0 (现在)   : PM framing 落地 (本文件) — DONE
Stage 1 (PA-15 done): PM dispatch data-integration-2 NEXT-2 audit
                      input: 本文件 §4 7 候选清单 + §2 实证起点
                      output: .state/api-snapshots/auth-split-brain-rootcause.md
                              + audit verdict on (c-1) vs (c-2)
                              + fix design (含 backup_rules.xml 草稿)
                      ETA: 2-3h
Stage 2 (audit done): PM 看 audit 报告
                      - 如 (c-1) → 直接 GO fix
                      - 如 (c-2) → 升 CTO 决策"是否变更 D-13"
Stage 3 (fix GO)     : data 拿 build-slot, 写 fix + tests + APK
                      ETA: 3h
Stage 4 (fix done)   : critic 5-leg + emulator install-cycle smoke
                      ETA: 1.5h
Stage 5 (critic PASS): CTO 手动复验 (install/uninstall/install/check)
Stage 6 (CTO PASS)   : PM 串行落 ICD-AuthState bump + commit batch
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

最少必要 spawn 原则沿用，model = Sonnet (lead PM Opus)。

---

## 8. R-? 新风险 + 已知风险关联

- **R-ALLOW-BACKUP** (新增 dormant)：`allowBackup=true` 不仅影响鉴权 — 用户的所有 app-private data（包括可能未来的语音录音、缓存、用户偏好）都会被 Google Drive 备份。GDPR/隐私合规面也有问题。fix 应顺手降低为 false 或精细 exclude。
- **R-ADDR-SLOT** (DORMANT-TRACKED)：与本题根因 B 同源 (双源 serverAddress)。NEXT-2 audit 顺手覆盖此风险，可降级或 close。
- **ESC-WATCH-1** (旧栈无 refresh 端点)：与本题根因 B-7 同源 (JWT TTL refresh)。NEXT-2 audit 顺手输出 TokenRefresher 状态报告，决定是否升 ESC-WATCH-1。
- **D-13 (Plan A 旧栈一行不改)**：c-2 路径触发变更，audit 决断点；非命中则 D-13 保持不动。

---

## 9. PM 后续动作

PA-15 done 之前 PM 不动：
- 不 dispatch
- 不开 build-slot
- 不写 audit
- 仅 holding pattern + 收 data progress

PA-15 done 立即：
- 用本文件作 dispatch message 主体派 data-integration-2 进 NEXT-2 audit
- ledger `pa15` 节段 close + 新 `pa16-next2-auth-split-brain` 节段开

— Written by PM (team-lead), 2026-05-30
