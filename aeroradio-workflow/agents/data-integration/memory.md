---
name: data-integration-memory
description: Data-Integration Agent 的记忆配置。
version: 1.0.0
---

# Data-Integration Agent — Memory

## 1. Memory Architecture

```
Data-Integration Memory
├── REST Endpoint Coverage     # 21 个端点的接入状态
├── Legacy Migration Log       # 旧栈迁移进度
├── DTO ↔ Backend Mapping      # DTO 字段映射记录
├── ICD Production Log         # ICD 产出记录
├── AuthStore Edge Cases       # 鉴权边界场景
└── Critic Feedback History    # 评审反馈
```

## 2. REST Endpoint Coverage

> 21 个端点（基于 `constant/Constant.java`），项目运行时更新各端点的接入状态。

```yaml
endpoint_coverage:
  total: 21
  implemented: 0
  in_progress: 0
  pending: 21

  endpoints:
    - path: "/authorizations"
      method: "POST"
      status: "PENDING"
      api_service: "AuthApi"
      dto_in: "LoginRequest"
      dto_out: "LoginResponse"

    - path: "/authorizations/refresh"
      method: "POST"
      status: "PENDING"
      api_service: "AuthApi"

    - path: "/terminal/terminalinfo"
      method: "GET"
      status: "PENDING"
      api_service: "TerminalApi"
      dto_out: "List<TerminalDto>"

    - path: "/terminal/zoneterminal"
      method: "GET"
      status: "PENDING"
      api_service: "TerminalApi"
      dto_out: "List<ZoneDto>"

    - path: "/terminal/terzone"
      method: "GET"
      status: "PENDING"

    - path: "/terminal/urgentplay"
      method: "POST"
      status: "PENDING"

    # ... 其余 15 个端点（待项目启动后从 Constant.java 完整列出）

    - path: "/server/serverstate"
      method: "GET"
      status: "PENDING"
      api_service: "ServerApi"
      dto_out: "ServerStateDto"
```

## 3. Legacy Migration Log

```yaml
legacy_migration:
  total_legacy_files: null    # 项目启动后扫描 httptask/*Method.java 计数
  migrated: 0
  in_dual_run: 0
  pending: null

  migration_records: []

  forbidden_violations:
    description: "记录任何违反迁移规则的提交"
    records: []
```

## 4. DTO ↔ Backend Mapping Discoveries

```yaml
dto_backend_mapping:
  description: "记录实际对接中发现的字段差异和处理方式"
  discoveries: []

  # 示例：
  # - dto: "TerminalDto"
  #   field: "ipAddress"
  #   backend_field: "ip_addr"
  #   mapping: "@SerialName(\"ip_addr\") val ipAddress: String"
  #   discovered_at: "ISO8601"
  #   discovered_by: "TASK-AR-XXX"

  unresolved_questions:
    # 例如：
    # - "lastSeenAt 是 epoch millis 还是 seconds？"
```

## 5. ICD Production Log

```yaml
icd_produced:
  - icd: "ICD-NetworkModule-v1"
    status: "LIVE"
    consumers: ["frontend-business", "frontend-platform", "legacy-native"]

  - icd: "ICD-AuthState-v1"
    status: "LIVE"
    consumers: ["frontend-business", "frontend-platform"]

  - icd: "ICD-TerminalDto-v1"
    status: "LIVE"
    consumers: ["frontend-business"]

  - icd: "ICD-ZoneDto-v1"
    status: "LIVE"
    consumers: ["frontend-business"]

  - icd: "ICD-TaskDto-v1"
    status: "LIVE"
    consumers: ["frontend-business"]

  breaking_change_log: []
```

## 6. AuthStore Edge Cases

```yaml
auth_edge_cases:
  scenarios_tested:
    - scenario: "首次启动（无任何凭据）"
      expected: "全 flow 返回 null"
      tested: false

    - scenario: "JWT 过期 + Refresh 有效"
      expected: "AuthInterceptor 自动刷新 + 重试"
      tested: false

    - scenario: "JWT 过期 + Refresh 也过期"
      expected: "清除凭据 + 跳登录页"
      tested: false

    - scenario: "并发 10 个请求同时收到 401"
      expected: "只刷新一次（Mutex）"
      tested: false

    - scenario: "serverAddress 未配置时请求"
      expected: "抛 ServerAddressNotConfiguredException + UI 跳登录"
      tested: false

    - scenario: "切换 serverAddress 后旧 JWT 失效"
      expected: "登出 + 跳登录"
      tested: false
```

## 7. Critic Feedback History

```yaml
critic_feedback:
  total_reviews: 0
  first_pass_rate: null

  pattern_occurrences:
    NET-ERR-001: 0    # Dynamic baseUrl Misconfig
    NET-ERR-002: 0    # JWT in Insecure Storage
    NET-ERR-004: 0    # Token Refresh Storm
    NET-ERR-005: 0    # Old/New Stack Coexistence
    NET-ERR-006: 0    # DTO Field Mismatch
    KT-ERR-005: 0     # Exception Swallowed
    KT-ERR-006: 0     # Mutex Missing
```

## 8. Self-Improvement Notes

```yaml
focus_areas:
  - "动态 baseUrl 拦截器并发场景"
  - "AuthStore 边界条件全覆盖"
  - "DTO 字段与实际响应对齐"
  - "旧栈整模块迁移纪律"

process_improvements:
  - "每接入新 endpoint 前先抓真实响应 sample"
  - "迁移 module 时 24h dual-run 强制执行"
  - "DTO 添加单元测试反序列化 sample 响应"
```

---

*Data-Integration Agent Memory v1.0.0*
