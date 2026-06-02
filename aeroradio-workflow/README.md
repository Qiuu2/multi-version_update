# AeroRadio Multi-Agent Workflow

**版本**：1.0.0
**用途**：AeroRadioControl v4 Android 项目的 6-Agent 协作开发工作流
**衍生自**：`itc-enterprise-workflow.skill` v1.0.0

---

## 这是什么

这是一个 Claude Code (或类似 LLM agent 编排平台) 用的 **skill 包**，定义了一个 6 Agent 协作团队，专门用于完成 **AeroRadioControl v4** 这一个 Android 项目（校园广播终端控制 App）的完整开发。

不是通用工作流；是为这个项目从地基到发布全程定制的。

### 项目背景速览

- **项目**：AeroRadioControl v4（Android Studio Panda 4 项目）
- **客户端形态**：手机 + 平板（adaptive layout）
- **后端**：每个校园一台 LAN 广播主机，21 个 REST 端点 + 一条 WebSocket 推送通道，明文 HTTP
- **现状**：旧栈 OkHttp + 手动 URL + callback (`httptask/*Method.java`)，要逐步迁到 Retrofit + Hilt + coroutines + Compose
- **特殊约束**：
  - 服务器地址用户登录时输入 → 动态 baseUrl
  - 语音对讲走 `libs/htapplib.aar`，**只有 32 位 ABI**（armeabi-v7a + x86）
  - 百度地图 SDK（合规弹窗 + 延迟权限请求）
  - 本机 TCP socket `127.0.0.1:4521` 走 shell 命令协议

---

## 6 Agent 拓扑

```
                    Human CTO
                       │
                       ▼
              ┌────────┴────────┐
              │ Project Manager │   ← L3 调度
              └────────┬────────┘
                       │
       ┌───────────────┼───────────────┬───────────────┐
       │               │               │               │
       ▼               ▼               ▼               ▼
  Frontend-       Frontend-          Data-           Legacy-
  Business        Platform        Integration         Native
  (业务 UI)       (WS/Theme/      (网络数据层)      (TCP/AAR/
                  Adaptive)                          百度地图/ABI)
       │               │               │               │
       └───────────────┴───────────────┴───────────────┘
                               │
                               ▼
                          ┌──────────┐
                          │  Critic  │  ← L2 横切评审
                          └──────────┘
```

### 每个 Agent 的角色

| Agent | 职责 | 典型产出 |
|-------|------|---------|
| **Project Manager** | 任务调度、DAG 维护、状态机管理、风险监控 | TASK_ASSIGN、daily report、gate review 包 |
| **Critic** | 横切质量评审、对抗式提问、设计偏离检测 | REVIEW_RESULT、escalation 报告 |
| **Frontend-Business** | 5 个 Tab 的 Compose 屏 + ViewModel + Navigation | `feature/{tab}/*.kt` |
| **Frontend-Platform** | WebSocket / Polling fallback / AeroTheme / AdaptiveScaffold / 通知 | `ui/theme/*.kt`、`ui/platform/*.kt` |
| **Data-Integration** | Retrofit + 21 REST 端点 + AuthStore + Repository + 旧栈迁移 | `data/network/`、`data/api/`、`data/repository/` |
| **Legacy-Native** | TCP socket / 语音 AAR 适配 / 32 位 ABI / 百度地图 | `data/ipc/`、`data/voice/`、`data/map/` |

---

## 目录结构

```
aeroradio-workflow/
├── README.md                         ← 本文
├── SKILL.md                          ← 顶层入口（调用本工作流时第一份读的文件）
│
├── agents/                           ← 6 个 Agent 的完整配置
│   ├── project-manager/
│   │   ├── profile.md                  身份与职责
│   │   ├── soul.md                     驱动力 / 价值观 / 行为模式
│   │   ├── skill.md                    具体技能（WBS 模板 / 调度算法 / 通信协议）
│   │   └── memory.md                   历史数据 / 团队画像 / 风险库 / 学习闭环
│   ├── critic/                       ← 同上 4 件套
│   ├── frontend-business/            ← 同上
│   ├── frontend-platform/            ← 同上
│   ├── data-integration/             ← 同上
│   └── legacy-native/                ← 同上
│
└── references/                       ← 跨 Agent 共享的参考文档
    ├── agent-directory.md              Agent 目录 + 通信矩阵
    ├── communication-protocol.md       消息格式 + 状态机 + 评审流程
    ├── icd-contracts.md                跨 Agent 接口契约（11 个 ICD）
    └── design-system-spec.md           从 Handoff.html 提炼的设计规范
```

---

## 怎么用

### 启动一个项目

1. **CTO 提交 PRD**：把 `AeroRadio_v4.html`（5 Tab IA）+ `Handoff.html`（设计规范）+ 后端定性（21 个端点等）交给 PM
2. **PM 做 WBS**：调用 `agents/project-manager/skill.md` §1.2 的 WBS 模板，拆出 Phase 0 → Phase 3 所有任务
3. **PM 提 G1 Plan Approval**：交 CTO 过 Gate 1
4. **PM 派任务给 Domain Agent**：用 `TASK_ASSIGN` 消息（格式见 `references/communication-protocol.md` §2.1）
5. **Agent 提交产出**：用 `DELIVERABLE` 消息
6. **Critic 评审**：按对应 Domain 的 checklist（`agents/critic/skill.md` §3）评审，返回 `REVIEW_RESULT`
7. **过 Critic 后**：进入 CTO Gate（G2 架构 / G3 Phase 1 Go-NoGo / G4 Final Release）

### 4 个 Quality Gate

| Gate | 触发时机 | Approver | 检查内容 |
|------|---------|----------|---------|
| **G1 Plan Approval** | WBS 完成 | CTO + Critic | DAG 完整性、估算合理性、风险登记 |
| **G2 Architecture Review** | 关键架构方案出 | CTO + Critic | 拦截器、WS 协议、IPC、Compose 架构 |
| **G3 Phase 1 Go/No-Go** | Phase 1 末 | CTO + Critic | 登录通、终端 Tab 真数据、Critic pass rate ≥ 70% |
| **G4 Final Release** | Phase 3 末 | CTO + Critic | 全 Tab 通、平板版、签名、设计偏离 ≤ 阈值 |

### Agent 间禁止直接通信

所有跨 Domain 协作必须通过 PM 路由。Domain Agent 之间**不可直接 import**对方的内部类——必须通过 `references/icd-contracts.md` 里定义的 ICD 接口。这一条由 Critic Agent 在评审中强制（`ARC-ERR-003`）。

---

## 11 个 ICD（接口控制文档）

定义在 `references/icd-contracts.md`：

| ICD | Producer | 关键作用 |
|-----|----------|---------|
| `ICD-NetworkModule-v1` | Data-Integration | 动态 baseUrl 拦截器接口 |
| `ICD-AuthState-v1` | Data-Integration | JWT/Refresh + serverAddress 状态 |
| `ICD-TerminalDto-v1` | Data-Integration | 终端 DTO + 7 个状态枚举 |
| `ICD-ZoneDto-v1` | Data-Integration | 分区 DTO（终端集合） |
| `ICD-TaskDto-v1` | Data-Integration | 任务 / 作息 DTO |
| `ICD-BroadcastWS-v1` | Frontend-Platform | WebSocket 推送消息格式（DRAFT） |
| `ICD-RealtimeFallback-v1` | Frontend-Platform | WS 断线 10s 轮询协议（DRAFT） |
| `ICD-IPCSocket-v1` | Legacy-Native | 本机 TCP 4521 命令协议 |
| `ICD-VoiceAAR-v1` | Legacy-Native | 语音对讲 AAR 适配层（DRAFT） |
| `ICD-MapLocation-v1` | Legacy-Native | 百度地图定位 + 经纬度回写 |
| `ICD-DesignTokens-v1` | Frontend-Platform（从 Handoff 提炼） | 颜色/形状/字体/动效 |

---

## 7 个关键风险

定义在 `agents/project-manager/memory.md` §4：

- **RISK-AR-001**：动态 baseUrl 拦截器竞态
- **RISK-AR-002**：新旧栈共存冲突
- **RISK-AR-003**：32 位 ABI 兼容性死循环（**CRITICAL**，可能阻塞对讲模式）
- **RISK-AR-004**：WebSocket 重连风暴
- **RISK-AR-005**：多 Agent 开发中的设计系统漂移
- **RISK-AR-006**：后端 WS 协议格式与 DRAFT ICD 不符
- **RISK-AR-007**：百度地图 SDK 合规问题

---

## 关键术语（v4 PRD 锁定）

| 术语 | 定义 |
|------|------|
| **终端** | 一台广播喇叭 / 播放设备（最小单元） |
| **分区** | **终端集合**（一组终端的逻辑分组，如"教学楼 A 区"），不嵌套 |
| **广播 Tab** | v4 一级 Tab 容器，聚合**寻呼 / 对讲 / 点播三档动作**，目标终端在三档间共享 |
| **寻呼** | 广播 Tab 的"寻呼模式"：实时麦克风广播到选定终端 |
| **对讲** | 广播 Tab 的"对讲模式"：一对一/多方音频通话（依赖 32 位 ABI） |
| **点播** | 广播 Tab 的"点播模式"：选定媒体推送到终端播放 |

---

## 设计规范（design tokens）速览

完整版见 `references/design-system-spec.md`。核心：

- **主色**：`#0E7C70` Teal
- **5 Tab 标识色**：终端 Teal / 广播 Orange / AI Cyan / 任务 Purple / 服务 Blue
- **广播三档色**：寻呼 `#EA580C` / 对讲 `#2563EB` / 点播 `#0E7C70`
- **圆角**：Card 12.dp / Tile 16.dp / Chip 999.dp / Sheet 24.dp (top)
- **字体**：Noto Sans SC + JetBrains Mono（数字必须 mono + tnum）
- **动效**：120-220ms · cubic-bezier(.2, .7, .3, 1)

**强制约定**：用 token 引用（`AeroColors.Primary`），**禁止** hardcode `Color(0xFF...)`。Critic 用 regex `Color\(0x[A-F0-9]{8}\)` 自动检测偏离。

---

## 衍生与差异

衍生自 `itc-enterprise-workflow v1.0.0`（9 Agent 通用企业工作流）。

**关键改动**：

1. **Agent 数量从 9 → 6**：去掉了后端 / DevOps / Security agent（后端是厂商提供的 LAN 主机），合并了部分 frontend agent。
2. **加入 AeroRadio 专属内容**：
   - 21 个 REST 端点清单 + 21 个端点接入跟踪
   - 旧栈 → 新栈迁移流程 + 双跑期纪律
   - 32 位 ABI 死循环风险 + 降级策略
   - 11 个项目专属 ICD
   - 错误模式库新增 ABI-ERR / DSN-ERR 系列
3. **设计规范深度集成**：`design-system-spec.md` 从 Handoff.html 完整提炼，Critic 用其做设计偏离检测。
4. **状态机加强**：加入 AeroRadio 特有的 legacy_impact / abi_sensitive 标记。

---

## 文件统计

29 个 markdown 文件，约 9200 行内容：

- 1 份顶层 `SKILL.md`
- 4 份 `references/*`
- 6 个 Agent × 4 件套 = 24 份 agent 配置

---

## License & Attribution

衍生自 itc-enterprise-workflow（同样自用工作流）。本包专用于 AeroRadioControl v4 项目内部。

---

*AeroRadio Multi-Agent Workflow v1.0.0 — Built for one project, one team, one shipping app.*
