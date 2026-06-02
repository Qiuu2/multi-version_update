# Gate 1 Review Package（方案 A 重排）— AeroRadio v4 · 只改 UI、协议零改动

> 作者：PM Agent · 日期：2026-05-28 · 触发：D-13 范围最终决策（方案 A）
> 流程：本包 → Critic 计划评审 → **CTO 重新 G1**（范围变，重新批一次）
> 方案 A 边界：后端零改 / `httptask/*Method.java` 一行不改全保留 / 协议·端点·字段·鉴权沿用 v3 / 不引入 WebSocket / 唯一新增网络代码 = callback→StateFlow 适配层（纯翻译）。

---

## R0. 修订记录（v2，回应 Critic 计划评审 PASSED_WITH_MINOR 的 accounting 校正）

| # | Critic finding | 本版修订 |
|---|----------------|---------|
| M-1 | Mapper 非"零改保留"——v3 返回 MachineInfo/*Model POJO 非 Gson DTO，类型绑定要重写 | §2 拆分：域模型+deriveStatus 逻辑**保留**；Mapper DTO 类型绑定**需对 v3 POJO 重写**(机械, @SerializedName↔MachineInfo 1:1, 低风险但非零工) |
| M-2 | 估算 18-22h 偏乐观 ~20-30%(漏 mapping/反序列化层) | §0/§3.3 上调至 **24-30h**；适配框架 6→8-10h、V3Repo 4→5-6h；明标"省一半多非四分之三" |
| I-1 | 适配样板 onRequestLister 签名错(实际双参+原始JSON) + 包RequestManger vs *Method 岔路 | §3.1 样板改双参签名 + 标设计岔路待实现钉死 |
| I-2 | v3 有同步 execute() 路径 | §3.1 标适配须兼容同步/异步两路防丢 emission |
| I-3 | 双层 state 判定(HTTP200但业务码失败)继承 | §5 R-A-NEW 点名此 v3 怪癖, 逻辑挪适配层 |
| — | 白做代码标 DORMANT | §1 白做的已 PASSED 代码标 **DORMANT/superseded-by-D13**(留作未来迁移资产, 防有人误接已作废新栈) |

> Critic 总评：白做列全(偏保守不隐瞒)；还能用方向对(UI/VM/契约/接口/域模型真保留)，唯 Mapper 过乐观已校正；估算上调后诚实。方案 A 省钱真实但不如初报满。

## 0. 给 CTO 的一页纸

- **核心好消息（seam 纪律的回报）**：因为团队把 UI/ViewModel 建在**接口/seam** 上、而非具体实现上，方案 A **白做集中在数据层 IMPL（可替换），UI/ViewModel/契约/基建全保留**。
- **白做 ≈ 6 项新栈 IMPL**（拦截器/AuthStore/单例OkHttp/Retrofit Repo impl/WS）——估算约 **40+ calibrated 小时**的实现工时。
- **还能用 ≈ 大头**：全部 Compose 屏 + ViewModel + 5 态机 + 设计 Token + 逆推 DTO 契约（**现在升值**：它们就是 v3 真值）+ 接口/域模型/Mapper + 语音/IPC/权限/基建。
- **新 Phase 0 估算 ≈ 24–30h**（远少于原 75h；Critic M-2 把初报 18-22h 上调 ~20-30%，因含 v3 POJO→domain mapping 重写层 + 反序列化处置）——主体是适配层 + 各 V3*Repository impl，UI/VM 大半已就绪。
- **O-1 后端问询作废**（方案 A 不跟后端谈协议）；**O-2 削到只剩 AAR/x86_64 真机部分**（WS 问询也随之作废）。
- **请重新 G1**：批方案 A 范围 + 新 WBS + 估算。

---

## 1. 白做清单（方案 A 作废 — 集中在新栈网络 IMPL）

| 原任务 | 白做的部分 | 为何作废 |
|--------|-----------|---------|
| AR-002 | DynamicBaseUrlInterceptor + AuthInterceptor（OkHttp 拦截器） | v3 RequestManger 自己处理 baseUrl + 鉴权头（Constant.header+ServerToken），无新 OkHttp |
| AR-002 | RetrofitLoginAuthenticator（Retrofit impl） | 登录走 v3 `/authorizations`；适配层包 v3 登录 callback |
| AR-003 | AuthStore 的加密存储 + 刷新去重 + TokenRefresher 那套 | v3 ServerToken/Constring 已存 token；无新拦截器→无 401 刷新去重需求 |
| AR-004 | 单例 OkHttp 统一 + 旧栈 RequestManger 复用 | 无新 OkHttp；v3 保留自己的 client（AR-004 当初让旧栈复用新单例——方向反转） |
| AR-101 | TerminalRepository**Impl**（Retrofit 实现） | 换成调 v3 ZoneMethod/*Method 的 V3 适配实现 |
| AR-103 | RealtimeClient（WebSocket）+ PollingFallback 的 WS 部分 | 方案 A 不引入 WS；实时用 v3 现有拿数据方式 |
| ICD | ICD-NetworkModule / BroadcastWS-v1 / RealtimeFallback DRAFT | 无新拦截器、无 WS |

> 估算口径：上述 IMPL 约 40+ calibrated 小时（AR-002 9h + AR-003 10h + AR-004 7.5h + AR-101 impl 部分 + AR-103 12h 主体）。**已 Critic-PASSED 的代码不删除、标 `DORMANT / superseded-by-D13`**（留作"若回头做新栈迁移"的资产；方案 A 是"现在不做迁移"非"永不迁移"）。**⚠ 标 DORMANT 是为防后续有人误接已作废的拦截器/AuthStore/Retrofit impl 当 active 新栈**（Critic 提醒）。
> Retrospective 记账（Critic 备注）：AR-002/003/004 当时 scope 下质量合格、Critic 已 PASS——现白做是 **CTO 范围反转(D-13)所致的 scope-change 损耗**，非 Critic escape、非实现缺陷。如实计入项目复盘的"范围变更损耗"。

## 2. 还能用清单（方案 A 全保留 — 占绝大多数）

| 资产 | 状态 | 方案 A 下的角色 |
|------|------|----------------|
| **逆推 DTO 字段定义**（Terminal/Zone/Task/Scheme/Tts/Media/ServerState + 4-int + 状态映射，AR-110/111/001） | **保留 + 升值** | **就是方案 A 适配层要解析的 v3 真值契约**（方案 A 用 v3，逆推的正是 v3 返回的字段）。从前"待后端验"现在"v3 跑通即事实"→ 可直接 LIVE |
| **TerminalRepository 接口 + 域模型(Terminal/Zone/TerminalStatus sealed+Unknown)**（AR-101） | **保留** | 只换 IMPL（Retrofit→V3 适配），接口/模型/上层 ViewModel **零变**（seam 回报，Critic VERIFIED） |
| **deriveStatus 派生逻辑**（AR-101 Mapper 的状态派生） | **保留** | 4-int→TerminalStatus 逻辑可复用 |
| ~~Mapper（整体）~~ → **DTO 类型绑定需重写**（M-1 Critic 校正） | **半保留** | ⚠ AR-101 Mapper 输入是 Gson `TerminalDto`(JSON形)；但 v3 ZoneMethod 返回**已反序列化的 `MachineInfo`/`ZoneModel` POJO**(JsonUtil.deSerializeString)——类型不匹配，需新写 `MachineInfo.toTerminal()` 绑定。**机械（@SerializedName 字段名与 MachineInfo 1:1，因逆推就源于它）、低风险，但非零工**。计入估算（M-2） |
| **全部 Compose 屏 + ViewModel + 5 态机**：LoginScreen(AR-005)/TerminalHub(AR-102)/ZoneDetail(AR-105)/广播目标(AR-106) | **保留** | 对接口编程，数据源换成 V3*Repository，**屏/VM 不动** |
| **设计 Token**（AeroTheme / ICD-DesignTokens-v1.1，AR-009） | **保留（核心）** | CTO 点名方案 A 重点工作 |
| **入口收敛**（AR-006） | 保留 | V4 单入口不变 |
| **权限编排**（AR-010） | 保留 | 协议无关 |
| **VoiceTalkAdapter + ICD-VoiceAAR**（AR-104/111） | 保留（核心） | 对讲 AAR + 真机，CTO 点名保留 |
| **LocalSocketClient + ICD-IPCSocket**（AR-007/111） | 保留 | IPC 4521 本机设备，协议无关 |
| **R-001 档案 + 真机清单**（AR-008） | 保留 | ABI/语音，协议无关 |
| **基建**：.gitattributes(AR-107) + 6 条团队标准 + daemon/CRLF 根因修复 | 保留 | 过程资产 |
| **LoginAuthenticator seam 接口**（fe 定义） | 保留 | impl 从 Retrofit 换成 V3 适配（一行 @Binds swap） |

> **关键**：seam/consumer-defined-interface 纪律让"换数据源"= 换 impl，**消费侧零改**。这是方案 A 没把 UI 工作一起白做的根本原因。

## 3. 方案 A 新 Phase 0 WBS

### 3.1 callback→StateFlow 适配层（唯一新网络代码 · 样板）

```kotlin
// data/v3bridge/V3CallAdapter.kt — 把 v3 RequestManger 一次性 callback 包成 Flow（纯翻译，不改 v3）
// ⚠ I-1: v3 onRequestLister 实际是双参 onSucess(code:Int, json:String)/onFailed(code:Int, msg:String)，成功给原始 JSON
inline fun <T> v3OneShot(
    crossinline call: (onRequestLister) -> Unit,      // 调 v3，一行不改 v3
    crossinline parse: (code: Int, json: String) -> T,// 复用逆推 DTO + 重写的 MachineInfo→domain 绑定(M-1)
): Flow<Result<T>> = callbackFlow {
    val lister = object : onRequestLister {
        override fun onSucess(code: Int, json: String) { trySend(runCatching { parse(code, json) }); close() }
        override fun onFailed(code: Int, msg: String)  { trySend(Result.failure(V3Error(code, msg))); close() }
    }
    call(lister)
    awaitClose { /* v3 无取消则空 */ }
}
// ⚠ I-1 设计岔路(实现任务钉死)：包 RequestManger(拿原始 JSON 自反序列化, 重复 *Method 的活) vs 包 *Method(拿 typed POJO 如 MachineInfo, 但各 *Method 回调形态不一)。
// ⚠ I-2: v3 既有 enqueue(异步,callbackFlow 安全) 也有 execute() 同步(:89)——适配须兼容两路, 防同步 callback 在收集前 fire 丢 emission。

// 复用 AR-101 的接口/模型/Mapper，只换 impl：
class V3TerminalRepository @Inject constructor(...) : TerminalRepository {  // 接口=AR-101，不变
    private val _zones = MutableStateFlow<List<Zone>>(emptyList())
    override fun observeZones() = _zones.asStateFlow()                       // ViewModel 消费不变
    override suspend fun refresh(): Result<Unit> =
        v3OneShot({ ZoneMethod.getZones(it) }, ::mapV3ZonesToDomain)         // 复用 AR-101 Mapper
            .first().onSuccess { _zones.value = it }.map { }
}
// 实时刷新：无 WS → 进入屏时 refresh() + 可选 N 秒轮询(复用 AR-103 的 ExponentialBackoff 纯工具类做退避)
```

**性质**：纯翻译层，不改 v3 一行，不碰协议。`@Binds` 把 `TerminalRepository`→`V3TerminalRepository`（替代原 Retrofit impl）。ViewModel（AR-102/105/106）**零改动**。

### 3.2 5 Tab 实现顺序（fe-business 主力）

| 序 | Tab | 工作 | 状态 |
|----|-----|------|------|
| 1 | **终端** | UI/VM 已建(AR-102/105/106)；data 出 V3TerminalRepository(适配 ZoneMethod/*Method) → @Binds swap | UI/VM **近完成**，只差 V3 impl |
| 2 | **任务** | 任务时间轴/作息/编辑/临时文件 UI+VM + V3TaskRepository(适配 TaskMainMethod) | DTO 逆推已就绪(AR-110)，UI 待建 |
| 3 | **服务** | 系统健康度 UI+VM + V3ServerStateRepository(适配 /server/serverstate) | 小，DTO 就绪 |
| 4 | **广播** | 三档：寻呼/点播走 v3 + **对讲走 AAR(VoiceTalkAdapter,AR-104 已建)** + 目标选择(AR-106 已建) | 模式逻辑待建，目标选择+对讲底子已有 |
| 5 | **AI** | Phase 3 / NLU-gated | 降级，本期不做 |

### 3.3 工作量重估（远少于原 75h）

> **估算口径（M-2 Critic 校正：原 18-22h 偏乐观 ~20-30%，已上调；漏了 v3 POJO→domain mapping 层 + 反序列化抉择）**

| 工作包 | 估算(校正后) | 说明 |
|--------|------|------|
| WP-A1 callback→Flow 适配框架 | **8–10h**（原 6h） | V3CallAdapter + 错误模型 + **同步/异步两路兼容(I-2)** + **v3 POJO→domain mapping 范式(M-1)** |
| WP-A2 V3*Repository 实现(终端/任务/服务) | **3×5–6h=15–18h**（原 12h） | 每个调 v3 *Method + **重写 MachineInfo/*Model→domain 类型绑定(M-1)** + 处理双层 state(I-3) |
| WP-A3 任务/服务 Tab UI+VM | （Phase 1 主体，分批） | 终端已完成；任务/服务复用终端模式 |
| WP-A4 设计 Token 收尾(AR-009) | 已 COMPLETED | — |
| WP-A5 对讲 AAR + 真机(AR-104+R-001) | 保留 | 协议无关 |
| **Phase 0(方案 A)核心** = WP-A1 + WP-A2 终端部分 | **≈ 24–30h**（原报 18-22h，M-2 上调） | 适配框架(含 mapping 范式+同步异步) + 终端 V3Repository 接通(含类型绑定重写)；任务/服务/广播 Tab 进 Phase 1 |

> 对比原 Phase 0 ~75h：方案 A 仍**显著省**（~24-30h vs 75h，省掉拦截器/AuthStore/单例/迁移/WS），但**不是 18h 那么满**——含已知的 v3 POJO→domain mapping 层 + 反序列化处置（Critic M-2）。诚实口径：省一半多，非省四分之三。

## 4. 问询包处置（CTO 指示）

- **O-1 后端契约问询 → 作废** ✅：方案 A 用 v3、不跟后端谈协议。原 O-1 的 5 问 + D-1(refresh)/D-2(form)/D-3(state 派生)/Q2(包络)/Q3(成功判定) **全部 moot**——v3 RequestManger 已在跑、已处理这些，适配层只包 v3 的成功/失败 callback，无需抽象知道契约。逆推的 DTO 字段从"待 O-1 验"升为"v3 跑通即 LIVE"。
- **O-2 厂商问询 → 削减保留**：
  - **保留**：x86_64 .so（O-3，AAR 模拟器开发体验）+ AAR 真机相关 + 4521 命令词表（O-4，IPC 若用）——均**协议无关**。
  - **作废**：O-2 的 **WS 协议 23 问**（方案 A 无 WS）。
- **真机清单（R-001）保留**：对讲 AAR 真机终验照旧。

## 5. 风险变化

| 风险 | 方案 A 下 |
|------|----------|
| R-002 WS 文档 / R-003 端点契约 / R-WS-NEW | **作废**（无 WS、不谈协议） |
| R-001 32位ABI | **不变**（AAR/语音保留，真机待验） |
| **R-A-NEW（方案 A 新增）** | v4 是"v3 数据层上换皮"：继承 v3 局限——**无实时推送**(靠轮询，延迟/体验弱于 v4 设计稿的实时预期)、**旧栈不退役**(httptask/*Method.java 永久保留=技术债不还)、**双层 state 判定继承(I-3 Critic)**：v3 `HTTP200 但 data[0].state` 业务子码(如 "15"=任务名重复)的"传输成功≠业务成功"语义，方案 A 照单继承——原 AR-002 ResponseSuccessPolicy 本为抽象它(现作废)，该逻辑挪到适配层/各 V3Repo 自己处理(实现时必须点名, 否则漏判业务失败)。**CTO 用"快交付+低风险"换"留技术债"的明确取舍，PM 记录不评判** |

## 6. Go / No-Go（重新 G1）

请 CTO 给 **APPROVE / REQUEST_CHANGES / REJECT**：
1. 方案 A 范围边界（§0）
2. 白做/还能用 accounting（§1/§2）—— 确认白做范围、salvage 判断
3. callback→Flow 适配层方案（§3.1 样板）
4. 5 Tab 顺序 + 重估 **~24–30h**（§3.2/3.3，M-2 校正后口径）
5. O-1 作废 / O-2 削减 / R-A-NEW 风险接受

> APPROVE 后：data 转 适配框架+V3*Repository（替代新栈），fe 续 5 Tab，G2 架构需**重新基线**（原 G2 批的拦截器/AuthStore/WS 方案已被 D-13 supersede——新架构=v3数据层+适配层+Compose UI，简单很多，可并入本次 G1 一起批或 Phase0 末轻量 G2）。

---

*gate1-planA-review-package.md — Generated by PM Agent · 待 Critic 计划评审 → CTO 重新 G1*
