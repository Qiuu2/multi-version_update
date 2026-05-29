# Gate 1 Review Package — AeroRadio v4 · Phase 0 计划批准 (v2)

> 作者：PM Agent · 日期：2026-05-27 · 分支：`claude/v4-screens-on-refactor`
> 触发：D-07 解冻 D-01（SPIKE-001 已回报）。流程：本包 → Critic 计划评审 → CTO Gate 1。
> 依据：`v3-audit-report.md`、`team-intake-notes.md`、`endpoint-inventory-draft.md`、`spike-aar64-report.md`、SKILL §1.2。

## R0. 修订记录（v2，回应 Critic DEL-GATE1-PLAN-v1 FAILED 的 5 点）

| # | Critic finding | 本版修订 |
|---|----------------|---------|
| F-1 (MAJOR) | 关键路径标注错误 | **重做 §2**：纯依赖关键路径 = AR-003→AR-005=19.6h（=登录闭环里程碑）；**新增「资源关键路径」= data-integration 4 任务 ~39h，才是 Phase 1 起点的真实约束**。同时澄清：按你 F-2，AR-001→AR-002 非硬边（AR-001 草案冻结答不了 O-1#3），故该边移除，28.5h 链不成立。 |
| F-2 (MAJOR) | AR-002 错误映射真正阻塞在 O-1 非 AR-001 | AR-002 错误映射 **hard-blocked-by O-1**；O-1 未答前只搭「可切换错误映射骨架（默认假设 + TODO + ICD_UPDATE 修正姿态）」。AR-001→AR-002 软依赖删除。 |
| F-3 (MINOR) | AR-005 刷新闭环依赖 AR-002 AuthInterceptor | AR-005 **depends 增 AR-002**（401 自动刷新链）。 |
| F-4 (MINOR) | 估算口径文字与数字不自洽 + AR-001 名实不符 | §1 口径改为「域系数 × **单个最高适用调整**，多 flag 不叠乘」；AR-001 正名「ICD-Endpoints-v1 **草案冻结(含 OPEN)**」，工时校正 12.0→12.5，并注明 O-1 答后有 v2 增量。 |
| F-5 (MINOR) | R-003 缺 owner 侧 fallback | §4 R-003 补「O-1 未答前按 documented-assumption DTO：`ignoreUnknownKeys=true` + sealed unknown 兜底，标记待 ICD_UPDATE」。 |
| F-6/7/8 (MINOR/INFO) | §7.3 字面 vs 意图 / AR-002 可拆 / G2 受 O-2 制约 | 见 §1 注脚 + §3 G2 说明。 |

---

## 0. 给 CTO 的一页纸

- **目标**：批准 Phase 0「地基」WBS + DAG + 估算，授权派发 9 个任务。
- **Phase 0 范围**：网络栈两个真空 P0（动态 baseUrl 拦截器 + AuthStore）+ 登录接真 + 端点权威 ICD 草案冻结 + 入口收敛 + IPC 协程化 + R-001 GO 后档案同步 + 设计 Token 校准。
- **真实关键约束（v2 校正）**：不是某条 19.6h 依赖链，而是 **`data-integration` 资源瓶颈——它独占 4/9 任务、~39h（占总量一半）**。Phase 1 起点（AR-001~004 全完）卡在它的吞吐上。建议 CTO 关注是否给 data-integration 加资源或允许 AR 任务跨 sprint。
- **Phase 0 估算合计 ~75.4 calibrated 小时**。
- **不在 Phase 0**：所有 Tab 接真（P1）、WS 实时（P1）、对讲 AAR（P1）、平板/地图/AI（P2-3）。
- **阻塞 Gate 的 OPEN 项**：O-1（后端 5 问）+ O-2（WS 文档）——建议 Gate 1 批准同时授权 PM 经 CTO 发起问询（见 §5）。

---

## 1. Phase 0 WBS（详细）

> **估算口径（v2 校正）**：`calibrated = raw × 域系数 × 单个最高适用任务调整`。**多 flag 不叠乘，取最高单项。** 域系数：fe-business 1.20 / fe-platform 1.40 / data 1.25 / legacy 1.60。任务调整：legacy_impact 1.50 / abi 1.40 / cross_domain 1.25 / icd 1.20 / novel 1.30。每个 L3 ≤ 16h。

| Task ID | 任务 | Owner | raw→cal (h) | P | flags(取最高) | depends_on | icd |
|---------|------|-------|:----:|:--:|------|-----------|------|
| **TASK-AR-001** | **ICD-Endpoints-v1 草案冻结(含 OPEN)**：endpoint-draft → `references/icd-contracts.md`，路径×方法最小单元，DTO 骨架（Phase0 触及的 auth 端点）+ 5 后端问题作 OPEN。**注：全量 DTO 实现在 Phase 1 各 Repository；本任务=契约文档冻结。O-1 答后有 v2 增量工时。** | data | 8→12.5 | P0 | cross_domain(1.25) | — | ICD-Endpoints-v1 |
| **TASK-AR-002** | **DynamicBaseUrlInterceptor + AuthInterceptor**（占位 baseUrl 替换、鉴权头注入、401 刷新）。**错误映射部分仅搭可切换骨架** | data | 6→9.0 | P0 | icd(1.20) | **O-1(硬, 仅错误映射)** | ICD-NetworkModule-v1 |
| **TASK-AR-003** | **AuthStore**（JWT+Refresh+ServerAddress，EncryptedSharedPreferences，并发刷新单测） | data | 8→10.0 | P0 | — | — | ICD-AuthState-v1 |
| **TASK-AR-004** | **单例 OkHttpClient 统一** + 旧栈 RequestManger 复用同实例（RISK-AUDIT-05） | data | 4→7.5 | P1 | legacy_impact(1.50) | AR-002, AR-003 | — |
| **TASK-AR-005** | **LoginScreen 接 AuthStore**（地址校验 + **JWT 刷新闭环**） | fe-business | 8→9.6 | P0 | — | AR-003, **AR-002(刷新闭环)** | — |
| **TASK-AR-006** | **入口收敛**（删 SignActivity LAUNCHER，仅留 V4Activity；纯 Manifest） | fe-business | 3→3.6 | P1 | — | — (D-03) | — |
| **TASK-AR-007** | **IPC 4521 协程化** LocalSocketClient + ShellCommand sealed | legacy-native | 8→12.8 | P2 | — | — | ICD-IPCSocket-v1 |
| **TASK-AR-008** | **R-001 GO 后档案同步**（build.gradle 注释 §8 diff + CLAUDE.md/SKILL RISK-AR-003 更正 + 真机清单交接工单） | legacy-native | 3→4.8 | P1 | — | (D-05/06 已批) | — |
| **TASK-AR-009** | **设计 Token 校准核对**（已落地 8 theme 文件 vs `design-system-spec.md`，补缺/纠偏） | fe-platform | 4→5.6 | P2 | — | — | ICD-DesignTokens-v1 |

**Phase 0 calibrated 合计 ≈ 75.4h**。按域：**data-integration 39.0h(AR-001/002/003/004)** · legacy-native 17.6h(007/008) · fe-business 13.2h(005/006) · fe-platform 5.6h(009)。

> **注脚（Critic F-6/F-7）**：
> - F-6：AR-004 改的是 `httptask/RequestManger.java`（非 `*Method.java`），协议 §7.3 字面不触发三方会签；但其为旧栈网络核心，**按意图扩展适用**，故标 legacy_impact + cross_domain，会签照走（保守正确）。
> - F-7（可选未采纳）：AR-002 可拆「baseUrl 拦截器」+「auth 拦截器」以隔离 O-1 风险；因未超 16h 阈值，暂不拆，由 data-integration 自行决定子任务分解。
>
> **与 SKILL 模板差异**：① AeroTheme 不从零建（审计 §4.2 证 8 文件已落地）→ AR-009 降为校准 4h；② 新增 AR-001（端点 ICD，Critic+data 判为一切迁移前置）；③ 新增 AR-008（R-001 档案同步，D-05/06）；④ abiFilters 无需改（spike §8 证），并入 AR-008 注释同步。

---

## 2. DAG + 关键路径（v2 重做）

```
外部阻塞 [O-1 后端5问] ──hard(仅错误映射)→ [AR-002]
                                              │
[AR-001 ICD草案冻结]  (无 Phase0 内部出边; 喂 Phase1 全部 Repository) ──D→ (Phase 1)
                                              │
[AR-002 拦截器(9)] ──┬──E→ [AR-004 单例OkHttp(7.5)]
                     │        ▲
[AR-003 AuthStore(10)]┴──┬────┘
                         └──D→ [AR-005 LoginScreen(9.6)]  (AR-005 亦依赖 AR-002 的401刷新)

[AR-006 入口收敛] · [AR-007 IPC] · [AR-008 档案同步] · [AR-009 Token校准]  —— 独立并行
```

- **无环**：通过（边集 O-1→002 / 002→004 / 003→004 / 003→005 / 002→005，无回边）。
- **纯依赖关键路径**：`AR-003 AuthStore(10) → AR-005 LoginScreen(9.6) = 19.6h`。**这同时是「登录接真闭环」里程碑**（Critic F-1：此处两者恰好重合，但二者概念不同）。
- **⚠ 资源关键路径（真实约束，Critic F-1 的实质）**：`data-integration` 独占 **AR-001+002+003+004 = 39.0h**（受其 max_capacity=3 与 AR-004 依赖 002+003 制约，串行上界 ~39h）。**Phase 1 起点解锁 = 这 4 个全 COMPLETED**，故 Phase 1 真实卡在 data-integration 吞吐，而非 19.6h 登录链。
- **高关注并行任务**：AR-001（12.5h，单任务最长之一 + 喂 Phase 1 全部 Repository，滑动直接拖 Phase 1 起点，虽不在登录关键路径上）；AR-007（12.8h，最长单任务，P2 但占 legacy 容量）。
- **O-1 外部依赖**：AR-002 错误映射 hard-blocked-by O-1；O-1 未答前 AR-002 按 documented-assumption 搭可切换骨架，O-1 回执触发 ICD_UPDATE（与 R-002 WS 同款姿态）。

---

## 3. Phase 1–3 outline（轮廓，详细 WBS 待 Gate 1 后逐 Phase 出）

| Phase | 工作包 | 关键任务（owner） | Gate |
|-------|--------|------------------|------|
| **P1 接真数据** | 实时框架 | RealtimeClient WS+心跳+退避(fe-platform, 依 AR-003/004)、轮询回退、断线 banner | **G3** |
| | 终端 Tab 接真 | TerminalRepository(data, 依 AR-001)、TerminalHubScreen ViewModel(fe-business)、分区详情多选 | |
| | 广播 Tab 三档 | Segmented(fe-business)、寻呼/点播(fe-business)、**对讲接 AAR**(legacy-native, 依 spike, 用 **CallBackIntf(21回调) 非 NativeTalkListener** — DP-4) | |
| **P2 完整 Tab** | 任务/服务/地图 | TaskRepository(data)、任务屏(fe-business)、ServerState、百度地图+定位(legacy, RISK-AR-007) | — |
| **P3 打磨发布** | 平板/清理/AI | AdaptiveScaffold(fe-platform)、TerminalHubTablet、旧栈清理(data)、AI 套件(P3 可降级) | **G4** |

> **G2 架构评审**（Critic F-8）：建议 Phase 0 末触发，输入 = 拦截器(AR-002)/AuthStore(AR-003)/WS 协议三方案；其中 **WS 方案受 R-002 文档制约，G2 时点须与 O-2 回执对齐**（文档未到则 WS 架构只能按 DRAFT 评，标条件性）。

---

## 4. 风险登记册快照（Gate 1 版 v2）

| ID | 风险 | 概率 | 影响 | score | 状态 | 缓解 |
|----|------|:----:|:----:|:----:|------|------|
| R-001 | 32 位 ABI / 对讲不可用 | 0.10↓ | LOW↓ | 0.10↓ | LOW-条件性(GO-pending-device) | spike 已降级(D-05)；真机回执后关闭(D-06) |
| R-002 | WS 协议文档未到，ICD-BroadcastWS DRAFT | 0.60 | HIGH | 0.42 | ACTIVE | 索要文档；**owner 侧 fallback：按 DRAFT 搭骨架，落地 ICD_UPDATE** |
| R-003 | 端点契约 5 问未答 → 迁移返工/ICD churn | 0.55 | HIGH | 0.41 | ACTIVE | 经 CTO 问后端(O-1)；**owner 侧 fallback(v2 补)：未答前按 documented-assumption DTO（`ignoreUnknownKeys=true` + sealed unknown 兜底），标记待 ICD_UPDATE；AR-002 错误映射搭可切换骨架** |
| R-AUDIT-02 | 双 LAUNCHER | — | MED | — | AR-006 收敛(D-03) |
| R-AUDIT-04 | 安全债(硬编码签名口令/sharedUserId/重权限/百度Key) | 0.50 | MED | — | 签名移 signingConfigs+local.properties；权限合规审查(Phase0末/P2) |

> R-001 退出头号位。当前最高 **R-002(0.42)/R-003(0.41)**，**两者 v2 均已补 owner 侧「假设推进 + ICD_UPDATE 修正」fallback**（不再是只会「等」）。

---

## 5. ⚠ 需 CTO 决策/授权的 OPEN 项

- **O-1**｜后端契约 5 问（阻塞 AR-002 错误映射 + Phase 1 迁移）：①多方法 DTO 是否各异 ②统一响应包络 code/message/data ③成功判定=业务码 vs HTTP 2xx ④{id}/{type} 路径参数 vs query ⑤multipart 字段名。→ 授权 PM 整理问询经 CTO 转后端。
- **O-2**｜WS 协议文档（阻塞 P1 实时 + G2 的 WS 架构）。→ 同 O-1 向厂商索要。
- **O-3**｜x86_64 `.so`（P3，非阻塞）：让 CTO 的 AS 模拟器也能跑语音，搭 O-2 一并问。
- **O-4**｜端点口径确认：「21 端点」= 路径×方法 ~40+ 操作，ICD 路径×方法为最小单元。
- **O-5**｜G2 触发点：建议 Phase 0 末（与 O-2 对齐）。

---

## 6. Go / No-Go 请求

请 CTO 给 **APPROVE / REQUEST_CHANGES / REJECT**：① Phase 0 WBS（9 任务）② DAG + 资源关键路径（data-integration 39h）③ 估算口径（域系数 × 单项最高，不叠乘）④ ICD-Endpoints-v1 口径（路径×方法）⑤ 授权 O-1/O-2 问询。

> APPROVE 后 PM 派根任务 AR-001/AR-003（无前置）+ AR-006/007/008/009（独立）；AR-002 待 O-1（错误映射骨架可先起）；AR-004 待 002+003；AR-005 待 003+002。所有产出先过 Critic。

---

*gate1-review-package.md v2 — Generated by PM Agent · 待 Critic DELTA 复审 → CTO Gate 1*
