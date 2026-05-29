# 对外问询包（待 CTO 转发）— AeroRadio v4 Phase 1 外部前置

> 维护者：PM Agent · 2026-05-27 · 来源：D-09 授权（O-1/O-2/O-3）
> 用途：汇总 data-integration（后端）+ fe-platform（厂商）的问询清单，供 CTO 转发。**状态：待 Critic 完整性快审 → CTO 转发。**

## 路由

| 清单 | 收件人 | 文件 | 解什么 |
|------|--------|------|--------|
| **O-1 后端契约 5 问 + 衍生 D-1/D-2** | **后端团队** | `.state/inquiry-O1-backend-contract.md` | AR-002 错误映射硬阻塞、Phase 1 迁移 DTO 契约、auth 刷新机制 |
| **O-2 WS 协议 23 问** | **厂商** | `.state/inquiry-O2-ws-protocol.md` §A | R-002（WS DRAFT）、Phase 1 实时、G2 WS 架构 |
| **O-3 x86_64 .so（搭车）** | **厂商** | `.state/inquiry-O2-ws-protocol.md` §B | CTO 的 AS 模拟器语音开发体验（P3） |
| **O-4 4521 守护进程命令词表（5 问）** | **厂商** | `.state/inquiry-O4-ipc4521.md` | UNK-002：现 SocketClient 发任意 shell 字符串疑 su/root，请厂商给权威命令清单 + 安全边界（喂 AR-007 ShellCommand） |

> 两份清单各附「我方 DRAFT 假设 + 厂商/后端确认或纠正二选一」，并各有一页纸纯问题版（去假设）便于直接转发。

## ⚠ 两个回执后可能触发 CTO 决策 / ICD_UPDATE 的实质点（请您在转发时知悉）

**ESC-WATCH-1 ｜ auth 刷新机制可能逼迫持久化密码（O-1 衍生 D-1，潜在架构级 escalation）**
- data-integration 实读 v3：旧栈**无 `/authorizations/refresh` 端点**，"刷新"靠存账号密码重新登录；但登录响应 `TokenModel` 含 `refresh_expired_at` 字段（暗示服务端本应有 refresh）。
- **若后端确认无 refresh 端点** → AuthStore 要么持久化密码（违反"密码永不持久化"原则，安全债），要么改短会话+到期重登 UX。
- PM 处置：AR-003 先按**安全假设**推进（密码不落盘 + 刷新抽象为可切换实现 + impl 留 TODO），**不**默认存密码。回执若为"无 refresh 端点"，我将带 3 个选项升级给您：(A) 请厂商/后端暴露 refresh 端点（TokenModel 已有字段，成本可能低）；(B) 加密持久化密码（EncryptedSharedPreferences，接受可控安全债）；(C) 短会话+到期重登。**现在不需您决策，先把问题问到。**

**ESC-WATCH-2 ｜ 终端状态枚举跨域不一致（O-2 A-4，回执后触发 ICD-TerminalDto-v1 ICD_UPDATE）**
- Handoff §807 终端状态字面值 = `online/offline/fault/playing/paging`；data-integration 的 ICD-TerminalDto-v1 枚举 = `online/offline/paging/talking/casting/urgent/alarm`。`fault↔alarm`、`playing↔casting/urgent` 对不上。
- PM 处置：O-2 已请厂商钉死 WS 实际字面值作为**权威真值**；回执后由 PM 协调 fe-platform + data-integration 对齐枚举映射并发 ICD_UPDATE。Phase 1 才落地，**不阻塞当前**，先记。

## 回执后动作（PM 跟踪）
- O-1 回执 → AR-001 出 ICD-Endpoints v2 增量；AR-002 错误映射切到真实约定；D-1 据答决定是否 escalate。
- O-2 回执 → ICD-BroadcastWS-v1 DRAFT→LIVE + ICD_UPDATE；A-4 枚举对齐。若厂商不支持 WS → 走纯轮询（fe-platform 已有预案），不阻塞 P1。
