# AeroRadioControl · 交接 & 多 Agent 架构方案

> 给下一个 session 的自包含交接文档。读完这份就能接着干，不需要回看上一段对话。
> 最后更新：v4 屏幕 #6–#10 移植 + 规格对齐之后。

---

## 第 0 部分：30 秒速览

- **项目**：`AeroRadioControl`（校园广播控制），一个**遗留 Android App 的现代化改造**——老的 Java/RxJava/OkHttp 栈 + 新的 Kotlin/Compose v4 UI 并存，逐屏迁移。
- **设计真相源**：仓库根目录 `Handoff.html`（文字规格：§02 设计 token、§03 组件、§787–1092 逐屏规格）。`AeroRadio v4.html` 引用了 `screens-*.jsx` 视觉稿，**但那些 jsx 没进仓库**（缺失）。
- **当前活跃分支**：`claude/v4-screens-on-refactor`（PR #1 → `refactor/arch-foundation`）。已编译通过、已在模拟器跑起来。
- **刚做完**：把 v4 屏幕 #6–#10 从旧分支的设计系统**重写**到 refactor 的设计系统上 + 修了缩放 bug + 5 项规格对齐。
- **下一步大方向**：补齐"大项"（见第 3 部分）、接真实数据、搞定 native/16KB、把验证自动化。考虑上多 agent（第 4 部分）。

---

## 第 1 部分：上下文交接

### 1.1 分支地图
| 分支 | 角色 |
|---|---|
| `main` | 老版主线 |
| `refactor/arch-foundation` | **保留的现代化基线**（Phase 0 基建 + refactor 自己的 Phase B 组件 + manifest/PendingIntent 修复）。所有新东西并到这里。 |
| `claude/festive-carson-2fkGC` | 旧分支，**自带一套独立的 Phase B**（token/组件 API 与 refactor 完全不同）。屏幕已被重写迁走，**此分支的 Phase B 已废弃**，去留待定。 |
| `claude/v4-screens-on-refactor` | **当前工作分支**，PR #1 的来源。 |

### 1.2 这次做了什么（PR #1 内容）
把 `#6–#10` 按 refactor 的设计系统**重写**（不是拷贝——两边 Phase B 差了约 1272/1278 行，是两套不兼容的设计系统）：
- **#6 终端**：`screens/terminal/` → `TerminalHubScreen`（筛选/分区折叠/故障 banner/骨架/多选）、`ZoneDetailScreen`、`TerminalTab`、`TerminalMockData`
- **#7 广播**：`screens/broadcast/BroadcastScreen`（寻呼/对讲/点播分段，按住讲话 + 计时）
- **#8 任务**：`screens/task/` → `TaskScreen`(+`TaskTab` 本地路由)、`SchemeDetailScreen`、`SchemeEditScreen`、`ExecutionLogScreen`、`TempFileBroadcastScreen`、`TaskMockData`
- **#9 AI/服务**：`screens/ai/AiScreen`、`screens/service/ServiceScreen`
- **新通用组件**：`components/molecules/` → `BackTopBar`、`EmptyState`、`Skeleton`、`NotificationBanner`
- **接线**：`MainScaffold` 五个 Tab 接真屏（终端/任务用 **Tab 内本地 state 导航**，不动 NavGraph）
- **修复**：`V4Activity` 实现 `CancelAdapt` → 退出老 AutoSize 的 density 改写（否则 Compose 界面在竖屏被缩成一小坨）
- **规格对齐 5 项**：任务迁移/对调金色、时间轴上午/下午/晚上分段、终端 FAB-bar 三动作(寻呼/对讲/点播)、寻呼计时、AI 示例文案+「去任务」链接、服务联系工程师→拨号

### 1.3 refactor 设计系统 / 架构关键事实（移植时必须遵守）
- **主题入口**：`ui/theme/AeroTheme.kt`，通过 `AeroTheme.colors/typography/shapes/spacing/elevation/motion` 访问。
  - `AeroColors` 字段：`bg, surface, surface2, surface3, ink, ink2, ink3, ink4, line, lineStrong, divider, primary, primaryInk, primarySoft, pageWarm, talkBlue, taskPurple, aiTeal, serviceBlue, statusOnline/Offline/Fault/Playing/Paging(+Soft)`。**没有** onSurface/surfaceVariant/danger/success 这类名字。
  - **没有 `AeroTheme.gradients`**；渐变是独立对象 `AeroGradients.Primary/Warm/Night`。
  - `AeroShapes`: `rCard(12) rTile(16) rChip(999) rInput(12) rSheet`。`AeroSpacing`: `pageH sectionV cardPad tileGap btnPadV btnPadH topBarH tabBarH tabRaise xs sm md lg xl xxl`。`AeroTypography`: `display topBar sectionTitle bodyLarge body bodySmall kicker numeric label button`。
- **组件是 slot API**：`MButton(text,onClick,variant,enabled,leading,trailing)`（变体枚举 `MButtonVariant{Filled,Tonal,Outline,Text,Danger,Success}`），`MChip(label,active,onClick,leading)`，`FabBar(visible,counterLabel,actions:@Composable)`，`TerminalTile(name,status,selected,onClick)`（**无 onLongClick**），`StatusPill(status)`（**无自定义 label**，枚举 `TerminalStatus{Online,Offline,Fault,Playing,Paging}`），`HeroStrip(kicker,title,brush,trailing)`（**无 subtitle**），`MInput(value,onValueChange,label,...)`，`MSwitch(checked,onCheckedChange)`。
- **导航**：`AppNavGraph` 只有 splash→login→main 三个目的地；Tab 切换是 `MainScaffold` 里的 `AeroTab` 本地 state，**没有嵌套 NavGraph**。二级页目前都用 **Tab 内本地 state** 切换。
- **入口**：`V4Activity`（Compose，`setContent{ AeroTheme{ AppNavGraph() } }`），manifest 里有**独立 LAUNCHER**、独立图标名 "AeroRadio v4"，与老 App 并存。

### 1.4 构建 / 运行的坑（已踩过）
- 用 **JDK 17 / Android Studio 自带 JBR**；`compileSdk/targetSdk = 35`，`minSdk 21`。
- 编译：`./gradlew :app:compileDebugKotlin`；装机：`installDebug` + `adb ... am start -n .../.ui.V4Activity`。
- **模拟器镜像要选稳定版**（API 34/35 普通 `x86_64`），**别选 `ps16k`/16KB page size/API 37 预览**——这 App 的旧 native 库（`htapplib.aar` 32 位、百度 SDK 等）未按 16KB 对齐，会装不上/崩。
- native ABI 只有 `armeabi-v7a`/`arm64-v8a`（无 x86）；x86_64 模拟器靠 **arm64 翻译**运行。
- **AutoSize 适配坑**：`screanadaption` 模块会按老横屏设计基准强改 density；任何新 Compose Activity 都要 `implements com.htgd.radiocontrol.screanadaption.CancelAdapt` 退出适配。
- **跨 session 注意**：云端容器是临时的、新 session 重新克隆——**代码必须 commit+push 才能延续**；本地改完要 `git pull` 才能拿到云端 agent 的改动。

---

## 第 2 部分：登录页的设计偏差（专门记一笔）
- 用户的「设计稿」(渐变 hero「欢迎回来」+ 服务器/端口并排 + 渐变登录按钮 + 底部协助文案) 来自 `AeroRadio v4.html` 渲染的 `screens-1.jsx`，**该 jsx 没进仓库**。
- `Handoff.html` 的**文字规格**(§745) 写的登录是「账号+密码+记住我+服务器折叠区」——**恰好就是 refactor 现有的登录页**。即实现忠于文字规格，但与那张更花哨的视觉稿不一致。
- **要精确还原视觉稿,必须先把 `screens-*.jsx` push 上来。** 登录页本身不在 #6–#10 移植范围内。

---

## 第 3 部分：待解决问题（Backlog，按类型）

### 3.1 屏幕规格"大项"（来自对照 Handoff §787–1092 的审计）
- 终端：分区卡 **3 颗动作 icon**(点了跳广播并预选)、展开 **3 列**瓦片(现 2 列)、**长按**进多选(现用「多选」按钮)、**顶部搜索框**、点瓦片进**终端详情页**(新屏)。
- 分区详情：**「开始广播」主按钮**(带分区 ID 跳广播)、成员/设置操作组。
- 广播：对讲的**对端状态/时长/静音/切扬声器**;点播的**音量/队列/循环单次**。
- 任务：顶部**今日日期 + 方案切换**、**长按操作 sheet**(取消/迁移/对调/复制)、作息方案"查看全部"。
- 任务子页：方案详情的**启用开关/日期范围/新增任务按钮**、执行页**逐终端结果+重试**、临时广播的**录音/TTS/优先级**。
- AI/服务：P1 完整流程(AI 迁移/对调/新建作息确认页;售后诊断对话/方案/工单)。

### 3.2 架构 / 数据
- **跨 Tab 导航**决策:继续本地 state 还是上嵌套 NavGraph(很多大项依赖"跳到另一个 Tab 并预选")。
- **真实数据**:目前全是 `TerminalMock`/`TaskMock`,按钮多为空操作。需要 Repository/ViewModel/API(Handoff 各屏列了 `GET /terminals`、`POST /broadcast/start`、`GET /tasks/today` 等契约)。

### 3.3 遗留 / 平台
- **native 库现代化**(Layer 3):16KB page size 对齐、64 位、语音对讲底层。
- 旧分支 `claude/festive-carson-2fkGC` 去留。

### 3.4 工程效率
- **验证自动化缺失**:云端无 Android SDK,编译/跑机/UI QA 全靠人工本地。这是当前最大瓶颈。

---

## 第 4 部分：为本项目量身的多 Agent 架构

> 借鉴你给的参考架构(4 层 + 横切 Critic / 四文件 Agent / 状态机 / 带类型 DAG / 结构化 Findings / 显式 Human Gate / 指标三分法),并**针对本项目的真实痛点裁剪**。
>
> **本项目最该被架构解决的两个痛点**(都是这次踩过的):
> 1. **设计系统分裂**——两条分支各造一套 Phase B,导致大量返工。➜ 必须有**唯一设计系统真相源 + Critic 强制一致性检查**。
> 2. **人工验证是吞吐天花板**——agent 编不了、看不到 UI。➜ 必须有**专职构建/验证执行层 + 云端 CI**,否则并行红利全被人工 QA 吃掉。

### 4.1 分层映射（4 层 + 1 横切）
```
L1 Human (你/CTO)   ── 设计签收、遗留/native 决策、合并 refactor 的批准
L2 Critic (横切)     ── 所有产出进入下一环前的全局守门员
L3 Orchestration + Domains
      Orchestrator/PM ── 调度、DAG、Gate、SLA
      Domain Agents   ── 见 4.2
L4 Execution        ── Build/Verify Agent、Git/Merge Agent
```

### 4.2 Agent 花名册（本项目专属）
| Agent | 层 | 职责 | 关键边界 |
|---|---|---|---|
| **Orchestrator/PM** | L3 | 拆任务、建带类型 DAG、跑状态机、管 Gate/SLA | 只管"谁做、何时做",不碰实现 |
| **Critic** | L2 | 所有产出过审(见 4.4) | 全局唯一质量标准;不做领域设计 |
| **Design-System Steward** | L3 域 | **唯一**能改 `ui/theme/*` 和 `components/*` 的 agent;维护冻结的 token/组件契约 | 任何屏 agent 想加 token/组件必须经它;**这是防"双 Phase B"的关键** |
| **Navigation/Scaffold 域** | L3 域 | **唯一**改 `MainScaffold`/`AppNavGraph`/`AppRoutes`/跨 Tab 导航 | 集成点单一 owner,避免并行冲突 |
| **Screen Agent ×N** | L3 域 | 一屏一个(终端/分区/广播/任务/子页/AI/服务),照冻结契约 + Handoff §规格实现 UI | **禁止**自造 token/组件;有需求提给 Steward |
| **Data/Repository 域** | L3 域 | mock→真实:Repository/ViewModel/API 契约(Handoff 各屏的 API) | 定义接口供屏 agent 依赖 |
| **Legacy/Native 域** | L3 域 | Java 互操作、native 库、16KB/64 位 | 多串行,需 Human 输入 |
| **Build/Verify** | L4 | 编译、lint、(可选)截图测试、跑机;回报结构化结果 | **解吞吐瓶颈的核心**;失败不回流给人 |
| **Git/Merge** | L4 | 分支卫生、合并顺序、冲突上报 | 按 DAG 关键路径合并 |

### 4.3 每个 Agent 的四文件（照抄参考架构）
`profile.md`(岗位:身份/IO/汇报/成功指标) · `soul.md`(性格:驱动力 + **Anti-Patterns 表** + sub-drive 权重) · `skill.md`(工作手册:checklist/模板) · `memory.md`(经验库)。

**本项目专属 Anti-Patterns 示例**(写进对应 soul.md):
- *Screen Agent*：❌ 自己 `Color(0x...)`/新建组件而不用冻结的 `AeroTheme`/`components`；❌ 偏离 Handoff §规格自由发挥；❌ 在 composition 里做阻塞/IO。
- *Design-System Steward*：❌ 为单个屏的特例往全局 token 里塞东西。
- *Navigation 域*：❌ 让屏 agent 各自改 scaffold。
- *PM*：❌ micromanagement。 *Critic*：❌ nitpicking(纠结风格而非正确性)。

**sub-drive 权重示例**：Screen Agent `SPEC_FIDELITY:0.4 / DESIGN_SYSTEM_CONSISTENCY:0.35 / VELOCITY:0.25`——冲突时优先规格与一致性。

### 4.4 Critic 的横切检查（本项目裁剪 + 参考架构的对抗式提问）
**跨域一致性检查(本项目最重要)**:
1. **设计系统合规**:产出是否只引用冻结的 `AeroTheme.*` / `components/*` 符号?有无私造 token/hex/组件?(直接堵死"双 Phase B")
2. **Handoff 规格符合度**:逐项对照对应 §(状态/变体/交互/导航),finding 必须引用 §编号 + `file:line`。
3. **跨屏一致性**:同一概念用同一组件(别 A 屏用 StatusPill、B 屏自画)。
4. **Compose 正确性**:state hoisting、重组、key、无阻塞。
5. **平台护栏**:新 Activity 是否 `CancelAdapt`;碰 native 的是否考虑 16KB/ABI。

**对抗式提问 6 式**(原样搬):反转假设 / 最坏情况 / 替代解释 / 新手视角 / 极端参数 / 时间演化。
**Findings 四级**:BLOCKER/MAJOR/MINOR/INFO,结构化 YAML(`severity/category/location/rationale/recommendation/reference`)。
**核心信念**:Assume Error(默认产出有错直到证否)。

### 4.5 三阶段执行（只有第②阶能并行）
| 阶段 | 并行 | 内容 | 退出 Gate |
|---|---|---|---|
| **① 地基**(串行) | ❌ | Steward 冻结设计系统;Nav 域定跨 Tab 导航契约;Data 域定 Repository/VM 接口;产出 **1 个样板屏** | Critic 通过 + **Human Gate 1:设计/契约签收** |
| **② 屏幕**(高并行) | ✅✅✅ | N 个 Screen Agent 各自分支,照冻结契约 + Handoff §规格实现;每屏:实现→Build/Verify 编译绿→Critic 审→**Human Gate:UI 视觉 QA** | 每屏 Critic PASS + 视觉签收 |
| **③ 集成+真数据**(串行) | ❌ | 按 DAG 合并;接真实 API;对接 Java/native;端到端验证 | **Human Gate:合并 refactor 批准** |

### 4.6 协议工程化（照搬参考架构，加本项目边类型）
- **统一消息信封** + `trace_id`(YAML:`id/type/from/to/timestamp/payload/priority/trace_id`)。
- **任务状态机** + SLA + `AUTO_REJECTED`(超时未审打回) + `ESCALATED`。
- **带类型 DAG** 边:`design_token_dependency`(屏依赖 Steward 冻结)、`nav_contract_dependency`、`data_contract_dependency`、`review_dependency`、`merge_order_dependency`;算关键路径(CPM)定合并序。

### 4.7 显式 Human Gates（本项目）
| Gate | 触发 | 动作 | SLA |
|---|---|---|---|
| G1 契约签收 | 地基阶段完成 | 你确认 token/组件/导航/数据契约 | — |
| G2 视觉 QA | 每屏 Critic PASS 后 | 你跑机截图对设计稿 | 每屏 |
| G3 合并 refactor | 集成阶段 PR 就绪 | 你批准合并 | — |
| Escalation | 设计稿缺失(如 jsx)/native 决策/规格冲突 | agent 停下上报你 | 立即 |

### 4.8 指标三分法（可被评估）
- **吞吐**:`screens_done/sprint`、`revision_cycles_avg`、`sla_breach_rate`。
- **质量**:`first_pass_critic_rate ≥ 70%`、`compile_green_rate`、`design_system_violations`(应趋于 0)、`spec_coverage_per_screen`、`ui_qa_pass_rate`。
- **健康**:`critic_false_positive_rate`、`blocker_detection_rate ≥ 98%`、关键路径阻塞时长。
- 三层学习回路:per_task / per_sprint / per_project 写回各 `memory.md`。

### 4.9 落地顺序建议
1. **先建唯一设计系统真相源**：把 `AeroTheme` token + 组件签名 + Handoff §映射写成一份**只读契约文档**,所有 Screen Agent 引用、不得新增(➜ 直接消灭"双 Phase B"坑)。
2. **先把验证自动化**:在云端环境装一次 Android SDK,让 Build/Verify Agent 能跑 `:app:compileDebugKotlin`(我之前在容器里能编 Kotlin,只缺 SDK);UI 截图可接模拟器/设备农场。否则②的并行红利会被人工 QA 吃光。
3. 把 `screens-*.jsx` 设计稿 push 上来,Critic 的"视觉符合度"才有像素级依据。
4. 再按 4.5 三阶段开工。

---

## 附：给新 session 的第一条指令建议
> "读 `docs/AGENT_TEAM_AND_HANDOFF.md`。当前在 `claude/v4-screens-on-refactor`(PR #1)。先确认 refactor 设计系统契约(§1.3)、再从第 3 部分 backlog 里选 X 开工;改完 commit+push,并提醒我 pull 编译验证。"
