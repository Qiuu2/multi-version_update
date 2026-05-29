# 权威端点清单草案 — AeroRadioControl

> RISK-AUDIT-03 缓解物 · 作者：PM Agent · 日期：2026-05-27
> 来源：`constant/Constant.java` + 全库调用点实扫（grep）+ 旧栈方法体阅读（RequestManger / TaskMainMethod / ZoneMethod / MainMethod）。
> 用途：作为 `ICD-Endpoints-v1` 的输入草案，供 Data-Integration 迁移时定方法与契约。**草案性质，待 CTO + v4 设计稿最终确认。**

---

## 0. 读法与方法学

### 0.1 HTTP 方法如何判定

旧栈无注解，方法由 `RequestManger` 的封装函数决定，调用方按命名约定选用：

| RequestManger 函数 | HTTP 方法 | 约定的常量前缀 |
|---|---|---|
| `get(url, lister)` | GET | `getXxx`、`SearchZone` |
| `postHashMap(...)` | POST (form) | `postXxx`、`setTaskMusic`、`addTempTask` |
| `putHaspMap(...)` | PUT (form) | `putXxx` |
| `deleteHaspMap(...)` | DELETE (form) | `deleteXxx` |
| `updateFile(...)` | POST (multipart) | `postFile`、`postTempMediaFile` |

> 鉴权：除登录外，所有请求由 `RequestManger` 统一加 `Authorization: Bearer <token>`（`Constant.header` + `ServerToken.serverToken`）。地址前缀来自 `PreferencesUtil("serverAddress")` 或 `Constant.serveraddress`。**新栈迁移后，这两件事（鉴权头 + 动态 baseUrl）应由拦截器统一处理**（见 v3-audit §6）。

### 0.2 三个重要更正/澄清

1. **`method/MainMethod.java` 不是 REST 调用方** —— 它封装的是**原生 AAR 语音指令**（`HTIntf.startpaging / startondemand / startspeech`），即寻呼/点播/对讲的 native 入口。**应从 REST 端点清单剔除**（同时更正 v3-audit §1.1 把它归入"网络旧栈"的说法）。这也佐证 spike `TASK-AR-SPIKE-001` 要测的 native 方法即 `startspeech/startpaging` 一族。
2. **同一路径按方法复用** —— 如 `/task/taskinfo` 同时承担 GET/POST/PUT/DELETE，`/terminal/terzone` 承担 GET/POST/DELETE。"21 端点"是**路径口径**；按"路径×方法"展开是 ~40+ 个操作。ICD 应以**路径×方法**为最小单元。
3. **4 个空串常量是占位** —— `getServerTime / getMachineInfo / getPartMachines / getTaskInfo` 在 `Constant.java` 中值为 `""`，无真实路径、无调用，是历史占位符。

### 0.3 状态标注图例

- ✅ **v4 在用**：对应 v4 五 Tab IA 的功能，需迁移到新栈。
- 🟡 **遗留/待确认**：v3 在用但 v4 IA 未明确保留，需设计确认。
- 🔵 **设计预留**：v3 中**定义但未调用**，且对应 v4 新功能（迁移时才接）。
- ✗ **废弃候选**：建议 v4 删除（死代码/空占位/被取代）。

---

## 1. 端点总表（按资源分组：方法 / 路径 / 常量 / 调用方 / 状态）

### 1.1 认证 Auth

| 方法 | 路径 | 常量 | 调用方 | 状态 |
|---|---|---|---|---|
| POST | `/authorizations` | `getAuthorization` | `LoginActivity`, `MyApplication`, `ReFreshTokenUtil`, `BaseActivity/BaseFragment` | ✅ 登录 + JWT/Refresh |

### 1.2 终端 Terminal（终端 Tab）

| 方法 | 路径 | 常量 | 调用方 | 状态 |
|---|---|---|---|---|
| GET | `/terminal/terminalinfo` | `getMahcinelistAll` | `LocationInMapActivity`, `AddCaiboActivity`, `AddWenziyuyinActivity`, `DialogTerminal`, `FragmentXunHu` | ✅ 全部终端 |
| GET | `/terminal/terminaldo/{id}` | `getMahcinelist` | `FragmentXunHu/DuiJiang/DianBo`, `TerminalUtil` | ✅ 终端明细/操作 |
| GET | `/terminal/gitude/{id}` | `getMahcineLatitude` | `FragmentXunHu` | ✅ 终端经纬度（地图视图，Phase 2） |
| POST | `/terminal/savegitude` | `saveMahcineLatitude` | `MapTerminalPop`, `LocationInMapActivity` | ✅ 存经纬度（地图视图） |
| GET | `/terminal/zoneterminal/{zoneid}` | `getGroupTerminal` | `ZonePop`, `TerminalPop`, `ZoneTerminalPop`, `ZoneDetailActivity`, `ZoneMethod` | ✅ 分区→终端 |
| POST | `/terminal/zoneterminal` | `postGroupTerminal` | `ZoneMethod` | ✅ 提交分区终端 |
| GET | `/terminal/terzone` | `SearchZone` | `ZoneManageActivity`, `ZonePop`, `TerminalPop`, `TempTTSActivity` | ✅ 查询分区 |
| POST | `/terminal/terzone` | `postZone` | `ZoneMethod` | ✅ 新建/更新分区 |
| DELETE | `/terminal/terzone` | `deleteZone` | `ZoneMethod` | ✅ 删除分区 |
| GET | `/terminal/mediainfo/{type}` | `getMusicInfo` | `SelectMusicActivity`, `ActivityMusicOrder`, `AddSchemeActivity`, `AddSchemeTaskActivity`, `MusicPop` | ✅ 按类型取媒体 |
| GET | `/terminal/mediainfo` | `getAllMusicInfo` | _(未调用)_ | 🟡 与上同路径，冗余常量 |
| POST | `/terminal/mediainfo` | `postFile` | `TempTTSActivity`（multipart） | ✅ 上传媒体文件 |
| GET | `/terminal/mediafolderinfo` | `getFolderInfo` | `ActivityMusicOrder`, `SelectMusicActivity` | ✅ 媒体文件夹 |
| POST | `/terminal/urgentplay` | `postUrgentPLAY` | _(未调用)_ | 🔵 v4 寻呼/紧急播放预留（WBS AR-WP01-BROADCAST 引用） |
| GET | `/terminal/terminalquicktask` | `postGetShortcutTask` | `FragmentShortcutTask` | 🟡 快捷任务（v4 IA 未明确，待确认） |
| GET | `/terminal/shortcutkey` | `getTerminalShortcutkey` | _(未调用)_ | ✗ 设备快捷键，无调用，v4 未见 |

### 1.3 任务 Task（任务 Tab）

| 方法 | 路径 | 常量 | 调用方 | 状态 |
|---|---|---|---|---|
| GET | `/task/taskinfo` | `getTaskList` | `TaskGuangboActivity` | ✅ 任务列表 |
| POST | `/task/taskinfo` | `postTaskInfo` | `TaskMainMethod`（postTask / postFileBroadTask） | ✅ 新建任务 |
| PUT | `/task/taskinfo` | `putTaskInfo` | `TaskMainMethod`（putTaskRefresh） | ✅ 更新任务 |
| DELETE | `/task/taskinfo` | `deleteTask` | `TaskMainMethod` | ✅ 删除任务 |
| GET | `/task/sechinfo` | `getsecheList` | `TaskZuoxiActivity` | ✅ 作息方案列表 |
| POST | `/task/sechetask` | `postSchemeTask` | `AddSchemeTaskActivity` | ✅ 创建作息方案任务 |
| PUT | `/task/sechetask` | `putSchemeTask` | `AddSchemeTaskActivity` | ✅ 更新作息方案任务 |
| POST | `/task/sechetaskinfo` | `postSchemeInfo` | `TaskZuoxiListActivity` | ✅ 作息方案信息 |
| POST | `/task/sechetaskinfo` | `postTaskListInfo` | _(未调用)_ | ✗ 与上同路径，冗余常量 |
| POST | `/task/sechenableordisable` | `postChangeTaskStatu` | `TaskManageUtils` | ✅ 启/停作息任务 |
| POST/PUT | `/task/ttstaskinfo` | `postTtsTaskInfo` | `TaskMainMethod`（postTtsTask=POST / putTtsTaskRefresh=PUT） | ✅ TTS 任务 增/改 |
| GET | `/task/ttstaskcontent` | `getTtsTaskContent` | `TaskGuangboDetailActivity`, `AddWenziyuyinActivity` | ✅ TTS 内容 |
| GET | `/task/taskterminal/{id}` | `getTaskMachines` | `TaskZuoxiDetailActivity`, `TaskGuangboDetailActivity`, `TaskMainMethod` | ✅ 任务→终端 |
| POST | `/task/taskterminal` | `postTaskTerminal` | `AddSchemeTaskActivity`, `TempTTSActivity`, `AddFileBroadActivity`, `TaskMainMethod` | ✅ 绑定任务终端 |
| DELETE | `/task/taskterminal` | `deleteTaskTerminal` | `AddSchemeTaskActivity`, `TaskMainMethod` | ✅ 删任务终端 |
| GET | `/task/taskmusic` | `getTaskMusics` | `TaskZuoxiDetailActivity`, `TaskGuangboDetailActivity`, `AddFileBroadActivity` | ✅ 任务→媒体 |
| POST | `/task/taskmusic` | `setTaskMusic` | `TaskMainMethod`, `TempTTSActivity` | ✅ 绑定任务媒体 |
| DELETE | `/task/taskmusic` | `deleteTaskMedia` | `TaskMainMethod` | ✅ 删任务媒体 |
| POST | `/task/taskenordis` | `postRunOrStopTask` | `TaskManageUtils` | ✅ 运行/停止方案 |
| POST | `/task/taskdoorno` | `postUserOrStopTask` | `TaskManageUtils` | ✅ 启用/停用方案 |
| POST | `/task/taskvolume` | `postSetTaskVoice` | `TaskManageUtils` | ✅ 任务音量 |
| POST | `/task/addtempttstask` | `addTempTask` | `TempTTSActivity` | ✅ 临时任务（临时文件广播子页） |
| POST | `/task/addtempttstaskmedia` | `postTempMediaFile` | `UpFileService`, `ReUpFileService`（multipart） | ✅ 临时任务媒体 |
| GET | `/task/gettempttstask` | `getTempTtsTask` | _(未调用)_ | 🔵 临时任务读取，v4 临时文件广播或需 |
| DELETE | `/task/deltemptts` | `deleteTempTtsTask` | _(未调用)_ | 🔵 临时任务删除，同上 |

### 1.4 服务 Service（服务 Tab）

| 方法 | 路径 | 常量 | 调用方 | 状态 |
|---|---|---|---|---|
| GET | `/server/serverstate` | `getServerState` | `TaskManageUtils` | ✅ 系统健康度 |

### 1.5 广播 Broadcast（广播 Tab）—— 注意：核心为 native，非 REST

广播 Tab 三档（寻呼/对讲/点播）的**音频通道走原生 AAR**（`MainMethod` → `HTIntf.startpaging/startspeech/startondemand`），不经 REST。REST 仅提供**目标选择**（复用 §1.2 的 `/terminal/terminalinfo`、`/terminal/zoneterminal`）与**紧急播放**（`/terminal/urgentplay`，🔵 预留）。→ 广播 Tab 的端点需求由终端域端点 + native AAR 共同满足，spike `TASK-AR-SPIKE-001` 结论是其前置。

### 1.6 空占位常量（无路径、无调用）

| 常量 | 值 | 处置 |
|---|---|---|
| `getServerTime` | `""` | ✗ 删除 |
| `getMachineInfo` | `""` | ✗ 删除 |
| `getPartMachines` | `""` | ✗ 删除 |
| `getTaskInfo` | `""` | ✗ 删除 |

---

## 2. 未调用端点清单（v3 中定义但零引用）

| 常量 | 路径 | 判定 |
|---|---|---|
| `postUrgentPLAY` | `/terminal/urgentplay` | 🔵 v4 寻呼/紧急播放将用 → 保留 |
| `getTempTtsTask` | `/task/gettempttstask` | 🔵 v4 临时文件广播可能用 → 保留待定 |
| `deleteTempTtsTask` | `/task/deltemptts` | 🔵 同上 → 保留待定 |
| `getAllMusicInfo` | `/terminal/mediainfo` | 🟡 路径与 `getMusicInfo`/`postFile` 重叠 → 合并/删冗余常量 |
| `postTaskListInfo` | `/task/sechetaskinfo` | ✗ 路径与 `postSchemeInfo` 重叠 → 删冗余常量 |
| `getTerminalShortcutkey` | `/terminal/shortcutkey` | ✗ v4 未见 → 废弃候选 |

---

## 3. 候选废弃清单（建议 v4 不再使用，需设计确认）

| 项 | 理由 | 风险 |
|---|---|---|
| `getServerTime / getMachineInfo / getPartMachines / getTaskInfo` | 空串占位，死代码 | 无（纯清理） |
| `postTaskListInfo` | 与 `postSchemeInfo` 同路径冗余 | 低（保留 `postSchemeInfo`） |
| `getAllMusicInfo` 常量 | 与 `getMusicInfo` 同路径 | 低（保留带 type 的版本，按需补无参版） |
| `getTerminalShortcutkey`（`/terminal/shortcutkey`） | 零调用，v4 IA 未见 | 中（需确认 v4 是否做设备快捷键） |
| `postGetShortcutTask`（`/terminal/terminalquicktask`） | v3 `FragmentShortcutTask` 在用，v4 广播 Tab 是否保留快捷任务未定 | 中（待 v4 设计确认） |

> ⚠ 所有废弃**需 v4 设计稿（AeroRadio v4.html / Handoff.html）确认**后才执行，避免误删。建议作为 Phase 3 `AR-WP03-LEGACY-CLEANUP` 的输入。

---

## 4. 推荐 ICD 候选端点（供 Data-Integration 优先接入，对齐 WBS）

按 v4 Phase 顺序与 WBS work package 排序，建议 Data-Integration 按此优先级建 Repository + DTO：

| 优先级 | 端点（路径×方法） | 对应 WBS | v4 Tab |
|---|---|---|---|
| **P0** | `POST /authorizations` | AR-WP00-NET（AuthStore） | 登录 |
| **P0** | `GET /terminal/terminalinfo` | AR-WP01-TERMINAL（TerminalRepository） | 终端 |
| **P0** | `GET /terminal/zoneterminal/{id}` | AR-WP01-TERMINAL | 终端/分区 |
| **P1** | `GET/POST/DELETE /terminal/terzone` | AR-WP01-TERMINAL | 分区管理 |
| **P1** | `POST /terminal/zoneterminal` | AR-WP01-TERMINAL | 分区终端 |
| **P1** | `POST /terminal/urgentplay` | AR-WP01-BROADCAST（寻呼） | 广播 |
| **P1** | `GET/POST/PUT/DELETE /task/taskinfo` | AR-WP02-TASK（TaskRepository） | 任务 |
| **P1** | `GET /task/sechinfo` · `POST/PUT /task/sechetask` · `POST /task/sechetaskinfo` · `POST /task/sechenableordisable` | AR-WP02-TASK（作息方案） | 任务 |
| **P1** | `POST/PUT /task/ttstaskinfo` · `GET /task/ttstaskcontent` | AR-WP02-TASK（TTS） | 任务 |
| **P1** | `GET/POST/DELETE /task/taskterminal` · `GET/POST/DELETE /task/taskmusic` | AR-WP02-TASK | 任务 |
| **P1** | `POST /task/taskenordis` · `POST /task/taskdoorno` · `POST /task/taskvolume` | AR-WP02-TASK | 任务 |
| **P2** | `GET /server/serverstate` | AR-WP02-SERVICE（ServerStateRepository） | 服务 |
| **P2** | `GET /terminal/mediainfo[/{type}]` · `POST /terminal/mediainfo` · `GET /terminal/mediafolderinfo` | 媒体（点播/任务媒体共用） | 终端/任务/广播 |
| **P2** | `GET /terminal/gitude/{id}` · `POST /terminal/savegitude` | AR-WP02-MAP（地图） | 终端地图 |
| **P3** | `POST /task/addtempttstask` · `POST /task/addtempttstaskmedia` · `GET /task/gettempttstask` · `DELETE /task/deltemptts` | 临时文件广播子页 | 任务 |

### 4.1 ICD 待澄清问题（迁移前需后端/设计回答）

1. **路径多路复用的 DTO 是否一致** —— 如 `/task/taskinfo` 的 GET 响应 vs POST/PUT 请求体结构，需分别定义 Request/Response DTO。
2. **统一响应包络** —— 旧栈普遍解析 `XxxRsp.getData()`（列表）。需钉死统一包络（code/message/data）写入 ICD。
3. **错误码** —— 旧栈用 `EorroCode.SUCESS` 判成功（非标准 2xx？）。需确认成功判定语义（业务码 vs HTTP 码），影响拦截器与 Repository 错误映射。
4. **`{id}/{type}` 路径参数 vs query** —— 旧栈手拼 `url + "/" + id`，需在 ICD 明确每个端点的路径参数形态。
5. **multipart 端点**（`postFile` / `postTempMediaFile`）—— 字段名 `mediafile` 等需进 ICD。

---

*endpoint-inventory-draft.md — RISK-AUDIT-03 缓解物，待与 spike 结论一并处理（D-2026-05-27-01）。所有废弃需 v4 设计稿确认。*
