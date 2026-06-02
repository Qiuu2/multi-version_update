# Real-Device Verification Handoff — htapplib.aar arm64-v8a 语音库终验

> 交接工单 · TASK-AR-008 产出 · Owner(执行)：CTO / QA（持 arm64 真机）· 来源：`.state/spike-aar64-report.md` §7.2
> 目的：完成 SPIKE-AAR64 唯一未闭环的 2 项真机动态验证，据此**正式关闭 R-001**（决策 D-06）。
> 预计耗时：一台 arm64 真机 ~10 分钟。

---

## 0. 背景（一句话）

SPIKE-AAR64 已用静态证据证明 `app/libs/htapplib.aar` 的 arm64-v8a 64 位语音库**结构完整、链接自洽、JNI 入口与 32 位等价**（结论 GO-pending-device-confirmation）。静态层面无反对 GO 的证据；**只差一台 arm64 真机做动态终验**——本工单即为此。开发执行环境无 arm64 真机，故转交持真机者。

## 1. 前提条件

| 项 | 要求 |
|----|------|
| 设备 | 一台 **arm64-v8a** Android 真机（如 Pixel 7 / 主流国产 arm64 机）。**不能用 x86_64 模拟器**——AAR 无 x86_64 .so，必 UnsatisfiedLinkError（这是预期，非 bug）。 |
| 系统 | Android 7.0+（AAR minSdk 21） |
| 构建 | 装一个含 `abiFilters=["armeabi-v7a","arm64-v8a"]` 的 debug APK（现 `app/build.gradle` 已是此配置） |
| 工具 | `adb`（连真机）、logcat |
| 权限（步骤 C 用） | Android 14+ 需 `FOREGROUND_SERVICE_MICROPHONE`（AAR Manifest 已声明，宿主合并即可）；运行期授予 `RECORD_AUDIO` |

## 2. 设备确认（先记录，回执用）

```bash
adb devices                                  # 确认设备在线
adb shell getprop ro.product.cpu.abi         # 期望: arm64-v8a
adb shell getprop ro.product.model           # 记录型号
adb shell getprop ro.build.version.release   # 记录 Android 版本
```

判定：`ro.product.cpu.abi` = `arm64-v8a` 方为有效终验设备（否则不是 arm64 真机，作废）。

---

## 3. 步骤 A — loadLibrary 验证（闭合验收项②）

**做什么**：触发 `MediaCodec` 的静态初始化（内部依次 `System.loadLibrary("mp3lame")` → `System.loadLibrary("audioplay")`），确认 64 位 .so 能在 arm64 真机上 dlopen 成功。

**最小代码**（塞进 V4Activity 一次性测试入口，或一个 instrumentation 测试）：
```kotlin
try {
    com.example.htapplib.MediaCodec.a()   // 触发 static{} 两次 loadLibrary
    android.util.Log.i("SPIKE_AAR64", "STEP_A_OK: libraries loaded")
} catch (t: Throwable) {
    android.util.Log.e("SPIKE_AAR64", "STEP_A_FAIL", t)
}
```

**抓日志**：
```bash
adb logcat -c && adb logcat | grep -iE "SPIKE_AAR64|UnsatisfiedLink|dlopen|library.*not found"
```

| 判定 | PASS | FAIL |
|------|------|------|
| 步骤 A | logcat 出 `STEP_A_OK`，无 `UnsatisfiedLinkError`、无 `dlopen failed / library "...so" not found` | 出现 UnsatisfiedLinkError 或 dlopen failed → **NO-GO**，记录完整 stacktrace 与 .so 名 |

---

## 4. 步骤 B — Mp3Encode* 调用链验证（闭合验收项③）

**做什么**：实调 4 个 native 方法走通一次「初始化→编码→flush→释放」，确认 JNI 不只是加载、还能正确执行（这才是「可用」而非仅「不崩」）。

**最小代码**：
```kotlin
val mc = com.example.htapplib.MediaCodec.a()
// 采样参数可对照旧栈 activity/TempTTSActivity.java 的实际取值
val r1 = mc.Mp3EncodeInit(/*inSampleRate*/8000, /*ch*/1, /*outSampleRate*/8000, /*?*/16, /*quality*/5)
val pcmL = ShortArray(1024)              // 测试可填静音/正弦波样本
val pcmR = ShortArray(1024)
val out  = ByteArray(8192)
val n    = mc.Mp3EncodeBuffer(pcmL, pcmR, 1024, out)   // 返回写入 out 的字节数
val f    = mc.Mp3EncodeBufferFLush(out)
mc.Mp3EncodeRelease()
android.util.Log.i("SPIKE_AAR64", "STEP_B: init=$r1 encodedBytes=$n flushBytes=$f")
```

| 判定 | PASS | FAIL |
|------|------|------|
| 步骤 B | `Mp3EncodeInit` 返回成功码、`Mp3EncodeBuffer` 返回 `n > 0`（产出 MP3 字节）、全程无 native crash（SIGSEGV/SIGABRT） | 任一返回错误码、`n<=0`、或进程 native crash → **NO-GO**，记录 tombstone/logcat |

> 注：参数语义未文档化（厂商黑盒）。若初值不产出字节，先对齐 `TempTTSActivity.java` 的实际调用参数再重试——这属参数调校，不影响「库可用」判定（库已 loadLibrary 成功且方法可进入即证 64 位 JNI 工作）。

---

## 5. 步骤 C — 端到端对讲（可选，验收③加强）

**做什么**：走真实业务路径，确认前台录音服务 + 回调链在 arm64 真机正常。

```
触发 method/MainMethod.startSpeech(chooseMachineList)
  → HTIntf.startspeech()（纯 Java 控制）
  → EncodeService 起（foregroundServiceType=microphone）
  → 录音 → MediaCodec.Mp3Encode*（native）
观察：CallBackIntf.onstartspeech(String) 回调是否到达；通知栏前台服务是否出现。
```

| 判定 | PASS | FAIL |
|------|------|------|
| 步骤 C | EncodeService 正常起、`onstartspeech` 回调到达、无 crash | 服务起不来 / 无回调 / crash → 记录现象（注意先排除权限/网络/服务器侧因素，非必然 native 问题） |

> 步骤 C 依赖服务器连通（`HTIntf.init/connectserver`），可能受 LAN 后端可达性影响；若环境不便，步骤 A+B 通过已足够支撑 R-001 关闭，C 作加强项。

---

## 6. 回执回填（执行后填此表，交回 PM / Legacy-Native）

| 字段 | 填写 |
|------|------|
| 设备型号 / Android 版本 | __________ |
| `ro.product.cpu.abi` | __________（应 arm64-v8a） |
| 步骤 A 结果（loadLibrary） | ☐ PASS ☐ FAIL；logcat 关键行：__________ |
| 步骤 B 结果（Mp3Encode*） | ☐ PASS ☐ FAIL；init=__ encodedBytes=__ flushBytes=__ |
| 步骤 C 结果（端到端，可选） | ☐ PASS ☐ FAIL ☐ 未做；现象：__________ |
| 终验结论 | ☐ GO（R-001 关闭） ☐ NO-GO（维持降级 + 触发厂商 escalation） |
| 执行人 / 日期 | __________ |

**关闭路径**：步骤 A+B PASS → R-001 由 LOW-条件性正式关闭（D-06），回填 `spike-aar64-report.md` §6.1 末两行 + 通知 PM 更新 decision-log。
**NO-GO 路径**：任一关键步骤 FAIL → R-001 维持，附 stacktrace，触发向厂商的 escalation（语音降级 UI 路径继续生效，见 spike 报告 §8.1）。

---

## 7. 重要提醒（不要误判）

- **x86_64 模拟器上失败是预期**：AAR 不含 x86_64 .so，本工单**只在 arm64 真机有效**。
- **降级路径不取消**：无论 GO 与否，`VoiceTalkAdapter.isAvailable()` + 64 位-only/缺 .so 设备的降级 UI 必须保留（spike 报告 §8.1）。GO 只降风险，不删兜底。
- **静态证据已在**：符号导出、链接自洽、ABI 完整性见 `spike-aar64-report.md` §1/§2/§4，无需重复；本工单只补「真机动态」这一块。

---

*realdevice-checklist-handoff.md — TASK-AR-008 · Legacy-Native Agent · 交 CTO/QA 真机执行*
