---
name: project-manager-memory
description: >
  Project Manager Agent 的记忆配置文件。
  包含 AeroRadio 项目历史数据、4 个 Domain Agent 能力图谱、
  风险库（含 Android 迁移、32 位 ABI、设计偏离等专属模式）
  和调度策略优化记录。所有记忆条目均可学习和更新。
version: 1.0.0
author: AeroRadio Architecture Team
derived_from: itc-enterprise-workflow/agents/project-manager v1.0.0
---

# Project Manager Agent — Memory

## 1. Memory Architecture

```
Memory Store
├── Project History           # 已完成任务的完整记录
│   ├── Task Records          # 每个任务的执行数据
│   ├── Schedule Data         # 计划 vs 实际对比
│   └── Quality Metrics       # 质量相关指标
│
├── Team Capability Map       # 4 个 Domain Agent + Critic 的能力画像
│   ├── Domain Expertise      # 领域专长评分
│   ├── Performance Metrics   # 历史性能数据
│   └── Workload History      # 负载和效率记录
│
├── Risk Library              # 历史风险模式与解决方案
│   ├── Generic Risk Patterns # Android 开发通用风险
│   ├── AeroRadio Patterns    # 项目专属（迁移/ABI/设计偏离）
│   ├── Mitigation Playbook   # 应对措施库
│   └── Outcome Records       # 风险处理结果
│
└── Scheduling Heuristics     # 调度策略优化记录
    ├── Estimation Models     # 各领域估算校准
    ├── Priority Rules        # 优先级规则演进
    └── Reallocation Log      # 资源调整历史
```

---

## 2. Project History

### 2.1 Task Record Template

```yaml
task_record_template:
  task_id: "TASK-AR-{seq}"
  project: "AeroRadio-v4"
  phase: "Phase 0..3"
  name: "task_name"
  domain: "frontend-business | frontend-platform | data-integration | legacy-native"
  assigned_agent: "agent_id"

  # Flags
  flags:
    legacy_impact: bool
    abi_sensitive: bool
    cross_domain: bool

  # Estimation
  estimated_hours: 0.0       # 初始估算
  calibrated_hours: 0.0      # 经 PM 校准
  actual_hours: 0.0          # 实际耗时
  estimation_error_pct: 0.0  # (actual - calibrated) / calibrated

  # Timeline
  created_at: "ISO8601"
  assigned_at: "ISO8601"
  started_at: "ISO8601"
  submitted_at: "ISO8601"
  reviewed_at: "ISO8601"
  completed_at: "ISO8601"

  # Quality
  critic_cycles: 0           # 通过 Critic 所需的轮次
  findings_total: 0
  findings_by_severity:
    BLOCKER: 0
    MAJOR: 0
    MINOR: 0
    INFO: 0
  design_deviations: 0       # AeroRadio 专属
  final_verdict: "PASSED|FAILED"

  # Dependencies & ICD
  depended_on: []
  blocked_tasks: []
  icd_produced: []           # 本任务产出的新 ICD
  icd_consumed: []           # 本任务消费的 ICD

  status: "COMPLETED|ARCHIVED"
  lesson_learned: ""
  tags: []
```

### 2.2 Historical Task Database (项目启动后填充)

```yaml
project_history:
  project: "AeroRadio-v4"
  tasks:
    # Phase 0 任务示例（未来填充）
    - task_id: "TASK-AR-001"
      name: "动态 baseUrl 拦截器实现"
      domain: "data-integration"
      phase: "Phase 0"
      flags:
        legacy_impact: false
        cross_domain: true
      estimated_hours: 6.0
      calibrated_hours: 7.5    # × 1.25 domain factor
      actual_hours: null       # 待执行后填充
      critic_cycles: null
      findings: null
      icd_produced: ["ICD-NetworkModule-v1"]
      lesson_learned: null
      status: "IN_PROGRESS"

  # 历史记录会在每个任务完成后追加
```

### 2.3 Schedule Performance Tracking

```yaml
schedule_performance:
  project: "AeroRadio-v4"

  gate_tracking:
    G1_plan_approval:
      planned: "TBD"
      actual: null
      delay_days: null

    G2_architecture_review:
      planned: "TBD"
      actual: null

    G3_phase1_go_nogo:
      planned: "TBD"
      actual: null

    G4_final_release:
      planned: "TBD"
      actual: null

  spi_trend: []                # Schedule Performance Index 时间序列

  critical_path_accuracy:
    predicted_critical_tasks: []
    actual_critical_tasks: []
    accuracy_pct: null
```

---

## 3. Team Capability Map

### 3.1 Domain Agent Initial Baseline

> 项目刚启动，所有数据为基线。每完成 1 个任务，PM 自动更新对应 agent 的数据。

```yaml
team_capability:
  agents:
    - agent_id: "agent.frontend.business"
      domain: "frontend-business"
      expertise:                       # 0-1 proficiency
        compose_ui: 0.70               # 基线，待校准
        navigation: 0.65
        viewmodel_stateflow: 0.70
        adaptive_layout: 0.50          # 平板适配是新领域
        material3: 0.75

      performance_metrics:
        tasks_completed: 0
        avg_estimation_error_pct: null
        avg_critic_cycles: null
        first_pass_rate_pct: null      # 基线假设 60%
        avg_task_duration_hours: null
        on_time_delivery_pct: null

      workload:
        current_tasks: 0
        max_capacity: 3
        utilization_pct: 0

      calibration:
        estimation_bias: "neutral"     # 基线，待校准
        recommended_buffer_pct: 20     # Phase 0 默认 +20%
        strength: "Compose 基础牢，Material3 熟悉"
        weakness: "平板 adaptive layout 是首次"

    - agent_id: "agent.frontend.platform"
      domain: "frontend-platform"
      expertise:
        websocket: 0.60
        polling_fallback: 0.65
        push_notification: 0.55
        adaptive_layout: 0.55
        error_handling: 0.70

      performance_metrics:
        tasks_completed: 0
        avg_estimation_error_pct: null
        avg_critic_cycles: null
        first_pass_rate_pct: null      # 基线假设 55%
        avg_task_duration_hours: null

      workload:
        current_tasks: 0
        max_capacity: 2                # 平台任务复杂，容量稍低
        utilization_pct: 0

      calibration:
        estimation_bias: "underestimate"
        recommended_buffer_pct: 40
        strength: "Coroutine Flow + StateFlow 模式熟"
        weakness: "WS 重连边界条件首次，adaptive layout 首次"

    - agent_id: "agent.data.integration"
      domain: "data-integration"
      expertise:
        retrofit: 0.85
        okhttp_interceptor: 0.75
        room_database: 0.70
        hilt_di: 0.80
        legacy_migration: 0.50         # 新旧栈共存是新课题

      performance_metrics:
        tasks_completed: 0
        first_pass_rate_pct: null      # 基线假设 65%

      workload:
        current_tasks: 0
        max_capacity: 3
        utilization_pct: 0

      calibration:
        estimation_bias: "neutral"
        recommended_buffer_pct: 25
        strength: "Retrofit + Hilt 经验丰富"
        weakness: "动态 baseUrl + 双栈共存策略需要摸索"

    - agent_id: "agent.legacy.native"
      domain: "legacy-native"
      expertise:
        tcp_socket: 0.65
        aar_integration: 0.55
        ndk_jni: 0.45                  # 不需自己写 native，但要懂调用
        baidu_map_sdk: 0.50
        abi_management: 0.40           # 32 位 ABI 处理首次

      performance_metrics:
        tasks_completed: 0
        first_pass_rate_pct: null      # 基线假设 50%（最低）

      workload:
        current_tasks: 0
        max_capacity: 2
        utilization_pct: 0

      calibration:
        estimation_bias: "underestimate"
        recommended_buffer_pct: 60     # 最大 buffer
        strength: "Socket 编程基础"
        weakness: "32 位 ABI 历史包袱第一次处理，AAR 黑盒"

    - agent_id: "agent.critic.reviewer"
      domain: "cross-cutting"
      expertise:
        compose_review: 0.80
        kotlin_review: 0.85
        architecture_review: 0.80
        design_deviation_detection: 0.70

      performance_metrics:
        reviews_completed: 0
        avg_review_duration_minutes: null
        false_positive_rate: null      # 基线假设 10%
        block_detection_rate: null     # 基线假设 90%

      workload:
        current_reviews: 0
        max_capacity: 5
```

### 3.2 Team Summary Dashboard

```
┌────────────────────┬──────────┬──────────┬──────────┬──────────┬──────────────┐
│ Agent              │ Tasks    │ Est.     │ Critic   │ On-Time  │ Capacity     │
│                    │ Done     │ Error%   │ Cycles   │ %        │ (used/max)   │
├────────────────────┼──────────┼──────────┼──────────┼──────────┼──────────────┤
│ frontend-business  │   0      │   N/A    │   N/A    │   N/A    │   0/3        │
│ frontend-platform  │   0      │   N/A    │   N/A    │   N/A    │   0/2        │
│ data-integration   │   0      │   N/A    │   N/A    │   N/A    │   0/3        │
│ legacy-native      │   0      │   N/A    │   N/A    │   N/A    │   0/2        │
├────────────────────┼──────────┼──────────┼──────────┼──────────┼──────────────┤
│ TEAM TOTAL         │   0      │   N/A    │   N/A    │   N/A    │   0/10       │
└────────────────────┴──────────┴──────────┴──────────┴──────────┴──────────────┘
```

---

## 4. Risk Library

### 4.1 AeroRadio-Specific Risk Patterns

```yaml
risk_library:
  patterns:
    # ─── 网络栈迁移类 ───
    - risk_id: "RISK-AR-001"
      name: "Dynamic baseUrl Interceptor Race Condition"
      category: "technical"
      domains: ["data-integration", "frontend-business"]
      frequency: 0                     # 项目内首次，待观察
      detection_signals:
        - "登录后立刻发请求出现 placeholder.invalid host 错误"
        - "AuthStore 状态切换瞬间的请求失败"
        - "多线程并发请求时偶发 NPE"
      typical_impact:
        delay_hours: 6
        affected_downstream: ["all frontend tasks"]
      root_causes:
        - "AuthStore 写入未完成时拦截器读到旧值"
        - "拦截器对 placeholder.invalid 的判断逻辑漏洞"
      mitigations:
        - "AuthStore 使用 first() 阻塞读取，确保写入完成"
        - "拦截器单元测试覆盖 placeholder / 真实地址 / 空地址三种 case"
        - "登录成功事件做一次 ping 验证"
      effectiveness_rating: null

    - risk_id: "RISK-AR-002"
      name: "Old Stack vs New Stack Coexistence Conflicts"
      category: "technical"
      domains: ["data-integration"]
      frequency: 0
      detection_signals:
        - "同一 endpoint 同时被新旧栈调用"
        - "旧栈缓存与新栈状态不一致"
        - "OkHttp 实例多个，连接池资源浪费"
      typical_impact:
        delay_hours: 4
        affected_downstream: ["repository tasks"]
      root_causes:
        - "迁移过程中两套栈共存"
        - "依赖未集中管理"
      mitigations:
        - "共享同一个 OkHttpClient 实例（通过 Hilt 注入）"
        - "迁移按模块进行，整模块切换后立刻删除旧栈"
        - "禁止新代码引用 httptask/*Method.java"

    # ─── 32 位 ABI 类 ───
    - risk_id: "RISK-AR-003"
      name: "32-bit ABI Compatibility Death Spiral"
      category: "technical_blocker"
      domains: ["legacy-native"]
      frequency: 0
      severity: "CRITICAL"
      detection_signals:
        - "libs/htapplib.aar 在 64 位-only 设备上 UnsatisfiedLinkError"
        - "abiFilters 配置后 APK 无法在某些设备安装"
        - "Google Play 上架被拒（要求 64 位）"
      typical_impact:
        delay_hours: 24                # 高风险项
        affected_downstream: ["对讲模式整个 Mode B"]
      root_causes:
        - "AAR 只有 armeabi-v7a + x86，无 arm64"
        - "Google Play 上架强制要求 64 位"
      mitigations:
        - "提前确认 AAR 是否能拿到 64 位版本（向厂商沟通）"
        - "若拿不到 → 降级方案：对讲模式标注'不可用'"
        - "或使用厂商提供的服务端转发方案绕过 AAR"
      escalation_trigger: "CTO 介入与厂商谈"
      effectiveness_rating: null

    # ─── 实时通信类 ───
    - risk_id: "RISK-AR-004"
      name: "WebSocket Reconnection Storm"
      category: "performance"
      domains: ["frontend-platform"]
      frequency: 0
      detection_signals:
        - "网络抖动时大量并发重连请求"
        - "服务器侧拒绝连接"
        - "UI 频繁闪烁 banner"
      typical_impact:
        delay_hours: 3
        affected_downstream: ["all realtime tasks"]
      root_causes:
        - "未实现指数退避"
        - "多个 WebSocket 实例"
      mitigations:
        - "单例 WebSocket 客户端"
        - "指数退避 2/4/8/16/max 60s"
        - "前台/后台切换时 pause/resume"

    # ─── 设计规范偏离类 ───
    - risk_id: "RISK-AR-005"
      name: "Design System Drift in Multi-Agent Development"
      category: "quality"
      domains: ["frontend-business", "frontend-platform"]
      frequency: 0
      detection_signals:
        - "Hardcoded Color(0xFF...) 出现"
        - "Spacing 不是 8.dp 倍数"
        - "数字未使用 mono + tnum"
        - "Card shape 不是 12.dp"
      typical_impact:
        delay_hours: 2                 # 单次偏离修复成本低
        affected_downstream: []        # 但累积起来很大
      root_causes:
        - "Agent 没读 design-system-spec.md"
        - "Critic checklist 漏检"
      mitigations:
        - "每个 frontend 任务在 TASK_ASSIGN 时显式引用 design-system-spec.md"
        - "Critic 强制运行 design deviation checklist"
        - "Lint 规则禁止 hardcoded color"

    # ─── 后端协议未定类 ───
    - risk_id: "RISK-AR-006"
      name: "Backend WebSocket Format Mismatch"
      category: "integration"
      domains: ["frontend-platform", "data-integration"]
      frequency: 0
      detection_signals:
        - "厂商后端 WS 消息格式与 ICD-BroadcastWS-v1 草案不符"
        - "字段名不一致 / 类型不一致"
      typical_impact:
        delay_hours: 8
        affected_downstream: ["realtime tasks"]
      root_causes:
        - "ICD-BroadcastWS-v1 当前是 DRAFT，未与后端验证"
      mitigations:
        - "Phase 0 阶段就和厂商索要 WS 协议文档"
        - "使用 sealed class + 兼容字段，未知字段忽略"
        - "对接验证时立刻更新 ICD"

    # ─── 第三方 SDK 类 ───
    - risk_id: "RISK-AR-007"
      name: "Baidu Map SDK Permission / Privacy Issues"
      category: "compliance"
      domains: ["legacy-native"]
      frequency: 0
      detection_signals:
        - "首次启动未弹合规弹窗"
        - "权限请求时机不当"
        - "用户拒绝定位后崩溃"
      typical_impact:
        delay_hours: 6
      mitigations:
        - "百度地图最新版的合规接入流程严格按文档"
        - "定位权限延迟到用户进入地图视图时再请求"
        - "拒绝时降级到列表视图"
```

### 4.2 Generic Android Development Risk Patterns

```yaml
generic_android_risks:
  - risk_id: "RISK-AND-001"
    name: "Compose Recomposition Performance"
    detection_signals:
      - "Frame drop on scroll"
      - "Profiler shows excessive recomposition"
    mitigations:
      - "Use derivedStateOf"
      - "Stable / Immutable annotations"
      - "@Composable 函数拆分"

  - risk_id: "RISK-AND-002"
    name: "Memory Leak from Coroutine Scope"
    detection_signals:
      - "LeakCanary 报警"
      - "Activity / Fragment 持续持有"
    mitigations:
      - "ViewModelScope / lifecycleScope 严格遵守"
      - "禁止 GlobalScope"

  - risk_id: "RISK-AND-003"
    name: "ProGuard / R8 Stripped Reflection"
    detection_signals:
      - "Release 构建崩溃，Debug 正常"
      - "JSON 反序列化失败"
    mitigations:
      - "kotlinx.serialization 替代 Gson reflection"
      - "ProGuard rules for kept classes"
```

### 4.3 Current Risk Register (项目运行时维护)

```yaml
current_risk_register:
  project: "AeroRadio-v4"
  last_updated: "2026-05-27"
  risks:
    - risk_id: "R-001"
      pattern_ref: "RISK-AR-003"
      description: "32 位 ABI 兼容性，对讲模式可能完全不可用"
      probability: 0.10            # was 0.40; SPIKE-001 证 arm64 库齐全自洽
      impact: "LOW"                # was CRITICAL; CTO D-05 批准条件性降级 2026-05-27
      score: 0.10                  # was 0.40
      status: "LOW-CONDITIONAL (GO-pending-device, 未 CLOSED)"  # was MONITORING
      owner: "legacy-native + CTO"
      downgrade_basis: >
        SPIKE-001 (.state/spike-aar64-report.md v2, Critic PASSED_WITH_MINOR HIGH, 独立复现):
        ① 厂商 5/14 已随 AAR 供 arm64-v8a 双 .so; ② 链接静态自洽, JNI 与 32 位 diff=IDENTICAL,
        零 32 位-only 悬空依赖; ③ 架构纠正——HTIntf.* 纯 Java, 唯一 native 面=MediaCodec.Mp3Encode*.
      closure_prerequisite: >
        正式关闭 (LOW→CLOSED) 唯一前置: 一台 arm64-v8a 真机(Android 7.0+) 执行报告 §7.2 步骤 A+B,
        回执含 设备型号 + ro.product.cpu.abi=arm64-v8a + 无 UnsatisfiedLinkError + Mp3EncodeInit 成功码 +
        Mp3EncodeBuffer>0 + 无 SIGSEGV + logcat 无 dlopen alignment/relocation 告警. (CTO D-06 持真机做)
      residual_caveat: "x86_64 模拟器无法验语音 native 面 (AAR 无 x86_64 .so); 仅 arm64 真机/arm64 镜像可验"
      mitigation: "降级期保留 armeabi-v7a 兜底 + 64 位-only 设备 VoiceTalkAdapter.isAvailable()/FallbackScreen 降级 UI (报告 §8.1, 不取消)"
      contingency: "若真机回执 NO-GO: 对讲模式标注'不可用' + 厂商沟通 escalation"

    - risk_id: "R-002"
      pattern_ref: "RISK-AR-006"
      description: "WS 协议厂商未提供文档，ICD 为 DRAFT"
      probability: 0.60
      impact: "HIGH"
      score: 0.42
      owner: "frontend-platform"
      status: "ACTIVE"
      mitigation: "立即向厂商索要；同时按 DRAFT 实现，对接时调整"
      trigger: "Phase 1 开始前"
```

---

## 5. Scheduling Heuristics

### 5.1 Estimation Calibration Models

```yaml
estimation_calibration:
  # 项目启动时的基线，会随任务完成动态更新
  domain_factors:
    frontend-business:
      raw_estimate_multiplier: 1.20
      variance: 0.20
    frontend-platform:
      raw_estimate_multiplier: 1.40
      variance: 0.25
    data-integration:
      raw_estimate_multiplier: 1.25
      variance: 0.20
    legacy-native:
      raw_estimate_multiplier: 1.60
      variance: 0.35

  task_type_adjustments:
    novel_work: 1.30
    repeat_work: 0.80
    legacy_impact: 1.50
    abi_sensitive: 1.40
    cross_domain: 1.25
    icd_producing: 1.20              # 产出 ICD 的任务额外开销
```

### 5.2 Priority Rules (Evolved)

```yaml
priority_rules:
  version: 1
  last_updated: "2026-05-27"

  rules:
    - id: "PR-AR-001"
      name: "Critical Path Priority"
      condition: "task is on critical path"
      action: "priority = max(current, P1_HIGH)"
      weight: 1.0

    - id: "PR-AR-002"
      name: "ICD Producer Boost"
      condition: "task produces ICD that >=2 downstream tasks consume"
      action: "priority += 1 level"
      weight: 0.90
      rationale: "ICD 阻塞多任务，必须先产出"

    - id: "PR-AR-003"
      name: "Gate Proximity"
      condition: "days_to_next_gate < 5 AND task is gate-blocking"
      action: "priority = P0_CRITICAL"
      weight: 0.95

    - id: "PR-AR-004"
      name: "Legacy Cleanup Wait"
      condition: "task is legacy cleanup AND new code path verified"
      action: "priority = P3_LOW"
      weight: 0.50
      rationale: "迁移完成再清理，避免误删"

    - id: "PR-AR-005"
      name: "Cross-Domain Sync"
      condition: "task is cross-domain AND blocks another domain"
      action: "priority += 1 level"
      weight: 0.85
```

### 5.3 Reallocation Decision Log

```yaml
reallocation_log: []   # 项目运行后填充
```

---

## 6. Learning & Adaptation Rules

```yaml
learning_rules:
  per_task_update:
    - "Update agent's avg_estimation_error with EMA (α=0.3)"
    - "Increment tasks_completed counter"
    - "Update critic_cycles average"
    - "If estimation_error > 30%, flag for calibration review"
    - "If task produced ICD: archive ICD reference to icd-contracts.md"

  per_sprint_update:
    - "Recalculate domain calibration factors"
    - "Update priority rule effectiveness scores"
    - "Review and update risk library with new observations"
    - "Generate sprint retrospective insights"

  per_gate_update:
    - "Full review of all tasks completed before gate"
    - "Update gate_tracking with actual dates"
    - "Identify which risk patterns realized"
    - "Update mitigation effectiveness ratings"

  per_project_update:
    - "Full recalibration of all estimation models"
    - "Update team capability scores"
    - "Archive risk patterns and outcomes"
    - "Generate project post-mortem report"
    - "Update scheduling heuristics based on project outcomes"
    - "Export learnings for future AeroRadio projects"
```

---

*Project Manager Agent Memory v1.0.0 — Learning from Every Task, Every Sprint, Every Gate*
