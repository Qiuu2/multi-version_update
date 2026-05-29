# Gate 2 Review Package — AeroRadio v4 · 架构评审 + Phase 0 Retrospective

> 作者：PM Agent · 日期：2026-05-27 · 触发：Phase 0 全 10 件（SPIKE + AR-001~010）Critic PASS
> 流程：本包 → Critic holistic 跨域一致性确认 → CTO Gate 2（APPROVE / REQUEST_CHANGES / REJECT）
> G2 mandate（SKILL §8.1）：CTO 评审跨领域架构一致性——拦截器方案 / AuthStore / WS 协议 / IPC 桥接 / Compose 架构

---

## 0. 给 CTO 的一页纸

- **Phase 0「地基」全部落地并经 Critic 验收**：网络栈现代化（动态 baseUrl + 鉴权 + 双栈隔离）、AuthStore、登录端到端、权限编排、IPC 客户端、设计 Token、入口收敛、R-001 档案同步。
- **跨域架构自洽**：6 个 ICD 登记（4 LIVE + 1 DRAFT-FROZEN + 若干 DRAFT），Critic **增量验过两侧契约一致**（登录 seam、AuthState、注册表）。
- **3 个架构方案请您 Gate**：① 拦截器/双栈隔离 ② AuthStore 鉴权状态 ③ IPC 桥接——均已建成+验收。**WS 协议方案为 DRAFT-条件性**（卡厂商 O-2 文档），建议 G2 **有条件批准**（WS 待 O-2 回执补全后增量过审）。
- **Phase 1 起点卡 2 个外部回执**（O-1 后端 / O-2 厂商）——**仍待您转发问询包**。

---

## 1. 跨域架构总览（as-built，已验收）

```
┌─ Frontend-Business ──────────┐   ┌─ Frontend-Platform ─────────┐
│ LoginScreen(5态)+ViewModel    │   │ DesignTokens v1.1(LIVE)      │
│ 权限编排(冷启动 critical)      │   │ AdaptiveScaffold (Phase3)    │
│ 入口收敛(V4 唯一 LAUNCHER)     │   │ RealtimeClient(WS) ⚠DRAFT    │
└──────┬───────────────────────┘   └──────┬──────────────────────┘
       │ consumes                          │ consumes
       ▼ (ICD-LoginAuthenticator/AuthState/DesignTokens)
┌─ Data-Integration ───────────────────────────────────────────┐
│ DynamicBaseUrlInterceptor(/api) + AuthInterceptor(401→refresh)│
│ 单例 OkHttpClient  ──newBuilder/clear拦截器──►  @LegacyOkHttp  │
│ AuthStore(Encrypted, refresh去重, 密码不落盘)                  │
│ RetrofitLoginAuthenticator(seam impl) · ResponseSuccessPolicy │
│ ICD-Endpoints-v1(DRAFT-FROZEN, 候 O-1)                         │
└──────┬────────────────────────────────────────────┬──────────┘
       │ 共享池(零行为代价)                            │ (旧栈不继承新拦截器)
       ▼                                              ▼
┌─ Legacy-Native ──────────────┐         ┌─ 旧栈 RequestManger ────┐
│ LocalSocketClient(4521,协程)  │         │ 回调路径零改, 自带       │
│ AAR/ABI: R-001 LOW-条件性     │         │ ServerToken header      │
│ 真机清单(待 CTO/QA)           │         │ (迁移期双栈鉴权互不扰)   │
└──────────────────────────────┘         └─────────────────────────┘
```

## 2. 三个架构方案（请 Gate）

### 2.1 拦截器 / 双栈隔离方案 ✅（已建+验收）
- **DynamicBaseUrlInterceptor**：placeholder.invalid → `http://host:port/api`（/api 单处注入，无双带，Critic grep 全 ApiService 验）。
- **AuthInterceptor**：Bearer 注入（/authorizations 豁免）；401 → `AuthStore.refresh(knownStaleJwt)` → 重试一次/失败重登。与 AuthStore 去重契约逐字对齐。
- **双栈隔离（RISK-AUDIT-05 解）**：新栈单例 OkHttpClient；旧栈 `@LegacyOkHttpClient` = `newBuilder()` **共享 Dispatcher+ConnectionPool** 但 `clear()` 拦截器 → 旧栈请求字节级不变、不被新拦截器误改、ServerToken header 保留。§7.3 三方会签 PASS。
- **错误映射**：`ResponseSuccessPolicy` 可切换骨架，默认 HTTP2xx，**hard-blocked by O-1#3**（成功判定=业务码 vs 2xx），回执后换绑定。
- **决策点**：此方案是否 Gate 批准定型。

### 2.2 AuthStore 鉴权状态方案 ✅（已建+验收，ICD-AuthState-v2 LIVE）
- jwt/refresh → EncryptedSharedPreferences；host/account → 普通 prefs（预填）；**密码永不落盘**。
- 并发刷新去重（RISK-AR-001）：Mutex + 时序无关双检（去重键=knownStaleJwt），10 并发 401 → 1 次网络刷新（Critic 真并发测验）。
- `TokenRefresher` 可切换 seam：当前 `UnsupportedTokenRefresher`（401→重登）；**真实刷新 impl hard-blocked by O-1 D-1**（旧栈无 refresh 端点 → ESC-WATCH-1 密码存储抉择待您）。
- **决策点**：① 方案定型；② D-1 回执后的刷新策略（A 请厂商暴露 refresh 端点 / B 加密存密码 / C 短会话重登）——**回执到了我带 3 选项升级**。

### 2.3 IPC 桥接方案 ✅（已建+验收，ICD-IPCSocket-v1）
- `LocalSocketClient`（127.0.0.1:4521 协程封装，Mutex+5s 超时+有界重连+Result）替代旧 SocketClient（孤儿死码）。
- `ShellCommand` = **Raw 透传骨架**，命令词表 **hard-blocked by O-4**（厂商守护进程协议未文档化，疑 su/root）。
- **决策点**：方案定型；命令词表待厂商 O-4 回执。

### 2.4 Compose / 前端架构（横切，已验收）
- ViewModel + StateFlow + 5 态 UI 模式（Login 为范本）；**consumer-defined seam DI 模式**（LoginAuthenticator/TokenRefresher：消费方定接口、提供方实现+单点 @Binds）；设计 Token（CompositionLocal + M3 桥接，v1.1 含别名映射）；权限编排（冷启动 critical + 运行时二次校验归 VoiceTalkAdapter/Phase1）。
- **WS 实时方案 ⚠ DRAFT**：连接生命周期/退避/单例防风暴可定，但消息字段/鉴权/心跳卡 O-2 厂商文档。**建议 G2 对 WS 有条件批准**（O-2 回执后增量过审 + ICD-BroadcastWS DRAFT→LIVE）。

## 3. 跨域 ICD 一致性（Critic 增量验过）

| ICD | 状态 | 一致性证据 |
|-----|------|-----------|
| ICD-AuthState-v2 | LIVE | Critic 核 §3 == AuthStore.kt 逐字；注册表 v2 一致(X-2) |
| ICD-LoginAuthenticator-v1 | LIVE | seam 两侧(AR-005 消费 + AR-002 impl)契约 Critic 验一致；@Binds 单点(kapt 证) |
| ICD-DesignTokens-v1.1 | LIVE | spec=ICD=Handoff 三方一致(R-2 实证)；别名映射非破坏 |
| ICD-NetworkModule-v1 | LIVE | /api 前缀 + 拦截器顺序 |
| ICD-IPCSocket-v1 | LIVE | connectionStateFlow 对齐(§7 ShellCommand 注待补) |
| ICD-Endpoints-v1 | DRAFT-FROZEN | 路径×方法口径；5 OPEN 候 O-1 |
| ICD-BroadcastWS/RealtimeFallback/VoiceAAR/MapLocation | DRAFT | Phase 1/2 落地，待外部回执/任务 |

> Housekeeping（非阻塞）：X-1 TaskDto 注册表占位无 section（Phase1 补）、X-3 IPC §7 ShellCommand 注（PM 待写）。

## 4. Phase 0 Retrospective

- **吞吐**：10 件（SPIKE+9 AR），单 sprint 完成；data-integration 39h/52% 是实测瓶颈（如 G1 预判）。
- **Critic 首过率 ~67%**（6/9 AR 首过）；3 次 FAILED→DELTA **全是机械/标注类**（CRLF×2、共享文件越界、失实自陈），**零设计返工**——说明方案质量高、把关有效。
- **风险实况**：R-001 CRITICAL→LOW（spike，最大成果）；RISK-AUDIT-05 双栈池解（AR-004）；UNK-001 关闭。**新发现风险**：共享工作树并发危害（CRLF×3 / 共享文件混提 / 编译瞬态误报 / ICD 注册表竞写 / Hilt 绑定竞态 / daemon stale）——**催生 6 条团队标准**（STD-CRLF/SHAREDFILE/COMPILABLE/ICD-WRITE/CANONICAL-STATE/DAEMON-STALE），已固化。
- **设计偏离**：经 ICD 治理消化（设计 Token R-1~R-5 走别名/补录/对齐，非静默漂移）；R-1 真改名(breaking)登记 Phase3 待 CTO。
- **诚信文化**：teammate 多次主动据实纠错（11→10 测数、失实自陈致歉、不谎报"测过"）——gate 文化健康。

## 5. ⚠ 仍待 CTO（Phase 1 起点 / 收尾前置）

| 项 | 性质 | 影响 |
|----|------|------|
| **转发问询包** `.state/inquiry-package-for-cto.md` | O-1→后端 / O-2-4→厂商 | **Phase 1 起点 + WS 方案 + D-1 抉择全卡此** |
| **跑真机清单** `.state/realdevice-checklist-handoff.md` | arm64 真机 10min | 关闭 R-001（LOW→CLOSED） |
| **D-1 密码存储抉择** | 待 O-1 回执后我带 3 选项升级 | AuthStore 刷新策略定型 |

## 6. G2 Go / No-Go 请求

请 CTO 给 **APPROVE / REQUEST_CHANGES / REJECT**：
1. 拦截器 / 双栈隔离方案（2.1）
2. AuthStore 鉴权状态方案（2.2）
3. IPC 桥接方案（2.3）
4. Compose / 前端架构（2.4）
5. **WS 协议方案有条件批准**（DRAFT，O-2 回执后增量过审）

> APPROVE 后进 **Phase 1（接真数据）**——但 Phase 1 多数任务（终端/广播 Tab 接真、WS 实时）卡 O-1/O-2 外部回执，故强烈建议 **G2 批准同时您转发问询包**。

---

*g2-review-package.md — Generated by PM Agent · 待 Critic holistic 一致性 → CTO Gate 2*
