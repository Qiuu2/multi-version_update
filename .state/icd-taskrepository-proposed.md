# TaskRepository 接口契约 — proposed（PA-03b 接口先行 · STD-ICD-WRITE）

> Producer: Data-Integration（领域 owner 拍板）· 日期: 2026-05-28 · 触发: fe-business 从任务 5 屏渲染反推消费需求，要 data 先钉接口（AR-101 纪律）
> 范围: 只定签名 + 领域类型，**无 impl**（impl 是后续 V3TaskRepository，方案 A 走 v3 *Method/RequestManger）。
> 契约源 = 已 LIVE 的逆推 DTO（SchemeDto/TaskDto/TtsTaskDto，见 .state/icd-reverse-engineered-proposed.md），方案 A 即 v3 真值。

---

## 4 个领域决策（回 PM 的 4 问）

1. **Scheme 嵌套 tasks**（采纳 fe 倾向，同 Zone 嵌套 terminals 的裁定）：`Scheme(... tasks: List<SchemeTask>)`。一致性 + 省 fe 一次 combine。
2. **task.state = sealed `SchemeTaskStatus` + `Unknown(raw)`**（Unknown-tolerant，同 TerminalStatus 套路）。已知 case 暂按 v3 SchemeDto 的 `taskstate`/`projectstate` 语义给保守集；真值集候 O-1 回执（方案 A 下改"v3 适配实测验证"）；fe 边界 domain→UI 映射 + Unknown 兜底不崩。
3. **执行日志 = `suspend getExecutionLog(): Result<List<TaskLog>>` 一次性**（非 observe）：v3 无推送、无独立日志流端点；一次性 + 轮询模型一致，fe 按需调，更简单。若后续发现真有日志流端点再升 observe（ICD_UPDATE）。
4. 契约源用 LIVE 逆推 DTO；**SchemeDto 服务器拼错字段 `projectstatetate` 的 @SerializedName 须照抄**（逆推已标 TaskZuoxiModel.java:40，impl 时不会漏——确认）。

---

## 接口（proposed · 只读 + 启停，PA-03b 范围；CRUD 后续增量）

```kotlin
// data/repository/TaskRepository.kt（接口；impl=V3TaskRepository 走 v3）
interface TaskRepository {
    /** 观察作息方案（每 Scheme 含嵌套 tasks）；refresh 成功后重发。 */
    fun observeSchemes(): Flow<List<Scheme>>

    /** 拉 v3 作息+任务入 SSOT；失败保留上一快照（同 V3TerminalRepository 契约）。 */
    suspend fun refresh(): Result<Unit>

    /** 启停一个方案（v3 /task/sechenableordisable）。成功后该方案 active 翻转并经 observeSchemes 重发。 */
    suspend fun setSchemeActive(schemeId: String, active: Boolean): Result<Unit>

    /** 执行日志一次性拉取（轮询模型；非独立流）。 */
    suspend fun getExecutionLog(): Result<List<TaskLog>>

    // CRUD（新建/删除/改 time/title/zone）= 后续增量；PA-03b 先「只读 + 启停」。
}
```

## 领域类型（proposed）

```kotlin
// data/model/Scheme.kt
data class Scheme(
    val id: String,
    val name: String,
    val active: Boolean,             // 方案是否运行中（projectstate 派生）
    val tasks: List<SchemeTask> = emptyList(),
)

data class SchemeTask(
    val id: String,
    val name: String,
    val status: SchemeTaskStatus,
    val startTime: String? = null,   // SchemeDto.starttime
    val mediaName: String? = null,   // SchemeDto.medianame
    val volume: Int? = null,
)

/** Unknown-tolerant（同 TerminalStatus，ESC-WATCH-2）。真值集候 v3 适配实测验证。 */
sealed interface SchemeTaskStatus {
    data object Idle : SchemeTaskStatus          // 未运行
    data object Running : SchemeTaskStatus        // 运行中
    data object Disabled : SchemeTaskStatus       // 已停用
    data class Unknown(val raw: String) : SchemeTaskStatus   // R-003 兜底
}

// data/model/TaskLog.kt
data class TaskLog(
    val id: String,
    val taskName: String,
    val timestamp: String,
    val message: String,
)
```

---

## 待 fe 确认 / 后续

- 已知 SchemeTaskStatus case（Idle/Running/Disabled）是保守占位，真实 taskstate/projectstate→status 派生候 v3 适配实测（同 deriveStatus 套路），届时 ICD_UPDATE，fe when 保 Unknown 分支即穷尽不变。
- CRUD 增量接口（addScheme/deleteTask/updateTaskTime 等）待 PA-03b 只读+启停 跑通后定。
- TaskLog 字段是保守骨架——v3 是否有真日志端点、字段为何，impl 时实读确认（endpoint-inventory 未见独立日志端点，可能从 taskinfo 派生，待定）。

*待 PM 过 Critic 后落 icd-contracts.md（ICD-TaskRepository-v1）+ 转 fe 让其从骨架进 VM。*
