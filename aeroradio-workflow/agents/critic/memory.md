---
name: critic-memory
description: >
  Critic Agent 的记忆配置文件。
  包含评审历史记录、AeroRadio 项目错误模式发生频率追踪、
  评审有效性指标、与各 Domain Agent 的反馈历史。
  所有记忆条目持续学习并校准评审策略。
version: 1.0.0
author: AeroRadio Architecture Team
derived_from: itc-enterprise-workflow/agents/critic v1.0.0
---

# Critic Agent — Memory

## 1. Memory Architecture

```
Critic Memory Store
├── Review History          # 所有评审的完整记录
│   ├── Review Records      # 每次评审详情
│   ├── Verdict Outcomes    # 评审结论与实际结果
│   └── Cycle Tracking      # 多轮评审循环统计
│
├── Error Pattern Tracking  # 错误模式频率追踪
│   ├── Pattern Frequency   # 每种错误的发生次数
│   ├── Domain Distribution # 各领域错误分布
│   └── Trend Analysis      # 错误趋势变化
│
├── Agent Feedback History  # 各 Agent 的评审历史
│   ├── First-Pass Rate     # 一次通过率
│   ├── Common Issues       # 常见问题
│   └── Improvement Trends  # 改进趋势
│
└── Calibration Data        # 严重程度校准数据
    ├── False Positive Log  # 误报记录
    ├── Escape Defects      # 漏报记录
    └── Severity Adjustments # 严重程度调整记录
```

---

## 2. Review History

### 2.1 Review Record Template

```yaml
review_record_template:
  review_id: "REV-{deliverable_id}-{cycle}"
  deliverable_id: "DEL-{task_id}-v{version}"
  task_id: "TASK-AR-{seq}"
  cycle_number: 1                   # 第几轮评审

  metadata:
    domain: "frontend-business | frontend-platform | data-integration | legacy-native"
    submitting_agent: "agent_id"
    project: "AeroRadio-v4"
    phase: "Phase 0..3"
    review_started_at: "ISO8601"
    review_completed_at: "ISO8601"
    review_duration_minutes: 0
    files_reviewed: 0
    loc_reviewed: 0

  verdict:
    result: "PASSED | PASSED_WITH_MINOR | FAILED | ESCALATED"
    confidence: "HIGH | MEDIUM | LOW"

  findings_summary:
    BLOCKER: 0
    MAJOR: 0
    MINOR: 0
    INFO: 0

  findings_detail:
    - id: "F-001"
      severity: "BLOCKER | MAJOR | MINOR | INFO"
      category: "ASSUMPTION | ERROR | STANDARD | CROSS_DOMAIN | DOCUMENTATION | DESIGN_DEVIATION"
      pattern_ref: "CMP-ERR-001 | KT-ERR-001 | NET-ERR-001 | ABI-ERR-001 | DSN-ERR-001 | ARC-ERR-001"
      title: ""
      location: "file:line-range"
      description: ""
      resolution_status: "OPEN | FIXED | DEFERRED | DISPUTED"

  cross_domain_checks:
    icd_references_verified: ["ICD-id-1", "ICD-id-2"]
    consistency_issues: []

  checklists_applied:
    - "frontend_business_checklist"
    - "cross_domain_checklist"

  outcome_followup:
    deliverable_passed_human_review: null   # 后续填充
    later_defects_found: []                 # 后续填充：漏报追踪
    findings_disputed_by_agent: []
    findings_overturned_by_cto: []

  lessons_learned: ""
```

### 2.2 Review Records Database (项目运行时填充)

```yaml
review_records: []   # 项目启动后追加
```

### 2.3 Review Performance Tracking

```yaml
review_performance:
  project: "AeroRadio-v4"
  rolling_metrics:
    last_7_days:
      reviews_completed: 0
      avg_duration_minutes: null
      avg_findings_per_review: null
      pass_rate_first_attempt: null

    last_30_days:
      reviews_completed: 0
      block_detection_rate: null     # 阻断率：BLOCKER 被拦截 / 总产出
      false_positive_rate: null
      escape_rate: null              # 漏报：通过后才发现的问题 / 总通过数

  per_domain:
    frontend-business:
      reviews_completed: 0
      avg_findings_per_review: null
      most_common_pattern: null

    frontend-platform:
      reviews_completed: 0
      avg_findings_per_review: null

    data-integration:
      reviews_completed: 0
      avg_findings_per_review: null

    legacy-native:
      reviews_completed: 0
      avg_findings_per_review: null
```

---

## 3. Error Pattern Tracking

### 3.1 Pattern Frequency Tracker

> 项目启动时为空，每次评审发现的错误自动累加。

```yaml
pattern_frequency:
  project: "AeroRadio-v4"
  last_updated: "2026-05-27"

  compose_errors:
    CMP-ERR-001: { occurrences: 0, agents: [], trend: "stable" }
    CMP-ERR-002: { occurrences: 0, agents: [], trend: "stable" }
    CMP-ERR-003: { occurrences: 0, agents: [], trend: "stable" }
    CMP-ERR-004: { occurrences: 0, agents: [], trend: "stable" }
    CMP-ERR-005: { occurrences: 0, agents: [], trend: "stable" }
    CMP-ERR-006: { occurrences: 0, agents: [], trend: "stable" }

  kotlin_errors:
    KT-ERR-001: { occurrences: 0 }
    KT-ERR-002: { occurrences: 0 }
    KT-ERR-003: { occurrences: 0 }
    KT-ERR-004: { occurrences: 0 }
    KT-ERR-005: { occurrences: 0 }
    KT-ERR-006: { occurrences: 0 }

  networking_errors:
    NET-ERR-001: { occurrences: 0 }
    NET-ERR-002: { occurrences: 0 }
    NET-ERR-003: { occurrences: 0 }
    NET-ERR-004: { occurrences: 0 }
    NET-ERR-005: { occurrences: 0 }
    NET-ERR-006: { occurrences: 0 }

  abi_native_errors:
    ABI-ERR-001: { occurrences: 0 }
    ABI-ERR-002: { occurrences: 0 }
    ABI-ERR-003: { occurrences: 0 }
    ABI-ERR-004: { occurrences: 0 }

  design_deviation_errors:
    DSN-ERR-001: { occurrences: 0 }
    DSN-ERR-002: { occurrences: 0 }
    DSN-ERR-003: { occurrences: 0 }
    DSN-ERR-004: { occurrences: 0 }
    DSN-ERR-005: { occurrences: 0 }
    DSN-ERR-006: { occurrences: 0 }
    DSN-ERR-007: { occurrences: 0 }

  architectural_errors:
    ARC-ERR-001: { occurrences: 0 }
    ARC-ERR-002: { occurrences: 0 }
    ARC-ERR-003: { occurrences: 0 }
```

### 3.2 Pattern Discovery Log

```yaml
pattern_discovery_log:
  description: "评审过程中发现的新模式（不在现有库中）"
  new_patterns: []   # 项目运行后填充
  proposal_workflow:
    - "Critic 发现疑似新模式"
    - "记录到 pattern_discovery_log"
    - "经过 3 次出现 → 提案到 skill.md 错误库"
    - "PM + CTO 评审后正式纳入"
```

### 3.3 AeroRadio Top Risk Patterns to Watch

```yaml
top_watch_patterns:
  # 基于 PM memory.md 的 RISK 库 + Critic 经验
  high_priority:
    - pattern: "DSN-ERR-001"
      reason: "多 agent 容易硬编码颜色"
      mitigation: "lint 规则 + 评审前必查"

    - pattern: "NET-ERR-001"
      reason: "动态 baseUrl 是新模式，团队首次使用"
      mitigation: "Phase 0 期间高频抽查"

    - pattern: "ABI-ERR-001"
      reason: "32 位 ABI 容易漏配置"
      mitigation: "build.gradle.kts 修改强制走 Critic"

    - pattern: "NET-ERR-005"
      reason: "新旧栈共存阶段高风险"
      mitigation: "data-integration 任务必查 httptask 引用"

  medium_priority:
    - pattern: "CMP-ERR-001"
      reason: "Compose 性能问题在大列表（终端 1000+）时放大"

    - pattern: "KT-ERR-006"
      reason: "JWT 刷新风暴可能在弱网测试时才暴露"

    - pattern: "DSN-ERR-004"
      reason: "Tab 颜色错配是用户最容易直接感知的偏离"
```

---

## 4. Agent Feedback History

### 4.1 Per-Agent Performance Profile

```yaml
agent_performance:

  - agent_id: "agent.frontend.business"
    deliverables_reviewed: 0
    first_pass_rate: null
    avg_findings_per_deliverable: null
    common_findings: []
    improvement_trend: null
    last_5_reviews: []

  - agent_id: "agent.frontend.platform"
    deliverables_reviewed: 0
    first_pass_rate: null
    common_findings: []

  - agent_id: "agent.data.integration"
    deliverables_reviewed: 0
    first_pass_rate: null
    common_findings: []

  - agent_id: "agent.legacy.native"
    deliverables_reviewed: 0
    first_pass_rate: null
    common_findings: []
```

### 4.2 Feedback Effectiveness

```yaml
feedback_effectiveness:
  description: "追踪 Critic 给的建议是否真正帮助 Agent 改进"
  metrics:
    - "Same pattern reoccurrence rate per agent"
    - "Average cycles to pass per task"
    - "Agent's self-correction rate (catches own issues in cycle 2+)"

  patterns_to_watch:
    - "Agent A 在 CMP-ERR-005 上反复犯错 → 说明反馈方式无效，需调整"
    - "Agent B 经过反馈后再无 NET-ERR-005 → 反馈有效"
```

---

## 5. Calibration Data

### 5.1 False Positive Log

```yaml
false_positives:
  description: "Critic 标记为问题但 CTO 或后续判定可接受的发现"
  records: []   # 项目运行后填充
  use_for: "校准 severity 阈值；调整 checklist"
```

### 5.2 Escape Defect Log (Critic 漏检)

```yaml
escape_defects:
  description: "通过 Critic 的产出后续被发现存在问题"
  records: []   # 项目运行后填充
  severity_impact:
    discovered_in_human_review: "moderate concern"
    discovered_in_production: "high concern — checklist gap"
    discovered_in_integration_test: "checklist need expansion"
  action_per_escape:
    - "Root cause analysis: why did Critic miss this?"
    - "Update checklist or pattern library"
    - "Document in pattern_discovery_log if new pattern"
```

### 5.3 Severity Adjustments

```yaml
severity_adjustments:
  description: "随项目进展调整某模式的默认严重程度"
  records: []
  example:
    - pattern: "DSN-ERR-002"
      original_severity: "MINOR"
      new_severity: "MAJOR"
      reason: "项目积累 10+ 次后发现累计影响大，提升以促进重视"
      effective_date: "TBD"
```

---

## 6. Cross-Project Learnings

```yaml
cross_project_knowledge:
  description: "从其他项目继承的评审经验（适用时）"
  inherited_from: "itc-enterprise-workflow"

  general_patterns:
    - "复杂表单往往隐藏验证逻辑漏洞"
    - "异步代码的边界条件最容易出错"
    - "第一次集成第三方 SDK 时合规问题最多"

  aeroradio_specific_learnings:
    - "迁移项目中，新旧栈共存期是问题高发期"
    - "32 位 ABI 是历史包袱，每次涉及 native 都要警惕"
    - "明文 HTTP 是约束，但密码相关零容忍"
    - "设计 token 的 hardcoded 偏离是最容易被忽视的"
```

---

## 7. Learning Rules

```yaml
learning_rules:
  per_review:
    - "Increment pattern_frequency for each finding"
    - "Update agent's common_findings list"
    - "If finding is new pattern → log to pattern_discovery_log"
    - "If finding overturned by CTO → add to false_positives"

  per_week:
    - "Compute rolling 7-day metrics"
    - "Identify top 5 most frequent patterns"
    - "Compare to previous week trend"
    - "Flag emerging patterns (>3 occurrences in 7 days)"

  per_gate:
    - "Comprehensive analysis: which patterns dominated this phase"
    - "Update top_watch_patterns for next phase"
    - "Adjust checklist if necessary"
    - "Report top issues to PM for sprint planning"

  per_project:
    - "Full audit of false_positive_log"
    - "Full audit of escape_defects"
    - "Update severity defaults based on accumulated data"
    - "Generate Critic post-mortem"
    - "Export AeroRadio-specific learnings for future Android projects"
```

---

## 8. Memory Maintenance

```yaml
memory_maintenance:
  hot_storage:
    - "Last 30 days of reviews"
    - "Current sprint patterns"
    - "Open findings still in resolution"

  warm_storage:
    - "Current project all reviews"
    - "Phase-aggregated metrics"

  cold_storage:
    - "Completed projects archives"
    - "Historical pattern frequencies"

  pruning:
    - "Records > 1 year old: summarize and archive"
    - "Closed findings > 6 months: keep summary only"
    - "False positives: keep all for calibration learning"
```

---

*Critic Agent Memory v1.0.0 — Every Review Sharpens the Next*
