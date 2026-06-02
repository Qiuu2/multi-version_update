# DELIVERABLE — TASK-AR-009 设计 Token 校准核对 (v2)

> 作者：Frontend-Platform Agent · 日期：2026-05-27 · 分支：`claude/v4-screens-on-refactor`
> 任务定性：**校准/补缺/纠偏**，非重写（8 个 theme 文件审计 §4.2 已证落地）。
> 基准：`aeroradio-workflow/references/design-system-spec.md`（强制对齐基准）+ `icd-contracts.md` §9（ICD-DesignTokens-v1.1）。
> 范围文件（实际包路径 `com/htgd/radiocontrol/aeroradiocontrol/ui/theme/`，**非** spec 模板写的 `com/aeroradio/`）：
> `Color.kt · Shape.kt · Spacing.kt · Type.kt · Elevation.kt · Motion.kt · Gradients.kt · AeroTheme.kt`

---

## R0. v2 修订记录（回应 Critic DEL-v1 FAILED 的 F-1/F-2 + 落 PM R-1~R-5 裁定）

### R0.1 Critic FAILED 两点 — 已修

- **F-1（MAJOR · CRLF 污染）已修**：ui/theme 下 8 个文件 HEAD 上**原本全是 LF**（`git cat-file -p HEAD:<f> | grep -c $'\r'` 全 = 0），我上轮 Edit 会话把**全部 8 个翻成了 CRLF**——含 7 个我只 Read 未做语义改动的文件。已 `sed -i 's/\r$//'` 全部恢复 LF，逐文件 `diff <(git cat-file -p HEAD:<f>) <f>` 验证：除 Color/Elevation/Type 外 5 个**字节级 IDENTICAL**。最终 `git diff --name-only` = 仅 3 个有真实改动文件，全部 CRLF=0。
- **F-2（MINOR · 自陈失真）已纠 + 致歉**：v1 我写「Type.kt 与同目录 Color/Shape.kt 一致本就是 CRLF（非本次引入）」——**此陈述与事实不符**。真相：HEAD 8 文件全 LF，CRLF 是**本轮 Edit 误引入**，非既有。我当时把「工作树状态」误当成「HEAD 基线」，未做 `git cat-file` 复核就下结论。这违反 soul「证据为王」。**更正后事实**：HEAD = LF；本轮 Edit 误转 CRLF；已恢复 LF。后续凡涉及"既有 vs 本次引入"的判断，一律先 `git cat-file`/`git diff` 取证再陈述。

### R0.2 PM R-1~R-5 裁定落地（均 PM 自决，无需 CTO）

| 裁定 | 落地 |
|------|------|
| **R-1**（命名偏离，非破坏路径） | 落地名**不改**（避免 breaking）；`icd-contracts.md` §9.1 新增**别名映射表**（落地名↔spec 权威名，含 numeric→MetricNum）；Phase 3 真改名登记为 backlog `BL-TOKEN-RENAME`（待 CTO 批的 breaking 条目） |
| **R-2**（bgBeige） | 查证 Handoff：`AeroRadio v4.html` line 11(`body bg:#f0eee9`)+line 200(token「米色背景」)**确用** → 补进 spec §1.1 `--bg-beige` + Color.kt `BgBeige`/`bgBeige` 字段 + ICD §9.2。spec=ICD=Handoff 一致。 |
| **R-3**（派生 token） | **补录不移除**；两条件均满足：①每个 *Soft 标注派生来源（对应 status 基色同色相浅底，line/lineStrong=Ink alpha，注释已加）；②同步登记 spec **新增 §1.5** + ICD §9.2。降 INFO。 |
| **R-4**（elevation 4/12 vs 2/8） | 无 Handoff 真机渲染反证 → **改为 2/8 对齐 spec §5**；Elevation.kt 注释说明「track M3 dp 等价值而非 CSS blur」。 |
| **R-5**（字体占位） | 登记 Phase 3 轻量子任务 backlog `BL-FONT-ASSETS`（Noto Sans SC + JetBrains Mono，OFL 开源无 licensing escalation，G4 前必落）；ICD §9.3 记占位状态。 |

### R0.3 v2 改动文件清单

- **code（3 文件，全 LF，`:app:compileDebugKotlin` EXIT=0）**：`Type.kt`（C-1 tnum + C-2 weight400，v1 已改）、`Color.kt`（R-2 bgBeige + R-3 派生注释）、`Elevation.kt`（R-4 e2→2/e3→8）。
- **doc**：`design-system-spec.md`（R-2 §1.1 / R-3 §1.5）、`icd-contracts.md`（§1 表升 v1.1 + §9 重写：9.1 别名映射 / 9.2 完整清单 / 9.3 字体状态）。
- ICD-DesignTokens-v1→v1.1：**ICD_UPDATE 候选**（含 R-1 别名，**非破坏**）；按 soul 我不擅自广播，发 PM 由其广播 frontend-business。

> 以下 §1-§6 为 v1 分析底稿，保留备查；R-1~R-5 的「我未擅自改」表述已被 R0.2 裁定取代（现已按裁定落地）。

## 0. 结论一句话

现有 8 文件**架构正确、整体高保真**（CompositionLocal 注入 + data-class token + M3 桥接，符合 spec §10 与 soul「token 通过 AeroTheme 注入，禁 hardcode」）。校准发现 **2 处真实 spec 偏离已直接修正（code）**，**5 处需 PM/Critic 决策的偏差或补缺（不擅自改，列建议）**，**3 处为已知可接受近似（记录在案）**。`:app:compileDebugKotlin` 通过（EXIT=0）。

---

## 1. 已直接修正（code，无歧义的 spec 偏离）

| # | 文件:行 | spec 依据 | 偏离 | 修正 |
|---|---------|----------|------|------|
| C-1 | `Type.kt` `numeric` | §4 大数字「22sp 700 mono **+ tabular-nums**」+ §9.3「数字是否 mono + tnum？」+ skill 模板 `fontFeatureSettings="tnum"` | `numeric` 仅 mono+Bold，**缺 tnum 显式声明** | 加 `fontFeatureSettings = "tnum"`。理由：等宽族已固定字宽，但 spec 明确要求 tnum 特性；显式声明可在 [AeroMono] 未来换成带比例数字的 fallback 时保持稳健。**已验证 compose-ui-text 1.7.5 支持该参数，编译通过。** |
| C-2 | `Type.kt` `kicker`(=Label) | §4 标签行「11sp **400** mono / 0.6 / UPPERCASE」 | 原 weight=Medium(500)，spec 为 400 | 改为 `FontWeight.Normal`(400)，对齐 spec。letter-spacing 0.6 已正确。 |

> 说明：UPPERCASE（大写）属**用法语义**，不能在 TextStyle 强制（需调用方 `.uppercase()` 或 `textTransform`）。已在变更说明中提示 Frontend-Business：用 `kicker` 渲染标签时需自行大写化。**此为给 FE-Business 的下游约定，建议 PM 在 ICD-DesignTokens-v1 备注。**

---

## 2. 需 PM/Critic 决策（偏差或补缺，未擅自改——涉及公开 token API / 跨域消费 / soul「不做创造性扩展」）

| # | 严重度 | 文件 | spec 依据 | 现状 vs spec | 我的建议 | 为何不擅自改 |
|---|:--:|------|----------|-------------|---------|------------|
| **R-1** | **MAJOR** | `Color.kt` | §1.3 Tab/Mode token 命名：`TabTerminal/TabBroadcast/TabAI/TabTask/TabService` + `ModePage/ModeTalk/ModeCast` | 现命名为 `pageWarm/talkBlue/taskPurple/aiTeal/serviceBlue`——**语义齐全但命名与 spec/ICD §9 token 名不一致**；且**无独立 `tabTerminal`**（终端 Tab 色 #0E7C70 复用了 `primary`），**无独立 `modeCast`**（点播色 #0E7C70 同样复用 primary） | 二选一供 CTO/Critic 定：①**改名对齐 spec**（破坏性：业务屏在引用这些 token，需 ICD_UPDATE + 迁移窗）；②**保留现名，在 ICD-DesignTokens-v1 登记别名映射表**（非破坏，但 spec 命名契约打折）。我倾向 ② 短期 + ① 排进 Phase 3 打磨。 | 改名 = breaking change（业务屏消费），protocol §7.1 禁私下；soul §2.3「ICD breaking 必广播」 |
| **R-2** | MINOR | `Color.kt` | `icd-contracts.md` §9 列 `bgBeige #F0EEE9`；skill 模板亦有 `BgBeige` | **缺 `bgBeige` token**（米色背景，Handoff 用于特定屏） | 补 1 个 token `bgBeige = Color(0xFFF0EEE9)`。**但 design-system-spec.md 正文未列 bgBeige**（只在 ICD §9 与 skill 出现）——属 spec 与 ICD 不一致，需先由 PM 裁定 bgBeige 是否纳入正式 token 集。 | spec 正文无定义；soul「未在 spec 里的 token 不主动添加，需走 PM 流程」 |
| **R-3** | MINOR | `Color.kt` | spec §1 仅定义到 §1.4 | **新增了 spec 外 token**：`line / lineStrong / divider` + 5 个 `*Soft` 状态浅底（StatusOnlineSoft 等） | 这些是合理的派生 token（StatusPill 浅底/分隔线确有用），但属 soul 禁止的「创造性扩展」。建议 PM **批准后补录进 ICD-DesignTokens-v1**（作为 v1.1 扩展），或要求移除改由调用方 alpha 合成。我倾向批准补录——它们已被组件消费，移除是倒退。 | soul §2.2「不做创造性扩展」；需 PM 决策是否正式纳入契约 |
| **R-4** | MINOR | `Elevation.kt` | §5 Compose 等价块建议 `CardHover=2 / Modal=8` | `e2=4`（建议 2）、`e3=12`（建议 8）——现值更贴 CSS px blur，偏离 spec 的**Compose 等价建议值** | 二选一：①对齐 spec 建议（e2→2, e3→8）；②保留现值并在注释标注「按 CSS blur 半径取值，非 M3 dp 建议值」。Compose 的 dp elevation 与 CSS box-shadow 本就不可逐像素对应，**我倾向 ② + 注释**，但请 Critic 裁定是否算 DESIGN_DEVIATION。 | spec §5 给的是「建议」而非硬值，定性模糊，交 Critic |
| **R-5** | INFO | `Type.kt` | §4「中文 Noto Sans SC / 数字 JetBrains Mono」 | 现用 `FontFamily.Default` / `FontFamily.Monospace` **占位**（代码注释已说明，res/font/ 为空，实扫确认无字体资源） | 需把 `noto_sans_sc` + `jetbrains_mono` 字体文件放入 `res/font/` 后替换。**这是字体资产交付缺口**，非 token 逻辑错误。建议 PM 排一个轻量子任务（拿字体 → 落 res/font → 改 2 行 AeroSans/AeroMono），或并入 Phase 3 打磨。 | 缺物料（字体文件），非我能在本任务内补 |

---

## 3. 已核对一致 / 已知可接受近似（记录在案，无需动作）

| 项 | spec | 现状 | 判定 |
|----|------|------|------|
| Shape 全套 | §2 rCard12/rTile16/rChip999/rSheet24顶/rInput12 | **逐项完全一致** | ✅ PASS |
| Spacing | §3 pageH 14-16 / cardPad 12-14 / tileGap10 / btn 10×18 / topBar~56 / tabBar80 / 抬起+16 | pageH=16、cardPad=14（均落在 spec 区间）、其余精确匹配 | ✅ PASS |
| Type display/topBar/section/body 系列 | §4 32/-0.5·22/-0.2·18·15/500·14·13 | 逐项匹配（含负 letter-spacing） | ✅ PASS |
| Gradients | §1.4 grad-primary/warm/night 三套色停 | 三套色停与角度方向一致（primary 0/.6/1、warm 三色、night 纵向 0/.8/1） | ✅ PASS（warm 用等距 colors 列表 vs spec 未标停点，等效） |
| Motion 时长 | §6 fabIn220/badge2200/pulse1500/wave900/skel1600/tap120-150 | 220/2200/1500/900/1600/130 **逐项匹配**；emphasizedEasing=(.2,.7,.3,1) 与 spec 曲线一致 | ✅ PASS |
| `standardEasing` 新增 | spec 未列 | data-class 多了 standardEasing(.2,0,0,1) | 可接受近似：M3 standard 曲线，非装饰性偏离，保留 |
| AeroTheme 组合 | §10「唯一根 Composable + CompositionLocal 注入」 | 6 个 LocalAero* + M3 colorScheme/typography/shapes 桥接 + `AeroTheme.xxx` 访问器 | ✅ PASS（架构优于 skill 模板：token 全走 CompositionLocal） |
| §9.5 prefers-reduced-motion | checklist 项 | Motion token 未含 reduced-motion 开关 | INFO：Compose 侧需 `Settings.Global.ANIMATOR_DURATION_SCALE` 感知，属组件层而非 token 层，记给 FE-Business/未来 a11y 任务 |

---

## 4. ICD-DesignTokens-v1 更新提案（待 PM 决策后落 references/icd-contracts.md §9）

依赖上面 R-1~R-3 的裁定，ICD §9 当前只列了 4 个 color + 3 shape + 2 elevation 的**速查子集**，与实际落地的完整 token 集（Color 30+ / Shape 5 / Spacing 16 / Type 9 / Elevation 4 / Motion 6+2 easing）**严重不同步**。建议：
- §9 速查表升级为**完整 token 清单 + Compose 引用名**（与本次校准后的 8 文件 1:1）。
- 钉死 R-1 的命名口径（spec 名 vs 现名映射）。
- R-2/R-3 决策后增删对应 token 行。
- 标注 R-5 字体占位状态（`Default/Monospace` → 待 Noto Sans SC / JetBrains Mono）。

> 此 ICD 更新本身是 ICD_UPDATE 候选；**待 PM 批准 R-1~R-3 方向后**，我出 ICD-DesignTokens-v1 修订版（若含 R-1 改名则为 breaking，走迁移窗 + 广播 frontend-business）。

---

## 5. 自评

- completeness: 90%（code 修正 2 项已落地+编译通过；R-1~R-5 给出建议待裁，未擅自越权改公开 API/补 spec 外 token——符合 soul 与 protocol §7.1/§7.2）
- confidence: 85%
- self_checked: true（逐文件对 spec §1-§6 + §9 checklist 核对）
- tested: 编译 ✅（`:app:compileDebugKotlin` EXIT=0，JBR-17）；Preview 渲染未跑（无 GUI 环境，建议 Critic 或 FE-Business 侧 Compose Preview 抽验 numeric/kicker 视觉）
- known_issues: R-1 命名偏离若 CTO 选「改名」将是 breaking，需单独迁移任务；R-5 字体仍占位

## 6. 给 PM 的待决问题（阻挡 ICD-DesignTokens-v1 定稿）

1. R-1 Tab/Mode 色 token **改名对齐 spec** 还是 **保留现名+别名映射**？（影响是否 breaking + 是否排迁移任务）
2. R-2 `bgBeige` 是否纳入正式 token 集？（spec 正文无、ICD §9 有——先统一 spec 与 ICD）
3. R-3 spec 外的派生 token（line/divider/*Soft）**批准补录 ICD** 还是要求移除？
4. R-4 Elevation e2/e3 取 **spec 建议 dp 值** 还是 **保留 CSS blur 近似值 + 注释**？（交 Critic 定性是否算 DESIGN_DEVIATION）
5. R-5 字体资产（Noto Sans SC / JetBrains Mono）由谁提供、何时落 res/font？

---

*del-AR-009-token-calibration.md — Frontend-Platform Agent · 待 Critic 评审 → CTO。code 修正见 Type.kt diff。*
