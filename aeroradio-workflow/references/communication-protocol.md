# Communication Protocol

AeroRadioControl 多 Agent 系统的 agent 间通信协议规范。定义消息格式、消息类型、状态转换、DAG 依赖类型和评审流程。

## 1. Message Envelope Format

所有 agent 间消息使用统一的 YAML 信封格式：

```yaml
message:
  header:
    message_id: "uuid-v4"
    timestamp: "ISO-8601"
    from: "agent-id"
    to: "agent-id | broadcast"
    message_type: "TASK_ASSIGN | STATUS_UPDATE | DELIVERABLE | REVIEW_REQUEST |
                   REVIEW_RESULT | ESCALATION | HUMAN_REVIEW | TOOL_CALL |
                   TOOL_RESULT | ICD_UPDATE"
    priority: "CRITICAL | HIGH | NORMAL | LOW"

  body:
    # type-specific payload (见下文每种类型)

  trace:
    project_id: "aeroradio-v4"
    task_id: "task-uuid"
    parent_message_id: "msg-id-parent"   # 引用上一条消息形成链
    chain: ["msg-id-1", "msg-id-2", ...]  # 完整消息链（用于分布式追踪）

  signature:
    integrity_hash: "sha256"
    version: "1.0"
```

## 2. Message Types

### 2.1 TASK_ASSIGN (Project Manager → Domain Agent)

```yaml
body:
  task:
    task_id: "TASK-AR-{seq}"
    title: "任务标题"
    description: "详细需求"
    domain: "frontend-business | frontend-platform | data-integration | legacy-native"
    deliverables:
      - type: "code | document | design | analysis"
        format: "kotlin | markdown | figma | yaml"
        location_hint: "应该提交到哪个目录"
    dependencies:
      met: ["TASK-AR-001"]           # 已完成的依赖
      pending: ["TASK-AR-003"]       # 尚未完成的依赖（heads-up）
    deadline: "ISO-8601"
    estimated_hours: 8.0
    acceptance_criteria:
      - "criterion 1"
      - "criterion 2"
    input_artifacts:
      - type: "file | data | reference"
        source: "artifact-uri 或 文档路径"
        format: "step | csv | pdf | url"
    context:
      prd_reference: "AeroRadio_v4.html 第 X 节"
      handoff_reference: "Handoff.html 第 Y 节"
      related_deliverables: ["DEL-prev-1"]
    priority: "P0_CRITICAL | P1_HIGH | P2_NORMAL | P3_LOW"
```

### 2.2 STATUS_UPDATE (Domain Agent → Project Manager)

```yaml
body:
  status:
    task_id: "TASK-AR-{seq}"
    state: "IN_PROGRESS | BLOCKED | READY_FOR_REVIEW"
    progress_pct: 65                  # 自评进度
    hours_spent: 5.5
    hours_remaining: 3.0
    blockers:
      - description: "需要 Data-Integration 提供 DTO 定义"
        blocking_since: "ISO-8601"
        proposed_action: "建议召集 PM 协调"
    questions:
      - to: "human-cto | data-integration | etc."
        question: "..."
        urgency: "HIGH | NORMAL"
    next_milestone: "完成 X 部分，预计 ISO-8601"
```

### 2.3 DELIVERABLE (Domain Agent → Project Manager)

```yaml
body:
  deliverable:
    task_id: "TASK-AR-{seq}"
    deliverable_id: "DEL-{task_id}-v{version}"
    status: "COMPLETED | PARTIAL | BLOCKED"
    version: 1                        # revision 时递增
    artifacts:
      - type: "kotlin_file | compose_screen | repository | dto | doc"
        path: "app/src/main/kotlin/.../FooBar.kt"
        format: "kotlin"
        checksum: "sha256-hash"
        loc: 120                       # lines of code（若适用）
    self_assessment:
      completeness_pct: 95
      confidence_pct: 80
      self_checked: true
      known_issues:
        - "Bug X 未修复，影响 Y 场景"
      tested:
        unit: true
        integration: false
        ui: true
    cross_domain_notes:
      - to: "data-integration"
        note: "我用了你新拦截器的 @Url 模式，注意保持向后兼容"
    next_steps_suggested:
      - "建议 Frontend-Platform 接 WS 后做 e2e 测试"
```

### 2.4 REVIEW_REQUEST (Project Manager → Critic)

```yaml
body:
  review:
    deliverable_id: "DEL-{task_id}-v{version}"
    task_id: "TASK-AR-{seq}"
    domain: "frontend-business | ..."
    agent: "agent-id"
    scope: "FULL | DELTA"             # 首次审 FULL，re-review 用 DELTA
    focus_areas:                       # 提示 Critic 重点关注什么
      - "Compose recomposition 性能"
      - "JWT 刷新时序"
    previous_reviews: ["REV-prev-1"]   # 若是 revision，列出历史
    context:
      project_phase: "PHASE_1_REAL_DATA"
      risk_level: "HIGH | MEDIUM | LOW"
      cross_domain_impact: true
      affected_domains: ["data-integration"]
    deadline: "review_due_timestamp"
```

### 2.5 REVIEW_RESULT (Critic → Project Manager)

```yaml
body:
  review_result:
    review_id: "REV-{deliverable_id}-{seq}"
    deliverable_id: "DEL-{task_id}-v1"
    cycle_number: 1                    # 第几轮 review
    verdict: "PASSED | PASSED_WITH_MINOR | FAILED | ESCALATED"
    confidence: "HIGH | MEDIUM | LOW"
    review_duration_minutes: 45
    severity_counts:
      BLOCKER: 0
      MAJOR: 1
      MINOR: 3
      INFO: 2
    findings:
      - id: "F-001"
        severity: "BLOCKER | MAJOR | MINOR | INFO"
        category: "ASSUMPTION | ERROR | STANDARD | CROSS_DOMAIN | DOCUMENTATION | DESIGN_DEVIATION"
        location: "FooScreen.kt:42-58"
        description: "Recomposition 范围过大，会导致整屏重组"
        rationale: "原因解释"
        recommendation: "使用 derivedStateOf 或拆分 Composable"
        reference: "Compose Performance Best Practices §3.2"
        related_findings: ["F-003"]
    cross_domain_checks:
      - domains: ["frontend-business", "data-integration"]
        interface_parameter: "ZoneListResponse.terminals"
        declared_value: "List<TerminalDto>"
        verified: true
    assumptions_challenged:
      - assumption: "假设服务器返回的 terminals 数组永远非空"
        challenge: "若空列表（如新建分区无终端），UI 崩溃风险"
        impact: "NPE → 强制退出"
        evidence_required: "请提供空列表场景的 UI 状态截图"
    review_statistics:
      time_spent_minutes: 45
      files_reviewed: 3
      loc_reviewed: 280
      checklists_applied: ["compose-checklist", "data-layer-checklist"]
      previous_escapes: 0
    summary: "overall assessment paragraph"
    recommended_action: "Domain Agent 修复 F-001 后重提交"
```

### 2.6 TOOL_CALL (Domain Agent → Tool / Execution Layer)

```yaml
body:
  tool_call:
    call_id: "call-uuid"
    tool: "gradle | adb | kotlin-compiler | git | retrofit-test | compose-preview"
    method: "build | install | run | query | analyze"
    parameters:
      module: "app | data | feature-terminal"
      args: ["clean", "assembleDebug"]
    timeout_seconds: 600
    resource_limits:
      memory_mb: 4096
      cpu_cores: 4
```

### 2.7 TOOL_RESULT (Tool → Domain Agent)

```yaml
body:
  tool_result:
    call_id: "call-uuid"
    status: "SUCCESS | ERROR | TIMEOUT"
    output:
      format: "text | json | binary"
      data: "stdout content"
      files: ["build/outputs/apk/debug/app-debug.apk"]
      logs_path: "build/reports/..."
    metrics:
      execution_time_ms: 45000
      memory_peak_mb: 2048
      warnings_count: 3
      errors_count: 0
    error:
      code: "E_BUILD_FAILED"
      message: "Kotlin compilation error in FooScreen.kt:42"
      recoverable: false
      stack_trace: "..."
```

### 2.8 ESCALATION (Any Agent → Human CTO)

```yaml
body:
  escalation:
    escalation_id: "ESC-{timestamp}-{seq}"
    level: "L1 | L2 | L3 | L4"        # L4 = 立即响应
    category: "TECHNICAL | SCHEDULE | RESOURCE | RISK | QUALITY | SAFETY"
    triggered_by: "auto | pm | critic | agent-id"
    summary: "一句话总结问题"
    detailed_description: "完整上下文"
    impact:
      schedule_impact_days: 2.0
      quality_impact: "P0 功能不可用"
      cost_impact: "无 / 0 / 重要的需要量化"
      business_impact: "影响 Phase 1 Gate"
    root_cause: "根因分析"
    options:
      - option_id: "A"
        description: "方案 A"
        pros: ["..."]
        cons: ["..."]
        estimated_impact: "延期 1 天，质量提升"
      - option_id: "B"
        description: "方案 B"
        pros: []
        cons: []
        estimated_impact: ""
    recommendation:
      recommended_option: "A"
      rationale: "..."
      confidence: "HIGH | MEDIUM | LOW"
    request:
      decision_needed_by: "ISO-8601"
      specific_ask: "需要 CTO 明确决策方向 A 还是 B"
```

### 2.9 ICD_UPDATE (跨域接口契约变更通知)

AeroRadio 专有消息类型。当 Domain Agent 间共享的接口（如 DTO 字段、WebSocket 消息格式）变更时，PM 必须广播此消息。

```yaml
body:
  icd_update:
    icd_id: "ICD-{interface_name}-v{version}"
    interface_name: "TerminalDto | BroadcastWSMessage | ..."
    change_type: "ADD | REMOVE | MODIFY | DEPRECATE"
    description: "TerminalDto 新增 lastSeenAt 字段"
    affected_agents: ["frontend-business", "data-integration"]
    migration_window:
      start: "ISO-8601"
      end: "ISO-8601"
    breaking_change: true | false
    backward_compatibility:
      strategy: "deprecation_then_remove | parallel_versions | hard_cutover"
      notes: "..."
    reference_doc: "references/icd-contracts.md#TerminalDto-v2"
```

## 3. Task State Machine

```
PENDING
  │
  │ PM assigns
  ▼
ASSIGNED ──(accept)──> IN_PROGRESS
  │                        │
  │ decline                │ submit
  │                        ▼
  │                   REVIEW ──(PASSED)──> COMPLETED
  │                      │                  │
  │                      │ FAILED           │ human_approve
  │                      ▼                  ▼
  │                   REJECTED          HUMAN_REVIEW
  │                      │                  │
  │                      │ revise           │ approved
  │                      ▼                  ▼
  └────────────────(restart)            APPROVED
                                            │
                                            ▼
                                        COMPLETED → MERGED → ARCHIVED

Special states:
  - ESCALATED: 任何状态 → escalate to human
  - BLOCKED: IN_PROGRESS → 被依赖阻塞
  - PAUSED: 任何状态 → 人为暂停
  - AUTO_REJECTED: REVIEW 超时 24h → 自动打回 PENDING
```

详细转换规则见 `SKILL.md` 第 4 章 State Transitions 表。

## 4. DAG Dependency Types

| Type | Symbol | 描述 | AeroRadio 典型例子 |
|------|--------|------|------------------|
| Data Dependency | `D→` | A 的输出是 B 的输入 | Repository → ViewModel |
| Resource Dependency | `R→` | A 和 B 共享有限资源 | 多个 agent 改同一文件 |
| Spatial Dependency | `S→` | 物理代码位置依赖 | Compose 父屏 → 子屏 |
| Execution Dependency | `E→` | B 必须在 A 完成后执行 | 拦截器 → Repository |
| Review Dependency | `V→` | B 在 A 过审后才能开始 | 架构方案过审 → 实现 |

## 5. Review Flow

```
Domain Agent 提交 deliverable
         │
         ▼
Project Manager 路由到 Critic
         │
         ▼
Critic 执行评审（max 3 轮）
         │
    ┌────┴────┐
    │         │
    ▼         ▼
 PASSED    FAILED
    │         │
    ▼         ▼
COMPLETED  RETURN
 (或       (Domain Agent
  HUMAN     revise &
  REVIEW)   resubmit)
```

所有 deliverable **必须**通过 Critic 评审才能进入 human review（L4 escalation 除外）。

### 5.1 Review Cycle Limit

- 最多 3 轮 revision
- 第 4 次仍失败 → Critic 自动 ESCALATE 到 CTO
- 每轮 revision 使用 DELTA review（只看变化部分 + 之前的 BLOCKER/MAJOR）

## 6. Quality Gates

AeroRadio 项目的 4 个强制 Gate（与 itc 一致但场景特化）：

| Gate | Phase | Approver | Check Items |
|------|-------|----------|-------------|
| G1 | Plan Approval | Human CTO + Critic | WBS 完整性、依赖 DAG、估算合理性、风险登记 |
| G2 | Architecture Review | Human CTO + Critic | 拦截器方案、WS 协议、IPC 桥接、Compose 架构 |
| G3 | Phase 1 Go/No-Go | Human CTO + Critic | 登录通、终端 Tab 真数据、Critic pass rate ≥ 70% |
| G4 | Final Release | Human CTO + Critic | 全 Tab 通、平板版、性能、签名、设计偏离 ≤ 阈值 |

## 7. AeroRadio 专有协议规则

### 7.1 Domain Agent 间禁止直接通信

```
WRONG:  Frontend-Business ──直接──► Data-Integration
RIGHT:  Frontend-Business ──► PM ──► Data-Integration
```

理由：
- 隐式契约 → 集成失败时无法追溯
- 决策无审计记录 → 项目复盘困难
- Critic 无法横切发现一致性问题

### 7.2 设计偏离检测的强制广播

任何 Domain Agent 发现自己即将偏离 `references/design-system-spec.md` 时，必须先 STATUS_UPDATE 通知 PM，PM 再决策是否调用 Critic 预审，禁止"先做了再说"。

### 7.3 旧栈代码改动的特殊路由

所有对 `httptask/*Method.java` 的修改必须**同时**经过：
- Data-Integration Agent（评估迁移影响）
- Legacy-Native Agent（评估对 native 调用的影响）
- Critic（评估是否破坏现有功能）

PM 在派发涉及旧栈的任务时，必须明确标记 `legacy_impact: true`。

---

*Communication Protocol v1.0.0 — AeroRadioControl Multi-Agent Standard*
