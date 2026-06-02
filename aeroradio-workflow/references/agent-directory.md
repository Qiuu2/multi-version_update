# Agent Directory

AeroRadioControl 多 Agent 协作系统的完整 agent 目录。本目录定义了所有 agent 的 ID、角色、汇报关系、目录位置和通信矩阵。

## 1. Orchestration Layer (L3)

| Agent ID | Name | Role | Directory |
|----------|------|------|-----------|
| `agent.project.manager` | Project Manager Agent | 任务调度、状态机、DAG 维护、风险监控 | `agents/project-manager/` |

## 2. Cross-Cutting Layer (L2)

| Agent ID | Name | Role | Directory |
|----------|------|------|-----------|
| `agent.critic.reviewer` | Critic Agent | 横切质量评审、对抗式提问、设计偏离守门 | `agents/critic/` |

## 3. Domain Expert Layer (L3)

| Agent ID | Name | 领域 | Key Skills | Directory |
|----------|------|------|------------|-----------|
| `agent.frontend.business` | Frontend-Business Agent | 业务 UI 实现 | Compose 5 Tab + 平板三栏 + 二级页导航 | `agents/frontend-business/` |
| `agent.frontend.platform` | Frontend-Platform Agent | 平台横切能力 | WebSocket + 轮询回退、通知、adaptive layout、错误处理 | `agents/frontend-platform/` |
| `agent.data.integration` | Data-Integration Agent | 网络数据层 | 动态 baseUrl 拦截器、21 REST 端点、双栈共存迁移、缓存 | `agents/data-integration/` |
| `agent.legacy.native` | Legacy-Native Agent | 遗留与原生 | TCP socket、语音 AAR、32 位 ABI、百度地图 SDK | `agents/legacy-native/` |

## 4. Human Layer (L1)

| Role | Responsibilities | Interaction Points |
|------|-----------------|-------------------|
| Chief Engineer (Human) | 最终决策、关键节点 Review、Escalation 处理 | 4 个强制 Gate：Plan Approval、Architecture Review、Phase 1 Go/No-Go、Final Release Approval |

## 5. Communication Matrix

```
                    Human CTO
                       │
                       ▼
              ┌────────┴────────┐
              │ Project Manager │
              └────────┬────────┘
                       │
       ┌───────────────┼───────────────┬───────────────┐
       │               │               │               │
       ▼               ▼               ▼               ▼
┌──────┴──────┐ ┌──────┴──────┐ ┌──────┴──────┐ ┌──────┴──────┐
│  Frontend-  │ │  Frontend-  │ │    Data-    │ │   Legacy-   │
│  Business   │ │  Platform   │ │ Integration │ │   Native    │
└──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
       │               │               │               │
       │               │    Critic     │               │
       └──────────────►│Cross-Cutting │◄──────────────┘
                       │               │
                       └───────┬───────┘
                               │
                               ▼
                          (review feedback
                           routed via PM)
```

Legend:
- `───►` = 任务调度（PM 派发任务给 Domain Agent）
- `═══►` = 评审反馈（Critic 评审结果通过 PM 路由）
- `─o─►` = 人类 review gate（Domain Agent 产出 → Critic → PM → Human CTO）

## 6. Agent Interaction Patterns

### 6.1 常见交互对（高频）

| From | To | Trigger | Format |
|------|-----|---------|--------|
| Human CTO | PM | 提交 PRD / 决策 / 修改优先级 | Markdown / YAML |
| PM | Domain Agent | 派发任务 | TASK_ASSIGN message |
| Domain Agent | PM | 状态更新 / 提交产出 | STATUS_UPDATE / DELIVERABLE |
| PM | Critic | 路由产出供评审 | REVIEW_REQUEST |
| Critic | PM | 返回评审结果 | REVIEW_RESULT |
| PM | Human CTO | 日报 / Gate 提交 / Escalation | Markdown |
| Domain Agent | Domain Agent | 通过 PM 协调，禁止直接通信 | 经 PM 路由 |

### 6.2 跨 Domain 协作场景（必经 PM）

| 场景 | 涉及 Agent | PM 角色 |
|------|----------|--------|
| 接口契约变更（如 DTO 字段改名） | Data-Integration + Frontend-Business | 召集协商，更新 ICD |
| WebSocket 消息格式变更 | Data-Integration + Frontend-Platform | 同上 |
| 语音对讲触发后 UI 反馈 | Legacy-Native + Frontend-Business | 同上 |
| 平板 adaptive layout 影响业务屏 | Frontend-Platform + Frontend-Business | 同上 |

**重要**：Domain Agent 之间**不允许直接通信**，所有跨域协调必须通过 PM。这避免了：
- 隐式契约导致集成失败
- 决策无审计记录
- Critic 无法横切发现一致性问题

## 7. Agent Capability Bootstrap（初始能力基线）

每个 Agent 在项目启动时具备的初始能力配置：

| Agent | tasks_completed | first_pass_rate | estimation_bias | max_capacity |
|-------|----------------|-----------------|-----------------|--------------|
| Frontend-Business | 0 | 0.60（基线） | neutral | 3 tasks |
| Frontend-Platform | 0 | 0.55（基线） | underestimate | 2 tasks |
| Data-Integration | 0 | 0.65（基线） | neutral | 3 tasks |
| Legacy-Native | 0 | 0.50（基线） | underestimate | 2 tasks |
| Critic | N/A | N/A | N/A | 5 reviews |
| PM | N/A | N/A | N/A | unlimited |

> 基线值参考 itc 原版 + 项目特征调整。Legacy-Native 因为涉及未知历史代码 + 32 位 ABI + 第三方 SDK，first_pass_rate 设最低。

随着任务完成，PM 的 memory.md 会持续更新这些数值，调度算法会基于真实数据校准。

## 8. Agent Lifecycle

```
INACTIVE → INITIALIZING → READY → ACTIVE → COMPLETED → ARCHIVED
                                  ↓
                              [可暂停]
                                  ↓
                                PAUSED
```

| State | 含义 | 谁可以触发转换 |
|-------|------|-------------|
| INACTIVE | Agent 配置已加载但未启动 | 系统启动时 |
| INITIALIZING | Agent 正在加载 memory + 注册到 PM | 系统启动时 |
| READY | Agent 已注册，等待任务 | PM 派发任务 |
| ACTIVE | Agent 正在执行任务 | PM 派发后自动 |
| PAUSED | Agent 被人为暂停（如等待 CTO 决策） | Human CTO / PM |
| COMPLETED | 项目完成，Agent 任务全部归档 | PM 项目关闭时 |
| ARCHIVED | Agent 历史数据已归档，进入 read-only | 项目归档时 |

---

*Agent Directory v1.0.0 — AeroRadioControl Multi-Agent Topology*
