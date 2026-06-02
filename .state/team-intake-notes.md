# Team Intake Notes — 团队组建首轮就绪输入

> 维护者：PM Agent · 记录于 2026-05-27（Agent Teams 模式激活当日）
> 用途：5 个 teammate 就绪确认时提出的前瞻性依赖 / 风险 / ICD 诉求，作为 **Phase 0 WBS + DAG + 风险登记册** 的输入。Phase 0 WBS 按 CTO D-01 暂停，待 SPIKE-001 结论后据此一并定稿。**本文件是 backlog 输入，不是已派任务。**

---

## 1. 跨域共识：ICD-Endpoints-v1 必须先于任何迁移任务定稿（最高优先）

`critic` 与 `data-integration` 各自独立强烈提出，指向 RISK-AUDIT-03 / D-04：

- 端点权威清单当前仅是 `.state/endpoint-inventory-draft.md` 草案，未落 `references/icd-contracts.md` 成权威 ICD。
- ICD 最小单元须为 **「路径 × HTTP 方法」**（"21" 是路径口径，按方法展开 ~40+ 操作）。
- `data-integration` 列出迁移前必须由后端/设计回答的 **5 个硬问题**（draft §4.1）：
  1. 同路径多方法的 Request/Response DTO 是否各异；
  2. 是否有统一响应包络 `code/message/data`；
  3. 成功判定 = 业务码 (`EorroCode.SUCESS`) 还是 HTTP 2xx —— 决定拦截器与 Repository 错误映射；
  4. `{id}`/`{type}` 是路径参数还是 query；
  5. multipart 字段名（`mediafile` 等）。
- **PM 行动**：将 ICD-Endpoints-v1 定稿作为 Gate 1 计划的一部分；上述 5 问与 spike 结论合并后一并向 CTO/后端落实。否则 Data-Integration 迁移与 Critic 验收对不齐，产生 ICD churn。

## 2. DAG 排序约束（Phase 0 → Phase 1）

来自 `fe-business` / `fe-platform` / `data-integration`，三方一致指向同一条关键链：

```
DynamicBaseUrlInterceptor + AuthStore (data-integration, Phase 0 两个 P0, 现真空)
   └─D→ 单例 OkHttpClient 全局复用 (含旧栈 RequestManger, §7.3 legacy_impact)
        ├─D→ Repository (TerminalRepository / TaskRepository ...)  (data-integration)
        │      └─D→ 各 Tab ViewModel→真数据 (fe-business; 现 0/5 接真, 无 ViewModel 层)
        └─D→ RealtimeClient WS 客户端 (fe-platform; connect() 需 token+serverAddr)
               └─D→ ConnectionBanner / 实时刷新
```

- `fe-business`：在 data P0 落地前，可承接「不接数据」的准备工作——5 态(loading/empty/error/success/partial)草图、组件抽取、NavGraph 路由骨架。接真数据时须清掉 `TerminalMockData.kt`/`TaskMockData.kt` 及各屏内联 mock（反模式 "Mock Data Forever"）。
- `fe-platform`：WS 任务应排在 AuthStore 之后；`@Singleton` + 指数退避(2/4/8/16/max60s) + 前后台感知内建（RISK-AR-004）。

## 3. 待向厂商索要（沿用 R-002 / RISK-AR-006）

- `fe-platform`：ICD-BroadcastWS-v1 仍 DRAFT，WS endpoint URL / 心跳间隔 / 鉴权方式 / 消息字段名四项全未经厂商验证。文档到位前只能按 DRAFT 假设（`ws://host:port/ws?token=`、ping/pong）做接口骨架，落地后须改并广播 ICD_UPDATE。
- **PM 行动**：Phase 0 即通过 CTO 向厂商索要 WS 协议文档（已是 R-002 缓解项，此处确认 owner=fe-platform）。

## 4. 跨域边界待确认（建 WBS 任务时钉死归属）

- **BL-LAUNCHER-FIX**（D-03）：`fe-business` 候选承接。但 `SignActivity`（旧）仍持有旧 `utils/SocketClient.java`（IPC 4521）。
  - 纯改 Manifest LAUNCHER 标签 → fe-business 可独立承接；
  - 若需把 SignActivity 内部跳转/IPC 逻辑迁到 Compose 侧 → 需 `legacy-native` 协同评估（IPC 现代化属其域）。
  - **PM 行动**：建任务时拆成「Manifest 入口收敛(fe-business)」与「IPC 4521 协程化(legacy-native, 原 AR-WP01-IPC)」两件，明确依赖。
- **method/MainMethod.java**：`data-integration` 确认其为 native 语音入口（`HTIntf.startpaging/startspeech/startondemand`），**不属 REST 迁移范围**，归 `legacy-native`。广播 Tab 的 REST 需求仅为目标选择(`/terminal/terminalinfo`、`/terminal/zoneterminal`) + 紧急播放(`/terminal/urgentplay`)，音频通道走 AAR。

## 5. SPIKE-001 环境约束（已登记，见 tasks.yaml + 给 CTO 的 decision point）

- 执行环境 = x86_64 主机、无 arm64 真机、无 adb。验收 ②③（loadLibrary 成功性、真机调用链）本环境无法动态验证；静态项 ①④⑤⑧ + javap 接口签名可完整完成。
- PM 已批准 **GO-pending-device-confirmation** 定位。真机终验是 R-001 收尾至 LOW 的残留前置。
- **CTO 决策点（非阻塞）**：是否调度一台 arm64 真机 / CI runner 做终验，或接受 GO-pending 并在 Gate 评审时补真机确认。待静态报告到手后决定。

---

## Critic 预置的 SPIKE-001 对抗式验收门槛（评审时套用）

`critic` 已预声明审 spike 报告的硬门槛，PM 已转达 legacy-native 提前对齐：
1. 「符号存在 / loadLibrary 不崩」≠「可用」——须区分"静态自洽"与"真机已验"。
2. 链接性闭包：arm64-v8a 两个 .so 不得悬空依赖仅 32 位提供的库 / 未解析系统符号（readelf -d DT_NEEDED 逐条）。
3. 样本代表性边界须显式标注（一台真机 ≠ 全量；x86_64 残留=开发期影响非发布阻塞）。
4. abiFilters PR：armeabi-v7a 不得移除（兜 32 位真机）；64 位-only 设备对讲降级 UI 路径保留。
