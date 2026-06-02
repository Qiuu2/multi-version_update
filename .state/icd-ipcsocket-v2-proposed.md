# ICD-IPCSocket-v2 — Proposed Update (源标注 + LIVE/DRAFT 分类)

> TASK-AR-111 · Legacy-Native · 2026-05-28 · CTO D-12 ICD 二分法落地
> 用途：供 PM 串行写入 `aeroradio-workflow/references/icd-contracts.md §7`（STD-ICD-WRITE，我不抢写）。
> 原则：可逆推（标 v3 源行号）→ LIVE；无确定值可逆推/待外部 → DRAFT。

---

## 总分类裁定

| 子面 | 分类 | 一句话理由 |
|------|:----:|-----------|
| 连接语义（地址/端口/帧格式/超时/错误前缀） | **LIVE** | 全部逆推自 `utils/SocketClient.java` 行号，code-fact |
| Kotlin 协程封装（LocalSocketClient 接口/状态机/重连） | **LIVE** | AR-007 已落地，Critic PASSED HIGH（10 测绿，含重连无死锁） |
| 命令词表（ShellCommand 具体命令） | **DRAFT-pending-vendor** | 旧栈发**任意 shell 字符串**，无确定词表可逆推；待 O-4 厂商回执（INQ-O-4） |

---

## A. 连接语义 — **LIVE**（逆推自 utils/SocketClient.java）

> 来源：`com.htgd.radiocontrol.aeroradiocontrol.utils.SocketClient`（v3，行号如下）。

| 字段/行为 | 值 | SocketClient.java 源行号 |
|-----------|----|------|
| 地址 | `127.0.0.1`（本机） | :17 `HOST = "127.0.0.1"` |
| 端口 | `4521` | :22 `int port = 4521` |
| 传输 | TCP `Socket` + `InetSocketAddress` | :10, :32 `socket.connect(new InetSocketAddress(HOST, port), 3000)` |
| 连接超时 | 3000ms | :32（连接超时参数） |
| 读超时（SO_TIMEOUT） | 3000ms | :34 `socket.setSoTimeout(3000)` |
| 发送帧 | 行式：`PrintWriter.println(cmd)`（追加换行 + flush） | :38, :89 `printWriter.println(cmd)` |
| 接收帧 | 行式：`BufferedReader.readLine()` 循环逐行回调 | :41, :69-70 `while ((line = reader.readLine()) != null) ... getSend(line)` |
| 编码 | 默认平台编码（v3 未显式指定；v4 LocalSocketClient 显式 UTF-8——见 §B 偏离说明） | :38/:41（InputStreamReader/OutputStreamWriter 无 charset 参数） |
| 错误回执前缀 | `###ShellRunError:` | :47 `mOnServiceSend.getSend("###ShellRunError:" + e)` |
| 回调契约 | `interface onServiceSend { void getSend(String) }` | :94-95 |

**裁定 LIVE 理由**：每一项都有确定的 v3 源行号，无推测。

> **v4 偏离备注（诚实标注，供 Critic 核）**：v3 SocketClient 用平台默认编码（:38/:41 的 InputStreamReader/PrintWriter 无 charset）；AR-007 LocalSocketClient 显式用 **UTF-8**（`SocketConnection.kt`）。这是 v4 主动收紧（默认编码不可靠），非逆推矛盾——属合理现代化。若厂商守护进程实际非 UTF-8，O-4 回执后再校正（走 ICD_UPDATE）。

---

## B. Kotlin 协程封装 — **LIVE**（AR-007 落地，Critic PASSED HIGH）

> 来源：`data/ipc/LocalSocketClient.kt` + `LocalSocketClientImpl.kt` + `SocketConnection.kt`（AR-007，Critic PASSED HIGH，10 单测绿）。

```kotlin
interface LocalSocketClient {
    val connectionStateFlow: StateFlow<ConnectionState>   // 名已对齐注册表
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun sendCommand(command: ShellCommand): Result<String>
}
enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }
```
- 物理参数（继承自 §A，v4 微调）：5s 连接超时（v4 放宽自 v3 3s）+ 3s 读超时（= v3 SO_TIMEOUT）；有界自动重连 3 次退避。
- 实现：Mutex 串行化 + Dispatchers.IO + Result<T>；SocketConnection seam 可纯 JVM 单测。

**裁定 LIVE 理由**：已落地、已过 Critic、有测试佐证。

---

## C. 命令词表（ShellCommand）— **DRAFT-pending-vendor**（无源可逆推）

> 来源（反证）：v3 `SocketClient` 构造器接受**任意 shell 命令字符串** `SocketClient(String commod, ...)`（:24），`send()` 原样 `println(cmd)`（:89）。**v3 无任何类型化命令枚举/常量**——发什么字符串由调用方拼，疑 su/root shell。故**无确定命令词表可逆推**。

```kotlin
sealed class ShellCommand(val raw: String) {
    data class Raw(val command: String) : ShellCommand(command)   // 透传，1:1 复刻 v3
    // 类型化命令待 INQ-O-4 厂商回执后 additive 扩展（保留 Raw 向后兼容）
}
```

**裁定 DRAFT 理由**：这是 D-12 的「需验证类」——无源、待外部（厂商）。已出 **INQ-O-4**（`.state/inquiry-O4-ipc4521.md`，5 问：命令词表/响应格式/安全边界/连接语义/现状确认）。回执后 Raw→类型化，走 additive ICD_UPDATE。
**注**：寻呼/对讲**不经此 socket**（走 HTIntf native，SPIKE-AAR64 证），故命令词表的 DRAFT 不阻塞语音；且 v4 当前无 live 消费者（旧 SocketClient 已孤儿），LocalSocketClient 作前瞻基建。

---

## 提议的注册表清单行（§1 表）

`| ICD-IPCSocket-v2 | 本机 127.0.0.1:4521 TCP（连接语义 + 协程封装）| legacy-native | frontend-business | LIVE（连接语义+协程封装）/ DRAFT-pending-vendor（命令词表 O-4）|`

---

*icd-ipcsocket-v2-proposed.md — Legacy-Native · TASK-AR-111 · 待 Critic 核 + PM 串写注册表*
