# INQ-O-4 — 本机 IPC 守护进程（127.0.0.1:4521）命令协议问询

> 来源：TASK-AR-007（Legacy-Native）· 关联：UNK-002（4521 协议未文档化命令）· 日期：2026-05-27
> 用途：供 PM 汇总进对厂商的问询包（建议搭 O-1/O-2/O-3 厂商沟通车一并发出）。
> 状态：OPEN（待厂商回复 → 触发 ICD-IPCSocket-v1 的 ICD_UPDATE）

## 背景（给厂商的上下文）

AeroRadio v4 客户端历史上通过 **TCP socket 连接本机 `127.0.0.1:4521`** 的守护进程下发指令：旧实现 `utils/SocketClient.java` 在连接后**写入一行任意字符串命令**，再从同一连接**按行读取响应**（首行即返回）。该实现：
- 无类型化命令集——发什么字符串完全由调用方拼；
- 出错时回调 `###ShellRunError:<exception>`；
- 字符串内容疑似 **shell / `su` root 命令**（本机提权执行）。

v4 已将其重写为协程封装 `LocalSocketClient`（`ICD-IPCSocket-v1`），但**命令词表与响应格式无任何厂商文档**，当前只能以 `ShellCommand.Raw(cmd: String)` 透传兜底。为避免我方凭空发明协议，需厂商提供权威信息。

## 问题（请厂商逐条回答）

1. **命令词表**：127.0.0.1:4521 守护进程接受哪些命令？请给出**完整命令清单 + 每条的参数格式与语义**（例如是否有 paging/volume/play/status 之类的结构化命令，还是确实只接受原始 shell 字符串）。
2. **响应格式**：每条命令的响应是**单行**还是多行？是否有结束标记 / 状态码 / 错误前缀约定（除已知的 `###ShellRunError:`）？编码是否为 UTF-8？
3. **安全边界**：该通道是否真的是 root/shell 执行？是否有命令白名单或鉴权？v4 在最小权限原则下应如何限制可下发命令范围，避免本机提权面被滥用？
4. **连接语义**（次要）：是长连接复用还是每命令一连接？守护进程对并发连接 / 命令幂等性的预期？超时/保活约定？
5. **现状确认**：v4 现有功能中是否**仍需要**该 4521 通道？（我方实扫发现客户端当前已无任何 live 调用点——`SocketClient` 仅剩一行死 import；寻呼/对讲实际走 HTIntf native 路径。若此通道已废弃，请确认，我方将据此把 `LocalSocketClient` 标记为 deprecated 而非继续投入。）

## 我方默认假设（厂商未答前的 documented-assumption，与 AR-002 错误映射同款姿态）

- 命令模型：`ShellCommand.Raw(String)` 透传，复刻旧 `SocketClient` 行为 1:1；
- 响应模型：单行 UTF-8，`readLine()` 取首行；`###ShellRunError:` 视为错误；
- 安全：在拿到白名单前**不暴露任意命令下发 UI**，仅保留库内 API；
- 一旦 Q1/Q2 有答 → `ShellCommand` 增类型化子类（additive），`LocalSocketClient.sendCommand` 视需要支持多行响应 → 走 ICD-IPCSocket-v1 的 ICD_UPDATE（向后兼容，保留 `Raw`）。

---

*inquiry-O4-ipc4521.md — Legacy-Native Agent · 待 PM 汇总进对外问询包*
