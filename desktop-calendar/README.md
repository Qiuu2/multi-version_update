# 本地日历 / 待办面板

按 `docs/design-handoff.md`（交接文档原文）实现的单窗口本地日历面板，980 × 620，纯本地、无服务端。

技术栈：**React 18 + TypeScript + Vite**，桌面外壳用 **Tauri 2**。

选这套的原因：本仓库原有的是 `AeroRadioControl` 那个 Android 工程（Java / minSdk 19），跟桌面日历没有可复用的组件或样式约定，所以按交接文档自己的指引「没有代码库就选定框架，纯本地桌面应用推荐 Tauri 或 Electron」另起了一个隔离的目录，不与 Android 工程共享任何配置。

## 跑起来

```bash
cd desktop-calendar
npm install

npm run dev          # 浏览器里开发，http://localhost:5173
npm run build        # 类型检查 + 产出 dist/
npm run tauri dev    # 以桌面应用启动（982 × 622 固定窗口）
npm run tauri build  # 打安装包
```

浏览器模式下数据存 `localStorage`；Tauri 模式下存应用数据目录里的 `calendar.json`
（Windows 是 `%APPDATA%\com.local.calendar\calendar.json`）。

## 目录

```
src/
  types.ts            条目 / 草稿 / 锚点等类型
  constants.ts        面板尺寸、分组识别色、提醒选项、临期分组定义
  lib/
    date.ts           纯日期运算，全部走 UTC 午夜避开夏令时
    color.ts          分组徽章底色（该色 12%）
    item.ts           显示时间、去时间前缀、月视图 tooltip
  store/
    reducer.ts        全部状态与写操作 + 撤销栈
    selectors.ts      派生数据（可见性、搜索、底栏统计…）
    context.tsx       Provider，负责读盘与防抖落盘
    persistence.ts    存储适配器：Tauri 文件 / localStorage
    seed.ts           示例数据
  hooks/
    useRangeSelect.ts 横拖框选
    usePanelDismiss.ts 点外部关闭
    useGlobalKeys.ts  ←/→、Enter、Esc
    useToastTimer.ts  提示条 5 秒自动消失
  components/         每个屏 / 浮层一个组件，配套 *.module.css
  styles/
    tokens.css        三套主题的 CSS 变量（全局）
    base.module.css   跨组件复用的按钮、下拉、复选框、胶囊开关
src-tauri/            Rust 外壳，两个命令：load_state / save_state
```

状态管理用 `useReducer` + Context，没引第三方状态库。所有写操作都走 reducer 里的
`commit()`，它在改动前压一份快照进撤销栈（上限 20 步）。

颜色一律取 `tokens.css` 里的变量，组件里没有主题外的写死颜色 —— 唯一例外是强调色块上的
文字固定 `#FFFFFF`，以及不随主题变化的分组识别色和四个临期分组色。

## 与原型的差异

交接文档要求高保真 1:1，视觉数值（尺寸、间距、圆角、字号、三套主题色）都照表实现。
下面这些是有意偏离的地方：

| 项 | 原型 | 这里 | 原因 |
|---|---|---|---|
| 今天 | 写死 `2026-09-12` | 取本机当天 | 产品不能把某一天钉死 |
| 示例数据日期 | 写死 `2026-09-xx` | 相对今天的偏移，疏密分布不变 | 否则换一天跑起来全是过期条目 |
| 悬停态 | React state 存 `hoverRow`，重渲染整栏 | CSS `:hover` | 视觉一致，少一次重渲染 |
| 每周重复 | 只在记录上存 `repeatOn` / `repeatWeeks` 两个标记，并不真的生成 | 保存时展开成 N 条共用 `repeatId` 的独立记录 | 文档与界面文案都写的是「将生成 N 条独立日程，可整组删除」 |
| 删除整组 | 按「标题相同且带重复标记」匹配 | 优先按 `repeatId` 匹配，示例数据没有该字段时才退回按标题 | 按标题匹配会误删同名的其他日程 |
| 撤销快照 | 只含 `tasks` + `hidden` | 另含 `nextId` 与 `customGroups` | 原来撤销后 `nextId` 不回退，新建的 id 会和被撤销的记录撞上 |
| 设置 / 配色 / 无期限三个菜单 | 只能靠再点一次或 Esc 关 | 点外部也关（触发按钮打了 `data-menu`，不会关了又立刻开） | 桌面端的常规预期 |
| 月网格文字 | 可被鼠标选中 | `user-select: none` | 横拖框选时会顺手选中日期数字 |

文档里「做成真实产品时建议改动」那几条的落地情况：

- **已做** —— 撤销栈快照修正、重复日程分组键。
- **保持原型形态** —— 跨天日程仍是逐日拆成多条同 `spanId` 的记录（改成一条 `start`/`end`
  横跨渲染要重做月网格的布局，超出本次范围）；时间仍是 `date` + `time` 两个字符串，
  没换成 ISO 时间戳 + 时区。
- **未实现** —— 提醒只存字段，没接系统通知与后台唤醒；没做 .ics 导入导出。
  这三项文档本身也列在产品化改动里，不属于本次要还原的界面与交互。

持久化目前是 JSON 文件。换 SQLite 只需要改 `src-tauri/src/lib.rs` 里那两个命令，
前端的 `CalendarStorage` 接口不用动。

## 验证情况

`npm run build`（含 `tsc`）通过。用 Chromium 实跑过一轮，确认：

- 面板实际尺寸 982 × 622（980 + 1px 描边），月网格 42 格
- 点日期格弹日详情浮卡，靠右边界时正确翻到格子左侧
- 浮卡里点条目开编辑弹窗；分组下拉、内嵌月历、双列时分选择器都能展开
- 横拖框选出选区描边与浮标，松手浮出批量操作条；「每天各一条」建出 4 条并弹提示条，
  点撤销后 4 条全部回退
- 拖条目到别的日期能改期并弹提示条
- 右键菜单、搜索下拉、三套主题切换、重载后主题与数据仍在
- 焦点在输入框里、或鼠标移出面板时，←/→ 都不翻月；移回面板内才翻
- 无 console 报错

**`src-tauri/` 没有在本次环境里编译过** —— 容器缺 Tauri 需要的系统 webview（webkit2gtk /
gdk-3.0），`cargo check` 停在系统库这一步。已经确认的是 Rust 源码语法无误（`rustfmt` 通过）、
Cargo 依赖图可解析。首次在你机器上跑 `npm run tauri dev` 时如果卡在外壳上，
用 `npm create tauri-app` 生成一份同版本脚手架对一下 `tauri.conf.json` 即可，
前端部分不受影响。
