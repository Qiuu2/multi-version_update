# Handover Snapshot — AeroRadioControl v4 (PM)

> 写于 2026-05-28，/clear 前。给 /clear 之后的 PM（你）无缝接手用。
> 你是 **team-lead，扮演 Project Manager**。6-agent Agent-Teams 工作流（`CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1`, teammateMode=tmux）。
> 启动协议：读 `aeroradio-workflow/SKILL.md` + `aeroradio-workflow/agents/project-manager/*.md`。权威账本 = `.state/tasks.yaml`（STD-CANONICAL-STATE）。决策记录 = `.state/decision-log.md`。

---

## 1. 当前进度

- **Phase 0：COMPLETE**。AR-001~010 全部 Critic PASSED（拦截器/AuthStore/OkHttp/Login/入口收敛/IPC/spike档案/设计token/权限/端点ICD）。
- **Phase 1 prep + ICD逆推：COMPLETE**。AR-101~107（终端 Repo/屏/VM、VoiceTalkAdapter、RealtimeClient骨架、EOL根因修）、AR-110/111（逆推 DTO + VoiceAAR/IPCSocket 来源标注）全 Critic PASSED。
- **方案 A（D-13）执行中**：
  - **PA-01（data v3 适配层）= Critic PASS HIGH**。V3CallbackAdapter + V3TerminalRepository(@Binds swap) + V3LoginAuthenticator + ServerConfig seam。动态 **19 测全绿**。
  - **PA-02（PollingRefreshScheduler）= Critic PASS HIGH**。6 测绿，轮询替 WS，挂死已修（backgroundScope）。
  - **终端 Tab = 代码完成 + 实证验证**（跑在 v3 真数据上，VM 零改证实）。剩**运行时 demo**（需 CTO 模拟器+v3主机，见 `.state/runtime-demo-checklist.md`）。
- **剩余 4 Tab（5-Tab 顺序：终端✓→任务→服务→广播→AI降级）**：
  - **任务 Tab**：UiState/类型骨架 done（Critic PASS），ViewModel **未做**——等 data 落 TaskRepository stub(.kt) 才能编译写 VM。ICD-TaskRepository-v1 已 LIVE。
  - **服务 Tab**：未开工（系统健康度，逆推 ServerStateDto 已 LIVE）。
  - **广播 Tab**：未开工（寻呼/点播走 v3 REST；对讲走 legacy 的 AAR）。
  - **AI Tab**：降级不做。

## 2. CTO 决策清单（D-01~D-14，每条一句）
- **D-01**：暂不进 Phase 0，WBS 待 spike 结论再出。
- **D-02**：批准 AAR arm64 spike，P0_CRITICAL。
- **D-03**：启动入口收敛（删 SignActivity LAUNCHER，留 V4Activity）。
- **D-04**：批准 spike 等待期任务（权威端点清单草案）。
- **D-05**：批准 R-001 条件性降级 CRITICAL→LOW（GO-pending-device，不标 CLOSED）。
- **D-06**：真机终验路径——CTO 持 arm64 真机做最终确认，开发期用模拟器（x86_64 模拟器验不了语音 native）。
- **D-07**：解冻 D-01，组装 Phase 0 WBS+DAG + ICD-Endpoints-v1，打包 Gate 1。
- **D-08**：Gate 1 APPROVE（Phase 0 计划/9任务/DAG/估算/ICD 口径）。
- **D-09**：授权 O-1/O-2/O-3 问询起草。
- **D-10**：Gate 2 APPROVE（WS 有条件）。【已被 D-13 部分 supersede】
- **D-11**：Phase 1 推进模式——CTO 取外部回执，团队做回执无关准备。【已被 D-13 supersede】
- **D-12**：ICD 契约建立策略二分法（可逆推类→LIVE / 需验证类→DRAFT）+ ICD逆推独立工作包；确认 **v3 代码零 WebSocket**。
- **D-13** 🔴：范围最终决策 = **方案 A**（只改 UI，协议/v3 数据层零改，httptask/*Method.java 一行不改，不引入 WS，唯一新网络代码=callback→StateFlow 适配层）。重新 G1 = **APPROVED-EFFECTIVE**。supersedes D-07/G2新栈架构/D-11/O-1。
- **D-14**：token 存储 = 沿用 v3 `ServerToken`（纯零改）；新栈 AuthStore 降级为 UI 校验工具（ServerAddress.parse）。

## 3. 关键架构事实
- **方案 A**：ViewModel → 现有 Repository 接口 → **V3*RepositoryImpl（@Binds swap 替 Retrofit impl）** → V3CallbackAdapter（callbackFlow 包 v3 RequestManger 拿 raw JSON）→ 复用逆推 DTO + AR-101 Mapper → domain → 内存 SSOT(MutableStateFlow) → Flow。**轮询（PollingRefreshScheduler）替 WS**。
- **path1 决策（PM 裁决，范围内）**：包 RequestManger 拿 raw JSON（不走 *Method、不碰 MachineInfo POJO）→ 复用 AR-101 DTO+Mapper。理由：wire JSON 同一份字节，正当复用 + 不沾 *Method 的 UI 耦合。字段保真已核：TerminalDto←MachineInfo 14/14 wire 字段 1:1 零丢失。
- **19 测全绿**（Critic 18:13 实跑）：V3TerminalRepositoryTest 7 + V3CallbackAdapterTest 3 + TerminalHubViewModelTest 5 + ZoneDetailViewModelTest 4。后 9 个证 **AC#7 消费侧 VM 零改**（@Binds swap 透明）。
- **Constant.java 零改**（Plan A 红线，Critic git status 核实）；v3 数据层一行未动。
- **token=v3 ServerToken**（D-14）：V3LoginAuthenticator 写 `ServerToken.serverToken`，v3 RequestManger 读它注鉴权头。
- **R-001**（32位ABI/语音）：LOW-条件性，GO-pending-device；arm64 真机终验=CTO 择机（关闭路径见 `.state/realdevice-checklist-handoff.md`）。
- **测试环境**：无 Robolectric；碰 Android 静态类（如 v3 `Constant`）须注入化绕开（ServerConfig seam 即此用，照 AR-003 范式）。

## 4. ICD 现状（registry: `aeroradio-workflow/references/icd-contracts.md`）
- **LIVE**：NetworkModule-v1、AuthState-v2、LoginAuthenticator-v1、TerminalDto-v2、ZoneDto-v2、TaskDto-v1、SchemeDto-v1（拼写字段 `projectstatetate` 须照抄）、TtsTaskDto-v1、MediaDto-v1、ServerStateDto-v1、DesignTokens-v1.1、**TaskRepository-v1**（§13）、IPCSocket-v2（连接语义LIVE）、VoiceAAR-v2（控制面+21回调LIVE）。Endpoints-v1=DRAFT-FROZEN。
- **DRAFT/未决**：IPCSocket 命令词表（pending-vendor O-4）、VoiceAAR native 执行（pending-device R-001）、TaskRepository 的 SchemeTaskStatus 真值集 + getExecutionLog 数据源（pending v3 实测）、TerminalStatus deriveStatus（documented-assumption pending 实测）。
- **已砍（WS）**：ICD-BroadcastWS-v1 + RealtimeFallback-v1 = **方案 A 下不引入 WebSocket**（R-WS-NEW：v3 零 WS、纯新增无源）。实时性由 **PollingRefreshScheduler 轮询**替代。MapLocation-v1=PLANNED(Phase2 百度地图)。

## 5. 已废弃 / 收口清单
- **新栈网络 IMPL 白做（DORMANT，留盘不删，作未来迁移资产）**：AR-002 拦截器 / AR-003 AuthStore 持久化那套 / AR-004 单例 OkHttp / AR-101 Retrofit `TerminalRepositoryImpl` / `RetrofitLoginAuthenticator` / AR-103 WS `RealtimeClient`。这些 @Binds 已撤（V3* 接管），文件留盘。
- **问询包**：**O-1 后端契约作废**（方案 A 不谈协议；逆推 DTO 从"待 O-1 验"升"v3 跑通即 LIVE"）；**O-2 WS 协议 23 问作废**（无 WS）。**保留**：O-3（x86_64 .so 索要）、O-4（4521 命令词表，UNK-002）——对厂商。
- **回执无关准备已收口**：AR-101~107 的骨架在方案 A 下 UI/VM/接口/Mapper **保留有效**（换 V3 impl，VM 零改证实）。
- **AuthStore**：降级为 UI 校验工具（ServerAddress.parse），不接管 token 持久化（D-14）。

## 6. git 状态
- 分支：**`claude/v4-screens-on-refactor`**（PR 目标 main）。
- 本轮 checkpoint commit（CTO 批准，按 commit_hygiene 分组，排除 .idea/.claude/.messages/未知文件）：
  - **96514cb** build: Plan A build config + EOL normalization（.gitattributes/.gitignore/build.gradle）
  - **9847826** data: Plan A v3 callback→Flow adapter + V3 repositories + DI
  - **cdc96b1** ui: 5-Tab Compose screens + AeroTheme tokens + VMs + polling + entry convergence
  - **b9e2ef6** test: unit tests for v3 adapter, repositories, VMs, polling, auth, ipc
  - **<本提交即 C5>** workflow+records: CLAUDE.md + aeroradio-workflow/ + .state/（含本 handover + runtime-demo-checklist）。C5 哈希见 `git log --oneline -5`（本 handover 不能含自身提交哈希）。
- 未提交（有意）：`.idea/*`（IDE 配置）、`.claude/`（gitignored）、`.messages/`（gitignored）、`skill_matlab_addendum.md`（**无关文件**，声学仿真 agent，待 CTO 处置）。

## 7. 5 teammate 当前角色 + 各自下一步
- **data-integration**（紫）：PA-01 done(PASS)。**下一步**：F-2（V3LoginAuthenticator 改走 ServerConfig.setBaseUrl）+ **TaskRepository stub**（接口+stub V3TaskRepository+@Binds 返回空，解 fe 任务 VM 编译）→ compile-only 验 → ping。当前 FROZEN（提交检查点）。
- **fe-business**（绿）：终端 Tab + 任务 UiState 骨架 done。**下一步**：拿 slot 后 ① 接 PollingRefreshScheduler 进终端 VM ② 起任务 Tab VM（接 TaskRepository + de-mock TaskMockData + SchemeTaskStatus 映射）③ 终端 3 屏设备验证。HOLDing。
- **fe-platform**（黄）：PA-02 done(PASS)。**下一步**：BL-GOLD-TOKEN（AeroTheme 补 gold 迁移/对调态 token；ICD-DesignTokens diff 草案应已备）；token 源码改等 slot。HOLDing。
- **legacy-native**（橙）：standby。**下一步**：对讲随广播 Tab 接入（VoiceTalkAdapter→AAR）；可选填窗：VoiceTalkAdapter callback→StateFlow 暴露面对齐 V3CallbackAdapter（走 A，V3CallbackAdapter 已落，现可启动，先发暴露面草案给 PM 转 fe）。真机 R-001=CTO。
- **critic**（蓝）：idle/ready。**下一步**：审 data 的 F-2+TaskRepository stub（核 Hilt 图不破/stub 标注/seam 统一）；真 V3TaskRepository impl 来了大审（SSOT/类型绑定/同步异步/**I-3 双层 state 写操作这次真要处理**/VM 测）。

## 8. 串行链下一个 slot（build-slot 串行协议见 §9）
**提交检查点完 → 解冻 → 下一个 slot 给 data**：
1. **data**：F-2 + TaskRepository stub → compile-only + scoped 验 → ping「stub 落+compile 绿」。
2. **fe-business**：任务 VM + 终端 polling 接线 + 终端验证（拿 slot）。
3. **fe-platform**：BL-GOLD-TOKEN token 源码改（拿 slot）。
之后按 5-Tab 顺序推服务/广播 Tab；广播对讲派 legacy。

## 9. 待 CTO 决策 / 待办 + 关键操作纪律
- **待 CTO**：① `skill_matlab_addendum.md` 无关文件如何处置（删/移走/无视）。② 服务/广播/AI Tab 的 WBS 是否现在排（建议 5-Tab 顺序逐个推）。③ 真机 R-001 终验择机。④ 运行时 demo = **已定**（5-Tab 全完再统一上模拟器，见 runtime-demo-checklist.md）。
- **build-slot 串行协议（关键，别再撞车）**：见记忆 [[gradle-pkill-self-kill-and-build-slot]]。要点：
  - **同时只允许一个 `--no-daemon` build**（并发会损坏构建产物/Kotlin 增量缓存）。给一个 agent「slot」，其余 HOLD **edit+build**（改源码会撞正在跑的 compile）。
  - JBR：`JAVA_HOME=/home/it1234/android-studio/jbr`；**never `--stop`**；验证用 `--no-daemon`。
  - 全量 `:app:testDebugUnitTest` 挂死已修（PollingRefreshSchedulerTest backgroundScope），但仍优先 **scoped --tests + `timeout`**。
  - **`pkill -f testDebugUnitTest` 会自杀脚本**（脚本自己 cmdline 含该串）——别在跑 gradle 的脚本里这么 cleanup。
  - **agent 发 idle ≠ 完成**：它们常跑完测/build 却不发 verdict 就 idle；PM 要去读 `app/build/test-results/.../*.xml` 取地面真相（按 mtime 判新鲜度，frozen log=hung）。
- **其它记忆**：[[plan-a-scope-change]]、[[terminal-wire-vs-domain]]、[[periodic-coroutine-test-no-advanceuntilidle]]、[[scheme-dto-misspelled-field]]、[[consumer-seam-binding-rule]]、[[std-icd-write-pm-serializes]] 等（见 MEMORY.md）。

---
*接手第一步建议：读本文件 + tasks.yaml(plan_a_pivot/pa_tasks) + decision-log D-13/D-14 → 给 data 发「checkpoint done，开 F-2+stub」解冻 → 按 §8 串行链推进。*
