# Plan A — v3 callback→Flow 适配层设计（DRAFT 预研 · 未提交 · 待 G1）

> Producer: Data-Integration · 日期: 2026-05-28 · 状态: **DRAFT 设计预研，PM 授权草稿不提交**（同 TaskRepository 规矩）
> 前提: CTO D-13 方案 A（UI-only，复用 v3 网络栈，无新 Retrofit/WS）。本文是 G1 批准后正式落地的蓝图，**现不写落盘代码、不动共享树**。
> 复用资产: TerminalRepository 接口 / 域模型(Terminal/Zone/TerminalStatus) / TerminalMapper / 逆推 DTO —— 全保留，只换 IMPL。

---

## 0. 核心思路

方案 A 下，数据来源 = v3 `RequestManger`（OkHttp callback，一次性回调，返回原始 JSON 串）。我把它适配成现有的 `TerminalRepository`（Flow + suspend）接口，**接口/模型/Mapper/上层 ViewModel 零改**，只替换 `*RepositoryImpl`。

链路：`ViewModel → TerminalRepository(现有接口) → V3TerminalRepository(新 IMPL) → CallbackAdapter → RequestManger.get/postHashMap(v3) → onRequestLister 回调 → Gson 解析(现有 DTO) → Mapper(现有) → 域模型 → 内存 SSOT(MutableStateFlow) → Flow 重发`

---

## 1. CallbackAdapter — v3 callback → suspend 的桥

v3 回调契约（实读）：`onRequestLister { onSucess(int code, String response); onFailed(int code, String message) }`（onRequestLister.java:6-9）；`RequestManger.get(url, lister)` / `postHashMap(MyRequestBuilder, lister)`（一次性回调，response 是原始 JSON 串）。

```kotlin
// data/adapter/V3CallbackAdapter.kt （草图）
suspend fun v3Get(url: String): Result<String> =
    suspendCancellableCoroutine { cont ->
        RequestManger.getInstance().get(url, object : onRequestLister {
            override fun onSucess(code: Int, response: String) {
                if (cont.isActive) cont.resume(Result.success(response))
            }
            override fun onFailed(code: Int, message: String) {
                if (cont.isActive) cont.resume(Result.failure(V3HttpException(code, message)))
            }
        })
        // RequestManger 无 cancel 句柄；cont.invokeOnCancellation 仅丢弃结果（OkHttp call 仍跑完，无副作用）
    }
// post 同理：v3Post(builder: MyRequestBuilder): Result<String>
```

设计要点：
- **直接打 `RequestManger`（纯网络原语）+ `Constant` 路径，绕开 UI 耦合的 `*Method` 包装类**（getMachineListFromServer 等内含 runOnUiThread/ButtonBox/Context，不可复用）。URL = `Constant.serveraddress + Constant.xxx`（+ 路径段，沿 v3 拼法）。
- 鉴权头/baseUrl 由 v3 RequestManger 自己加（ServerToken.serverToken + serveraddress）——**方案 A 不引入拦截器**，这块零改。
- 成功判定沿用 v3：RequestManger 已按 `code==200` 分流到 onSucess/onFailed，适配层直接用（ResponseSuccessPolicy seam 此处可不接，或退化为"onSucess 即成功"）。
- 在 IO dispatcher 上发起（RequestManger.enqueue 本就异步，但解析放 IO）。

## 2. V3TerminalRepository — 现有接口的 v3 IMPL

```kotlin
// data/repository/V3TerminalRepository.kt （草图；实现现有 TerminalRepository 接口，零改接口）
@Singleton
class V3TerminalRepository @Inject constructor(
    private val adapter: V3CallbackAdapter,
    private val gson: Gson,
    @IoDispatcher private val io: CoroutineDispatcher,
) : TerminalRepository {
    private val zonesFlow = MutableStateFlow<List<Zone>>(emptyList())
    override fun observeZones(): Flow<List<Zone>> = zonesFlow.asStateFlow()
    override fun observeTerminals(): Flow<List<Terminal>> = zonesFlow.map { it.flatMap(Zone::terminals) }

    override suspend fun refresh(): Result<Unit> = withContext(io) {
        runCatching {
            val zonesJson = adapter.v3Get(url(Constant.SearchZone)).getOrThrow()       // /terminal/terzone
            val terminalsJson = adapter.v3Get(url(Constant.getMahcinelistAll)).getOrThrow() // /terminal/terminalinfo
            val zoneDtos = gson.fromJson(zonesJson, ZoneEnvelopeDto::class.java)?.data.orEmpty()
            val terminalDtos = gson.fromJson(terminalsJson, TerminalEnvelopeDto::class.java)?.data.orEmpty()
            val terminals = terminalDtos.mapNotNull { it.toTerminalOrNull() }          // 现有 Mapper
            val byZone = terminals.groupBy { it.zoneId }
            zonesFlow.value = zoneDtos.mapNotNull { it.toZoneOrNull() }
                .map { it.copy(terminals = byZone[it.id].orEmpty()) }
        }
    }
}
```
- **复用现有**：ZoneEnvelopeDto/TerminalEnvelopeDto + toTerminalOrNull/toZoneOrNull(Mapper) + Terminal/Zone/TerminalStatus 域模型——零改。
- Gson 解析复用 NetworkModule 的 `provideGson`（setLenient + 容未知，R-003）——这个 Gson provider 方案 A 保留（它不依赖 Retrofit）。

## 3. 实时刷新 = 轮询（方案 A 无 WS/无推送）

- v3 无服务器推送 → "实时"靠 **UI 层定时 refresh()** 或用户手动下拉。
- 接口已支持：observeZones(Flow) 持续观察 SSOT，refresh() 触发一次 v3 拉取→更 SSOT→Flow 重发。**轮询节奏(如 10s)由 ViewModel/UI 决定，不在 Repository**（Repository 只提供 refresh 原语）。
- 这与原 ICD-RealtimeFallback "10s 轮询" 思路一致，只是去掉了 WS 主通道，轮询成唯一通道。

## 4. 登录适配（LoginAuthenticator 的 v3 IMPL）

- 现有 `LoginAuthenticator` seam 保留；新 `V3LoginAuthenticator` 实现它：调 v3 登录（RequestManger.postHashMap → /authorizations，form username/userpwd）→ 解析 TokenModelRsp → 写 v3 ServerToken/Constring（**方案 A 用 v3 的 token 存储，不用 AuthStoreImpl 加密那套**）→ 返回 AuthResult。
- ⚠ 决策点(留 G1)：方案 A 下 token 存储用 v3 既有（ServerToken 内存 + Constring/Prefs）还是仍走我的 AuthStore？倾向**用 v3 的**（方案 A=数据层零改），AuthStore 接口/ServerAddress.parse 作为 UI 校验工具保留但不接管 token 持久化。需 PM/CTO 定。

## 5. 各 V3*Repository 清单（G1 后按 WBS 逐个落）

| Repository | v3 端点(Constant) | 现有可复用 | 备注 |
|---|---|---|---|
| V3TerminalRepository | SearchZone / getMahcinelistAll / getGroupTerminal | TerminalRepository 接口 + Mapper + DTO | §2 |
| V3TaskRepository | getTaskList / postTaskInfo / ... | 逆推 TaskDto(AR-110) | 接口待 fe 任务 Tab 消费集(同 AR-101 教训) |
| V3SchemeRepository | getsecheList / sechetask | 逆推 SchemeDto | |
| V3MediaRepository | getMusicInfo / getFolderInfo | 逆推 MediaDto | |
| V3ServerStateRepository | getServerState | 逆推 ServerStateDto | 健康度，可轮询 |

## 6. 待 G1 定的决策点（不在草稿层拍）

1. token 存储：v3 ServerToken/Constring vs 我的 AuthStore（§4）——倾向 v3。
2. 轮询节奏与归属：ViewModel 定时 vs Repository 内置定时器——倾向 ViewModel/UI 层。
3. 作废 IMPL 的处置：留盘作迁移资产 vs 删除——等 PM WBS 指示（现不删）。
4. RequestManger 是否需小改以支持 cancel/超时透出——尽量零改 v3（§7.3 legacy_impact，需会签）；倾向适配层用 suspendCancellable 丢弃结果即可，不改 v3。

## 7. 逆推 ICD DRAFT 项改挂（方案 A）

原挂钩 O-1 后端回执的 DRAFT 项（派生规则/刷新端点/包络 code-message），方案 A 下改挂 **"v3 适配实测验证"**：跑通 v3 *Method 抓真实 JSON → 验证 DTO 解析 → DRAFT 升 LIVE。无需问后端。

---

*DRAFT 预研，PM 授权草稿不提交。G1 批准方案 A 后正式派 → 我据此快速落地（接口/模型/Mapper 已在，主要写 CallbackAdapter + 各 V3*RepositoryImpl + V3LoginAuthenticator）。*
