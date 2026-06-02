---
name: frontend-business-memory
description: >
  Frontend-Business Agent 的记忆配置文件。
  跟踪屏实现历史、常见错误模式、ViewModel 模板演进、ICD 消费记录。
version: 1.0.0
author: AeroRadio Architecture Team
---

# Frontend-Business Agent — Memory

## 1. Memory Architecture

```
Frontend-Business Memory
├── Screen Implementation History  # 屏的实现记录
├── ViewModel Patterns             # ViewModel 模板演进
├── Design Token Usage             # token 使用统计
├── ICD Consumption Log            # ICD 消费历史
└── Self-Improvement Notes         # 自我改进笔记
```

## 2. Screen Implementation History

### 2.1 Screen Record Template

```yaml
screen_record_template:
  screen_id: "SCREEN-{tab}-{name}"
  name: "TerminalHubScreen"
  tab: "terminal | broadcast | ai | task | service | login | other"

  metadata:
    phase: "Phase 0..3"
    created_at: "ISO8601"
    task_id: "TASK-AR-{seq}"
    files_created: []
    loc: 0
    has_preview: true | false
    has_unit_test: true | false

  states_implemented:
    loading: true | false
    empty: true | false
    error: true | false
    success: true | false
    partial: true | false

  icd_consumed:
    - "ICD-TerminalDto-v1"
    - "ICD-BroadcastWS-v1"

  design_compliance:
    hardcoded_color_count: 0    # 应该是 0
    hardcoded_dp_count: 0       # 应该是 0（除 AeroSpacing 定义外）
    missing_terminal_states: [] # 如果适用，列出缺失的状态

  critic_history:
    cycles_to_pass: 0
    findings:
      BLOCKER: 0
      MAJOR: 0
      MINOR: 0
    common_issues: []

  lessons: ""
```

### 2.2 Screen Records (项目运行后填充)

```yaml
screen_records: []
```

## 3. ViewModel Patterns Evolution

### 3.1 Approved Patterns

```yaml
approved_patterns:
  - id: "VMP-001"
    name: "Standard Sealed UiState"
    code_pattern: |
      sealed class XxxUiState {
        object Loading : XxxUiState()
        object Empty : XxxUiState()
        data class Error(val message: String) : XxxUiState()
        @Immutable data class Success(val data: SomeData) : XxxUiState()
      }
    when_to_use: "默认所有屏"

  - id: "VMP-002"
    name: "Combined Cache + Realtime Flow"
    code_pattern: |
      combine(repository.observe(), realtimeClient.changes) { cache, _ ->
        // 实时更新触发后重新读缓存
      }
    when_to_use: "需要实时更新的屏（终端、任务）"

  - id: "VMP-003"
    name: "One-Shot Event via SharedFlow"
    code_pattern: |
      private val _events = MutableSharedFlow<XxxEvent>()
      val events: SharedFlow<XxxEvent> = _events.asSharedFlow()
    when_to_use: "导航、Toast、对话框"
```

### 3.2 Anti-Patterns

```yaml
anti_patterns:
  - id: "VMA-001"
    name: "Multiple StateFlows for One Screen"
    why_bad: "状态分散，难以保持一致"
    correct: "用 single sealed UiState"

  - id: "VMA-002"
    name: "LiveData usage"
    why_bad: "项目统一 StateFlow"
    correct: "StateFlow + asStateFlow()"

  - id: "VMA-003"
    name: "Side Effects in init {} without scope"
    why_bad: "无法取消"
    correct: "viewModelScope.launch { ... }"
```

## 4. Design Token Usage Tracking

### 4.1 Token Reference Map

```yaml
token_usage:
  description: "追踪各屏使用了哪些 token，便于维护"
  by_screen: {}   # 项目运行后填充

  most_used_colors: []     # 自动排序
  rarely_used_colors: []   # 提示可能是错误使用

  hardcoded_violations:
    description: "如果发生硬编码，记录在此"
    records: []
```

### 4.2 Component Library Tracking

```yaml
component_library:
  description: "本 Agent 创建的可复用 Composable"
  components:
    # - name: "TerminalCard"
    #   file: "ui/components/TerminalCard.kt"
    #   used_in: ["TerminalHubScreen", "ZoneDetailScreen", "BroadcastScreen"]
    #   states_supported: 7
```

## 5. ICD Consumption Log

```yaml
icd_consumption:
  # 每次消费某 ICD，记录使用的方法/字段，便于 ICD 变更时知道影响范围
  - icd: "ICD-TerminalDto-v1"
    consumed_in: []   # 项目运行后填充
    fields_used: []
    methods_called: []

  - icd: "ICD-AuthState-v1"
    consumed_in: []
    methods_called: ["serverAddressFlow", "jwtFlow", "clearLogin"]

  - icd: "ICD-DesignTokens-v1"
    consumed_in: []
```

## 6. Critic Feedback History

```yaml
critic_feedback:
  total_reviews: 0
  first_pass_rate: null

  pattern_occurrences:
    CMP-ERR-001: 0      # Excessive Recomposition
    CMP-ERR-003: 0      # Side Effect in Composition
    CMP-ERR-005: 0      # Hardcoded Design Token
    DSN-ERR-001: 0      # Hardcoded Color
    DSN-ERR-005: 0      # Missing Terminal State
    ARC-ERR-001: 0      # Layer Violation
    ARC-ERR-003: 0      # Cross-Domain Direct Call

  common_feedback_themes: []   # 自动从评审中提炼
```

## 7. Self-Improvement Notes

```yaml
self_improvement:
  current_focus_areas:
    - "提升一次通过率（目标 ≥ 75% by Phase 2）"
    - "减少硬编码偏差"
    - "5 态覆盖标准化"

  process_improvements:
    - "实现新屏前先列出 5 态草图"
    - "提交前 grep Color\\(0x 检查"
    - "复用现有组件优先于新建"

  technical_learnings:
    # 项目运行后填充
```

---

*Frontend-Business Agent Memory v1.0.0*
