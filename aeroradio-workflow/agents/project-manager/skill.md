---
name: project-manager-skill
description: >
  Project Manager Agent 的技能配置文件。
  包含 AeroRadio v4 项目的 WBS 模板、依赖图 (DAG) 构建与维护、
  任务状态机管理、调度算法、风险识别与 escalation 机制、
  与人类 CTO 的汇报格式、与 Critic Agent 的交互协议、与 4 个 Domain Agent 的任务派发接口。
version: 1.0.0
author: AeroRadio Architecture Team
derived_from: itc-enterprise-workflow/agents/project-manager v1.0.0
---

# Project Manager Agent — Skill

## 1. Work Breakdown Structure (WBS)

### 1.1 WBS Decomposition Method

```yaml
wbs_method:
  levels:
    L0: "Project (AeroRadio-v4)"
    L1: "Phase (Phase 0..3)"          # 与 backlog 对齐
    L2: "Work Package"                # 可交付成果级别
    L3: "Task"                        # 分配给单个 Agent
    L4: "Sub-task"                    # 可选，Agent 自行分解

  decomposition_rules:
    - "每个 L3 任务分配给唯一一个 Domain Agent"
    - "每个 L3 任务有明确的交付物和验收标准"
    - "每个 L3 任务预估工时 ≤ 16 小时（超过需拆分）"
    - "每个 L3 任务有唯一的任务 ID：TASK-AR-{seq}"
    - "任务依赖必须在 DAG 中明确定义"
    - "涉及旧栈代码的任务必须标记 legacy_impact: true"
    - "涉及 32 位 ABI 的任务必须标记 abi_sensitive: true"
```

### 1.2 WBS Template for AeroRadio v4

```yaml
wbs_template:

  L1_phase_0_foundation:
    name: "Phase 0 · 地基"
    duration_estimate: "1-2 周"
    L2_work_packages:
      - wp_id: "AR-WP00-NET"
        name: "网络栈现代化基础"
        L3_tasks:
          - task: "动态 baseUrl 拦截器实现"
            agent: "data-integration"
            deliverable: "DynamicBaseUrlInterceptor.kt + 单测"
            est_hours: 6
            priority: P0_CRITICAL
            icd_produced: "ICD-NetworkModule-v1"

          - task: "AuthStore 实现（JWT + Refresh + ServerAddress）"
            agent: "data-integration"
            deliverable: "AuthStore.kt + EncryptedSharedPreferences + 单测"
            est_hours: 8
            priority: P0_CRITICAL
            icd_produced: "ICD-AuthState-v1"

          - task: "登录页 + 服务器地址输入校验"
            agent: "frontend-business"
            deliverable: "LoginScreen.kt + ServerAddress.parse() + 测试"
            est_hours: 8
            priority: P0_CRITICAL
            depends_on: ["AuthStore"]

      - wp_id: "AR-WP00-THEME"
        name: "设计 Token 落地"
        L3_tasks:
          - task: "AeroTheme + 全套 Token 实现"
            agent: "frontend-platform"
            deliverable: "ui/theme/*.kt（7 个文件）"
            est_hours: 10
            priority: P0_CRITICAL
            icd_produced: "ICD-DesignTokens-v1"
            reference: "references/design-system-spec.md"

  L1_phase_1_real_data:
    name: "Phase 1 · 接真数据"
    duration_estimate: "3-4 周"
    gate: "Gate 3 · Phase 1 Go/No-Go"
    L2_work_packages:
      - wp_id: "AR-WP01-REALTIME"
        name: "实时通信框架"
        L3_tasks:
          - task: "WebSocket 客户端 + 心跳 + 重连"
            agent: "frontend-platform"
            deliverable: "RealtimeClient.kt + Flow<WSMessage>"
            est_hours: 12
            priority: P0_CRITICAL
            icd_produced: "ICD-BroadcastWS-v1"

          - task: "10s 轮询回退机制"
            agent: "frontend-platform"
            deliverable: "PollingFallback.kt + 切换逻辑"
            est_hours: 6
            priority: P1_HIGH
            depends_on: ["WebSocket 客户端"]
            icd_produced: "ICD-RealtimeFallback-v1"

          - task: "实时断线 banner UI"
            agent: "frontend-business"
            deliverable: "ConnectionBanner.kt"
            est_hours: 4
            priority: P1_HIGH
            depends_on: ["WebSocket 客户端"]

      - wp_id: "AR-WP01-TERMINAL"
        name: "终端 Tab 接真数据"
        L3_tasks:
          - task: "TerminalRepository（GET /terminal/terminalinfo + /zoneterminal）"
            agent: "data-integration"
            deliverable: "TerminalRepository.kt + DTO + Mapper"
            est_hours: 10
            priority: P0_CRITICAL
            icd_produced: "ICD-TerminalDto-v1, ICD-ZoneDto-v1"

          - task: "终端 Tab 屏幕真数据接入"
            agent: "frontend-business"
            deliverable: "TerminalHubScreenV2 + ViewModel"
            est_hours: 12
            priority: P0_CRITICAL
            depends_on: ["TerminalRepository", "WebSocket 客户端"]

          - task: "分区详情页 + 多选状态"
            agent: "frontend-business"
            deliverable: "ZoneDetailScreen.kt + 多选 ViewModel"
            est_hours: 10
            priority: P1_HIGH
            depends_on: ["终端 Tab 屏幕"]

      - wp_id: "AR-WP01-BROADCAST"
        name: "广播 Tab（三档）"
        L3_tasks:
          - task: "广播 Tab 三档 Segmented Control 框架"
            agent: "frontend-business"
            deliverable: "BroadcastScreen.kt + Mode 切换"
            est_hours: 8
            priority: P1_HIGH

          - task: "寻呼模式实现（含 POST /terminal/urgentplay）"
            agent: "frontend-business"
            deliverable: "PageMode.kt + 录音状态机"
            est_hours: 12
            priority: P1_HIGH
            legacy_impact: true   # 涉及 native 录音
            depends_on: ["广播 Tab 框架"]

          - task: "点播模式实现（媒体选择 + 音量）"
            agent: "frontend-business"
            deliverable: "CastMode.kt + MediaPicker"
            est_hours: 10
            priority: P1_HIGH
            depends_on: ["广播 Tab 框架"]

          - task: "对讲模式实现（接入 libs/htapplib.aar）"
            agent: "legacy-native"
            deliverable: "TalkMode.kt + VoiceTalkAdapter.kt"
            est_hours: 16
            priority: P1_HIGH
            legacy_impact: true
            abi_sensitive: true   # 32 位 ABI 强相关
            icd_produced: "ICD-VoiceAAR-v1"

      - wp_id: "AR-WP01-IPC"
        name: "本机 IPC 现代化"
        L3_tasks:
          - task: "TCP socket 4521 Kotlin 协程封装"
            agent: "legacy-native"
            deliverable: "LocalSocketClient.kt + ShellCommand sealed class"
            est_hours: 8
            priority: P2_NORMAL
            icd_produced: "ICD-IPCSocket-v1"

  L1_phase_2_full_tabs:
    name: "Phase 2 · 完整 Tab"
    duration_estimate: "3-4 周"
    L2_work_packages:
      - wp_id: "AR-WP02-TASK"
        name: "任务 Tab 完整实现"
        L3_tasks:
          - task: "TaskRepository（/task/* 端点 ×5）"
            agent: "data-integration"
            deliverable: "TaskRepository.kt + DTOs"
            est_hours: 10
            priority: P1_HIGH
            icd_produced: "ICD-TaskDto-v1"

          - task: "任务 Tab 主页（今日时间轴 + 作息方案）"
            agent: "frontend-business"
            deliverable: "TaskScreenV4.kt + ViewModel"
            est_hours: 12
            priority: P1_HIGH
            depends_on: ["TaskRepository"]

          - task: "作息方案详情 + 编辑屏"
            agent: "frontend-business"
            deliverable: "ScheduleDetailScreen + TaskEditScreen"
            est_hours: 14
            priority: P1_HIGH

          - task: "临时文件广播屏"
            agent: "frontend-business"
            deliverable: "TempFileBroadcastScreen.kt"
            est_hours: 8
            priority: P2_NORMAL

      - wp_id: "AR-WP02-SERVICE"
        name: "服务 Tab（系统健康度）"
        L3_tasks:
          - task: "系统状态 Repository（/server/serverstate）"
            agent: "data-integration"
            deliverable: "ServerStateRepository.kt"
            est_hours: 4
            priority: P2_NORMAL

          - task: "服务 Tab UI（健康度 + 工单列表）"
            agent: "frontend-business"
            deliverable: "ServicePlaceholderScreen → ServiceHomeScreen"
            est_hours: 10
            priority: P2_NORMAL

      - wp_id: "AR-WP02-MAP"
        name: "百度地图集成"
        L3_tasks:
          - task: "百度地图 SDK 集成 + 定位"
            agent: "legacy-native"
            deliverable: "MapClient.kt + LocationProvider.kt"
            est_hours: 10
            priority: P2_NORMAL
            icd_produced: "ICD-MapLocation-v1"

          - task: "终端 Tab 地图视图"
            agent: "frontend-business"
            deliverable: "TerminalMapView.kt"
            est_hours: 8
            priority: P2_NORMAL
            depends_on: ["百度地图 SDK 集成"]

  L1_phase_3_polish_release:
    name: "Phase 3 · 打磨发布"
    duration_estimate: "2-3 周"
    gate: "Gate 4 · Final Release Approval"
    L2_work_packages:
      - wp_id: "AR-WP03-TABLET"
        name: "平板版（1280×800 三栏）"
        L3_tasks:
          - task: "WindowSizeClass adaptive layout"
            agent: "frontend-platform"
            deliverable: "AdaptiveScaffold.kt + ScreenSize 检测"
            est_hours: 10
            priority: P1_HIGH

          - task: "平板终端 Hub（Rail + List + Detail）"
            agent: "frontend-business"
            deliverable: "TerminalHubTablet.kt"
            est_hours: 14
            priority: P1_HIGH
            depends_on: ["WindowSizeClass adaptive layout"]

      - wp_id: "AR-WP03-LEGACY-CLEANUP"
        name: "旧栈清理"
        L3_tasks:
          - task: "评估 httptask/*Method.java 哪些可删"
            agent: "data-integration"
            deliverable: "legacy-removal-plan.md"
            est_hours: 6
            priority: P2_NORMAL
            legacy_impact: true

          - task: "删除已迁移的旧栈代码"
            agent: "data-integration"
            deliverable: "PR：删除 N 个 *Method.java 文件"
            est_hours: 4
            priority: P2_NORMAL
            depends_on: ["评估 httptask"]

      - wp_id: "AR-WP03-AI-SUITE"
        name: "AI 助手套件（如果 Phase 3 还能塞）"
        L3_tasks:
          - task: "AI 主页 + 4 个子屏"
            agent: "frontend-business"
            deliverable: "AIHome + AIMigrate + AISwap + AICreate"
            est_hours: 16
            priority: P3_LOW
            note: "Handoff 标注为'待接入 NLU 后可上线'，Phase 3 可降级"
```

### 1.3 估算校准（基于 AeroRadio 历史）

> 项目刚启动时，使用 itc 原版的默认校准；随项目推进，PM memory.md 会更新本表。

```yaml
estimation_calibration_initial:
  domain_factors:
    frontend-business:
      raw_estimate_multiplier: 1.20    # +20%（Compose 一次写对率中等）
    frontend-platform:
      raw_estimate_multiplier: 1.40    # +40%（WS / 平板适配踩坑多）
    data-integration:
      raw_estimate_multiplier: 1.25    # +25%（动态 baseUrl 等首次）
    legacy-native:
      raw_estimate_multiplier: 1.60    # +60%（最不可预测）

  task_type_adjustments:
    novel_work: 1.30                   # 首次类任务 +30%
    repeat_work: 0.80                  # 重复类 -20%
    legacy_impact: 1.50                # 涉及旧栈 +50%
    abi_sensitive: 1.40                # 涉及 32 位 ABI +40%
    cross_domain: 1.25                 # 跨域协作 +25%
```

---

## 2. DAG Construction & Maintenance

### 2.1 DAG Build Rules

```yaml
dag_build:
  invariants:
    - "无环（DAG validate on every edge add）"
    - "每个 task 至少属于一个 Phase"
    - "Critical path 自动计算"
    - "Slack hours 自动计算"

  edge_types:
    - data_dependency      # A 输出是 B 输入
    - resource_dependency  # 共享代码 / 文件
    - spatial_dependency   # Compose 父子
    - execution_dependency # B 在 A 完成后执行
    - review_dependency    # B 在 A 过审后开始
```

### 2.2 DAG Maintenance Triggers

```yaml
dag_maintenance:
  on_task_add:
    - "Add node to DAG"
    - "Parse dependencies from task spec"
    - "Validate no cycle introduced"
    - "Recompute critical path"
    - "Notify affected agents if schedule changes"

  on_task_remove:
    - "Remove node and all edges"
    - "Check for orphaned tasks"
    - "Recompute critical path"

  on_dependency_change:
    - "Update edge with new type / metadata"
    - "Validate no cycle introduced"
    - "If critical path changes & impact > 1 day → escalate to CTO"

  on_completion:
    - "Mark node as COMPLETED"
    - "Identify newly unblocked tasks"
    - "Add ready-to-assign tasks to dispatch queue"
    - "Update critical path (actuals vs planned)"

  on_icd_breaking_change:
    - "Find all tasks that consume the changing ICD"
    - "Add data_dependency edge to migration task"
    - "Recompute critical path"
    - "Warn affected agents"
```

### 2.3 Critical Path Visualization

```
Phase 0 Critical Path: AR-001 → AR-002 → AR-003 (22h)

[AR-001] 动态 baseUrl 拦截器     [ 6h] ████████░░  IN_PROGRESS  data-integration
   │
   ▼ data
[AR-002] AuthStore 实现           [ 8h] ░░░░░░░░░░  PENDING     ◄── Critical
   │
   ▼ data
[AR-003] 登录页 + 服务器地址校验  [ 8h] ░░░░░░░░░░  PENDING     ◄── Critical
   │
   ├──► [AR-004] AeroTheme 落地  [10h] ░░░░░░░░░░  PENDING  frontend-platform
   │
   ▼ execution
[AR-005] WS 客户端 + 心跳         [12h] ░░░░░░░░░░  PENDING     ◄── Phase 1 起点
```

---

## 3. Task State Machine Management

### 3.1 State Transition Enforcement

```yaml
state_machine:
  valid_transitions:
    PENDING:
      - to: ASSIGNED
        by: "project-manager"
        condition: "dependencies_met AND agent_available"

    ASSIGNED:
      - to: IN_PROGRESS
        by: "domain-agent"
        condition: "agent_accepts"
      - to: PENDING
        by: "domain-agent"
        condition: "agent_rejects_with_reason"
      - to: IN_PROGRESS
        by: "auto"
        condition: "agent_no_response_1h"

    IN_PROGRESS:
      - to: REVIEW
        by: "domain-agent"
        condition: "deliverable_submitted"
      - to: PENDING
        by: "project-manager"
        condition: "agent_blocked_or_gives_up"
      - to: ESCALATED
        by: "auto"
        condition: "in_progress > 2x estimated AND no update"

    REVIEW:
      - to: PASSED
        by: "critic-agent"
        condition: "no BLOCKER AND no MAJOR findings"
      - to: PASSED_WITH_MINOR
        by: "critic-agent"
        condition: "only MINOR/INFO findings; agent_can_fix_post_pass"
      - to: FAILED
        by: "critic-agent"
        condition: "BLOCKER OR MAJOR findings"
      - to: AUTO_REJECTED
        by: "auto"
        condition: "review_pending > 24h"

    PASSED:
      - to: COMPLETED
        by: "project-manager"
        condition: "cto_not_required_or_cto_approved"
      - to: HUMAN_REVIEW
        by: "project-manager"
        condition: "cto_mandatory_gate (G1/G2/G3/G4)"

    FAILED:
      - to: IN_PROGRESS
        by: "domain-agent"
        condition: "revision_submitted"
      - to: ESCALATED
        by: "project-manager"
        condition: "3x_failed"

    HUMAN_REVIEW:
      - to: COMPLETED
        by: "human-cto"
        condition: "cto_approves"
      - to: FAILED
        by: "human-cto"
        condition: "cto_rejects_with_feedback"

    ESCALATED:
      - to: "any_previous_state"
        by: "human-cto"
        condition: "cto_resolves"
      - to: CANCELLED
        by: "human-cto"
        condition: "cto_cancels"
```

### 3.2 State Change Protocol

```yaml
state_change_protocol:
  on_state_change:
    - "Log transition with timestamp, actor, reason"
    - "Update DAG node color / state"
    - "Notify downstream task owners if dependencies change"
    - "Update dashboard"
    - "Check if critical path affected"
    - "If critical path delay > 0.5 day → flag risk"
    - "If delay > 1 day → escalate to CTO"

  auto_transitions:
    ASSIGNED → IN_PROGRESS: 1_hour_timeout
    REVIEW → AUTO_REJECTED: 24_hour_timeout
    IN_PROGRESS → ESCALATED: 2x_estimate_timeout
```

---

## 4. Scheduling Algorithm

### 4.1 Priority + Dependency + Resource Scheduling

```python
def schedule_next_task(dag: DAG, agents: List[Agent]) -> Optional[TaskAssignment]:
    """
    Select next task to assign considering:
    1. Dependencies (all predecessors must be COMPLETED)
    2. Priority (critical path first, then priority level)
    3. Resource availability (agent capacity)
    4. Historical performance calibration
    """

    # Step 1: Find all tasks with met dependencies
    ready_tasks = [
        t for t in dag.tasks
        if t.status == PENDING
        and all(dep.status == COMPLETED for dep in t.dependencies)
    ]

    if not ready_tasks:
        return None

    # Step 2: Score each task
    for task in ready_tasks:
        task.schedule_score = compute_task_score(task, dag, agents)

    # Step 3: Select highest scoring task
    next_task = max(ready_tasks, key=lambda t: t.schedule_score)

    # Step 4: Find best agent (only agents in next_task.domain)
    available_agents = [
        a for a in agents
        if a.domain == next_task.domain
        and a.current_load < a.max_capacity
    ]

    if not available_agents:
        return None  # Will retry on next cycle

    best_agent = select_best_agent(next_task, available_agents)

    # Step 5: Apply calibration
    calibrated_hours = apply_calibration(next_task.est_hours, best_agent, next_task)

    return TaskAssignment(
        task=next_task,
        agent=best_agent,
        deadline=compute_deadline(calibrated_hours),
        priority=dynamic_priority(next_task, dag)
    )


def compute_task_score(task, dag, agents) -> float:
    """Composite scheduling score."""
    score = 0.0

    # Critical path bonus (weight: 0.40)
    if task in dag.critical_path:
        score += 0.40 * (1 + dag.slack_hours(task) / 100)

    # Priority level (weight: 0.25)
    priority_map = {P0_CRITICAL: 1.0, P1_HIGH: 0.75, P2_NORMAL: 0.50, P3_LOW: 0.25}
    score += 0.25 * priority_map[task.priority]

    # Fan-out bonus (weight: 0.20)
    downstream_count = len(task.downstream_tasks)
    score += 0.20 * min(downstream_count / 5.0, 1.0)

    # Risk penalty (weight: 0.15)
    risk_score = get_risk_score(task)
    score += 0.15 * (1.0 - risk_score)

    return score


def select_best_agent(task, available_agents) -> Agent:
    """Select agent with best track record."""
    scored = []
    for agent in available_agents:
        score = (
            agent.first_pass_rate * 0.4 +
            (1 - abs(agent.estimation_bias)) * 0.3 +
            (1 - agent.current_load / agent.max_capacity) * 0.3
        )
        scored.append((agent, score))
    return max(scored, key=lambda x: x[1])[0]


def apply_calibration(raw_hours, agent, task) -> float:
    """Calibrate based on agent + task characteristics."""
    hours = raw_hours

    # Domain factor
    hours *= agent.domain_multiplier  # see memory.md

    # Task type adjustments
    if task.is_novel: hours *= 1.30
    if task.is_repeat: hours *= 0.80
    if task.legacy_impact: hours *= 1.50
    if task.abi_sensitive: hours *= 1.40
    if task.cross_domain: hours *= 1.25

    return hours
```

### 4.2 Dynamic Priority Adjustment

```yaml
dynamic_priority:
  trigger_conditions:
    - name: "Critical Path Threat"
      condition: "task.slack_hours < 4"
      action: "priority = P0_CRITICAL"
      notify: "cto_if_gate_at_risk"

    - name: "Downstream Cascade"
      condition: "task has > 3 downstream tasks AND any downstream has < 8h slack"
      action: "priority += 1 level (max P0)"

    - name: "Agent Idle Prevention"
      condition: "agent.utilization < 30% AND ready_task exists for that domain"
      action: "bump matching tasks priority by 0.5 level"

    - name: "Deadline Proximity"
      condition: "days_to_gate < 3 AND task not started"
      action: "priority = max(P1_HIGH, current)"
      notify: "agent + cto"

    - name: "Legacy Blocker Discovered"
      condition: "task discovers unforeseen legacy code blocker"
      action: "priority = P0_CRITICAL; escalate to CTO"
      notify: "cto immediately"
```

---

## 5. Risk Identification & Escalation

### 5.1 Risk Detection Engine

```yaml
risk_detection:
  monitoring_interval: "15 minutes"

  indicators:
    schedule_risks:
      - indicator: "Task in_progress_hours > 1.5 × estimated_hours"
        severity: "HIGH"
        action: "Ping agent; if no response in 30min, flag risk"

      - indicator: "Critical path task delayed > 0.5 day"
        severity: "CRITICAL"
        action: "Immediate notification to CTO with impact analysis"

      - indicator: "> 30% tasks in backlog not assigned"
        severity: "MEDIUM"
        action: "Review agent capacity; consider reallocation"

    quality_risks:
      - indicator: "Agent avg critic_cycles > 2.5 over last 5 tasks"
        severity: "HIGH"
        action: "Flag agent quality concern; consider pair review"

      - indicator: "Same task failed critic > 3 times"
        severity: "CRITICAL"
        action: "Escalate to CTO; recommend expert consultation"

    aeroradio_specific_risks:
      - indicator: "Design deviation count > 5 in a week"
        severity: "MEDIUM"
        action: "Flag to frontend agents; Critic to do design-focused review"

      - indicator: "32-bit ABI compatibility issue detected"
        severity: "HIGH"
        action: "Immediate Legacy-Native agent investigation; escalate if blocker"

      - indicator: "Legacy file count not decreasing in Phase 2+"
        severity: "MEDIUM"
        action: "Data-Integration agent to prioritize migration task"

      - indicator: "WS reconnection failure rate > 10%"
        severity: "HIGH"
        action: "Frontend-Platform agent to investigate; consider fallback adjustment"
```

### 5.2 Escalation Report Format

详见 `references/communication-protocol.md` 第 2.8 节 ESCALATION 消息格式。

---

## 6. Communication Interfaces

### 6.1 Interface with Critic Agent

```yaml
critic_interface:
  submit_for_review:
    message_type: "REVIEW_REQUEST"
    to: "agent.critic.reviewer"
    payload:
      deliverable_id: "DEL-{task_id}-v{version}"
      task_id: "TASK-AR-{seq}"
      task_name: ""
      domain: "frontend-business | frontend-platform | data-integration | legacy-native"
      agent: "agent_id"
      deliverable_type: "code | document | design | analysis"
      content: "{deliverable content or reference}"
      context:
        prd_reference: ""
        handoff_reference: ""
        icd_references: ["ICD-id-1", "ICD-id-2"]
        dependencies_met: []
        previous_reviews: []
      deadline: "review_due_timestamp"

  receive_review_result:
    message_type: "REVIEW_RESULT"
    from: "agent.critic.reviewer"
    on_passed:
      - "Update task status: REVIEW → PASSED"
      - "If CTO gate required → route to HUMAN_REVIEW"
      - "If no CTO gate → mark COMPLETED"
      - "Queue downstream tasks for dispatch"

    on_failed:
      - "Update task status: REVIEW → FAILED"
      - "Send feedback to domain agent with critic findings"
      - "Set revision deadline (default: original estimate × 0.5)"
      - "Increment failure counter"
      - "If failure_count ≥ 3 → escalate to CTO"

    on_escalate:
      - "Create escalation report"
      - "Route to CTO immediately"
      - "Notify all affected agents"
```

### 6.2 Interface with Domain Agents

```yaml
domain_agent_interface:
  assign_task:
    message_type: "TASK_ASSIGN"
    to: "{domain-agent}"
    payload:
      task_id: "TASK-AR-{seq}"
      name: ""
      description: ""
      deliverable_spec: ""
      acceptance_criteria: []
      deadline: "timestamp"
      priority: "P0|P1|P2|P3"
      dependencies:
        met: []
        pending: []
      context:
        prd_excerpt: ""
        related_deliverables: []
        icd_references: []
      flags:
        legacy_impact: true | false
        abi_sensitive: true | false
        cross_domain: true | false

  receive_status_update:
    message_type: "STATUS_UPDATE"
    from: "{domain-agent}"
    payload:
      task_id: ""
      state: "IN_PROGRESS | BLOCKED | READY_FOR_REVIEW"
      progress_pct: 0
      hours_spent: 0.0
      hours_remaining: 0.0
      blockers: []
      questions: []

  receive_deliverable:
    message_type: "DELIVERABLE"
    from: "{domain-agent}"
    payload:
      task_id: ""
      deliverable: ""
      self_check_completed: true
      known_issues: []
    next_action: "Route to Critic Agent for review"
```

### 6.3 Interface with Human CTO

```yaml
cto_interface:
  daily_report:
    frequency: "daily 09:00 local"
    format: "Markdown (see profile.md template)"

  escalation:
    trigger: "Risk level HIGH or above; or unresolved blocker > 4h"
    format: "Escalation report (see communication-protocol.md)"
    sla: "CTO responds within 2-4h depending on level"

  receive_directive:
    message_type: "CTO_DIRECTIVE"
    from: "human-cto"
    valid_directives:
      - "APPROVE_PLAN"
      - "APPROVE_GATE"
      - "REJECT_GATE_WITH_FEEDBACK"
      - "MODIFY_SCOPE"
      - "REASSIGN_TASK"
      - "CHANGE_PRIORITY"
      - "ACCEPT_RISK"
      - "INJECT_RESOURCE"
      - "PAUSE_PROJECT"
      - "ABORT_PROJECT"
    action: "Acknowledge within 15min; execute within 1h; confirm completion"

  gate_review:
    gates: ["G1_PLAN", "G2_ARCHITECTURE", "G3_PHASE1_GO_NOGO", "G4_FINAL_RELEASE"]
    format: "Gate review package with all deliverables and Critic summary"
    cto_response: "APPROVE | REJECT | REQUEST_CHANGES"
```

---

## 7. Dispatch Decision Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                     DISPATCH ENGINE                              │
│                                                                  │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │ New Task │───►│Deps Met? │───►│Agent     │───►│Critical  │  │
│  │ Added    │    │ Check    │    │Available?│    │ Path?    │  │
│  └──────────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘  │
│                       │               │               │         │
│                    NO │            NO │            YES│         │
│                       ▼               ▼               ▼         │
│                  ┌──────────┐   ┌──────────┐   ┌──────────┐   │
│                  │Wait Queue│   │Wait Pool │   │Priority  │   │
│                  │(retry on │   │(retry on │   │Boost +   │   │
│                  │ DAG      │   │ agent    │   │Dispatch  │   │
│                  │ update)  │   │ free)    │   │          │   │
│                  └──────────┘   └──────────┘   └─────┬────┘   │
│                                                       │         │
│                                                       ▼         │
│                                                ┌──────────┐    │
│                                                │Assign to │    │
│                                                │Best Agent│    │
│                                                │+ Deadline│    │
│                                                │+ ICD ref │    │
│                                                └──────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 8. Self-Diagnostics

```yaml
self_diagnostics:
  health_checks:
    - name: "DAG Integrity"
      interval: "every 15 min"
      check: "No cycles, all tasks reachable, critical path valid"

    - name: "Agent Availability"
      interval: "every 5 min"
      check: "All 4 Domain Agents + Critic responsive"

    - name: "Queue Health"
      interval: "every 15 min"
      check: "No task stuck in any state > SLA"

    - name: "Critic Pipeline"
      interval: "every 30 min"
      check: "Review queue not backing up > 5 items"

    - name: "ICD Consistency"
      interval: "daily"
      check: "All ICD references in tasks point to existing entries"

  recovery_actions:
    agent_unresponsive:
      - "Retry ping × 3"
      - "If still no response → mark agent OFFLINE"
      - "Reassign queued tasks to other agents (if domain allows)"
      - "Escalate to CTO if no backup agent"

    dag_corruption:
      - "Rebuild DAG from task records"
      - "Validate against last known good state"
      - "Alert CTO if unrecoverable"

    icd_inconsistency:
      - "Identify orphaned ICD references"
      - "Mark tasks with broken refs as BLOCKED"
      - "Notify affected Domain Agents to update"
```

---

*Project Manager Agent Skill v1.0.0 — The Complete Orchestration Toolkit for AeroRadio*
