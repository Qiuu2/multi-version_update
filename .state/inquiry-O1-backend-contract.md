# INQ-O-1 — 后端契约问询清单（5 问 + 2 衍生确认）

> 任务：TASK 序前置「①INQ-O-1」· 作者：Data-Integration Agent · 日期：2026-05-27
> 触发：CTO D-09 授权（Gate 1 §5 O-1）· 路由：本清单 → PM 汇总 → CTO 转后端/厂商
> 目的：钉死 v4 新栈（Retrofit + 拦截器 + Repository）迁移所需的接口契约，**消除 AR-002 错误映射硬阻塞、防 Phase 1 全量 Repository 返工/ICD churn（R-003，score 0.41）**。
> 方法：所有问题已用 v3 旧栈实读证据落地（`RequestManger.java` / `TaskMainMethod.java` / `ReFreshTokenUtil.java` / `*Rsp.java` / `EorroCode.java` / `Constant.java`），故问的是"确认/澄清"而非"猜测"。

---

## 0. 给后端/厂商的一句话背景

我们正把这套 App 的网络层从手拼 URL + OkHttp 回调（旧栈）重写为 Retrofit + 统一拦截器（新栈）。旧栈的行为我们已逐行读过，下面 5 个问题是把"旧栈实际怎么做"与"后端真实契约"对齐——**请基于服务器端真实实现回答，若与旧栈现状不符，请直接指出**（旧栈很可能有将错就错之处）。

---

## Q1 ｜ 同一路径多 HTTP 方法，Request / Response DTO 是否各异？

**问题**：像 `/task/taskinfo` 这一个路径同时承担 GET（查列表）/ POST（新建）/ PUT（更新）/ DELETE（删除）；`/terminal/terzone` 承担 GET/POST/DELETE。请确认：
1. 每个「路径 × 方法」的**请求体字段集**是否不同？（我们将按「路径×方法」为最小单元各定义 Request DTO）
2. 每个「路径 × 方法」的**响应体结构**是否不同，还是共用同一种？

**为何要问**：新栈 Retrofit 接口必须为每个「路径×方法」声明独立的 `@Body`/`@Query` 与返回类型。若契约不清，要么 DTO 写错导致解析崩，要么过度拆分浪费工时。

**旧栈实读证据（供对照）**：
- 请求体：旧栈对 POST/PUT/DELETE 一律用**扁平 form 表单**（`TransBeanMapUtil.transBeanToMap(model)` 把 model 摊平成 `HashMap<String,String>`），DELETE 也带 form body（仅一个 `id` 字段，例 `deleteTaskTerminal` 放 `map.put("id", taskid)`）。**没有 JSON 请求体**。
- 响应体：POST/PUT/DELETE `/task/taskinfo` 三者**都反序列化成同一个 `TaskIdModelRsp`**（即 `{ "data": [ TaskIdModel ] }`，`TaskIdModel` 含 `taskid` / `state`）；而 GET `/task/taskinfo`（`getTaskList`）走的是另一种列表模型。→ 初步看：**同路径下响应"包络一致、data 内元素类型按方法不同"**，请确认。

**期望答案形态**：一张「路径×方法 → 请求字段列表（名/类型/必填）/ 响应 data 内元素结构」对照表；或直接给后端接口定义/swagger。最低限度：逐个确认"请求是 form 还是 JSON"、"DELETE 用 body 还是 query 传 id"。

**不答的影响**：AR-001 的 auth 端点 DTO 之外、Phase 1 每个 Repository 都要靠抓包猜字段，高概率字段名/必填性错→运行时解析崩 / 静默丢字段（R-003 实体化）。

---

## Q2 ｜ 是否有统一响应包络（code / message / data）？

**问题**：所有 REST 响应是否统一为 `{ "code": ..., "message": ..., "data": ... }` 这种包络？还是只有 `data`？`data` 恒为数组，还是有时为对象/标量？

**为何要问**：拦截器 / Repository 的成功-失败判定与错误信息提取，取决于包络里有没有业务 `code` 和 `message`。这与 Q3 强耦合。

**旧栈实读证据（关键）**：旧栈**所有** `*Rsp` 包络类（`MachineListRsp` / `TokenModelRsp` / `CommonRsp` / `TaskIdModelRsp` …）**只有一个 `data` 字段，没有 `code`，也没有 `message`**。`BaseResponse` 同样只有 `data`。即旧栈看到的包络是 `{ "data": [...] }`，且 `data` 在所有已知端点里**恒为 JSON 数组**（即使单条结果也包成单元素数组，旧栈一律 `responseData.getData().get(0)` 取首元素）。

**期望答案形态**：①确认是否存在 `code`/`message`（哪怕旧栈没读，后端可能在发）；②`data` 是否恒为数组；③若有错误，错误详情放在哪（HTTP body？某字段？）。

**不答的影响**：新栈若假设有 `code`/`message` 而实际没有，或反之，会导致错误提示无法呈现给用户、或成功被误判为失败。直接影响 AR-002 错误映射与全部 Repository 的 `Result` 封装。

---

## Q3 ｜ 成功判定 = HTTP 状态码(2xx) 还是 业务码？★ 解 AR-002 硬阻塞

**问题**：客户端判断"这次请求成功了"，应当依据 **HTTP 状态码**（200/2xx）还是 **响应体里的业务码**？两者会不会出现"HTTP 200 但业务失败"或"业务成功但 HTTP 非 2xx"的组合？另：失败时后端返回的 HTTP 码具体有哪些（如 400/401/403/404/409/500）、各代表什么？

**为何要问（最高优先级）**：这是 **AR-002 DynamicBaseUrl + Auth 拦截器错误映射的硬阻塞项**（Gate 1 §2 标注 hard-blocked-by O-1）。拦截器与 Repository 的错误分支必须知道"哪一层是权威成功信号"。

**旧栈实读证据（重要且有矛盾）**：
- 旧栈 `RequestManger` 的成功判定是 **`response.code() == EorroCode.SUCESS`，而 `EorroCode.SUCESS = 200`（HTTP 码）**——即**用 HTTP 200 判成功**，并非读 body 业务码。非 200 一律走 `onFailed(response.code(), jsonStr)`。
- `EorroCode` 里还定义了 `TOKEN_EXPIRED = 401`（与 HTTP 401 同值），暗示 **401 = token 过期**走自动刷新（见 Q1-衍生 + AR-002 的 401 链）。
- **但存在业务级状态**：HTTP 200 之后，调用方仍会读 `data[0].state`，例如 `postTask` 里 `tmodel.getState()=="15"` 被当作"任务名重复"提示用户。即旧栈语义是 **"HTTP 码定传输成败 + data 内 state 字段定业务结果"** 的双层模型，但这个 `state` 不在包络层、且各端点含义可能不同。

**期望答案形态**：明确"权威成功信号 = HTTP 2xx"（确认/否认）；列出失败 HTTP 码语义表；若存在 data 内 `state`/`result` 这类业务状态码，给出其取值含义（尤其 `state="15"` 之类）。

**不答的影响**：AR-002 只能搭「可切换错误映射骨架（默认按 HTTP 2xx + TODO + 待 ICD_UPDATE）」无法定稿；Phase 1 各 Repository 的 `Result.failure` 文案/重试策略全部悬空。**此问回执直接解除 AR-002 错误映射部分的 hard-block。**

---

## Q4 ｜ `{id}` / `{type}` 是路径参数还是 query 参数？

**问题**：诸如"按 id 取任务终端""按 type 取媒体""按 id 取终端经纬度"这类带参端点，参数是拼在**路径段**（`/task/taskterminal/123`）还是 **query**（`/task/taskterminal?id=123`）？请逐个确认带参端点的参数形态与参数名。

**为何要问**：Retrofit 用 `@Path` 还是 `@Query` 取决于此；写错会导致 404 或参数被后端忽略。

**旧栈实读证据**：旧栈是**手拼路径段**。证据：
- `getTaskMachines`（`/task/taskterminal`）调用处：`url = serverAddress + Constant.getTaskMachines + "/" + i`（id 拼成路径段）。
- `Constant.java` 中以 `/` 结尾的常量明显是"待拼接 id/type 的基址"：`getMahcinelist="/terminal/terminaldo/"`、`getMahcineLatitude="/terminal/gitude/"`、`getMusicInfo="/terminal/mediainfo/"`（取媒体按 type）。
- 而 DELETE 类（如 `deleteTaskTerminal`）的 `id` 反而走 **form body**（`map.put("id", ...)`），不是路径也不是 query。

**期望答案形态**：逐端点「参数名 / 路径段 or query or body / 是否必填」。特别确认：媒体 `mediainfo/{type}` 的 type 取值集合；DELETE 系列究竟收 body 还是 query 的 id（旧栈用 body，需后端确认是否真支持 DELETE-with-body）。

**不答的影响**：每个带参端点都可能 404；DELETE 用 body 这种非标准用法若后端不支持，新栈改 query 才对，不问会照抄旧栈错误。

---

## Q5 ｜ multipart 上传端点的字段名？

**问题**：文件上传端点（媒体上传 `POST /terminal/mediainfo`、临时任务媒体 `POST /task/addtempttstaskmedia`）的 multipart 表单**字段名**是什么？除文件本体外还需要哪些**伴随表单字段**（如 taskid/speed/volume 等）？

**为何要问**：Retrofit `@Multipart` + `@Part` 必须用与后端完全一致的 part 名，否则后端收不到文件/参数。

**旧栈实读证据**：`RequestManger.updateFile` 写死文件 part 名为 **`mediafile`**（`builder.addFormDataPart("mediafile", file.getName(), ...)`，且从 `requestdata.getBodyMap().get("mediafile")` 取本地路径）。另有一行被注释掉的旧 URL 暗示曾用 query 传 `taskid/speed/male/volume/taskname/sort/content` 等伴随参数（现已注释，需确认这些参数现在走哪、是否仍需）。

**期望答案形态**：每个 multipart 端点的 part 列表（part 名 / 是否文件 / 是否必填）；确认文件 part 名确为 `mediafile`；列出必需的伴随字段及其传法（form-part vs query）。

**不答的影响**：上传端点（Phase 3 临时文件广播子页）必然失败，且 multipart 失败排查成本高。

---

## 衍生确认项（实读旧栈 + Critic 评审时发现，强烈建议一并问 —— 直接影响 AR-003 AuthStore / AR-002 401 链 / Phase 1 TerminalDto 双源一致）

> D-1/D-2 来自实读证据：旧栈现状与我方 skill 模板假设（`/authorizations/refresh` + JSON body + refresh_token）**不一致**，若不澄清会直接做错 AR-003/AR-002。D-3 来自 Critic F-4：REST 与 WS 两路终端 state 须对齐同一真值。

**D-1 ｜ 是否存在独立的 token 刷新端点？JWT 刷新机制到底是什么？**
- 旧栈**没有** `/authorizations/refresh` 这个路径。`ReFreshTokenUtil.reFreshToken()` 的"刷新"是**用存储的账号密码（`GetTokenModel{username,userpwd}`）重新 POST `/authorizations`** 换新 token。
- 但登录响应 `TokenModel` 同时返回 `token` / `expired_at` / `refresh_expired_at` / `priority` 四个字段——**既然有 `refresh_expired_at`，是否本应有 refresh token + 独立刷新端点，只是旧栈没用？**
- **请确认**：①是否存在独立刷新端点（路径/方法/入参）？②`refresh_expired_at` 配套的 refresh_token 在登录响应里以什么字段返回？③还是确实就靠"重存密码、过期重登"？
- **影响**：决定 AR-003 AuthStore 是存 refresh_token（soul 要求 JWT/refresh 进 EncryptedSharedPreferences、密码永不持久化）还是被迫存密码（与 soul「密码永不持久化」**直接冲突**，需 CTO 决策）。也决定 AR-002 拦截器 401 后是"调刷新端点"还是"重登"。**这条与 Q3 并列为 AuthStore 落地的关键前置。**

**D-2 ｜ 登录请求体是 form 还是 JSON？字段名确认。**
- 旧栈登录是 **POST form**，字段名 `username` + `userpwd`（注意是 `userpwd` 不是 `password`），经 `transBeanToMap` 摊平。鉴权头用 `Constant.header` + `token_tag`（形如 `Bearer ` 前缀，由 `ServerToken.serverToken` 持有）。
- 我方 skill 模板原假设登录是 `@Body LoginRequest`(JSON)。**请确认**：登录是 form 还是 JSON？字段名是否确为 `username`/`userpwd`？鉴权头 key 与 token 前缀（`Bearer ` 还是别的）具体是什么？
- **影响**：AR-002 AuthInterceptor 注入头的 key/前缀、AR-005 LoginScreen 接 AuthStore 的请求构造。错了登录直接 401。

**D-3 ｜ 终端状态：4 个 int 字段各自语义 + 如何派生显示状态 + 与 WS 对齐同一真值（Critic F-4，AR-101 实读修正粒度）。**

⚠ **粒度修正**：原 D-3 问"REST 终端**单一** `state` 字段的枚举值"——AR-101 实读旧栈 `MachineInfo` 证实**问错了粒度**：`GET /terminal/terminalinfo` 返回的终端**没有单一 state 字段**，而是**四个独立的 int 状态字段** `taskstate` / `devicestate` / `netstate` / `speechstate`。故 D-3 改为问这 4 个 int 的真相：

1. **四个 int 各自取值含义**：请逐个说明 `taskstate` / `devicestate` / `netstate` / `speechstate` 的取值枚举与语义（每个 int 各自代表什么、各取值含义）。
2. **如何派生显示状态**：终端在 UI 上的**单一显示状态**（在线/离线/故障/播放中/寻呼中 …）应如何从这 4 个 int **派生**？请给出权威派生规则（例如：哪个 int=几 → 哪个显示状态；优先级如何）。
3. **与 WS 推送对齐同一真值**：厂商已（A-4）定 WS 推送的终端状态字面值；请确认 WS 推送的状态与上面"从 4 个 int 派生的显示状态"**对齐到同一套真值**（避免 REST 4-int 派生 ↔ WS 字面值各自为政，导致同一终端两路状态不一致）。

- **附我方当前 documented-assumption 派生（请后端"确认/纠正"，非凭空猜）**：无任何状态字段→Unknown；`netstate==0`(或缺)→**离线**；`isinstancy==1` 或 `speechstate≠0`→**寻呼/语音占用**；`taskstate≠0`→**播放中**；否则→**在线**。⚠ 我**无法**从已知 4 int 派生"故障(fault)"——请确认故障状态由哪个 int/取值表达。
- **为何要问**：`TerminalDto` 有 REST(4 int) 与 WS 两个数据源；不对齐则同一终端两路状态映射成不同结果/兜底 Unknown，UI 状态错乱或闪烁。
- **影响**：`ICD-TerminalDto-v2` 的 wire DTO(4 int) → domain `TerminalStatus`(sealed) 的 **Mapper.deriveStatus 派生规则**（现为占位），及 WS↔REST 双源一致性。不对齐则 Phase 1 终端 Tab 真数据接入返工。

---

## 附：与 ICD-Endpoints-v1（AR-001）的衔接

- 以上回执到位后，AR-001 出 **v2 增量**：把 Q1/Q4/Q5 的「路径×方法→DTO/参数形态」填入契约表，Q2/Q3 的包络与成功判定写入「统一约定」章，D-1/D-2 写入 auth 端点契约；**D-3 的 state 枚举写入 ICD-TerminalDto-v1（Phase 1 终端 Repository 接入时）**。
- 在回执前，AR-001 仅冻结**草案**：5 问对应处标 `OPEN(INQ-O-1 Q#)`；auth 端点 DTO 骨架按 **documented-assumption**（包络 `{data:[]}` 无 code/message、成功=HTTP 2xx、登录 form `username/userpwd`、刷新=重登待确认）落，并显式标注"待 D-1/D-2 回执触发 ICD_UPDATE"。
- AR-002 错误映射在 Q3 回执前只搭可切换骨架（默认 HTTP 2xx 判定 + TODO）。

---

*inquiry-O1-backend-contract.md — Data-Integration 产出，待 PM 汇总转 CTO。证据均来自 v3 旧栈实读，行号可复核。*
