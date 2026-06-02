# ICD 逆推工作包 — proposed diff（TASK-AR-110 · STD-ICD-WRITE：PM 过 Critic 后落 icd-contracts.md）

> Producer: Data-Integration · 日期: 2026-05-28 · 触发: CTO D-12 ICD 二分法
> 方法：从 v3 旧栈源码逆推，**每字段标来源 `逆推自 file:line`**。分类规则：
> - **LIVE**（code-fact）：旧栈已实现并跑通的 REST 字段——代码即事实。
> - **DRAFT**（待后端实测）：v4 IA 新增、旧栈无对应实现的端点/字段。
> 边界：本包**不依赖 O-1/O-2 回执**（逆推自既有 v3 代码）。所有 DTO 用 Gson `@SerializedName`，全 nullable（R-003 防御解析，`setLenient` 容未知字段）。

> 范围说明：legacy-native 域的 ICD-VoiceAAR / ICD-IPCSocket 来源标注由 **TASK-AR-111** 负责，不在本包。

---

## 第 1 部分：给已 LIVE 的逆推类 ICD 补字段来源标注

### 1.1 ICD-TerminalDto-v2（§4）— 4 int 状态字段来源（CTO 明确要求补行号）

终端 DTO 逐字逆推自 `model/responseModel/MachineInfo.java`：

| DTO 字段 | 类型 | 逆推自来源 | 分类 |
|---|---|---|---|
| id | Int? | MachineInfo.java:38 `private int id` | LIVE |
| name | String? | MachineInfo.java:36 `private String name` | LIVE |
| ip | String? | MachineInfo.java:37 `private String ip` | LIVE |
| zone | Int? | MachineInfo.java:35 `private int zone` | LIVE |
| groupid | Int? | MachineInfo.java:40 `private int groupid` | LIVE |
| type | Int? | MachineInfo.java:28 `private int type` | LIVE |
| **taskstate** | Int? | MachineInfo.java:29 `private int taskstate //任务状态` | LIVE |
| **devicestate** | Int? | MachineInfo.java:30 `private int devicestate //设备状态` | LIVE |
| **netstate** | Int? | MachineInfo.java:31 `private int netstate //网络状态` | LIVE |
| **speechstate** | Int? | MachineInfo.java:32 `private int speechstate` | LIVE |
| volume | Int? | MachineInfo.java:33 `private int volume` | LIVE |
| isinstancy | Int? | MachineInfo.java:34 `private int isinstancy` | LIVE |
| longitude | String? | MachineInfo.java:41 `private String longitude` | LIVE |
| latitude | String? | MachineInfo.java:42 `private String latitude` | LIVE |

→ wire 字段全 **LIVE**（旧栈 `getMahcinelistAll`=/terminal/terminalinfo 实际反序列化为 `MachineListRsp{data:[MachineInfo]}`，MachineListRsp.java:12）。**唯一 DRAFT 残留 = 4 int → 单一 `TerminalStatus` 的派生规则**（Mapper.deriveStatus，候 INQ-O-1 D-3/O-2，非字段本身）。

### 1.2 ICD-ZoneDto-v2（§5）— 来源

逆推自 `model/ZoneModel.java`：

| DTO 字段 | 类型 | 逆推自来源 | 分类 |
|---|---|---|---|
| id | Int? | ZoneModel.java:85 `private int id` | LIVE |
| name | String? | ZoneModel.java:87 `private String name` | LIVE |
| description | String? | ZoneModel.java:88 `private String description` | LIVE |
| count | Int? | ZoneModel.java:27 `private int count` | LIVE |
| online | Int? | ZoneModel.java:30 `private int online` | LIVE |
| offline | Int? | ZoneModel.java:31 `private int offline` | LIVE |
| busyline | Int? | ZoneModel.java:32 `private int busyline` | LIVE |

→ 全 LIVE（`SearchZone`=/terminal/terzone → `ZoneRsp{data:[ZoneModel]}`，ZoneRsp.java:12）。

### 1.3 ICD-Endpoints-v1 / NetworkModule / AuthState-v2 — 来源标注

- **Endpoints base `/api`**：逆推自 `LoginActivity.java:495`（`serverAddress="http://"+ip+":"+port+"/api"`）+ `MyRequestBuilder.java:34`（`url = Constant.serveraddress + 端点常量`）。LIVE。
- **鉴权头**：key=`Authorization` 逆推自 `Constant.java:9`；前缀=`Bearer ` 逆推自 `Constant.java:10`（`token_tag`）；注入逆推自 `RequestManger.java:63`（`addHeader(Constant.header, ServerToken.serverToken)`）。LIVE。
- **成功判定 HTTP 200**：逆推自 `RequestManger.java:106/138/etc`（`response.code()==EorroCode.SUCESS`）+ `EorroCode.java:7`（`SUCESS=200`）。LIVE（documented-assumption 标注可保留——这是旧栈事实，但后端是否还有 body 业务码仍候 O-1 Q3）。
- **包络 `{data:[]}` 无 code/message**：逆推自 `BaseResponse.java:10`（仅 `data`）+ 全部 `*Rsp.java`（仅 data 字段）。LIVE（旧栈所见结构事实）。
- **AuthState TokenDto 字段**：token/expired_at/refresh_expired_at/priority 逆推自 `TokenModel.java`（token/expired_at/refresh_expired_at/priority 四 getter）。登录 form 字段 username/userpwd 逆推自 `GetTokenModel.java:10/13`。LIVE。**刷新端点存在性 = DRAFT**（旧栈无 /authorizations/refresh，候 D-1）。

---

## 第 2 部分：逆推未建的 DTO ICD（任务域 / 媒体 / 服务）

### 2.1 ICD-TaskDto-v1（任务/广播任务）— LIVE 主体 + 少量 DRAFT

逆推自 `model/responseModel/TaskGuangboModel.java`（任务实体，`getTaskList`=GET /task/taskinfo → `TaskGuangboListRsp`）。所有字段 **public**，行号如下：

| DTO 字段 | 类型 | 逆推自来源 | 分类 |
|---|---|---|---|
| taskid | String | TaskGuangboModel.java:12 | LIVE |
| taskname | String | TaskGuangboModel.java:22 | LIVE |
| tasktype | Int | TaskGuangboModel.java:21 | LIVE |
| taskstate | Int | TaskGuangboModel.java:36 | LIVE |
| enablestate | Int | TaskGuangboModel.java:37 | LIVE |
| projectstate | Int | TaskGuangboModel.java:39 | LIVE |
| volume | Int | TaskGuangboModel.java:15 | LIVE |
| priority | Int | TaskGuangboModel.java:16 | LIVE |
| startdate / enddate | String | TaskGuangboModel.java:18 / :19 | LIVE |
| starttime | String | TaskGuangboModel.java:23 | LIVE |
| timelength / timelengthtype | String | TaskGuangboModel.java:24 / :25 | LIVE |
| execmode | Int | TaskGuangboModel.java:20 | LIVE |
| israndomplay | Int | TaskGuangboModel.java:26 | LIVE |
| medianame | String | TaskGuangboModel.java:27 | LIVE |
| sechename | String | TaskGuangboModel.java:28 | LIVE |
| isinstancy | Int | TaskGuangboModel.java:38 | LIVE |
| playing | Int | TaskGuangboModel.java:49 | LIVE |
| prepower / level / datasendmodel / bandrate / samplerate / caiboprepower | Int | TaskGuangboModel.java:13/14/17/31/34/35 | LIVE (高级音频参数, v4 是否暴露待 UI 定) |
| liveterminalid / liveterminalname | Int/String | TaskGuangboModel.java:32/33 | LIVE (对讲直播相关) |
| cmd / cmdargs | Int/String | TaskGuangboModel.java:29/30 | LIVE |

任务写操作响应：`TaskIdModel`（POST/PUT/DELETE /task/taskinfo → `TaskIdModelRsp`）：
| taskid | String | TaskIdModel.java:24 | LIVE |
| id | Int | TaskIdModel.java:25 | LIVE |
| state | String | TaskIdModel.java:35 | LIVE（业务子状态，如 "15"=任务名重复，逆推自 TaskMainMethod.java:313） |

> 分类：以上全 **LIVE**（旧栈 `TaskMainMethod.java` 实际 post/put/delete 并解析）。无 DRAFT 字段——任务域旧栈实现完整。

### 2.2 ICD-SchemeDto-v1（作息方案）— LIVE

逆推自 `model/responseModel/TaskZuoxiModel.java`（`getsecheList`=GET /task/sechinfo → `TaskZuoxiRsp`）：

| DTO 字段 | 类型 | 逆推自来源 | 分类 |
|---|---|---|---|
| taskid | Int | TaskZuoxiModel.java:21 | LIVE |
| name | String | TaskZuoxiModel.java:22 | LIVE |
| taskcount | Int | TaskZuoxiModel.java:10 | LIVE |
| mediaid | String | TaskZuoxiModel.java:11 | LIVE |
| medianame | String | TaskZuoxiModel.java:12 | LIVE |
| starttime | String | TaskZuoxiModel.java:13 | LIVE |
| startdate / enddate | String | TaskZuoxiModel.java:14 / :15 | LIVE |
| execmode | Int | TaskZuoxiModel.java:16 | LIVE |
| volume | Int | TaskZuoxiModel.java:17 | LIVE |
| taskstate | Int | TaskZuoxiModel.java:18 | LIVE |
| projectstate | Int | TaskZuoxiModel.java:19 | LIVE |
| priority | Int | TaskZuoxiModel.java:20 | LIVE |
| projectstatetate | String | TaskZuoxiModel.java:40 | LIVE（注：字段名疑似拼写错"statetate"，逆推保留原样，新栈 @SerializedName 须照抄旧名） |

### 2.3 ICD-TtsTaskDto-v1（TTS 文字语音任务）— LIVE

逆推自 `model/responseModel/TtsTaskContentModel.java`（`getTtsTaskContent`=GET /task/ttstaskcontent → `TtsTaskContentRsp`）：

| DTO 字段 | 类型 | 逆推自来源 | 分类 |
|---|---|---|---|
| state | Int | TtsTaskContentModel.java:26 | LIVE |
| taskid | Int | TtsTaskContentModel.java:36 | LIVE |
| speed | Int | TtsTaskContentModel.java:72 | LIVE |
| male | Int | TtsTaskContentModel.java:73 | LIVE（性别/音色） |
| contents | String | TtsTaskContentModel.java:74 | LIVE（TTS 文本内容） |

### 2.4 ICD-MediaDto-v1（媒体/媒体文件夹）— LIVE

媒体文件 逆推自 `model/responseModel/MusicInfoModel.java`（`getMusicInfo`=GET /terminal/mediainfo/{type} → `MusicInfosRsp`）：
| mediaid | Int | MusicInfoModel.java:16 | LIVE |
| name | String | MusicInfoModel.java:24 | LIVE |
| folderid | Int | MusicInfoModel.java:14 | LIVE |
| size | Int | MusicInfoModel.java:17 | LIVE |
| format | String | MusicInfoModel.java:18 | LIVE |
| bitrate | Int | MusicInfoModel.java:19 | LIVE |
| length | Int | MusicInfoModel.java:22 | LIVE（时长） |

媒体文件夹 逆推自 `model/responseModel/MusicFolderInfoModel.java`（`getFolderInfo`=GET /terminal/mediafolderinfo → `MusicFolderInfosRsp`）：
| folderid | Int | MusicFolderInfoModel.java:48 (getter) | LIVE |
| parentid | Int | MusicFolderInfoModel.java:56 | LIVE |
| name | String | MusicFolderInfoModel.java:64 | LIVE |
| all / count / start / state | Int | MusicFolderInfoModel.java:16/24/32/40 | LIVE（分页/状态元数据） |

### 2.5 ICD-ServerStateDto-v1（系统健康度）— LIVE

逆推自 `model/responseModel/SeverStateModel.java`（`getServerState`=GET /server/serverstate → `SeverStateRsp`）：
| state | Int | SeverStateModel.java:10 | LIVE |
| connection | Int | SeverStateModel.java:11 | LIVE（当前连接数） |
| taskcount | Int | SeverStateModel.java:12 | LIVE |
| bandwidth | Int | SeverStateModel.java:13 | LIVE |
| maxconnection | Long | SeverStateModel.java:14 | LIVE |
| ctrlport / dataport | Int | SeverStateModel.java:15 / :16 | LIVE |
| name | String | SeverStateModel.java:17 | LIVE |
| ip | String | SeverStateModel.java:18 | LIVE |
| gate | String | SeverStateModel.java:19 | LIVE（网关） |

---

## 第 3 部分：注册表 PLANNED 项处置

| 注册表项 | 处置 | 依据 |
|---|---|---|
| **ICD-TaskDto-v1**（现 PLANNED） | **升 LIVE**（带来源，见 §2.1/2.2/2.3） | 旧栈 TaskGuangboModel/TaskZuoxiModel/TtsTaskContentModel + TaskMainMethod 完整实现 |
| **ICD-MapLocation-v1**（现 PLANNED, legacy-native producer） | **保持 PLANNED/DRAFT** | 百度地图回调是 native/v4 集成，非 REST 逆推范围；终端经纬度字段 longitude/latitude 已在 TerminalDto-v2 (LIVE, MachineInfo:41/42)，但地图回调契约本身待 Phase 2 |

新增建议注册表行（proposed）：
- ICD-TaskDto-v1：Producer data, Consumer fe-business, **LIVE**（含 Task/Scheme/Tts 三子 DTO）
- ICD-MediaDto-v1：Producer data, Consumer fe-business, **LIVE**
- ICD-ServerStateDto-v1：Producer data, Consumer fe-business, **LIVE**

---

## 第 4 部分：DRAFT 项汇总（v4 新增、旧栈无源，待后端/O-1 回执）

| 项 | 为何 DRAFT | 挂钩 |
|---|---|---|
| TerminalStatus 派生规则（4 int → 单一 status） | 旧栈无单一 status，派生是 v4 新建模 | INQ-O-1 D-3 + O-2 |
| /authorizations 刷新端点 | 旧栈无 refresh 端点 | INQ-O-1 D-1 |
| /terminal/urgentplay 请求/响应 DTO | 旧栈 `postUrgentPLAY` 常量定义但**零调用**（endpoint-inventory §2），无实际字段可逆推 | 后端对接 |
| /task/gettempttstask · /task/deltemptts 响应 | 旧栈定义未调用（endpoint-inventory §2） | 后端对接 |
| 统一响应是否含 code/message | 旧栈包络仅 data；后端可能另有 | INQ-O-1 Q2 |

---

## 验收提示（给 Critic）

- 每个标 **LIVE** 的字段都附了 `file:line`，可逐条核「该行确在 v3 源码且确是该字段」。
- 每个标 **DRAFT** 的项都说明了「v4 新增 / 旧栈零调用 / 无源」的理由。
- 本包**纯文档**（proposed ICD），无代码改动，不碰 icd-contracts.md（STD-ICD-WRITE，PM 统写）。
- 字段名一律按旧栈**原样**（含疑似拼写 `projectstatetate`），新栈 DTO 的 @SerializedName 须照抄，否则反序列化丢字段。

*待 Critic 核 LIVE 确有 v3 源 / DRAFT 确为 v4 新增无源 → PM 落 icd-contracts.md §4/§5 标注 + 新增 TaskDto/MediaDto/ServerStateDto 章 + 升 ICD-TaskDto-v1 LIVE。*
