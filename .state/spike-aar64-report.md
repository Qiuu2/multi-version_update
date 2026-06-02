# SPIKE-AAR64 Report — htapplib.aar arm64-v8a 可用性验证

> 任务：`TASK-AR-SPIKE-001`（P0_CRITICAL）· Owner：Legacy-Native Agent · 日期：2026-05-27
> 目标：在 arm64 上验证 `app/libs/htapplib.aar` 64 位语音库是否真正可用，给出 GO（降风险）/ NO-GO（维持降级）结论，驱动 R-001 风险调整与 Phase 1 关键路径。
> 输入基线：`.state/v3-audit-report.md` §3（ABI 实扫）、§7 RISK-AUDIT-01。

---

## 0. 结论（一句话给 CTO）

**GO（条件性）—— `GO-pending-device-confirmation`。**

静态证据充分证明：`htapplib.aar` 的 arm64-v8a 64 位原生库**结构完整、链接自洽、JNI 入口齐全、与已验证的 32 位库完全等价**，具备在 arm64 真机上可用的全部前提。**唯一未闭环项是真机动态执行**（`System.loadLibrary` 实测成功 + `Mp3Encode*` 实调用），因本执行环境无 arm64 物理真机/可用镜像而无法在此完成 —— 已附**可复现真机测试步骤**交 QA/CTO 闭环。

> 建议据此将 **R-001（RISK-AR-003 / 32 位 ABI）由 CRITICAL 降级为 LOW**，附带条件「真机回执确认后正式关闭」。降级理由见 §6。

---

## 1. ABI 产物实扫（验收项：ABI 完整性）

解压 `app/libs/htapplib.aar`（539 KB，2026-05-14），`jni/` 内容如下（`file(1)` 实测类型）：

| ABI 目录 | `libaudioplay.so` | `libmp3lame.so` | ELF 类型实测 | 完整性 |
|----------|:----:|:----:|------|------|
| `jni/arm64-v8a/` | ✅ 7,344 B | ✅ 336,264 B | **ELF 64-bit LSB, ARM aarch64**（stripped） | **完整（64 位）** |
| `jni/armeabi-v7a/` | ✅ 5,296 B | ✅ 427,584 B | ELF 32-bit LSB, ARM EABI5 | 完整（32 位，基线） |
| `jni/armeabi/` | ❌ 缺 | ✅ 501,468 B | ELF 32-bit LSB（not stripped, debug_info） | 不完整（仅 lame，无 audioplay） |
| `x86 / x86_64` | — | — | — | **不存在** |

**确认审计 §3.2 属实**：arm64-v8a 下两个语音 `.so` 均为货真价实的 64 位 ARM ELF，且 `file` 确认是合法 aarch64 共享对象（非占位/空文件）。

---

## 2. 导出符号清单（验收项①：nm -D）

### 2.1 `arm64-v8a/libaudioplay.so` — JNI 入口（关键）

`nm -D --defined-only` 导出的 JNI 绑定符号：

```
T Java_com_example_htapplib_MediaCodec_Mp3EncodeInit
T Java_com_example_htapplib_MediaCodec_Mp3EncodeBuffer
T Java_com_example_htapplib_MediaCodec_Mp3EncodeBufferFLush
T Java_com_example_htapplib_MediaCodec_Mp3EncodeRelease
（另含 C++ 内部符号 _Z21mp3codec_init_encoder... 等 4 个）
```

这 4 个 `Java_com_example_htapplib_MediaCodec_Mp3Encode*` **正是 Java 层 `MediaCodec` 类 4 个 `native` 方法的 JNI 实现**（见 §3）。**arm64 与 armeabi-v7a 的 JNI 导出符号集 `diff` 结果为 IDENTICAL** —— 64 位库提供的 JNI 入口与已验证的 32 位库完全一致，无缺失。

### 2.2 `arm64-v8a/libmp3lame.so` — LAME 编码器

导出 233 个 `T` 符号（`lame_init / lame_init_params / lame_encode_buffer / lame_encode_flush / lame_close / lame_set_*` 全部齐全，另含 `hip_decode* / id3tag_*` 等）。

> **注 — 符号数差异不是缺陷**：arm64 `libmp3lame.so` 导出 233 个、armeabi-v7a 导出 479 个。差异源于 **arm64 是 stripped、v7a 是 not stripped**（`file(1)` 实测如此），多出的是 v7a 暴露的内部/局部符号。**对外 API 符号（`libaudioplay` 实际消费的那批）两边均存在**——见 §4 链接解析测试。

---

## 3. Java/JNI API 映射（接口逆向，验收项相关）

解压 `classes.jar`（`javap -p` via JBR-17），`com.example.htapplib` 包公开类：

| 类 | 性质 | 角色 |
|----|------|------|
| `HTIntf` | **纯 Java**（无 native 方法） | 寻呼/对讲/点播**控制面**：`startpaging/stoppaging/startspeech/stopspeech/startondemand/...`、`init/connectserver/release`、回调注册 |
| `MediaCodec` | **持有全部 `native` 方法** | 音频 **MP3 编解码面** —— 唯一被 `.so` 支撑的部分 |
| `EncodeService` | `IntentService`（`foregroundServiceType=microphone`） | 录音 → 调 `MediaCodec` 编码 |
| `DecodeService` | `IntentService`（`foregroundServiceType=mediaPlayback`） | 播放/解码 |
| `CallBackIntf` | interface | 21 个事件回调（`onstartspeech/onstopspeech/onspeechrequest/...`） |

### 3.1 关键架构澄清（reconcile 任务前提）

任务 assignment 与审计 §3.1 把验证目标描述为 native 方法 `startspeech/startpaging/startTalk`。**实测纠正**：

- `HTIntf.startspeech() / startpaging() / startondemand()` 等**全是普通 Java static 方法，不是 `native`**。对讲/寻呼的**控制协议在纯 Java/Kotlin 层实现**（混淆类 `a..t`，推测为 TCP/socket 与服务器通信），**不依赖 `.so`**。
- **唯一 native 依赖是 MP3 编码路径**：`MediaCodec.Mp3EncodeInit/Buffer/BufferFLush/Release` → `libaudioplay.so` → （DT_NEEDED）`libmp3lame.so`。
- 因此 **arm64 必须满足的 native 面非常窄**：只有 MP3 编码。这进一步降低 64 位风险——要适配的 native 表面积小且边界清晰。

### 3.2 `System.loadLibrary` 调用（验收项②的静态前置）

`javap -c -p com.example.htapplib.MediaCodec` 反汇编其 `static {}`：

```
ldc "mp3lame";   invokestatic System.loadLibrary   // → libmp3lame.so
ldc "audioplay"; invokestatic System.loadLibrary   // → libaudioplay.so
```

- 加载库名 = `mp3lame`、`audioplay`，**两者在 arm64-v8a/ 均有对应 `.so`** → 库名解析无缺口。
- **加载顺序正确**：先 `mp3lame` 后 `audioplay`（因 `libaudioplay.so` 的 DT_NEEDED 引用 `libmp3lame.so`，先加载被依赖者）。

### 3.3 运行时调用链（真机测试将走的路径）

现有调用点 `method/MainMethod.java`（assignment 指认的 native 指令封装）：

```
MainMethod.startSpeech(list)
  → HTIntf.newspeechitem() / setspeechitem(id)        [纯 Java 控制]
  → HTIntf.startspeech()                               [纯 Java；触发录音会话]
      → EncodeService (foreground microphone)
          → MediaCodec.Mp3EncodeInit/Buffer/...        [native：触发 loadLibrary]
              → libaudioplay.so → libmp3lame.so        [arm64 已具备]
```

`activity/TempTTSActivity.java` 是另一直接使用 `MediaCodec` 编码的调用点，可作真机最小验证入口。

---

## 4. 链接性检查（验收项④：是否依赖未随 arm64 提供的 32 位库）

`readelf -d` 看 DT_NEEDED + `nm -D` 看 undefined 符号：

### 4.1 `arm64-v8a/libaudioplay.so` 的 DT_NEEDED
```
libmp3lame.so   ← 随 arm64-v8a/ 一并提供 ✅
liblog.so       ← Android 系统库 ✅
libm.so libdl.so libc.so   ← Android 系统库 ✅
SONAME: libaudioplay.so
```

### 4.2 `arm64-v8a/libmp3lame.so` 的 DT_NEEDED
```
libm.so libdl.so libc.so   ← 全是 Android 系统库 ✅
SONAME: libmp3lame.so
```

### 4.3 跨库符号解析实测（核心结论）

`libaudioplay.so` 的 10 个 `lame_*` undefined 符号，**逐一比对 arm64 `libmp3lame.so` 导出表**：

```
lame_close / lame_encode_buffer / lame_encode_flush / lame_init /
lame_init_params / lame_set_brate / lame_set_in_samplerate /
lame_set_num_channels / lame_set_out_samplerate / lame_set_quality
→ 全部 [OK]（均由 arm64 libmp3lame.so 导出）
```

`libaudioplay.so` 其余 undefined 符号仅 3 个：`__android_log_print`（liblog）、`__cxa_atexit / __cxa_finalize`（libc）—— 全是系统原语。

> **链接性结论：arm64-v8a 链接面完全自洽，无任何悬空依赖、无任何 32 位-only 库依赖。** 64 位库可在标准 arm64 Android 设备的系统库上完成动态链接。

---

## 5. x86_64 模拟器情况（验收项⑤）

- AAR **无 `x86` / `x86_64` `.so`**（仅 armeabi、armeabi-v7a、arm64-v8a）。
- 影响：**x86_64 模拟器跑语音/MP3 编码必 `UnsatisfiedLinkError`**（`loadLibrary("mp3lame")` 找不到 x86_64 .so）。这是**开发期影响**（多数 CI/模拟器是 x86_64），**非发布阻塞**（目标终端与 arm64 真机不受影响；本项目校园 LAN 内部分发，不强求 Google Play 64 位策略）。
- 建议：① 开发期对讲功能在 arm64 真机验证，模拟器侧靠 `VoiceTalkAdapter.isAvailable()` 降级 UI 走查；② **可向厂商索要 x86_64 `.so` 以补齐模拟器开发体验**（非阻塞、优先级 P3）。是否索要属 escalation 范畴，建议 PM 在与厂商沟通 WS 文档时**搭车一并问**，不单开沟通成本。

---

## 6. GO / NO-GO 结论与风险建议

### 6.1 结论：GO（pending device confirmation）

| 验收维度 | 状态 | 证据 |
|---------|:----:|------|
| arm64 两 .so 存在且为合法 64 位 ELF | ✅ | §1 |
| JNI 入口导出齐全、与 32 位等价 | ✅ | §2.1（diff IDENTICAL） |
| LAME 对外 API 符号齐全 | ✅ | §2.2 + §4.3 |
| 链接自洽、无 32 位-only 依赖 | ✅ | §4 |
| `loadLibrary` 库名/顺序正确 | ✅（静态） | §3.2 |
| **真机 loadLibrary 实测成功** | ⏳ **待真机** | 本环境无 arm64 真机 |
| **真机 Mp3Encode* 调用链实测** | ⏳ **待真机** | 同上 |

7/8 维度静态闭环；剩余 2 项（真机动态）受环境限制无法在此执行（见 §7）。**静态层面无任何反对 GO 的证据。**

### 6.2 风险登记册建议（驱动 decision-log 待跟踪项）

- **R-001（RISK-AR-003 / 32 位 ABI）：CRITICAL → LOW**（条件性）。
  - 理由：① 64 位库已随 AAR 提供（5/14 厂商已更新）；② 链接自洽、JNI 与 32 位等价；③ native 依赖面仅 MP3 编码，边界窄。
  - 条件：真机回执（§7 步骤）确认后正式关闭；在此之前保留 LOW + 降级 UI 兜底。
- **同步修正项**（GO 后执行，本报告已备料）：
  - `app/build.gradle` 滞后注释（§8）；
  - `CLAUDE.md` 关键约束段「htapplib.aar 只有 armeabi-v7a + x86」；
  - `aeroradio-workflow` 内 SKILL `RISK-AR-003`、PM-memory `R-001`、Legacy-Native soul §2.4 / memory `abi_supported`。

---

## 7. 可复现测试步骤（验收项②③⑦：真机闭环交接）

### 7.1 静态验证（已在本报告执行，任何 x86_64 主机可复现）

```bash
WORK=/tmp/htapp_spike; rm -rf "$WORK"; mkdir -p "$WORK"
unzip -o app/libs/htapplib.aar -d "$WORK"
# 1) ABI 完整性
find "$WORK/jni" -name '*.so' -exec file {} \;
# 2) JNI 导出符号
nm -D --defined-only "$WORK/jni/arm64-v8a/libaudioplay.so" | grep Java_
# 3) DT_NEEDED
readelf -d "$WORK/jni/arm64-v8a/libaudioplay.so" | grep NEEDED
# 4) 跨库符号解析（应全部命中）
comm -23 \
  <(nm -D "$WORK/jni/arm64-v8a/libaudioplay.so" | awk '$1=="U"&&$2~/^lame_/{print $2}' | sort -u) \
  <(nm -D --defined-only "$WORK/jni/arm64-v8a/libmp3lame.so" | awk '{print $3}' | sort -u)
#   ↑ 预期输出为空 = 无未解析符号
# 工具版本：nm/readelf=GNU binutils；javap=JBR 17 (~/.jdks/jbr-17.0.14/bin/javap)
```

### 7.2 真机动态验证（需 QA/CTO 在 arm64 真机执行 — 本环境无法做）

**前提**：一台 arm64-v8a Android 真机（如 Pixel 7 / arm64 国产机），Android 7.0+（AAR minSdk 21）。

```
步骤 A — loadLibrary 验证（验收②）
  在 V4Activity 或一次性测试入口调用：
    com.example.htapplib.MediaCodec.a();   // 触发 static{} 的两次 loadLibrary
  预期：无 UnsatisfiedLinkError；logcat 无 "dlopen failed / library not found"。
  采集：设备型号 + `adb shell getprop ro.product.cpu.abi`（应为 arm64-v8a）+ Android 版本。

步骤 B — Mp3Encode* 调用链验证（验收③）
  MediaCodec mc = MediaCodec.a();
  int r1 = mc.Mp3EncodeInit(8000, 1, 8000, 16, 5);  // 采样率/声道等按现有 TempTTSActivity 取值
  byte[] out = new byte[ /* getmp3encodebuffersize 返回值 */ ];
  int n  = mc.Mp3EncodeBuffer(pcmLeft, pcmRight, len, out);   // n>0 = 编码成功产出字节
  int f  = mc.Mp3EncodeBufferFLush(out);
  mc.Mp3EncodeRelease();
  预期：Init 返回成功码、Buffer 返回 >0、无 native crash（SIGSEGV）。

步骤 C — 端到端对讲（可选，验收③加强）
  复用 MainMethod.startSpeech(list) 路径（HTIntf.startspeech → EncodeService → MediaCodec），
  确认 EncodeService（foregroundServiceType=microphone）正常起、CallBackIntf.onstartspeech 回调到。
  注意 Android 14：需 FOREGROUND_SERVICE_MICROPHONE 权限（AAR Manifest 已声明，宿主需合并）。

回执：将 步骤A 设备信息 + 步骤B 返回码 + logcat 关键行回填本报告 §6.1 末两行，R-001 即可正式关闭。
```

> ⚠ 我（Legacy-Native）**未**在真机执行上述步骤，因执行环境为 x86_64 主机、无 arm64 真机、无已安装 system-image/AVD、无连接设备（`adb devices` 空）。x86_64 模拟器因 §5 缺 .so 也无法替代。这两项必须由具备 arm64 真机的人闭环——已据实标注，未虚报 GO。

---

## 8. build.gradle 修改草案（验收项⑧）

**重要**：`app/build.gradle` 现状 `abiFilters = ["armeabi-v7a", "arm64-v8a"]` **已包含 arm64-v8a**，故**无需任何功能性 gradle 改动**——APK 已会打包 arm64 语音库。需修的只是**滞后注释**（审计 §3.3 已指出注释与产物矛盾）。

```diff
 ndk {
-    // armeabi-v7a is the htapplib.aar's only ABI (voice intercom native libs).
-    // arm64-v8a is added so the APK can install on 64-bit-only devices
-    // (Pixel 7+, modern emulators) for UI/non-voice testing during the
-    // multi-version upgrade. Voice intercom will not work on arm64 until
-    // the aar vendor ships 64-bit native libs (Layer 3).
-    // Baidu Map SDK already ships .so for both ABIs in app/libs/.
+    // htapplib.aar (2026-05-14) ships BOTH armeabi-v7a and arm64-v8a voice
+    // native libs (libaudioplay.so + libmp3lame.so); SPIKE-AAR64 verified the
+    // arm64-v8a libs are self-contained (no 32-bit-only deps) and export the
+    // same JNI surface as armeabi-v7a. Voice intercom (MP3 encode path) is
+    // therefore expected to work on arm64 — pending one real-device confirmation
+    // (see .state/spike-aar64-report.md §7.2). x86/x86_64 .so are NOT shipped,
+    // so voice/MP3 encode is unavailable on x86_64 emulators (dev-time only).
+    // Baidu Map SDK already ships .so for both ABIs in app/libs/.
     abiFilters = ["armeabi-v7a", "arm64-v8a"]
 }
```

> 此改动仅注释、零行为变更。按协议 §7.3，涉及 `httptask/*Method.java` 才需三方会签；本改动不触旧栈网络层。但 `abiFilters` 变更属我 `requires_pm_approval`（profile §3）——**本次未改 abiFilters 本身（已含 arm64），仅改注释**，故按常规 Critic 评审即可，无需 abiFilters 专项 PM 批准。落地时机建议并入 Phase 0 第一批（与 `BL-LAUNCHER-FIX` 同梯队）。

### 8.1 GO 不等于取消任何兜底（明确约束）

GO 只降低 R-001 风险，**不删除任何降级路径**。落地时必须坚持：

- **`armeabi-v7a` 必须保留在 `abiFilters` 中**：兜底现网 32 位真机（目标终端多为 armeabi-v7a），不可因新增 arm64 而移除。当前 `["armeabi-v7a", "arm64-v8a"]` 双 ABI 是正确终态——本草案不增不减 ABI。
- **64 位-only 设备的对讲降级 UI 路径必须保留**：`VoiceTalkAdapter.isAvailable()`（检测 `loadLibrary` 是否成功）+ 失败时的 `FallbackScreen` 仍是硬要求。原因：① x86_64 模拟器开发期始终不可用（§5）；② arm64 真机回执前按 LOW 风险兜底；③ 任何未来缺 .so 的 ABI 设备仍需优雅降级（soul §2.3 Degradation Grace）。**GO 改变的只是「arm64 真机预期可用」，不改变「不可用时不崩、显示降级 UI」这条铁律。**
- **跨域**：此降级路径由 Frontend-Business 消费（profile/skill §3.2），需经 PM 知会其在对讲屏保留 `isAvailable()` 判定 + 降级 UI；Kotlin 适配以 AAR 的 `CallBackIntf`（21 回调，§3）为准，而非 skill 模板的 `NativeTalkListener`。

---

## 9. 残留 / 移交事项

| 项 | 责任 | 优先级 | 说明 |
|----|------|:----:|------|
| 真机 loadLibrary + Mp3Encode* 闭环（§7.2） | QA/CTO（需 arm64 真机） | P0 | R-001 正式关闭的唯一前置 |
| x86_64 `.so` 是否向厂商索要 | CTO 决策 / PM 搭车厂商沟通 | P3 | 仅改善模拟器开发体验，非阻塞 |
| build.gradle 注释修正 + 档案同步（§6.2） | Legacy-Native（GO 后） | P1 | 本报告已备料，待 PM 建任务 |
| `UNK-001`（厂商能否供 64 位 AAR） | 可关闭 | — | 已证厂商**已**供 64 位（arm64-v8a），UNK-001 前提消解 |

---

## 10. 测试环境与工具记录（可追溯）

- 执行主机：x86_64 Linux（`uname -m` = x86_64），**无 arm64 物理真机**、`adb devices` 空、`~/Android/Sdk` 有 adb/emulator 但无 system-image/AVD。
- 静态工具：GNU `nm` / `readelf` / `file` / `unzip`（`/usr/bin`）；`javap` = JBR 17（`/home/it1234/.jdks/jbr-17.0.14/bin/javap`）。`jadx` 不可用 → 类逆向用 `javap -p/-c` 覆盖方法签名与 `static{}` 字节码（不含完整方法体）。
- 被测产物：`app/libs/htapplib.aar`（539,881 B，mtime 2026-05-14 11:21）。

---

*spike-aar64-report.md — Generated by Legacy-Native Agent · TASK-AR-SPIKE-001 · 等待 Critic 评审*
