---
name: aeroradio-workflow
description: >
  AeroRadioControl v4 企业级多 Agent 协作系统入口技能。
  面向校园广播管理 Android 客户端（手机 + 平板）+ 局域网广播主机对接的
  四层 + 一横切架构。涵盖前端业务（5 Tab）、前端平台特性（实时通信/通知/平板）、
  数据集成（新旧网络栈共存）、遗留与原生（TCP socket / 语音 AAR）四大领域，
  由 Critic Agent 横切所有领域进行质量守门。
  本技能定义系统整体架构、通信协议、状态机、依赖图规范及启动流程。
metadata:
  version: 1.0.0
  author: AeroRadio Architecture Team
  category: enterprise-workflow
  industry: android-mobile-client
  derived_from: itc-enterprise-workflow v1.0.0
---

# AeroRadioControl Enterprise Multi-Agent Workflow

## 1. System Architecture: 四层 + 一横切

```
┌─────────────────────────────────────────────────────────────┐
│                    HUMAN CTO / 总工程师                       │
│              （关键节点 Review · 最终决策 · Escalation）        │
└──────────────┬──────────────────────────────┬───────────────┘
               │ 实线=任务调度   虚线=评审反馈   │
               ▼                              ▼
┌─────────────────────────────────────────────────────────────┐
│  ◄─────────── CROSS-CUTTING LAYER ───────────►              │
│                  CRITIC AGENT 评审 Agent                      │
│       （质量守门 · 对抗式提问 · 设计偏离检测 · 先审后发）         │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
┌──────────────▼──────────────┬───────────────▼───────────────┐
│   ORCHESTRATION LAYER (L3)  │      DOMAINS LAYER (L3)        │
│     Project Manager Agent   │  ┌────────────┐ ┌────────────┐ │
│  （Scrum Master + Tech Lead │  │ Frontend-  │ │ Frontend-  │ │
│   状态机 · DAG · 任务调度）   │  │ Business   │ │ Platform   │ │
│                             │  └────────────┘ └────────────┘ │
│                             │  ┌────────────┐ ┌────────────┐ │
│                             │  │ Data-      │ │ Legacy-    │ │
│                             │  │ Integration│ │ Native     │ │
│                             │  └────────────┘ └────────────┘ │
└──────────────┬──────────────┴───────────────┬───────────────┘
               │                              │
┌──────────────▼──────────────────────────────▼───────────────┐
│                    EXECUTION LAYER (L4)                      │
│  Android Studio · Gradle · ADB · Compose 预览 · Kotlin 编译  │
│  OkHttp/Retrofit · Hilt · WebSocket · Native AAR · Git/CI    │
└─────────────────────────────────────────────────────────────┘
```

### 1.1 Layer Definitions

| Layer | Name | Responsibility | Key Agents |
|-------|------|----------------|------------|
| L1 | **Human Layer** | 总工程师决策、关键 review、escalation 处理 | Human CTO（你） |
| L2 | **Cross-Cutting Layer** | 质量守门，所有产出先过 Critic 再交付 | Critic Agent |
| L3 | **Orchestration Layer** | 项目总调度、状态机、DAG、任务派发 | Project Manager Agent |
| L3 | **Domains Layer** | 领域专业工作：业务/平台/数据/遗留 | 4 Domain Agents |
| L4 | **Execution Layer** | 工具执行、编译运行、代码生成 | Tool Callers（外部） |

### 1.2 Cross-Cutting Principle

> **核心原则：所有 Agent 产出必须先经过 Critic Agent 评审，才能进入下一环节或提交人类 Review。**

```
Domain Agent ──产出──► Critic Agent ──通过──► Project Manager ──► 下一环节 / 人类 Review
                          │
                          └── 不通过 ──► 反馈修正 ──► Domain Agent（循环直到通过）
```

### 1.3 Why 6 Agents (粒度决策依据)

AeroRadioControl 与 ITC 原版（音频硬件研发）有本质区别：

| 维度 | ITC 原版 | AeroRadio |
|------|---------|-----------|
| 领域跨度 | 跨学科（声学/DSP/硬件/结构） | 单一学科（Android 开发） |
| Agent 互替性 | 不可替代（专业壁垒） | 部分可替代（职责不重叠但技术栈相通） |
| 拆分原则 | 按学科 | 按"代码物理位置 + 技术性质" |

按"技术性质"拆分得到 6 个 agent，理由：
- **Frontend-Business 与 Platform 必须分**：业务 agent 关注"做什么"（5 Tab 各自的业务），Platform 关注"怎么做"（实时通信/通知等横切于所有 Tab 的能力）
- **Data-Integration 必须独立**：新旧网络栈（OkHttp 老栈 ↔ Retrofit 新栈）共存是迁移期的核心矛盾，需要专人守门
- **Legacy-Native 必须独立**：TCP socket + 语音 AAR + 32 位 ABI + 第三方 SDK 性质完全不同于 HTTP REST，混入其他 agent 会污染

---

## 2. Project Domain Glossary（AeroRadio 专属术语表）

为避免 agent 间术语漂移，统一定义：

| 术语 | 定义 | 出处 |
|------|------|------|
| **校区** | 一个独立的校园部署单位 | 业务概念 |
| **广播主机** | 校区机房内的局域网服务器，HTTP REST 后端 | 厂商硬件 |
| **服务器地址** | `<IP>:<port>` 形式，登录时手填，存 SharedPreferences | `Constring.serverAddress` |
| **终端** | 单台广播设备（如一只吸顶喇叭） | `/terminal/terminalinfo` |
| **分区** | 终端集合（一组终端的逻辑分组，如"教学楼 A 区"） | `/terminal/zoneterminal` |
| **广播 Tab** | v4 一级 Tab 容器，聚合寻呼/对讲/点播三档动作，目标终端在三档间共享 | `BroadcastScreen` |
| **寻呼** | 广播 Tab 的"寻呼模式"：实时麦克风广播到选定终端/分区 | 广播 Tab · mode="page" |
| **对讲** | 广播 Tab 的"对讲模式"：一对一/多方音频通话 | 广播 Tab · mode="talk" |
| **点播** | 广播 Tab 的"点播模式"：选定媒体文件推送到终端播放 | 广播 Tab · mode="cast" |
| **作息方案** | 定时任务的集合（晨读铃/课间铃） | `/task/sechinfo` |
| **TTS 任务** | 文本转语音定时播放 | `/task/ttstaskinfo` |
| **紧急播放** | 跳过队列的最高优先级播放 | `/terminal/urgentplay` |
| **临时文件广播** | 即兴上传文件广播一次 | 任务 Tab · 子页 4 |
| **JWT** | 24h 短令牌，登录时换 | `/authorizations` |
| **Refresh Token** | 30d 长令牌，刷新 JWT 用 | Handoff 文档 |
| **本机 IPC** | `127.0.0.1:4521` TCP socket，连设备本机守护进程 | `utils/SocketClient.java` |
| **语音对讲 AAR** | `app/libs/htapplib.aar`，native 实时音频通道，**arm64-v8a + armeabi-v7a 双 ABI**（缺 x86/x86_64；R-001 经 SPIKE-AAR64 降为 LOW-条件性） | `app/libs/` |
| **旧栈** | OkHttp + Gson + 手拼 URL + 回调，散落在 `httptask/*Method.java` | 历史代码 |
| **新栈** | Retrofit + Gson + 协程 + Hilt，`data/api/*.kt` | Phase 0 已起步 |
| **占位 baseUrl** | `http://placeholder.invalid/`，新栈用 `@Url` 动态注入 | `di/NetworkModule.kt` |

---

## 3. Communication Protocol

### 3.1 Line Types

| Line Style | Meaning | Direction | Trigger |
|------------|---------|-----------|---------|
| **实线 (───)** | 任务调度 | PM → Domain Agent | 任务分配、状态变更、优先级调整 |
| **虚线 (- - -)** | 评审反馈 | Critic → Domain Agent / PM | 评审意见、质量拦截、修正要求 |
| **粗线 (═══)** | 人类介入 | Human ↔ PM / Critic | 关键决策、escalation、最终审批 |
| **点线 (···)** | 状态同步 | Agent ↔ PM | 进度汇报、状态更新、阻塞报告 |

### 3.2 Message Envelope

所有 agent 间消息统一信封格式（详见 `references/communication-protocol.md`）：

```yaml
message:
  header:
    message_id: "uuid-v4"
    timestamp: "ISO-8601"
    from: "agent-id"
    to: "agent-id | broadcast"
    message_type: "TASK_ASSIGN | STATUS_UPDATE | DELIVERABLE | REVIEW_REQUEST |
                   REVIEW_RESULT | ESCALATION | HUMAN_REVIEW | TOOL_CALL | TOOL_RESULT"
    priority: "P0_CRITICAL | P1_HIGH | P2_NORMAL | P3_LOW"
  body:
    # type-specific payload
  trace:
    project_id: "aeroradio-v4"
    task_id: "task-uuid"
    chain: ["msg-id-1", "msg-id-2"]
```

---

## 4. Task State Machine

```
                    ┌─────────────┐
                    │   PENDING   │◄────────────┐
                    │  （待分配）  │              │
                    └──────┬──────┘              │
                           │ PM assigns          │
                           ▼                     │
                    ┌─────────────┐              │
                    │  ASSIGNED   │              │
                    │  （已分配）  │              │
                    └──────┬──────┘              │
                           │ Agent accepts       │
                           ▼                     │
              ┌───►┌─────────────┐               │
              │    │ IN_PROGRESS │               │
         reject    │  （执行中）   │              │
              │    └──────┬──────┘               │
              │           │ Agent submits         │
              │           ▼                       │
              │    ┌─────────────┐    ┌──────────┴──────────┐
              │    │    REVIEW   │───►│   AUTO_REJECTED    │
              └───┐│  （评审中）  │    │  （自动打回 PENDING） │
                  │ └──────┬──────┘    └────────────────────┘
                  │        │ Critic reviews
                  │        ▼
                  │ ┌─────────────┬─────────────┐
                  │ │   PASSED    │   FAILED    │
                  └─┤  （已通过）  │  （未通过）  │
                    └──────┬──────┘──────┬──────┘
                           │             │
                           ▼             ▼
                    ┌─────────────┐    (loop back
                    │   COMPLETED │     to PENDING)
                    │   （已完成） │
                    └──────┬──────┘
                           │
         ┌─────────────────┼─────────────────┐
         ▼                 ▼                 ▼
   ┌──────────┐     ┌──────────┐     ┌──────────┐
   │  MERGED  │     │ ARCHIVED │     │ REJECTED │
   │（已合并到 │     │（已归档）  │     │（已否决）  │
   │  main）  │     │           │     │           │
   └──────────┘     └──────────┘     └──────────┘
```

### 4.1 State Transitions

| From → To | Trigger | Actor | SLA |
|-----------|---------|-------|-----|
| PENDING → ASSIGNED | PM 创建任务并指派 | Project Manager | 立即 |
| ASSIGNED → IN_PROGRESS | Domain Agent 接受任务 | Domain Agent | 1h |
| IN_PROGRESS → REVIEW | 提交产出物（含代码 + 自测） | Domain Agent | 按任务 SLA |
| REVIEW → PASSED | Critic 评审通过 | Critic Agent | 2h |
| REVIEW → FAILED | Critic 发现 BLOCKER/MAJOR | Critic Agent | 2h |
| REVIEW → AUTO_REJECTED | 超时未审 / Agent 放弃 | Auto-timeout | 24h |
| PASSED → COMPLETED | PM 确认完成 | Project Manager | 1h |
| PASSED → HUMAN_REVIEW | 需要人类决策（架构级） | Project Manager | 按 CTO SLA |
| FAILED → PENDING | 打回修正 | Critic Agent | 立即 |
| COMPLETED → MERGED | 合并到主分支 | Project Manager | 按流程 |
| COMPLETED → ARCHIVED | 归档记录 | Project Manager | 定期 |
| * → ESCALATED | P0 问题 / 阻塞 > 4h | Any Agent | 立即 |

---

## 5. Dependency Graph (DAG) Specification

### 5.1 AeroRadio 关键依赖示例

```yaml
example_dag_phase_1:
  nodes:
    - task_id: "TASK-AR-001"
      name: "动态 baseUrl 拦截器实现"
      domain: "data-integration"
      estimated_hours: 6
      priority: "P0_CRITICAL"
      rationale: "所有 Retrofit 调用都依赖此拦截器"

    - task_id: "TASK-AR-002"
      name: "登录页 + JWT 刷新逻辑"
      domain: "frontend-business"
      estimated_hours: 8
      priority: "P0_CRITICAL"
      depends_on: ["TASK-AR-001"]

    - task_id: "TASK-AR-003"
      name: "WebSocket + 10s 轮询回退框架"
      domain: "frontend-platform"
      estimated_hours: 12
      priority: "P0_CRITICAL"
      depends_on: ["TASK-AR-001"]

    - task_id: "TASK-AR-004"
      name: "终端 Tab 接真数据（/terminal/terminalinfo）"
      domain: "frontend-business"
      estimated_hours: 10
      priority: "P1_HIGH"
      depends_on: ["TASK-AR-001", "TASK-AR-003"]

    - task_id: "TASK-AR-005"
      name: "TCP socket 4521 IPC 封装到协程"
      domain: "legacy-native"
      estimated_hours: 8
      priority: "P2_NORMAL"
      depends_on: []

    - task_id: "TASK-AR-006"
      name: "广播 Tab · 寻呼模式（含语音 AAR 接入）"
      domain: "legacy-native"
      estimated_hours: 16
      priority: "P1_HIGH"
      depends_on: ["TASK-AR-004", "TASK-AR-005"]
```

### 5.2 Dependency Types

| Type | 描述 | 在 AeroRadio 中的典型例子 |
|------|------|-------------------------|
| `data_dependency` | Task B 需要 Task A 的输出数据 | UI 任务需要 Repository 完成 |
| `resource_dependency` | Task B 需要 Task A 释放的资源 | 多个 agent 改同一文件需顺序 |
| `spatial_dependency` | 物理布局/代码位置依赖 | Compose 父组件 → 子组件 |
| `execution_dependency` | B 必须在 A 之后执行 | 拦截器先于 Repository |
| `review_dependency` | B 在 A 通过 Critic 后才能开始 | 架构方案过审后才能落地代码 |

---

## 6. Startup / Initialization Sequence

```
Step 1: Human CTO 提交 PRD（AeroRadio_v4.html + Handoff.html + 后端定性段落）
        └── PM Agent 启动，读取 PRD

Step 2: PM Agent 执行 WBS 分解
        └── 按 4 个领域拆 L1 → L2 → L3 任务
        └── 每个 L3 任务 ≤ 16h，超过需拆分
        └── 构建 DAG，检测环
        └── 计算关键路径
        └── 生成 project_plan.yaml + risk_register.md

Step 3: PM 提交 project_plan 给 Critic Agent
        └── Critic 评审：WBS 完整性、依赖正确性、估算合理性
        └── 若 FAILED → PM 修正
        └── 若 PASSED → 进入下一步

Step 4: Critic 通过 → 提交 Human CTO Review
        └── CTO 批准 / 修改 / 否决整体计划
        └── 若批准 → workflow 正式启动

Step 5: PM 按 DAG 派发任务
        └── 根任务（无依赖）→ 优先派发
        └── 后续任务 → 依赖完成后入派发队列
        └── 每次派发执行调度算法（见 PM skill.md 第 4 章）

Step 6: Domain Agent 接任务 → 执行 → 提交产出
        └── 产出物 = 代码 + 自测 + 文档 + 影响范围说明
        └── 提交即触发状态 IN_PROGRESS → REVIEW

Step 7: PM 路由产出到 Critic
        └── Critic 按 domain 加载对应 checklist + error pattern
        └── 评审结果 PASSED / FAILED / ESCALATED

Step 8: 进入循环（Step 5-7）直到所有任务 COMPLETED

Step 9: 项目阶段性 Gate Review（见第 7 节）
        └── 4 个强制 Gate
```

### 6.1 PM Entry Point

```yaml
workflow_start:
  trigger: "Human CTO submits AeroRadio PRD"
  entry_agent: "project-manager"
  entry_skill: "agents/project-manager/skill.md"
  inputs:
    - "PRD: AeroRadio_v4.html (5 Tab IA + 屏幕规格)"
    - "Handoff: Handoff.html (设计规范 + 实时性/错误/离线策略)"
    - "Backend Brief: 局域网广播主机 REST 端点清单（21 个端点）"
    - "constraints.yaml: timeline, manpower, tech stack"
    - "milestones.json: 4 个 Gate 的目标日期"
  outputs:
    - "project_plan.yaml: WBS + DAG + assignments"
    - "risk_register.md: 识别到的风险（含旧栈迁移、32 位 ABI、动态 baseUrl 等）"
    - "schedule.json: Gantt-style timeline"
    - "agent_capability_baseline.yaml: 各 agent 初始能力基线"
```

---

## 7. Agent Directory

```
aeroradio-workflow/
├── SKILL.md                              # ← 本文件（入口）
├── agents/
│   ├── project-manager/                  # L3 调度
│   │   ├── profile.md                    #   身份定义
│   │   ├── soul.md                       #   核心驱动与价值观
│   │   ├── skill.md                      #   WBS + DAG + 调度算法
│   │   └── memory.md                     #   项目历史 + 团队能力图谱
│   │
│   ├── critic/                           # L2 横切评审
│   │   ├── profile.md
│   │   ├── soul.md
│   │   ├── skill.md                      #   Android/Compose error pattern
│   │   └── memory.md                     #   评审历史 + 设计偏离案例
│   │
│   ├── frontend-business/                # L3 业务领域
│   │   ├── profile.md, soul.md, skill.md, memory.md
│   │   #   5 Tab 业务功能 + 平板三栏
│   │
│   ├── frontend-platform/                # L3 平台领域
│   │   ├── profile.md, soul.md, skill.md, memory.md
│   │   #   WebSocket/轮询 + 通知 + adaptive layout
│   │
│   ├── data-integration/                 # L3 数据领域
│   │   ├── profile.md, soul.md, skill.md, memory.md
│   │   #   动态 baseUrl + 21 个端点 + 旧栈→新栈迁移
│   │
│   └── legacy-native/                    # L3 遗留与原生
│       ├── profile.md, soul.md, skill.md, memory.md
│       #   TCP socket + 语音 AAR + 32 位 ABI + 百度地图
│
└── references/
    ├── agent-directory.md                # 完整 agent 目录与通信矩阵
    ├── communication-protocol.md         # 消息格式 / 状态机 / Gate 规范
    ├── icd-contracts.md                  # Agent 间接口控制文档
    └── design-system-spec.md             # 从 Handoff.html 提炼的设计规范
```

### 7.1 Agent Roles Summary

| Agent | Role | Layer | Reports To | 守护领域 |
|-------|------|-------|------------|---------|
| Project Manager | Scrum Master + Tech Lead | Orchestration | Human CTO | DAG / 状态机 / 调度 |
| Critic | Quality Gatekeeper | Cross-Cutting | Human CTO（escalation） | 全部产出 |
| Frontend-Business | 业务 Compose 屏 工程师 | Domains | PM + Critic | 5 Tab + 平板 + 二级页 |
| Frontend-Platform | 平台/横切能力工程师 | Domains | PM + Critic | WS + 通知 + adaptive |
| Data-Integration | 网络与数据层工程师 | Domains | PM + Critic | 21 REST 端点 + 双栈共存 |
| Legacy-Native | 遗留与原生工程师 | Domains | PM + Critic | TCP/AAR/百度 SDK/32 位 ABI |

---

## 8. Human CTO Intervention Points

### 8.1 Mandatory Review Gates (4 个强制门)

```
Gate 1: Project Plan Approval（计划批准）
  └── 触发：PM 完成 WBS+DAG，Critic 评审通过
  └── 决策：CTO 批准 / 修改 / 否决整体计划
  └── SLA：48h
  └── AeroRadio 落地：确认任务粒度、迁移策略、Phase 划分

Gate 2: Architecture Review（架构评审）
  └── 触发：4 个领域 agent 各自完成架构方案，Critic 通过
  └── 决策：CTO 评审跨领域架构一致性
  └── SLA：24h
  └── AeroRadio 落地：确认拦截器方案、WS 协议、IPC 桥接策略

Gate 3: Phase 1 Go/No-Go（首个里程碑）
  └── 触发：登录 + 终端 Tab 接通真数据，Critic 通过
  └── 决策：CTO 决定是否进入 Phase 2
  └── SLA：24h
  └── AeroRadio 落地：确认旧栈→新栈迁移节奏

Gate 4: Final Release Approval（发布批准）
  └── 触发：所有 Tab 接真 + 平板版 + 性能/安全过审
  └── 决策：CTO 批准签名打包发布
  └── SLA：72h
```

### 8.2 Escalation Triggers

| 条件 | 升级路径 | 响应时间 |
|-----|---------|---------|
| BLOCKER 问题 > 4h 未解决 | Agent → PM → CTO | CTO 2h |
| 跨领域冲突（如新旧栈兼容性） | PM + Critic → CTO | CTO 4h |
| 时间/范围超标风险 | PM → CTO | CTO 8h |
| 安全/合规问题（明文 HTTP、JWT 泄露） | Any → Critic → CTO（立即） | CTO 1h |
| 同任务 Critic 失败 ≥ 3 次 | Critic → CTO | CTO 4h |
| 32 位 ABI 兼容性死锁 | Legacy-Native → CTO | CTO 4h |

---

## 9. Critic Agent Cross-Cutting Workflow

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  Frontend-   │  │  Frontend-   │  │    Data-     │  │   Legacy-    │
│  Business    │  │  Platform    │  │  Integration │  │   Native     │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │                 │
       └─────────────────┼─────────────────┼─────────────────┘
                         ▼                 ▼
                  ┌─────────────┐
                  │    CRITIC   │◄──────────────────────────────┐
                  │    AGENT    │   （所有产出必须经过此关卡）       │
                  │             │                               │
                  │ • 对抗式提问   │                              │
                  │ • Android/   │◄──────────────────────────────┘
                  │   Compose    │   （从 Execution Layer 验证数据）
                  │   错误模式    │
                  │ • 设计偏离检测 │
                  │ • 跨域一致性  │
                  └──────┬───────┘
                         │
              ┌──────────┼──────────┐
              ▼          ▼          ▼
         ┌────────┐ ┌────────┐ ┌────────┐
         │ PASSED │ │ FAILED │ │ ESCAL  │
         └───┬────┘ └───┬────┘ └───┬────┘
             │          │          │
             ▼          ▼          ▼
          下一环节    打回修正    上报 CTO
```

### 9.1 Critic Review Protocol

1. **Receive**: Critic 从 PM 收到 deliverable
2. **Screen**: 快速扫描明显错误（格式、完整性）
3. **Deep Review**: domain 专属 checklist + 对抗式提问 + error pattern 自动检测
4. **Cross-Domain Check**: 验证与其他 domain 产出的一致性（如 ICD 契约）
5. **Grade**: 分级（BLOCKER/MAJOR/MINOR/INFO）
6. **Report**: 评审报告 → PM + Domain Agent
7. **Track**: 写入 memory 供学习

---

## 10. Execution Layer Integration

### 10.1 AeroRadio 工具栈

| Category | Tools | Used By |
|----------|-------|---------|
| **IDE/Build** | Android Studio Panda 4, Gradle 8+, KSP | All frontend agents |
| **Compose** | Jetpack Compose BOM, Material 3 | Frontend-Business, Platform |
| **网络** | Retrofit, OkHttp, Gson, kotlinx.serialization | Data-Integration |
| **架构** | Hilt, ViewModel, StateFlow, Navigation | All frontend agents |
| **数据** | Room, DataStore, SharedPreferences | Data-Integration |
| **实时** | OkHttp WebSocket, Coroutine Flow | Frontend-Platform |
| **Native** | NDK, 32-bit ABI, libs/htapplib.aar | Legacy-Native |
| **第三方** | 百度地图 SDK + 定位 SDK | Legacy-Native |
| **测试** | JUnit, Espresso, Compose UI Test | Critic（验证用） |
| **CI** | GitHub Actions / 自建 CI | PM 监控 |

### 10.2 Tool Call Pattern

```yaml
tool_execution:
  agent: "{domain_agent}"
  tool: "{tool_name}"
  inputs: "{structured_params}"
  outputs: "{structured_results}"
  validation:
    - "output_format_check"
    - "value_range_check"
    - "consistency_with_prev_results"
  review_required: true  # always → goes to Critic
```

---

## 11. Metrics & Observability

### 11.1 System Health Metrics

```yaml
metrics:
  throughput:
    - tasks_completed_per_day
    - review_cycle_time_avg  # submit → pass
    - critical_path_adherence
  quality:
    - critic_findings_per_deliverable
    - false_positive_rate
    - revision_cycles_avg
  aeroradio_specific:
    - mock_to_real_data_migration_pct  # Tab 接真数据进度
    - legacy_stack_remaining_files     # 旧栈剩余文件数
    - 32bit_abi_blocker_count          # 32 位 ABI 阻塞项
    - design_deviation_count           # 偏离 Handoff 规范的次数
  health:
    - blocked_tasks_count
    - escalation_frequency
    - sla_breach_rate
```

### 11.2 Dashboard

PM Agent 维护实时项目仪表盘：

```
┌─────────────────────────────────────────────────────────────────┐
│ AeroRadio Dashboard [Project: AeroRadio-v4-Production]          │
├──────────────┬──────────────┬──────────────┬────────────────────┤
│ Progress     │ Quality      │ Schedule     │ Risks              │
│ ████████ 75% │ Cycles: 1.6  │ On Track     │ ⚠ 2 Medium         │
│ 18/24 tasks  │ Issues: 31   │ CPI: 1.02    │ ✖ 0 Critical       │
│ 3 in review  │ FP rate: 7%  │ EAC: 28 days │ ⚡ 1 Escalation    │
├──────────────┼──────────────┼──────────────┼────────────────────┤
│ Mock→Real    │ Legacy Files │ 32-bit ABI   │ Design Deviation   │
│ 3/5 Tabs ✓   │ 8 remaining  │ Resolved     │ 2 minor cases      │
└──────────────┴──────────────┴──────────────┴────────────────────┘
```

---

## 12. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-05-27 | Initial architecture，基于 itc-enterprise-workflow v1.0.0 派生，针对 AeroRadioControl v4 定制 6-agent 拆分 |

---

*AeroRadio Enterprise Multi-Agent System v1.0.0 — Four Layers + One Cross-Cut, Tailored for Campus Broadcast Android Client*
