# Vendor Inquiry — O-2 (WebSocket Protocol) + O-3 (x86_64 .so)

> 作者：Frontend-Platform Agent · 日期：2026-05-27 · 分支：`claude/v4-screens-on-refactor`
> 目的：把 Gate 1 OPEN 项 **O-2（WS 协议文档）** 与搭车的 **O-3（htapplib.aar x86_64 .so）** 写成可直接转发厂商/后端的问题清单。
> 流程：PM 汇总 → CTO 转厂商。回执到位后由 Frontend-Platform 把 `ICD-BroadcastWS-v1` 从 DRAFT 转 LIVE 并广播 ICD_UPDATE（解 R-002，喂 G2 WS 架构）。
> 依据：`icd-contracts.md` §6（ICD-BroadcastWS-v1 DRAFT 四项待验）、`Handoff.html`（§实时性 1130-1138 / §终端 806 / §广播 891）、`spike-aar64-report.md` §5（x86_64 缺失）、`gate1-review-package.md` §5（O-2/O-3 定义）。

---

## 0. 给厂商/后端的背景（一段话，请随问题一起转达）

我们正在为 AeroRadioControl v4（Android / 校园 LAN 内每校一台主机 / 明文 HTTP）重建实时通道。设计要求"终端状态与任务进度走 WebSocket 推送，断线回退 10s 轮询"。客户端侧已按**我们的预期格式**建了 DRAFT 契约（`ICD-BroadcastWS-v1`），但 WS 的真实**端点、心跳、鉴权、消息字段**四项均未经你方确认。以下问题逐项请你方给出权威答案；**若现网后端尚未实现 WS，请直接告知"暂不支持 WS"**——我们会据此走纯轮询回退，不会阻塞。问题分两组：A 组 = WS 协议（O-2，阻塞实时功能 + 架构评审 G2，优先级高）；B 组 = 模拟器原生库（O-3，开发期便利，非阻塞，P3）。

---

## A 组 · WebSocket 协议（O-2）— 优先级 HIGH，阻塞 P1 实时 + G2

> 每问附「我方当前 DRAFT 假设」，方便你方"确认/纠正"二选一作答，降低沟通往返。

### A-1 端点 URL（路径 / 端口 / scheme）

1. WS 端点的**完整 URL 模式**是什么？（含路径，如 `/ws`、`/websocket`、`/api/v1/ws`）
2. **端口**：与 REST 同端口复用，还是独立端口？（请给出具体端口号或"同 HTTP 端口"）
3. **scheme**：`ws://`（明文，与现状明文 HTTP 一致）还是 `wss://`（TLS）？现网部署是否有证书？
4. 是否**每校园主机一个 WS 端点**（与 REST baseUrl 同主机），客户端用登录时配置的 `serverAddress(host:port)` 拼接即可？

- 我方 DRAFT 假设：`ws://<serverHost>:<serverPort>/ws?token=<jwt>`，与 REST 同主机同端口，明文 `ws://`。

### A-2 鉴权方式

5. JWT 通过哪种方式带入？三选一并确认：
   - (a) **URL query**：`ws://host:port/ws?token=<jwt>`
   - (b) **HTTP header**：握手请求带 `Authorization: Bearer <jwt>`
   - (c) **首帧鉴权**：连接后客户端先发一条 `{"type":"auth","token":"<jwt>"}`，服务端校验后才推数据
6. 我方 JWT 24h 有效 / Refresh 30d。**JWT 过期时 WS 行为**是什么？服务端会主动 close（给哪个 close code / reason？）还是静默停推？客户端刷新 JWT 后是否必须**断开重连**携带新 token，还是可在连接内发送刷新帧？
7. 鉴权失败的**握手响应**：HTTP 401 还是建立后立即 close？close code 是多少？

- 我方 DRAFT 假设：query token（方案 a）；过期后服务端 close、客户端刷新 JWT 后重连。

### A-3 心跳 / ping-pong 格式

8. 心跳的**方向与发起方**：客户端发 ping、服务端回 pong，还是服务端主动推 ping？
9. 用**协议层 WebSocket ping/pong 帧**（控制帧）还是**应用层 JSON 消息**？若是 JSON，字段名和结构是什么？
10. **心跳间隔**与**超时判定**各是多少秒？（客户端多久没收到对端活动即判定断线）
11. 服务端是否对**客户端静默**有超时强制 close？阈值多少？

- 我方 DRAFT 假设：应用层 JSON `{"type":"ping","timestamp":<epochMillis>}`，客户端 15s 发一次，**30s 未收到对端活动判定断线**（Handoff §实时性定义 30s）。请确认 15s/30s 是否与服务端预期匹配，否则给出服务端期望值。

### A-4 消息字段名与结构

> 设计要求 WS 至少承载三类信息（依据 Handoff）：(i) 终端状态变化 state/playing/paging（§806、§1136）；(ii) 广播过程中**每终端 success/fail**（§891）；(iii) 任务进度，会话内持续推、结束后转 30s 轮询（§1137）。

12. **消息总体包络**：所有 WS 消息是否共用统一外层结构（如 `{"type": "...", "data": {...}, "timestamp": ...}`），还是各消息类型平铺字段？（这决定客户端 sealed-class 反序列化按 `type` 分发的方式）
13. **类型判别字段**：用哪个字段区分消息类型？字段名是 `type` 吗？各类型的字面值是什么（如 `terminal_state` / `task_progress` / `broadcast_result` / `ping`）？
14. **终端状态变化**消息：字段名与终端 ID / 状态枚举 / 播放信息 / 时间戳分别叫什么？状态枚举的**字面值**是什么？
    - 我方 DRAFT 终端状态枚举（`ICD-TerminalDto-v1`）：`online / offline / paging / talking / casting / urgent / alarm`。请确认你方 WS 推送用的状态字面值是否一致，特别是 Handoff §807 写的是 `online/offline/fault/playing/paging`——**`fault` vs `alarm`、`playing` vs `casting/urgent` 的映射请你方明确**（这是当前 DRAFT 里最大的字段歧义点）。
15. **广播 per-终端结果**消息（§891 success/fail per 终端）：字段名是什么？是按单个终端逐条推，还是一条消息带一个 results 数组？是否带失败原因 code/message？是否带 broadcast session/任务 ID 关联到发起的 `POST /broadcast/start`？
16. **任务进度**消息：字段名与 taskId / 进度值（0-100 整数？还是其他刻度）/ 状态字面值分别是什么？"会话结束"如何在 WS 上体现（终态 status 值？还是单独的 end 消息）？
17. **时间戳格式**：epoch 毫秒、epoch 秒，还是 ISO-8601 字符串？时区？
18. **初始快照**：客户端刚连上时，服务端是否会先推一份**全量终端状态快照**，还是只推后续增量（客户端需自己先 `GET /terminal/terminalinfo` 拉一次基线）？
19. 是否需要客户端**订阅**特定 topic/终端（发订阅帧），还是连上即收到全部推送？
20. 单条消息是否可能**批量**（一条 frame 带多个终端的状态变化数组）？

- 我方 DRAFT 字段假设（`icd-contracts.md` §6）：
  - 终端状态：`{type:"terminal_state", terminalId, state, playing:{mediaId,mediaName,startedAt,volume}, timestamp}`
  - 任务进度：`{type:"task_progress", taskId, progress:<int 0-100>, status}`
  - 心跳：`{type:"ping", timestamp}`
  - **广播 per-终端结果消息我方 DRAFT 尚缺**（Handoff §891 要求但 ICD 未定义）——请你方提供该消息格式，这是补缺项。

### A-5 连接生命周期 / 边界

21. 服务端是否限制**单账号并发 WS 连接数**？（同一账号在多设备/多页面登录时）
22. 服务端**主动断开**的常见 close code 及含义清单（鉴权过期 / 服务重启 / 心跳超时 / 顶号）？
23. 是否有**消息序列号或 ack 机制**？断线重连后能否补推断线期间漏掉的状态（resume），还是重连后需客户端自行 `GET` 拉全量对账？

- 我方重连策略（已定，无需你方确认，仅供你方了解客户端行为）：指数退避 2/4/8/16s，max 60s，单例连接，前台+网络可用时才重连，重连成功后回到 WS 主通道并停轮询。

---

## B 组 · htapplib.aar x86_64 原生库（O-3）— 优先级 P3，非阻塞

> 背景：spike 实扫（`spike-aar64-report.md` §5）确认当前 `htapplib.aar`（539KB，2026-05-14）的 `jni/` 仅含 `armeabi`、`armeabi-v7a`、`arm64-v8a`，**无 `x86` / `x86_64`**。arm64 真机语音/MP3 native 面已验证结构完整（R-001 已降级 LOW）；**唯一缺口是 x86_64 模拟器**——CTO 在 Android Studio 默认 x86_64 模拟器上跑语音/MP3 编码面会 `UnsatisfiedLinkError`（`loadLibrary("mp3lame")` / `loadLibrary("audioplay")` 找不到 x86_64 .so）。这只影响开发期模拟器走查，**不影响发布**（目标终端与 arm64 真机不受限；本项目校园 LAN 内部分发，不走 Google Play 64 位策略）。

B-1. 是否能提供一版 `htapplib.aar`，在 `jni/` 下补入 **`x86_64`** 的 `libaudioplay.so` + `libmp3lame.so`？（让 Android Studio x86_64 模拟器也能跑语音/MP3 编码面，便于开发期联调）

B-2. 若提供，请确保 x86_64 的两个 `.so` 与现有 arm64-v8a 版本**JNI 导出符号一致**——具体即 `libaudioplay.so` 需导出 `Java_com_example_htapplib_MediaCodec_Mp3Encode{Init,Buffer,BufferFLush,Release}` 四个 JNI 绑定，且 `libaudioplay.so` 对 `libmp3lame.so` 的 `lame_*` 依赖在 x86_64 版同样可解析（与 arm64 等价）。

B-3. （可选）`armeabi/`（旧 32 位）当前只有 `libmp3lame.so`、缺 `libaudioplay.so`——这是否为有意废弃？我方 abiFilters 现已锁 `armeabi-v7a + arm64-v8a`，不依赖 `armeabi`，仅确认你方意图。

---

## C. 回执处理预案（Frontend-Platform 侧，无需厂商作答，供 PM/CTO 参考）

| 回执情形 | 我方动作 |
|---------|---------|
| **A 组全部明确** | 按真实协议改 `ICD-BroadcastWS-v1` → 转 LIVE，广播 ICD_UPDATE（affected: frontend-business, data-integration）；RealtimeClient 按真协议实现（Phase 1） |
| **A 组部分明确** | 已明确项落地，未明确项保留 DRAFT 假设 + `// TODO(O-2)` 标记 + sealed unknown 兜底，待二次回执 |
| **后端暂不支持 WS** | RealtimeClient 直接进**纯轮询模式**（10s 终端 / 30s 任务结束 / 5s 详情页，依 Handoff §实时性），ConnectionBanner 常态显示"轮询中"；WS 通道留接口骨架，后端就绪后再启用。**不阻塞 P1。** |
| **A-4 状态枚举与我方不一致**（fault/playing 歧义） | 走 PM 协调与 data-integration 对齐 `ICD-TerminalDto-v1` 枚举映射（跨域，禁私下约定）；可能触发 TerminalDto 的 ICD_UPDATE |
| **B 组提供 x86_64 .so** | 转 Legacy-Native 验证符号 + 放入 AAR；abiFilters 增 x86_64（仅 debug 变体即可，release 不需要） |
| **B 组拒绝/无 x86_64** | 维持现状；开发期对讲/MP3 面在 arm64 真机走查，模拟器靠 `VoiceTalkAdapter.isAvailable()` 降级（spike §5 建议，已是预案） |

---

## D. 一页纸问题清单（给 CTO 直接转发用，去掉假设与背景）

**WS 协议（O-2，必答）**
1. WS 完整 URL 路径？端口（同 REST 还是独立）？`ws://` 还是 `wss://`？
2. JWT 鉴权方式：URL query / header / 首帧三选一？
3. JWT 过期时 WS 行为？刷新后是否需断开重连？鉴权失败 close code？
4. 心跳：方向？协议帧还是 JSON？间隔与超时秒数？
5. 消息是否有统一外层包络？用哪个字段（`type`?）判别类型？各类型字面值？
6. 终端状态消息字段名 + 状态枚举字面值？（`fault` 还是 `alarm`、`playing` 还是 `casting`？）
7. 广播 per-终端 success/fail 消息格式？（逐条还是数组、是否带失败原因、是否关联 broadcast session）
8. 任务进度消息字段名 + 进度刻度 + "会话结束"如何表示？
9. 时间戳格式（epoch ms / s / ISO-8601）？
10. 连上后是否先推全量快照？是否需订阅帧？单账号并发连接限制？断线重连能否 resume？
11. **若现网后端暂不支持 WS，请直接告知**——我方走纯轮询，不阻塞。

**原生库（O-3，可选 P3）**
12. 能否提供含 `x86_64` `.so` 的 `htapplib.aar`（与 arm64 JNI 符号一致），便于 x86_64 模拟器开发期联调？

---

*inquiry-O2-ws-protocol.md — Frontend-Platform Agent · 待 PM 汇总转 CTO → 厂商。回执触发 ICD-BroadcastWS-v1 DRAFT→LIVE + ICD_UPDATE。*
