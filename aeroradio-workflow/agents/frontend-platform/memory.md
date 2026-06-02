---
name: frontend-platform-memory
description: Frontend-Platform Agent 的记忆配置。
version: 1.0.0
---

# Frontend-Platform Agent — Memory

## 1. Memory Architecture

```
Frontend-Platform Memory
├── ICD Production Log         # 产出的 ICD 历史
├── WS Stability Metrics       # WebSocket 稳定性指标
├── Token Implementation Log   # 设计 token 落地历史
├── Adaptive Layout Tests      # 平板适配测试记录
└── Critic Feedback History    # Critic 反馈历史
```

## 2. ICD Production Log

```yaml
icd_produced:
  - icd: "ICD-DesignTokens-v1"
    status: "LIVE"
    consumers: ["frontend-business"]
    last_updated: "TBD"

  - icd: "ICD-BroadcastWS-v1"
    status: "DRAFT"   # 待后端对接验证
    consumers: ["frontend-business", "data-integration"]
    pending_verification:
      - "WS endpoint URL"
      - "心跳间隔"
      - "鉴权方式"
      - "消息字段名"

  - icd: "ICD-RealtimeFallback-v1"
    status: "DRAFT"
    consumers: ["frontend-business"]
```

## 3. WS Stability Metrics (项目运行后填充)

```yaml
ws_stability:
  successful_connect_rate: null
  reconnect_success_rate: null
  avg_reconnect_attempts: null
  fallback_to_polling_count: null

  weekly_metrics: []
```

## 4. Token Implementation Log

```yaml
token_implementation:
  total_tokens_implemented: 0
  tokens_by_category:
    colors: 0
    shapes: 0
    spacing: 0
    typography: 0
    elevation: 0
    motion: 0

  variance_from_spec: []   # 如果 token 落地有任何偏差，记录原因
```

## 5. Adaptive Layout Tests

```yaml
adaptive_layout_tests:
  tested_devices: []
  test_matrix:
    - device: "Pixel 6"
      width_dp: 393
      class: "COMPACT"
      status: null

    - device: "Pixel Fold (folded)"
      width_dp: 380
      class: "COMPACT"
      status: null

    - device: "Pixel Fold (unfolded)"
      width_dp: 700
      class: "MEDIUM"
      status: null

    - device: "Galaxy Tab S9 (Portrait)"
      width_dp: 800
      class: "EXPANDED"
      status: null

    - device: "Galaxy Tab S9 (Landscape)"
      width_dp: 1280
      class: "EXPANDED"
      status: null
```

## 6. Critic Feedback History

```yaml
critic_feedback:
  total_reviews: 0
  first_pass_rate: null

  pattern_occurrences:
    KT-ERR-001: 0      # GlobalScope
    KT-ERR-002: 0      # Unstructured Concurrency
    KT-ERR-006: 0      # Mutex Missing
    DSN-ERR-007: 0     # Wrong Motion Duration
```

## 7. Self-Improvement Notes

```yaml
focus_areas:
  - "WS 重连边界场景覆盖（弱网、闪断、长尾）"
  - "AdaptiveScaffold API 简洁性"
  - "Token 落地与 spec 100% 对齐"

process_improvements:
  - "每个 token 落地前 grep design-system-spec.md 找完整定义"
  - "WS 测试时使用 chuck/charles 模拟网络问题"
  - "AdaptiveScaffold 用 Compose Preview Multi-Device 全覆盖"
```

---

*Frontend-Platform Agent Memory v1.0.0*
