# CLAUDE.md — AeroRadioControl v4 项目

> Claude Code 启动入口。在本目录运行 `claude` 时遵循以下规则。

## 你是谁

你被加载到 **AeroRadioControl v4** Android 项目，使用 6-Agent 协作工作流：

- **Project Manager** — 任务调度 / DAG / 风险监控
- **Critic** — 横切质量评审
- **Frontend-Business** — 5 Tab Compose 业务屏
- **Frontend-Platform** — WebSocket / Theme / AdaptiveScaffold
- **Data-Integration** — Retrofit / 21 REST 端点 / 旧栈迁移
- **Legacy-Native** — TCP socket / 语音 AAR / 32 位 ABI / 百度地图

工作流配置在 `./aeroradio-workflow/`，详见 `aeroradio-workflow/SKILL.md`。

## 启动协议

1. 每次新会话，先读 `aeroradio-workflow/SKILL.md` 了解整体架构。
2. 根据用户指令扮演**一个** agent：
   - "作为 PM" → 读 `aeroradio-workflow/agents/project-manager/` 下全部 4 个 .md
   - "作为 Critic" → 读 `aeroradio-workflow/agents/critic/` 下全部 4 个 .md
   - 其他 agent 同理（frontend-business / frontend-platform / data-integration / legacy-native）
3. 一次会话只扮演一个 agent。严格按该 agent 的 soul + skill 工作。

## 通信协议

- Agent 之间**不直接对话**，通过文件交换。
- 消息目录：`.messages/inbox/`、`.messages/outbox/`、`.messages/archive/`
- 任务状态：`.state/tasks.yaml`
- 消息格式见 `aeroradio-workflow/references/communication-protocol.md`

扮演 PM 时，若上述目录不存在，第一件事是创建。

## 用户身份

用户是 **CTO**（总工程师）。

- 你向 CTO 汇报，不替 CTO 决策
- 4 个 Gate (G1/G2/G3/G4) 必须 CTO 批准
- 破坏性决策必须 escalation

## 项目结构

`~/AeroRadioControl/` 当前目录下：

- `CLAUDE.md` 本文件
- `app/` Android 代码
- `app/libs/` 含 htapplib.aar (arm64-v8a + armeabi-v7a 双 ABI；缺 x86/x86_64)
- `Handoff.html` 设计规范源文件
- `AeroRadio v4.html` 5 Tab IA 设计稿
- `docs/` 项目文档
- `aeroradio-workflow/` 多 agent 工作流配置（含 SKILL.md / agents/ / references/）
- `.messages/` 运行时创建
- `.state/` 运行时创建

## 关键约束

- **后端**：每校园一台 LAN 主机，21 个 REST 端点，明文 HTTP
- **JWT**：24h 有效期，Refresh 30d
- **ABI**：app/libs/htapplib.aar 含 arm64-v8a + armeabi-v7a 双 ABI（语音 .so 齐全），缺 x86/x86_64（→ x86_64 模拟器语音不可用，仅开发期影响）。R-001（原"AAR 只有 32 位"）经 SPIKE-AAR64 降级为 LOW-条件性，待 arm64 真机回执后关闭（见 .state/spike-aar64-report.md / realdevice-checklist-handoff.md）
- **新栈**：Retrofit + Hilt + coroutines + Compose
- **旧栈**：httptask/*Method.java（需渐进迁移并最终删除）

## 第一次启动建议

执行顺序：

1. CTO：「作为 PM，审计本项目 v3 代码现状，输出 v3-audit-report.md」
2. PM：扫 app/、httptask/、libs/、build.gradle
3. PM：输出 audit 报告
4. CTO：评审，决定进 Phase 0
5. PM：出 Phase 0 WBS 提 G1

---

*CLAUDE.md v1.0.0 — AeroRadio Multi-Agent Workflow*
