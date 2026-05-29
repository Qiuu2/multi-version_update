# Runtime Demo Checklist — 5-Tab 代码完成后的最小真机验证

> 触发条件：**5 个 Tab 的代码层全部推完**（终端✓ / 任务 / 服务 / 广播 / AI降级）后，统一上模拟器/真机。
> CTO 指示（2026-05-28）：不为单 Tab 安排真机 demo；代码全完一次性跑。
> 本清单是「让 CTO 照着跑」的步骤，非自动化。

## 0. 前置条件
- [ ] **一台可达的 v3 LAN 主机**（校园主机），提供 21 个 REST 端点（明文 HTTP，base = `http://<host>:<port>/api`）。模拟器/真机与该主机同网可达。
- [ ] 设备：
  - **x86_64 模拟器**：可验所有 UI + REST 数据流；**但语音对讲 native 不可用**（AAR 无 x86_64 .so）——广播 Tab 的「对讲」档在模拟器上会降级/不可用，属预期。
  - **arm64-v8a 真机（Android 7.0+）**：验全部，含语音对讲 native（这也同时关闭 R-001，见 §6）。
- [ ] 构建环境：`JAVA_HOME=/home/it1234/android-studio/jbr`；`--no-daemon`。

## 1. 构建 & 安装
- [ ] `JAVA_HOME=/home/it1234/android-studio/jbr ./gradlew --no-daemon :app:assembleDebug`
- [ ] 装机：`adb install -r app/build/outputs/apk/debug/app-debug.apk`
- [ ] 唯一 LAUNCHER = V4Activity（AR-006），冷启动进权限编排（AR-010）→ 登录。

## 2. 登录（鉴权闭环，token=v3 ServerToken / D-14）
- [ ] 登录页输入 v3 主机地址（`ServerAddress.parse` 校验 host:port）+ 账号/密码。
- [ ] 成功 → token 存 v3 `ServerToken`，`Constant.serveraddress` 设为 `http://host:port/api`（V3LoginAuthenticator）。
- [ ] 5 态可见：idle/loading/error(地址错/认证失败)/success → 导航进 V4 主界面。

## 3. 终端 Tab（已代码完成，PA-01/PA-02 实证绿）
- [ ] Hub 显示**真实终端**（来自 v3 `/terminal/terminalinfo`），分区来自 `/terminal/terzone`，Zone 嵌套 terminals。
- [ ] 5 态 StatusPill 正确（Online/Offline/Fault/Playing/Paging）；**未识别状态 → Unknown 兜底不崩**（domain TerminalStatus.Unknown）。
- [ ] **轮询实时刷新**：终端 10s、详情页 5s（PollingRefreshScheduler）；前台刷新、后台暂停、回前台**立即刷新**；banner 反映 PollingState（REFRESHING/POLLING/ERROR）。
- [ ] ZoneDetail：进区看终端列表；空列表不崩。
- [ ] 广播目标选择屏：终端多选接真数据。
- [ ] ⚠ 验证「状态派生假设」：deriveStatus 是 documented-assumption（候真后端实测）——若真实状态与 UI 显示不符，**记下 raw 值**（Unknown(raw) 会带），回填 TerminalMapper.deriveStatus + ICD-TerminalDto ICD_UPDATE。

## 4. 任务 Tab（待代码完成后验）
- [ ] 作息方案列表 + 当前 active 方案 + 任务时间轴（v3 逆推 SchemeDto/TaskDto，注意 `projectstatetate` 拼写字段）。
- [ ] 启停方案（`/task/sechenableordisable`）→ active 翻转 + observeSchemes 重发。
- [ ] 执行日志（getExecutionLog 一次性）；**TaskLog 数据源是 impl 时钉死的**——若日志为空/字段不符，记下，可能需从 taskinfo 派生（ICD-TaskRepository OPEN）。
- [ ] task.state 映射 + Unknown 兜底。
- [ ] 任务 30s 轮询。

## 5. 服务 / 广播 / AI Tab（待代码完成后验）
- [ ] **服务 Tab**：系统健康度（v3 逆推 ServerStateDto：connection/taskcount/bandwidth 等）。
- [ ] **广播 Tab** 三档：
  - 寻呼（paging）/ 点播（casting）走 v3 REST。
  - **对讲（talk）走 AAR**：仅 **arm64 真机**可验 native（模拟器降级）。验 startTalk/音量/挂断；RECORD_AUDIO 权限时机（AR-010 F-1 待验）。
- [ ] **AI Tab**：降级（不实现）——确认是占位/降级提示，不崩。

## 6. R-001 语音 native 真机终验（关闭 R-001 的唯一前置）
- [ ] 照 `.state/realdevice-checklist-handoff.md` §A+B：arm64-v8a 真机跑语音。
- [ ] 回执含：设备型号 + `ro.product.cpu.abi`=arm64-v8a + Android 版本 + 类初始化无 `UnsatisfiedLinkError` + `Mp3EncodeInit` 成功码 + `Mp3EncodeBuffer>0` + 无 SIGSEGV + logcat 无 dlopen alignment/relocation 告警。
- [ ] 通过 → R-001 LOW→CLOSED（决策 D-05/D-06）。

## 7. 通过标准 & 证据
- [ ] 每 Tab：截图 + 关键 logcat（请求 URL、响应码、解析无异常）。
- [ ] 无崩溃；未知状态/空数据/网络失败均降级不崩（Unknown/Empty/Error 态）。
- [ ] 轮询行为符合 Handoff 节奏（终端10s/任务30s/详情5s）。
- [ ] 偏差项（状态派生、TaskLog 源、枚举真值集）记录 → 回 ICD_UPDATE（这些是 DRAFT/documented-assumption，本就候真实测）。

## 备注
- 实时方案是**轮询**（无 WebSocket，R-WS-NEW / 方案 A）；不要期待 server push。
- 旧栈 httptask/*Method.java 一行未改（Plan A 红线）；数据走 V3CallbackAdapter→RequestManger。
