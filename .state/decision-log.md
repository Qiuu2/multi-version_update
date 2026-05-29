# Decision Log — AeroRadio v4

> CTO 决策的可追溯记录。维护者: PM Agent。

| ID | 日期 | 决策 | 理由/背景 | 落地动作 | 状态 |
|----|------|------|----------|---------|------|
| D-2026-05-27-01 | 2026-05-27 | **暂不进 Phase 0；WBS 待 spike 结论再出** | AAR 64 位结论会改写关键路径与风险登记册 | PM 暂停 WBS，等 TASK-AR-SPIKE-001 回报 | ACTIVE |
| D-2026-05-27-02 | 2026-05-27 | **批准 spike，P0_CRITICAL** | 验证 htapplib.aar arm64-v8a 可用性 | 已派 TASK-AR-SPIKE-001 给 Legacy-Native，限 2 工作日 | DISPATCHED |
| D-2026-05-27-03 | 2026-05-27 | **启动入口收敛：保留 V4Activity，删 SignActivity 的 LAUNCHER 标签** | 解决双 LAUNCHER（RISK-AUDIT-02） | PM 统筹，放入 Phase 0 第一批；现记入 tasks.yaml backlog `BL-LAUNCHER-FIX`，待 WBS 正式建任务 | DEFERRED |
| D-2026-05-27-04 | 2026-05-27 | **批准等待期任务：RISK-AUDIT-03 权威端点清单草案** | 充分利用 spike 等待期，零代码改动 | 已产出 `.state/endpoint-inventory-draft.md`，归档待与 spike 结论合并处理 | DONE |
| D-2026-05-27-05 | 2026-05-27 | **批准 R-001 条件性降级 CRITICAL → LOW（GO-pending-device，不标 CLOSED）** | SPIKE-001 PASSED_WITH_MINOR（Critic 独立复现）：arm64 库链接自洽、JNI 与 32 位等价、native 面仅 MP3 编码 | PM 已更 PM-memory `current_risk_register`；GO 后档案同步（build.gradle 注释/CLAUDE.md/SKILL RISK-AR-003）派 legacy-native 出 PR 过 Critic | ACTIVE |
| D-2026-05-27-06 | 2026-05-27 | **真机终验路径：CTO 持真机做最终确认；开发期用模拟器** | R-001 正式关闭(LOW→CLOSED)唯一前置 | PM 把报告 §7.2 真机清单作交接工单挂 Gate1/Phase1 前。⚠ 已告知 CTO：x86_64 模拟器**无法**验语音 native 面（AAR 无 x86_64 .so），语音确认须 arm64 真机 | ACTIVE |
| D-2026-05-27-07 | 2026-05-27 | **解冻 D-01：令 PM 组装 Phase 0 WBS+DAG + ICD-Endpoints-v1，打包 Gate 1** | spike 结论已回报，D-01 暂停条件解除 | PM 产出 `.state/gate1-review-package.md`，路由 Critic 计划评审后提交 CTO Gate 1 | DONE |
| **D-2026-05-27-08** | 2026-05-27 | **Gate 1 APPROVE：批准 Phase 0 计划（9 任务/DAG/估算/ICD 口径）** | Critic v2 PASSED(HIGH)，计划主体可执行；data-integration 52% 负载为已知排期点 | PM 材料化 9 任务，按 DAG 派发根任务（AR-001/003 + 独立 AR-006/007/008/009），AR-002/004/005 依赖满足后入队；所有产出先过 Critic | ACTIVE |
| **D-2026-05-27-09** | 2026-05-27 | **授权 O-1/O-2/O-3 问询起草** | Phase 1 起点外部前置（后端契约 + 厂商 WS 文档） | data-integration 出 O-1（后端 5 问），fe-platform 出 O-2（WS 协议）+ O-3（x86_64 .so），PM 汇总转 CTO 发厂商/后端 | DONE（问询包 `.state/inquiry-package-for-cto.md` 已呈 CTO，含 O-1 Q1-5+D-1/D-2/D-3 / O-2 / O-3 / O-4）|
| **D-2026-05-27-10** | 2026-05-27 | **Gate 2 APPROVE（WS 有条件）** | Phase 0 全 10 件 Critic PASS，跨域架构自洽 | 拦截器/双栈隔离 + AuthStore + IPC + Compose 四方案定型批准；WS 方案 DRAFT-条件性（O-2 回执后增量过审 + ICD-BroadcastWS DRAFT→LIVE）。**已正式生效**（Critic holistic PASSED HIGH，实核 3 跨域 seam + 6 ICD 一致） | EFFECTIVE |
| **D-2026-05-27-11** | 2026-05-27 | **Phase 1 推进模式：CTO 取外部回执，团队做回执无关准备** | Phase 1 实质卡 O-1(后端)/O-2(厂商) 真实回执，不可团队自产 | CTO 实际向后端/厂商发问询取回执；同时 PM 派团队回执无关的 Phase1 结构准备（UI/Repository/WS 生命周期/VoiceTalkAdapter 骨架，documented-assumption，回执到 ICD_UPDATE 修正）。R-001 真机终验 CTO 择机跑 | ACTIVE |
| **D-2026-05-28-12** | 2026-05-28 | **ICD 契约建立策略二分法 + ICD逆推独立工作包** | CTO 方向性指示。⚠ CTO 称"D-05"，但 D-05 已用于 R-001 条件降级——为保编号链不冲突，记为 D-12（请知悉） | 见下方「ICD 建立策略（D-12）」详述；ICD逆推派 data/legacy 为独立工作包（回执无关，正好填消化窗）；WS 维持 DRAFT 标风险 | ACTIVE |
| **D-2026-05-28-13** | 2026-05-28 | **🔴 范围最终决策 = 方案 A（只改 UI、协议/v3 数据层零改动）** | CTO 最终确认。⚠ CTO 称"D-06"，但 D-06 已用于真机路径——记为 D-13。**最高优先级，supersede 部分既往决策** | 后端零改 / httptask/*Method.java 一行不改全保留 / 协议沿用 v3 / 不引入 WS。唯一新增=callback→StateFlow 适配层。SUPERSEDES: D-07/G2新栈架构/D-11/O-1。**重新 G1 = CTO APPROVED**（校正版 ~24-30h，Critic 计划评审过）。详见 `.state/gate1-planA-review-package.md` | **APPROVED-EFFECTIVE** |
| **D-2026-05-28-14** | 2026-05-28 | **方案 A token 存储 = 沿用 v3 ServerToken（纯零改）** | 重新 G1 同批，CTO 选"纯零改"优先 | 登录后 token 存 v3 ServerToken/Constring（数据层零改）；新栈 AuthStore 降级为纯 UI 校验工具(ServerAddress.parse)、不接管持久化。⚠ 接受 v3 token 存储的安全级别(可能明文/非加密 SharedPreferences)——属 R-A-NEW"继承 v3"取舍的一部分 | ACTIVE |
| **D-2026-05-29-15** | 2026-05-29 | **5-Tab build 完成里程碑 — 3 项指令** | CTO 在「5-Tab build complete」里程碑（终端✓任务✓服务✓广播✓, AI 降级）的方向选择 | ①**推进 real V3TaskRepository**（替空 stub, Critic 大审）; ②**提交 5-Tab build** = 已落 5 grouped commits 4ccdd13(data)/4e3ab39(legacy)/82fd1de(ui)/6c87a74(test)/85ea115(workflow) on claude/v4-screens-on-refactor（排除 .idea/.claude/skill_matlab_addendum）; ③**对讲/寻呼保持 tap-to-start/end**（非 Handoff press-hold PTT）=终版, Critic+PM 认 session-seam-honest, 偏离 Handoff 已 CTO 批准; 日后要 PTT=UX-only follow-up | ACTIVE |

## SPIKE-001 结论已回报（2026-05-27T10:00Z）— 以下为 PM 建议，待 CTO 批准

> TASK-AR-SPIKE-001 已 COMPLETED：Critic 评审 **PASSED_WITH_MINOR (HIGH，独立复跑 nm/readelf/javap 复核)**，结论 **GO-pending-device-confirmation**。证据：arm64-v8a 双 .so 为合法 64 位 ELF、JNI 入口与 32 位 diff=IDENTICAL、链接面零 32 位-only 悬空依赖；**架构纠正成立**——HTIntf.* 全是纯 Java，唯一 native 面=MediaCodec 4 个 Mp3Encode*。详见 `.state/spike-aar64-report.md`(v2, a70c3f9) + tasks.yaml `review_result`。

**PM 建议 1 — R-001 条件性降级（待 CTO 批准，非 PM 擅自）**
- 建议：`R-001`(RISK-AR-003 / 32 位 ABI) **CRITICAL → LOW（条件性，状态=GO-pending-device，不标 CLOSED）**。
- 依据（Critic 背书 (a)）：① 厂商 5/14 已随 AAR 供 arm64-v8a 双 .so；② 链接静态自洽、JNI 与 32 位等价（Critic 已独立复现）；③ native 依赖面仅 MP3 编码，边界窄。降级期间保留 LOW 兜底 + 全部降级 UI 路径，不视为已消除。
- 正式关闭(LOW→CLOSED)唯一前置（Critic (b)）：一台 arm64-v8a 真机(Android 7.0+) 跑报告 §7.2 步骤 A+B，回执含 设备型号 + `ro.product.cpu.abi`=arm64-v8a + Android 版本 + 类初始化无 UnsatisfiedLinkError + Mp3EncodeInit 成功码 + Mp3EncodeBuffer>0 + 无 SIGSEGV + logcat 无 dlopen alignment/relocation 告警。属 QA/CTO 动作。
- 批准后档案同步点（GO 后落地工单，legacy-native 已备料未改）：build.gradle 滞后注释(§8 零行为 diff)、CLAUDE.md 关键约束段、SKILL `RISK-AR-003`、PM-memory `R-001`、legacy-native 人格 memory。

**PM 建议 2 — UNK-001 关闭**：「厂商能否供 64 位 AAR」前提已消解（已证供），建议关闭。

**PM 建议 3 — 3 个 MINOR 不返工**：F-001/F-002/F-003（措辞精度 + Mp3EncodeInit 参数溯源）随「GO 后落地工单 + 真机清单交接」一并消化（Critic recommended_action）。

**待 CTO 决策点（非阻塞）**
- DP-1：是否批准 R-001 条件性降级（建议 1）。
- DP-2：arm64 真机终验——接受条件性 GO 挂 Gate1/Phase1 前由 QA 补跑，or 现在调度真机/CI。
- DP-3：是否解冻 D-01、令 PM 组装 Phase 0 WBS+DAG + 权威端点 ICD-Endpoints-v1，打包 Gate 1。
- DP-4（跨域 ICD）：派 ICD-VoiceAAR 任务时须钉死「Kotlin 适配以 AAR 实际 `CallBackIntf`(21 回调) 为准，非 skill 模板 NativeTalkListener」（Critic + legacy-native 双方提示）。

---

## ICD 建立策略（D-12，CTO 2026-05-28 方向性指示）

**二分法**：
| 类别 | 内容 | 流程 | LIVE 条件 |
|------|------|------|----------|
| **可逆推类** | AAR 接口 / TCP 4521 命令 / 旧栈已实现的 REST 字段 | legacy-native / data-integration 从 v3 代码逆推；**每字段标注来源**（如「逆推自 TalkService.java:42」） | **可直接 LIVE**——代码是跑得通的事实 |
| **需验证类** | WebSocket 协议 / v4 新增端点 | 先逆推/按 Handoff 出 **DRAFT** | **必须后端实测对接才转 LIVE**；对接前下游必须 `ignoreUnknownKeys` 未知字段容错 |

**ICD逆推工作包（独立, 回执无关——填消化窗）**：
- **data-integration**：逆推旧栈 REST 字段 → 更新 5 个 DTO ICD（TerminalDto-v2 已含 4-int 逆推自 MachineInfo；补 Zone/Task/Media/Server 等 + 字段来源标注）。可逆推 code-fact → LIVE；v4 新增端点 → DRAFT。
- **legacy-native**：逆推 AAR + TCP 命令 → ICD-VoiceAAR / ICD-IPCSocket，字段标来源。结构可逆推→LIVE；**但 VoiceAAR 真机面 DRAFT-pending-device、4521 命令词表无源可逆推（发任意 shell 串）属需验证→DRAFT 待厂商 O-4**。
- **WebSocket ICD-BroadcastWS**：维持 **DRAFT「待后端对接」**。

**⚠ 风险确认（CTO 问）：v3 代码零 WebSocket**。grep 全 legacy `.java`（WebSocket/ws://、newWebSocket）= **零命中**（与 v3-audit RISK-AUDIT-07 一致）。故 **ICD-BroadcastWS 是纯新增、无代码可逆推**——WBS 标为风险项 **R-WS-NEW**：实时方案全靠后端 O-2 文档 + 实测对接，无 v3 事实兜底；对接前 RealtimeClient 必须 parseMessage→Unknown 容错（AR-103 已实现）。
