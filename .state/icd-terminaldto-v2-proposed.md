# ICD-TerminalDto-v2 + ICD-ZoneDto-v2 — 提交草案（STD-ICD-WRITE：PM 过 Critic 后落 §4/§5 + 广播 fe）

> Producer: Data-Integration · 日期: 2026-05-27 · 触发: TASK-AR-101 接口对位 fe AR-102 消费需求 + 3 建模决策
> 核验基准：已落地 `data/model/{Terminal,Zone}.kt` / `data/dto/TerminalDto.kt` / `data/repository/{TerminalRepository,TerminalMapper,TerminalRepositoryImpl}.kt`
> change_type: MODIFY（§4/§5 现行 v1 是模板，与实落地不符）· 这是 AR-101「接口先行」交付，解锁 fe AR-102/105

---

## 3 个建模决策（data 作为领域 owner 拍板，回 fe 的 3 问）

1. **Zone 嵌套 terminals**（采纳 fe 倾向，省一次 combine）：`Zone(id, name, description?, terminals: List<Terminal>)`。派生计数（每区 online/fault、全局 fault）**不在 repo**，fe UI 层自算（按你转达）。
2. **domain status = sealed `TerminalStatus` + `Unknown(raw)` 兜底**（ESC-WATCH-2）。已知 case 对齐 fe UI 的 StatusPill 集合（Online/Offline/Fault/Playing/Paging），故 fe 边界 domain→UI 映射对已知值 1:1；真实值集候 O-2/D-3，未识别值 → `Unknown(raw)` 不崩。**fe 的 `when` 必须保留 else/Unknown 分支（sealed 强制穷尽）**。回执到 ICD-TerminalDto ICD_UPDATE 钉真值（新增已知 case 加这里，fe when 仍穷尽）。
3. **Partial 不暴露**：repo **无法**区分"成功 vs 缓存命中但刷新失败"——当前 SSOT 是内存（进程死即清），无持久缓存层。故 repo 只给 `refresh(): Result<Unit>`（成功/失败）。**fe 把 Partial 绑到 fe-platform 的 connectionState（后续），AR-102 先 4 态 + Partial 占位**（按你转达的 fallback）。Room 缓存落地后 repo 可升级 result 类型 → ICD_UPDATE。

---

## TerminalRepository 接口（fe AR-102/105 消费契约 · 已 @Binds 可注入）

```kotlin
interface TerminalRepository {
    fun observeZones(): Flow<List<Zone>>        // 观察，每 Zone 含嵌套 terminals；refresh 成功后重发
    fun observeTerminals(): Flow<List<Terminal>> // 扁平终端流（同一 SSOT）
    suspend fun refresh(): Result<Unit>          // 拉网络入 SSOT；失败保留上一快照（驱动 error/retry）
}
```
- 实现 `TerminalRepositoryImpl`：内存 `MutableStateFlow` SSOT（Room 后续 drop-in）；refresh 拉 zones+terminals → 按 zoneId 把 terminals join 进 zones → 两个 fetch 都成功才发布（观察者不见半更新）；失败 Result.failure 且保留旧快照。
- DataModule 已 `@Binds TerminalRepositoryImpl→TerminalRepository`（单例）。fe 直接 @Inject。

## 域模型（fe 消费）

```kotlin
// data/model/Terminal.kt
data class Terminal(id:String, name:String, zoneId:String, status:TerminalStatus,
                    volume:Int?=null, longitude:String?=null, latitude:String?=null)

sealed interface TerminalStatus {           // Unknown-tolerant（ESC-WATCH-2）
    data object Online; data object Offline; data object Fault
    data object Playing; data object Paging
    data class Unknown(val raw:String)      // R-003 兜底；raw=源 token 便于诊断
}

// data/model/Zone.kt
data class Zone(id:String, name:String, description:String?=null, terminals:List<Terminal> = emptyList())
```

## Wire DTO（data 内部，镜像服务器；fe 不消费）

```kotlin
// data/dto/TerminalDto.kt — Gson @SerializedName，全 nullable，包络 {data:[]} 无 code/message
data class TerminalDto(id:Int?, name:String?, ip:String?, zone:Int?, groupid:Int?, type:Int?,
    taskstate:Int?, devicestate:Int?, netstate:Int?, speechstate:Int?,  // ← 服务器返 4 个独立 int 状态，无单一 state
    isinstancy:Int?, volume:Int?, longitude:String?, latitude:String?)
data class ZoneDto(id:Int?, name:String?, description:String?, count:Int?, online:Int?, offline:Int?, busyline:Int?)
```
**关键实读**：现行 §4 假设终端返单个 state——错。MachineInfo 证服务器返 4 个独立 int（taskstate/devicestate/netstate/speechstate）。故 wire/domain 两层：DTO 镜像 int，domain 由 Mapper 派生单一 TerminalStatus。

## status 派生（documented-assumption · 候 OPEN INQ-O-1 D-3 + O-2）

TerminalMapper.deriveStatus 占位序：无状态字段→Unknown("no-status") / netState==0→Offline / isUrgent==1‖speechState≠0→Paging / taskState≠0→Playing / else→Online。**Fault 暂不可从已知 int 派生（无确认的 fault 字段）；v4 的 talking/casting/urgent/alarm 不在此映射**。回执后仅换 deriveStatus，domain 类型+fe 映射不变。

## 端点对应（ICD-Endpoints-v1）
- GET /terminal/terminalinfo → List<TerminalDto>
- GET /terminal/terzone → List<ZoneDto>
- （refresh 内部并这两个端点 join；getZoneTerminals/{id} 暂未纳入接口——ZoneDetail 从 observeZones 的嵌套 terminals 取；若 AR-105 需单独懒拉再加，走 ICD_UPDATE）

## §5 ICD-ZoneDto-v2 注
现行 §5 "ZoneDto 含 terminals 子列表"——**域模型 Zone 确实嵌套 terminals**（决策 1），但来源是 join /terminalinfo 按 zoneId 分组，非单个 zone 端点内嵌。

### 变更历史
- v2.0 (2026-05-27, AR-101 接口先行): observe+refresh 接口；Zone 嵌套 terminals；status=sealed+Unknown(ESC-WATCH-2)；wire/domain 两层（4 int→派生）；Partial 不暴露(无缓存)。v1 单 state 假设作废。派生+真值集候 D-3/O-2。

---

*待 Critic 评 AR-101 时核「proposed == 已落地」→ PM 落 §4/§5 + 广播 ICD_UPDATE(affected: frontend-business)。status 真值集 / Fault 派生 / Partial 三处留 OPEN。*
