# 多版本升级风险登记册

记录升级过程中发现的、当前未解决但已知的风险。每项风险标注严重度、阻塞范围、当前
缓解措施、根治路径。

---

## R-001 ★★ ~~htapplib.aar 仅 32 位 ARM,无源码~~ [RESOLVED (with caveat)]

**已解决** ✅ — htapplib 64-bit version received from 陶工 on 2026-05-11,
committed as `Layer 3: Replace htapplib.aar with 64-bit ported version`.
Verified working on Pixel 10 Pro emulator (API 37).

**注意事项(剩余尾巴):**
陶工的 64-bit 版本编译时**没加 `-Wl,-z,max-page-size=16384` flag**,
所以 .so 不是 16 KB page aligned。当前能在 Pixel 10 Pro 模拟器跑通,
是因为模拟器有 Berberis(ARM-to-x86 翻译层)对 4 KB 对齐宽容。

**在物理 Pixel 7+ 设备上,htapplib 可能仍报 16 KB alignment 错误**
(同 R-006 处理百度时遇到的问题)。陶工答应稍后重编 16 KB 对齐版本,
拿到后直接替换 `app/libs/htapplib.aar` 即可,无需改代码。

如果暂时只在模拟器 / Pixel 6 及之前的物理设备 / 国产机部署,**此风险不
阻塞**。如果要部署到 Pixel 7+ 物理设备,**等待陶工 16 KB 版本**。

---

## R-006 ★★ ~~百度地图 SDK 不兼容 16 KB 内存页~~ [RESOLVED]

**已解决** ✅ — 百度地图 SDK 从 v7.4.0 (2019) 升级到 v8.0.0 + Location SDK
v9.6.8 (2024+),新版 .so 已经按 16 KB page 对齐编译,完全兼容
Android 15+ / Pixel 7+ 设备。

**升级动作总结(2026-05-11):**
- `app/libs/BaiduLBS_Android.jar` → `BaiduLBS_Android.aar`
- 全部 .so 文件升级:
  - `BaiduMapSDK_*_v7_4_0.so` → `*_v8_0_0.so`
  - `liblocSDK8a.so` → `liblocSDK8b.so`
  - `libgnustl_shared.so` → `libc++_shared.so` (现代 STL)
  - 新增 `libindoor.so`、`libtiny_magic.so`
- 代码适配:
  - `MyApplication.initBaiduMap()` 新增 PIPL 隐私合规调用
    (`SDKInitializer.setAgreePrivacy` + `LocationClient.setAgreePrivacy`)
  - `LocationInMapActivity` 两处 `new LocationClient(this)` 加 try/catch
    (新 SDK 构造函数声明 throws Exception)

涉及 commits:`Layer 4` 系列(b9a76fe、7a8d153、0563044)。

---
## R-002 ★★ 内网服务器依赖,模拟器/家庭网络无法端到端测试

**严重度:** ★★ (阻塞最终验收,不阻塞代码改造)

**事实:** App 登录依赖局域网内的服务器,在公司外网环境无法连接,导致模拟器无法
完成完整业务流程测试。

**当前缓解:**
- 代码改造期间**仅依赖 Build 通过 + manifest/APK 静态检查**,不要求设备运行通过
- UI 渲染层验证可在 32 位兼容设备上做(见 R-001 缓解)
- 完整业务验收推迟到上线前,在公司内网 + 物理手机环境完成

**根治路径:** 不需要根治,这是部署形态决定的。如有必要可考虑搭建 mock 服务器
用于离网开发,但 ROI 低。

---

## R-003 ★ ~~targetSdk 仍为 33,未真正经历 Android 14+ 强制策略~~ [RESOLVED]

**已解决** ✅ Layer 2-5 commit `Bump targetSdk 33 -> 35`。targetSdk 现在
是 35,Android 14 / 15 / 16 的全部强制规则已经在 Layer 1+2 的改动中提前
适配完成,运行验证已通过(Pixel 10 Pro 模拟器跑通到登录页)。

---

## R-004 ★ ~~LoginActivity 权限请求字符串有空格和系统级权限名~~ [RESOLVED]

**已解决** ✅ commit B1

**原始问题:** `LoginActivity.initSubViews()` 用字面字符串调用 `judgePermission`,
其中两处末尾带空格 + 一处用了普通应用永远拿不到的 `READ_PRIVILEGED_PHONE_STATE`,
导致这些权限请求被 Android 静默忽略。

**修复方式:** 改用 `Manifest.permission.*` 常量(IDE 会校验拼写),并将
`READ_PRIVILEGED_PHONE_STATE` 替换为 `READ_PHONE_STATE`(普通 dangerous 权限)。

**遗留:** LoginActivity 这种"在初始化时调用一连串 judgePermission"的模式本身
是过时的霰弹枪做法,理想应跟 SignActivity 一样按版本动态构建。但 SignActivity
启动时已经请求了大部分权限,LoginActivity 的调用现在只会在用户拒绝过的情况下
重新弹一次,行为可接受。深度重构留给未来。

---

## R-005 ★ SignActivity 不等权限授权就跳页

**严重度:** ★ (UX 问题,不崩)

**事实:** `SignActivity.initSubViews()` 里依次调用:

```java
getThePermission();    // 弹权限对话框(异步,不阻塞)
setgifs();             // 启动 400ms 定时器,到点跳 LoginActivity
```

定时器在用户点"允许"之前就到点跳页了。`onRequestPermissionsResult`
回调到达时 SignActivity 已经销毁,grant 状态没人接收。

**影响:** 权限对话框会"飘"到 LoginActivity 上面,用户体验上能用但不优雅。
功能不受影响,因为 LoginActivity 自己也再次检查权限。

**缓解:** 暂未处理。

**根治:** 重构 SignActivity 让跳页等待 onRequestPermissionsResult,或者把
权限请求挪到 LoginActivity 内部。涉及启动流程改动,Layer 2 不处理。

---

## 风险登记规范

新增风险时,请按上面格式编号(R-NNN),包含:
- 严重度(★ / ★★ / ★★★)
- 事实(可观察、可验证)
- 触发场景
- 当前缓解
- 根治路径
- (可选)不变性边界
