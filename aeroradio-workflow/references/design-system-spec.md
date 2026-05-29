# Design System Specification

AeroRadioControl v4 设计规范，从 `Handoff.html` 提炼。本文档是所有 Frontend Agent 的**强制对齐基准**，Critic Agent 用本文档检测设计偏离。

> **核心约定**：实现时**用 token 命名引用，不要 hardcode 颜色 / 数值**。

---

## 1. Color Tokens

### 1.1 中性色（Surfaces & Ink）

| Token | Hex | 用途 | Compose 引用 |
|-------|-----|------|-------------|
| `--bg` | `#F4F5F7` | 页面背景 | `AeroColors.Background` |
| `--surface` | `#FFFFFF` | 卡片表面 | `AeroColors.Surface` |
| `--surface-2` | `#F8FAFB` | 次表面（嵌套卡片） | `AeroColors.Surface2` |
| `--surface-3` | `#EEF0F3` | 第三层（边框分隔） | `AeroColors.Surface3` |
| `--ink` | `#0D1117` | 主文字 | `AeroColors.Ink` |
| `--ink-2` | `#4A5260` | 次文字 | `AeroColors.Ink2` |
| `--ink-3` | `#8A929F` | 辅助文字 | `AeroColors.Ink3` |
| `--ink-4` | `#B8BEC8` | 弱化文字 | `AeroColors.Ink4` |
| `--bg-beige` | `#F0EEE9` | 米色背景（特定屏 / Splash） | `AeroColors.bgBeige` |

> `--bg-beige` 来源：`AeroRadio v4.html` line 11（`body background:#f0eee9`）+ line 200 token 列表「米色背景」。R-2 裁定：Handoff 确实使用 → 纳入正式 token，spec↔ICD 一致。

### 1.2 品牌与状态色

| Token | Hex | 用途 | Compose 引用 |
|-------|-----|------|-------------|
| `--primary` | `#0E7C70` | 品牌主色（Teal） | `AeroColors.Primary` |
| `--primary-ink` | `#095C54` | 主色深色（数字强调） | `AeroColors.PrimaryInk` |
| `--primary-soft` | `#E6F4F2` | 主色浅色（选中底色） | `AeroColors.PrimarySoft` |
| `--status-online` | `#16A34A` | 终端在线（绿） | `AeroColors.StatusOnline` |
| `--status-offline` | `#8A929F` | 终端离线（灰） | `AeroColors.StatusOffline` |
| `--status-fault` | `#DC2626` | 终端故障（红） | `AeroColors.StatusFault` |
| `--status-playing` | `#2563EB` | 终端播放中（蓝） | `AeroColors.StatusPlaying` |
| `--status-paging` | `#EA580C` | 终端寻呼中（橙） | `AeroColors.StatusPaging` |

### 1.3 模式色（Mode Colors）

5 个一级 Tab 的标识色，用于 TabBar 高亮、模式切换、Section 强调：

| Tab | Hex | 含义 | Compose 引用 |
|-----|-----|------|-------------|
| 终端 | `#0E7C70` | Teal 主色 | `AeroColors.TabTerminal` |
| 广播 | `#EA580C` | 寻呼橙（容器默认色） | `AeroColors.TabBroadcast` |
| AI | `#14B8A6` | 青绿 | `AeroColors.TabAI` |
| 任务 | `#7C3AED` | 紫 | `AeroColors.TabTask` |
| 服务 | `#2563EB` | 蓝 | `AeroColors.TabService` |

**广播 Tab 内三档子模式色**：

| Mode | Hex | Compose 引用 |
|------|-----|-------------|
| 寻呼（page） | `#EA580C` | `AeroColors.ModePage` |
| 对讲（talk） | `#2563EB` | `AeroColors.ModeTalk` |
| 点播（cast） | `#0E7C70` | `AeroColors.ModeCast` |

### 1.4 渐变（仅用于装饰）

| Token | 定义 | 用途 |
|-------|------|------|
| `--grad-primary` | `linear-gradient(135deg, #0E7C70 0%, #14B8A6 50%, #06B6D4 100%)` | FAB / hero / 抬起 Tab |
| `--grad-warm` | `linear-gradient(135deg, #EA580C, #F97316, #FB923C)` | 寻呼模式 |
| `--grad-night` | `linear-gradient(180deg, #0D2826 0%, #0E7C70 80%, #14B8A6 100%)` | AI / 全屏来电 |

> **使用规则**：渐变仅用于装饰元素（按钮、Hero、Tab）。**禁止**用于正文背景、卡片背景、列表项。

### 1.5 派生 / 工具色（R-3 — 已落地并补录，非新色相）

> 这些 token 是 §1.1-1.2 既有色的浅色派生 / alpha 派生，已被组件（StatusPill / 分隔线）消费。R-3 裁定：补录 spec + ICD（不移除），**条件：每项必须是既有色的派生，不得引入新色相**。状态浅底逐一为对应 status 基色的同色相浅色。

| Token | Hex | 派生来源 | 用途 | Compose 引用 |
|-------|-----|---------|------|-------------|
| `--line` | `rgba(13,17,23,0.06)` | `--ink` @ 6% alpha | 卡片描边 / 弱分隔 | `AeroColors.line` |
| `--line-strong` | `rgba(13,17,23,0.12)` | `--ink` @ 12% alpha | 强分隔 / 输入框边 | `AeroColors.lineStrong` |
| `--divider` | `#E8EBEF` | 中性发丝线（介于 `--bg` 与 `--surface-3`） | 列表分隔线 | `AeroColors.divider` |
| `--status-online-soft` | `#E8F7EC` | `--status-online` 浅底 | 在线 pill 底 | `AeroColors.statusOnlineSoft` |
| `--status-offline-soft` | `#EEF0F3` | `--status-offline` 浅底（= `--surface-3`） | 离线 pill 底 | `AeroColors.statusOfflineSoft` |
| `--status-fault-soft` | `#FDECEC` | `--status-fault` 浅底 | 故障 pill 底 | `AeroColors.statusFaultSoft` |
| `--status-playing-soft` | `#E8EFFD` | `--status-playing` 浅底 | 播放 pill 底 | `AeroColors.statusPlayingSoft` |
| `--status-paging-soft` | `#FDEEE2` | `--status-paging` 浅底 | 寻呼 pill 底 | `AeroColors.statusPagingSoft` |

---

## 2. Shape Tokens

| Token | 值 | 用途 | Compose 引用 |
|-------|----|------|-------------|
| `--r-card` | `12.dp` | 卡片 | `AeroShapes.Card` |
| `--r-tile` | `16.dp` | 瓦片（终端网格） | `AeroShapes.Tile` |
| `--r-chip` | `999.dp` | 胶囊 / 按钮 | `AeroShapes.Chip` |
| `--r-sheet` | `24.dp`（仅顶部圆角） | 底部 Sheet | `AeroShapes.Sheet` |
| `--r-input` | `12.dp` | 输入框 | `AeroShapes.Input` |

---

## 3. Spacing Tokens (8px base)

| 名称 | 值 | 用途 |
|------|----|------|
| 页面左右内边距 | `14-16.dp` | 主区域与屏幕边缘 |
| 卡片内边距 | `12-14.dp` | 瓦片 / 列表 item |
| 区段顶部间距 | `12.dp` | section label 与上一块的距离 |
| 瓦片间距 (gap) | `10.dp` | 3 列终端网格 |
| 按钮内边距 | `10×18.dp` | 填充按钮垂直×水平 |
| TopBar 高度 | `~56.dp` | padding 14/16/12，标题 22pt |
| TabBar 高度 | `80.dp` | 含安全区，中间抬起 +16.dp |

---

## 4. Typography

字体族：
- 中文 / 拉丁：**Noto Sans SC**
- 数字 / 等宽：**JetBrains Mono**（数字必须用等宽，避免跳动）

| 用途 | 字号 | Weight | letter-spacing | 引用 |
|------|------|--------|----------------|------|
| 显示标题 | 32sp | 600 | -0.5 | `AeroType.Display` |
| TopBar 标题 | 22sp | 600 | -0.2 | `AeroType.TopBarTitle` |
| 分区标题 | 18sp | 600 | 0 | `AeroType.SectionTitle` |
| 正文（输入框 / 列表） | 15sp | 500 | 0 | `AeroType.BodyMedium` |
| 正文 | 14sp | 400 | 0 | `AeroType.Body` |
| 次要文字 | 13sp | 400 | 0 (ink-2) | `AeroType.Secondary` |
| 标签（大写） | 11sp | 400 mono | 0.6, UPPERCASE | `AeroType.Label` |
| 大数字（统计） | 22sp | 700 mono | tabular-nums | `AeroType.MetricNum` |

> **数字规则**：**所有数字（计数、时间、温度、SPL、统计）一律用等宽 mono + tnum**，避免跳动。

---

## 5. Elevation / Shadow

| Token | Shadow 值 | 用途 |
|-------|----------|------|
| `--e1` | `0 1px 2px rgba(13,17,23,0.04), 0 1px 1px rgba(13,17,23,0.06)` | 卡片静态 |
| `--e2` | `0 2px 4px rgba(13,17,23,0.04), 0 4px 12px rgba(13,17,23,0.06)` | 卡片悬浮 |
| `--e3` | `0 4px 12px rgba(13,17,23,0.08), 0 12px 32px rgba(13,17,23,0.10)` | 模态 / Sheet |
| `--e-fab` | `0 4px 16px rgba(14,124,112,0.32), 0 2px 4px rgba(14,124,112,0.18)` | FAB（带 primary 染色） |

Compose 等价（M3）：

```kotlin
object AeroElevation {
    val Card = 1.dp        // ≈ e1
    val CardHover = 2.dp   // ≈ e2
    val Modal = 8.dp       // ≈ e3
    val FAB = 6.dp         // ≈ e-fab
}
```

---

## 6. Motion / Animation

| 名称 | 时长 / 曲线 | 用途 |
|------|------------|------|
| fabIn | `220ms · cubic-bezier(.2,.7,.3,1)` | 多选弹出 FAB-bar / 底部动作条 |
| tabBadgePulse | `2200ms · ease-out · infinite` | 红点角标呼吸 |
| mPulse | `scale 0.6 → 1.6 · 1500ms` | 呼叫 / 寻呼中扩散圆 |
| wave | `scaleY 0.5 → 1 · 900ms` | 4 条柱状播放波形 |
| skel | `1600ms · linear` | 加载占位扫光 |
| tap / hover | `120-150ms · ease` | chip / tile / btn 状态切换 |

Compose 等价（部分）：

```kotlin
object AeroMotion {
    val FabIn = tween<Float>(220, easing = CubicBezierEasing(.2f, .7f, .3f, 1f))
    val TapHover = tween<Float>(150, easing = FastOutSlowInEasing)
    val Skel = infiniteRepeatable<Float>(
        tween(1600, easing = LinearEasing),
        RepeatMode.Restart
    )
}
```

---

## 7. Component Anatomy

### 7.1 终端卡片 5 态

| 状态 | 视觉 | 状态色 | 关键元素 |
|------|------|--------|---------|
| 默认（在线空闲） | 白底 + 绿点 | `status-online` | 终端名 + IP + 在线点 |
| 离线 | 灰底 + 灰文字 + 不可选 | `status-offline` | 名 + "离线 N 分钟" |
| 寻呼中 | 橙边框 + 扩散圆动画 | `status-paging` | 名 + "寻呼中" + 持续时长 |
| 对讲中 | 蓝边框 | `mode-talk` | 名 + 通话头像 + 时长 |
| 点播中 | Teal 边框 + 波形动画 | `mode-cast` | 名 + 媒体名 + 波形 |
| 故障 | 黄警告图标 | `status-fault` | 名 + 错误码 |
| 选中 | 主色描边 + 主色浅底 | `primary` + `primary-soft` | 多选模式下 |

### 7.2 TabBar（5 Tab）

- 高度：`80.dp`（含安全区）
- 中间 AI Tab **抬起 +16.dp**，使用 `--grad-primary` 渐变
- 角标：8.dp 圆点 + `tabBadgePulse` 动画
- 当前 Tab：图标 + 文字着 Tab 标识色

### 7.3 顶部 Segmented Control（广播 Tab 三档）

- 容器：`r-chip` 胶囊形
- 选中档：填充对应 mode 色 + 白色文字
- 未选中档：透明底 + ink-2 文字
- 切换动画：`tap / hover` 150ms

---

## 8. 实时性 / 错误处理 / 离线（Handoff 第 6 节）

### 8.1 实时性策略

| 数据 | 策略 |
|------|------|
| 终端状态 (state/playing/paging) | **WebSocket push**，回退 10s 轮询。断线时顶部出黄色 banner "实时已断开"。 |
| 任务进度 | 会话内 WS 持续推送；会话结束后改 30s 轮询。 |
| 工单状态 | 15s 轮询足够；进入详情页改 5s。 |
| 媒体库 / 作息 | 按需 fetch + 本地缓存（ETag）。 |

### 8.2 错误处理

| 层级 | 表现 | 恢复 |
|------|------|------|
| 致命（无网络） | 顶部红 banner + 缓存数据加灰 | 自动重试 + 手动 "重新连接" |
| 请求失败 | Toast 3s + 失败原因 | 动作按钮自动恢复可点 |
| 部分终端失败（广播） | 结果卡列出失败终端 | "重试失败项" 按钮 |
| 表单校验 | 输入框红边 + 下方提示 | 用户修正后实时校验 |

### 8.3 离线策略

> 本应用**严重依赖在线**（广播 / 对讲 / 点播都需要联网）。

离线时：
- 所有"动作类"按钮 disabled，给 "离线，不可用" 提示
- 仍可**查看缓存的**：终端列表（标灰 + 最后同步时间）、作息方案、工单列表
- 登录页：检测到网络后自动重连

### 8.4 权限（Handoff 第 6 节）

```kotlin
// 必需权限清单
val requiredPermissions = listOf(
    Manifest.permission.INTERNET,
    Manifest.permission.ACCESS_NETWORK_STATE,
    Manifest.permission.ACCESS_WIFI_STATE,
    Manifest.permission.RECORD_AUDIO,          // 寻呼 + 对讲
    Manifest.permission.ACCESS_FINE_LOCATION,  // 终端地理位置
    Manifest.permission.POST_NOTIFICATIONS,    // Android 13+
)
```

---

## 9. 设计偏离检测 Checklist（Critic 用）

Critic Agent 在评审任何 frontend deliverable 时，必须按本 checklist 逐项检查：

### 9.1 颜色检查

- [ ] 是否所有颜色都来自第 1 节定义的 token？
- [ ] 是否出现 hardcode 的 `Color(0xFF...)`？（自动检测正则：`Color\(0x[A-F0-9]{8}\)`）
- [ ] 终端状态色是否对应正确的 TerminalState？
- [ ] 渐变是否仅用于装饰元素？

### 9.2 形状检查

- [ ] 卡片是否用 `AeroShapes.Card` (12.dp)？
- [ ] 按钮 / Chip 是否用 `AeroShapes.Chip` (999.dp)？
- [ ] 底部 Sheet 是否仅顶部圆角 24.dp？

### 9.3 字体检查

- [ ] 数字是否使用 mono + tnum？
- [ ] TopBar 标题是否 22sp / 600？
- [ ] 标签是否全大写 + letter-spacing 0.6？

### 9.4 间距检查

- [ ] 是否遵循 8px base？
- [ ] 页面左右内边距 14-16.dp？
- [ ] TabBar 80.dp + AI 抬起 +16.dp？

### 9.5 动效检查

- [ ] 切换动画时长是否 120-220ms 区间？
- [ ] 是否使用 cubic-bezier(.2,.7,.3,1) 或同等曲线？
- [ ] 是否考虑 `prefers-reduced-motion`？

### 9.6 状态检查

- [ ] 终端卡片是否覆盖全部 7 个状态？
- [ ] 错误处理是否分 4 层（致命/请求失败/部分失败/表单）？
- [ ] 离线场景是否禁用动作按钮 + 显示提示？

### 9.7 实时性检查

- [ ] WS 断线是否有黄 banner？
- [ ] 是否实现 10s 轮询回退？
- [ ] 任务进度是否会话结束后切到 30s 轮询？

---

## 10. Compose Token 落地建议

建议在 `app/src/main/kotlin/com/aeroradio/ui/theme/` 下建立以下文件：

```
theme/
├── AeroColors.kt        # 第 1 节 color tokens
├── AeroShapes.kt        # 第 2 节 shape tokens
├── AeroSpacing.kt       # 第 3 节 spacing tokens
├── AeroType.kt          # 第 4 节 typography
├── AeroElevation.kt     # 第 5 节 elevation
├── AeroMotion.kt        # 第 6 节 motion
└── AeroTheme.kt         # 组合上述所有 token 的 MaterialTheme 包装
```

`AeroTheme` 应该是所有屏的**唯一根 Composable**，强制 token 通过 CompositionLocal 注入。

---

*Design System Spec v1.0.0 — Extracted from Handoff.html, single source of truth for AeroRadio frontend*
