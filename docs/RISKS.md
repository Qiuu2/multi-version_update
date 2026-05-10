# 多版本升级风险登记册

记录升级过程中发现的、当前未解决但已知的风险。每项风险标注严重度、阻塞范围、当前
缓解措施、根治路径。

---

## R-001 ★★★ htapplib.aar 仅 32 位 ARM,无源码

**严重度:** ★★★ (阻塞 Android 14+ 上线)

**事实:**
- `app/libs/htapplib.aar` 的 native 库 `libmp3lame.so` / `libaudioplay.so` /
  `liblog.so` **只有 armeabi-v7a 版本**
- 该 aar 由公司内部已离职员工于约 2014 年编写,源码不在当前仓库
- 详细分析见 `docs/htapplib-internals.md`

**触发场景:**
- 在仅支持 64 位的设备上运行时,Android 选择 arm64-v8a ABI 加载,
  `MediaCodec` 类的 static 初始化调用 `System.loadLibrary("mp3lame")`
  抛出 `UnsatisfiedLinkError`,app 在启动早期闪退
- 受影响设备:Pixel 7 及之后所有 Pixel、大多数 64-bit-only 模拟器(包括
  Pixel 10 Pro AVD)、部分 2023+ 高端 Android 设备

**当前缓解(临时):**
- `app/build.gradle` 的 `abiFilters` 加入 `arm64-v8a`,使 APK **能装上** 64 位
  设备(否则连安装都被拒绝)
- 但运行到 htapplib 初始化时仍会崩
- 在仍支持 32 位的设备(国产手机大部分、Pixel 6 及之前)上运行不受影响

**根治路径(三选一,优先级从高到低):**

1. **找回源码并重新编译为 multi-ABI**(首选)
   - 公司 Gitlab/SVN 搜索 `htapplib`、`EncodeService`、`HTIntf` 等关键字
   - 联系 IT 找回离职作者的工作机硬盘
   - 公司技术群内部询问
   - 一旦拿到源码,重新编译时在 `Application.mk` / `build.gradle` 中加入 64 位 ABI 即可

2. **反编译混淆 Java 类 + 重写 native 部分**(次选)
   - Java 部分用 CFR / Procyon 反编译,大部分逻辑可读
   - native 部分:
     - `libmp3lame.so` → 替换为开源 LAME 64 位 prebuilt(GitHub 可找)
     - `libaudioplay.so` → 用 Android `AudioTrack` 重写(Java 层即可,无需 JNI)
     - `liblog.so` → 替换为 `android.util.Log`
   - 重新打包为新的 multi-ABI aar
   - 工作量:高,但不依赖外部

3. **整体替换为现代音频方案**(远期)
   - 用 Android 内置 `android.media.MediaCodec` 替代 LAME
   - 用 WebRTC 或 RTP 协议栈替代私有协议
   - 工作量:极高,需要对接服务器协议改造
   - 价值:彻底摆脱遗留 SDK,跨平台迁移时复用度高

**当前不变性边界(改造时必须保留的契约):**

详见 `docs/htapplib-internals.md` 第 5 节。简述:
- 包名 `com.example.htapplib`
- `HTIntf` 所有 public static 方法签名
- `CallBackIntf` 所有方法
- `EncodeService` / `DecodeService` 的 manifest 注册名和 intent-filter action

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

## R-003 ★ targetSdk 仍为 33,未真正经历 Android 14+ 强制策略

**严重度:** ★ (当前不影响,Layer 2 后会改变)

**事实:** `app/build.gradle` 中 `targetSdkVersion 33`,Android 14+ 的多项强制
检查(前台服务类型、通知权限、隐式 PendingIntent 等)对本应用处于"宽松模式",
未真正发生。

**当前缓解:** Layer 1 已经为前台服务类型问题做好 manifest 准备。

**根治路径:** Layer 2 升 targetSdk 到 34 或 35,同步配套权限申请代码。但完整
验证仍受 R-001 阻塞(没有 64 位 aar 就跑不起来对讲)。

---

---

## R-004 ★ LoginActivity 权限请求字符串有空格和系统级权限名

**严重度:** ★ (静默失败,不崩,但权限永远拿不到)

**事实:** `app/src/main/java/.../activity/LoginActivity.java:117-118`

```java
permissionUtils.judgePermission("android.permission.READ_PRIVILEGED_PHONE_STATE ");
permissionUtils.judgePermission("android.permission.WRITE_EXTERNAL_STORAGE ");
```

两处问题:
1. 字符串末尾有空格,Android 按字面字符串匹配,带空格的权限名找不到任何
   已知权限,系统静默忽略
2. `READ_PRIVILEGED_PHONE_STATE` 是 `signature|privileged` 权限,普通应用
   不可能被授予。可能本意是写 `READ_PHONE_STATE`

**影响:** 这两次调用永远不会成功授权,但也不会崩溃 —— 因为系统无视未知/不可
得的权限。LoginActivity 即使不持有这两个权限也照常运行,说明业务逻辑实际上不
依赖它们(否则早就出问题了)。

**缓解:** 暂未处理。修复时:
- 删除字符串末尾空格
- `READ_PRIVILEGED_PHONE_STATE` 改成 `READ_PHONE_STATE`(若确需)或直接删除调用

不在 Layer 2 范围,因为修复需要确认业务是否真的需要电话状态权限。

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
