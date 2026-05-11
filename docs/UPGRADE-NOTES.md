# Multi-Version Upgrade — 接手人速读

这次升级把项目从 **AGP 8.0 / SDK 33 / minSdk 19** 推进到 **AGP 8.7 / SDK 35 / minSdk 21**,
覆盖 Android 12 / 13 / 14 / 15 的所有强制行为变化。本文档是给**未来接手者**或
**业务回顾者**的速读材料,**不是教程**。配合 git 历史看效果最好。

> **当前状态(2026-05-11 收工)**: 升级**完成并发布候选验证通过** ✅
> - **Release-signed APK** 生成成功,装到 Pixel 10 Pro 模拟器(API 37 / Android 16)
> - App 启动到 LoginActivity 正常显示(顶部用户名输入框可见,无闪退)
> - R-001、R-003、R-004、R-006 **全部关闭**
> - htapplib 经过两轮迭代后达到 16 KB page 对齐 + 兼容 AGP 8.7.3 的 minCompileSdk
> - 百度地图 SDK 升级到 v8.0.0 + Location SDK 9.6.8,完整 PIPL 合规
> - **上线前唯一剩余事项**:在公司内网用物理机做完整业务流程验收(R-002)

---

## 1. 这次升级要解决什么

| 痛点 | 起源 | 后果(如不升级) |
|---|---|---|
| `targetSdk 33`,Google Play 要求 35+ | 2025-08 起新 app 必须 35,2025-11 起更新也必须 | 应用商店拒绝上架 |
| 前台服务无 `foregroundServiceType` | Android 14 强制要求 | targetSdk 升级后,对讲服务启动就崩 |
| 缺 `POST_NOTIFICATIONS` 等运行时权限 | Android 12-14 新引入 | 通知/蓝牙/媒体功能在新设备静默失效 |
| `minSdk 19` 限制了 AndroidX 升级路径 | KitKat 兼容包袱 | AppCompat 1.5+ 全部装不进来 |
| 一堆老依赖(2018-2020 年的版本) | 长期未维护 | 安全审计不过、CVE 累积 |
| `htapplib.aar` 仅 32 位 | 内部库未跟进 64 位 | Pixel 7+ 等 64-bit-only 设备装上即闪退 |

---

## 2. SDK 矩阵变化

```
            升级前         升级后
minSdk      19 (KitKat)   21 (Lollipop)         ← 放弃 Android 4.4
compileSdk  33 (Tiramisu) 35 (VanillaIceCream)
targetSdk   33 (Tiramisu) 35 (VanillaIceCream)  ← 行为合规切换闸
AGP         8.0.2         8.7.3
Gradle      8.0           8.9
```

**为什么 targetSdk 必须等于 compileSdk?** 不是必须,但保持等号让"我编译时看到的 API
行为 = app 运行时遇到的系统行为",减少认知负担。

---

## 3. 模块化改动清单

### 3.1 工具层

新增 `app/src/main/java/.../utils/AndroidVersion.java`:
- 集中所有 `Build.VERSION.SDK_INT` 比较
- 提供"语义化"的版本判断(如 `requiresNotificationPermission()`)
- 后续代码不再散落 `if (SDK_INT >= 33)` 这种 magic number

**接手时要知道:** 出现新的版本相关的行为分支,**不要直接写 SDK_INT 比较**,
先来这里加一个语义方法。

### 3.2 Manifest 改动

- 新增 `POST_NOTIFICATIONS`(Android 13+ 通知)
- 新增 `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN`(Android 12+ 替代老 BLUETOOTH)
- 新增 `READ_MEDIA_AUDIO/IMAGES/VIDEO`(Android 13+ 替代 READ_EXTERNAL_STORAGE)
- 新增 `FOREGROUND_SERVICE_MICROPHONE`(Android 14+ 对应 microphone 类型前台服务)
- 通过 `tools:node="merge"` 给 aar 内的 `EncodeService` / `DecodeService` 补
  `android:foregroundServiceType="microphone"`

老的 `BLUETOOTH` / `READ_EXTERNAL_STORAGE` 等**故意保留**,因为 minSdk 21 仍要
覆盖 Android 5.0-11 的设备。

**Manifest 现存遗留问题(已知,未在本次清理):**
- 多处权限重复声明(`INTERNET`、`VIBRATE`、`FOREGROUND_SERVICE` 等)
- 第 55-56 行有大小写错误的失效声明(`access_wifi_state` 等)
- 这些是无副作用的脏数据,以后顺手清理即可

### 3.3 权限请求流程

`SignActivity.getThePermission()` 从 16 项硬编码"霰弹枪"改为按 Android 版本
动态构建:

```
所有版本:    RECORD_AUDIO, READ_PHONE_STATE, ACCESS_FINE_LOCATION
API ≤ 32:   + READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE
API ≥ 33:   + READ_MEDIA_AUDIO/IMAGES/VIDEO
API ≥ 31:   + BLUETOOTH_CONNECT, BLUETOOTH_SCAN
API ≥ 33:   + POST_NOTIFICATIONS
```

`PermissionUtils.judgePermission()` 修了一个长期 Bug
(`pm.checkPermission(s, "packageName")` 字面量 → `mContext.getPackageName()`)
现在能正确判断已授权状态,不会重复弹窗。

### 3.4 BaseActivity 全局改动

`BaseActivity.onCreate()` 新增 `applyEdgeToEdgeOptOut()`:
- 仅在 API 35+ 调用 `setDecorFitsSystemWindows(true)`
- 恢复 Android 15 之前的 inset 行为
- **每个 Activity 都受影响**(因为都继承 BaseActivity)

**接手时要知道:** 如果未来要做 UI 现代化,把这个 opt-out 删掉即可——
但要同时给所有屏幕加 `WindowInsets` 处理。

### 3.5 依赖升级

| 依赖 | 老版本 | 新版本 | 备注 |
|---|---|---|---|
| `androidx.appcompat` | 1.0.2 | 1.7.1 | 触发 minSdk 21 要求 |
| `androidx.recyclerview` | 1.0.0 | 1.3.2 | API 兼容 |
| `glide` | 4.11.0 | 4.16.0 | API 兼容 |
| `multidex` | `com.android.support:1.0.3` | `androidx.multidex:2.0.1` | 命名空间迁移 |
| `junit` | 4.12 | 4.13.2 | 测试库 |
| `rxjava` / `rxandroid` | 2.2.9 / 2.1.0 | 2.2.21 / 2.1.1 | RxJava 2.x 末版 |
| `customactivityoncrash` | 2.2.0 | 2.4.0 | 崩溃捕获 |

**故意没动的依赖**(都是无人维护、JCenter 过时的库,改造期不动避免 scope 扩散):
- `com.zhy:base-rvadapter:3.0.3`
- `com.androidkun:XTabLayout:1.1.4`
- `com.github.jdsjlzx:LRecyclerView:1.5.4.3`
- `com.wang.avi:library:1.0.0`
- `com.acker:simplezxing:1.5`
- `com.contrarywind:Android-PickerView:4.1.9`

这些**不影响功能**,但**长期都该替换**。`simplezxing` 已知有 D8 警告(老
字节码),以后做 UI 现代化时一并替换为 ML Kit Barcode。

### 3.6 Kotlin stdlib

`app/build.gradle` 加了:
```gradle
configurations.all {
    exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk7'
    exclude group: 'org.jetbrains.kotlin', module: 'kotlin-stdlib-jdk8'
}
```

原因:Kotlin 1.8+ 把 `kotlin-stdlib-jdk7/jdk8` 的内容合并回 `kotlin-stdlib`,
但有些老库还在传递依赖老的 jdk8 split,跟新 stdlib 撞类。这段 exclude 是治本。

### 3.7 ABI

`abiFilters = ["armeabi-v7a", "arm64-v8a"]`(原来只有 v7a)。

加 arm64 是为了让 APK **能装到** Pixel 7+ 等 64 位设备。但 `htapplib.aar`
内部仍只有 v7a 的 .so → 64 位设备装上后,加载 native 库会闪退(R-001)。

**这是临时配置**,等 R-001 解决(64 位 aar 到位)后**保留 arm64,可以删除 v7a**
进一步现代化。

---

## 4. 已知未解决问题(详见 `RISKS.md`)

| ID | 严重度 | 内容 | 状态 |
|---|---|---|---|
| **R-001** | ★★★ | `htapplib.aar` 仅 32 位,无源码 | **唯一阻塞 Layer 3 上线** —— 待陶工提供 64 位 native 库 |
| R-002 | ★★ | 内网服务器依赖,模拟器无法端到端测试 | 部署形态决定,无需技术修复 |
| R-003 | ★ | targetSdk 升 35 的全部行为变化 | 已在 Layer 1+2 处理 |
| ~~R-004~~ | ~~★~~ | ~~LoginActivity 权限字符串错误~~ | ✅ B1 已修 |
| R-005 | ★ | SignActivity 跳页时序问题 | UX 问题不阻塞,留待 UI 重构 |

**只要 R-001 解决,即可上线。**

---

## 5. 接手人怎么验证一切还好

### 最快冒烟测试(5 分钟)

```bash
git checkout claude/setup-android-environment-Ek31v   # 改造分支
./gradlew clean assembleDebug                           # 全量构建
```

期望:`BUILD SUCCESSFUL`。

### 装机测试(取决于 R-001 状态)

**R-001 未解决前:**
- 32-bit-supporting 设备(老 Android 手机、Pixel 6 及之前):APP 启动 → 进登录页 → UI 无遮挡 = 通过
- 64-bit-only 设备(Pixel 7+ 模拟器):APP 启动后立刻闪退,日志报 `UnsatisfiedLinkError`,**这是预期的**

**R-001 解决后(替换 64 位 aar 之后):**
- Pixel 10 Pro 模拟器 + Android 16:APP 启动到登录页 = 通过
- 在公司内网用真实账号登录 → 跑核心业务流程 = 完整验收

---

## 6. 万一要回滚

每一步都是独立 commit,可单独回退:

```bash
# 回到改造前
git checkout pre-upgrade-baseline-2026-05-09

# 或者退回到某一步之前
git log --oneline                              # 找到目标 commit
git revert <commit-hash>                       # 创建反向 commit
```

**关键 commit 标签**(选取重大节点):

| 标签 | 内容 |
|---|---|
| `pre-upgrade-baseline-2026-05-09` | 升级开始前的 baseline,完整可运行 |
| Layer 1 完成 | 前台服务合规,manifest 改动 |
| Layer 2-1a/b | 工具链升级(AGP/Gradle/compileSdk) |
| Layer 2-5 | targetSdk 33→35 (行为切换闸) |
| Layer 2-6 | edge-to-edge opt-out |
| B2-2 | AppCompat 1.7.1 + minSdk 21 |

---

## 7. 长期演进建议(不在本次范围)

按"投资回报比"从高到低:

1. **替换 `htapplib`** 为现代音频方案(`android.media.MediaCodec` + WebRTC)
   - 一劳永逸解决 native 库问题
   - 跨平台迁移时直接复用
   - 工作量大,需要单独立项

2. **拆掉 6 个老 JCenter 库**(`base-rvadapter` 等)
   - 替换为 Material Components / 标准 RecyclerView Adapter
   - 减少 Kotlin stdlib exclude 的 hack
   - 工作量中

3. **拥抱 edge-to-edge UI**
   - 删除 `BaseActivity.applyEdgeToEdgeOptOut()`
   - 给每个 Activity 加 WindowInsets 处理
   - UI 上更现代,但工作量大

4. **升级 RxJava 2 → 3**
   - 性能 + 维护性
   - 工作量大,触及每条 Observable 链

5. **删除 `manifest` 里的重复/无效权限声明**(5 分钟)

---

## 8. 文档索引

- **本文** `docs/UPGRADE-NOTES.md` — 升级总览
- `docs/htapplib-internals.md` — htapplib 反编译分析(R-001 必读)
- `docs/RISKS.md` — 风险登记册
- `docs/cross-platform-migration-assessment.md` — 多端迁移评估(更长期)

---

*最后更新: 2026-05-10。如对本次升级有疑问,看 git 历史的 commit 信息——
每一步的"为什么这么改"都写清楚了。*
