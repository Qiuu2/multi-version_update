# ICD-VoiceAAR-v2 — Proposed Update (源标注 + LIVE/DRAFT 分类)

> TASK-AR-111 · Legacy-Native · 2026-05-28 · CTO D-12 ICD 二分法落地
> 用途：供 PM 串行写入 `aeroradio-workflow/references/icd-contracts.md §8`（STD-ICD-WRITE，我不抢写）。
> 原则：每字段/方法标**来源**（逆推自 AAR classes.jar javap / .so nm -D / v3 调用点行号）；可逆推→**LIVE**，无源/待外部→**DRAFT**。

---

## 总分类裁定

| 子面 | 分类 | 一句话理由 |
|------|:----:|-----------|
| HTIntf 控制面（寻呼/对讲/点播 + 生命周期，纯 Java） | **LIVE** | 全部可从 AAR javap + v3 调用点逆推，code-fact |
| CallBackIntf 21 回调 | **LIVE** | AAR javap 定义 + v3 `HTIntfHandler implements CallBackIntf` 逐回调实现，双源 |
| MediaCodec MP3 native 接口结构（Mp3Encode* 签名/符号/链接） | **LIVE（结构）** | javap 签名 + libaudioplay.so nm -D 符号 + spike 链接自洽，静态 code-fact |
| MP3 native **运行时执行**（真机 loadLibrary/编码产出） | **DRAFT-pending-device** | R-001 真机回执前不声称运行已验（无设备源） |
| VoiceTalkAdapter Kotlin 适配层（AR-104 落地） | **LIVE-pending-device** | 接口/桥接/gate 已编译+gate 测绿；happy-path native 候真机 |

---

## A. HTIntf 控制面 — **LIVE**（纯 Java，code-fact）

> 来源：`com.example.htapplib.HTIntf`（AAR classes.jar，`javap -p`）+ v3 调用点。SPIKE-AAR64 已证 HTIntf.* native 方法数=0（全纯 Java 控制面）。

| 方法（签名逆推自 javap） | 用途 | v3 调用点来源（行号） |
|------|------|----------------------|
| `static void setcallbackinterface(CallBackIntf)` | 注册回调 | `service/HTIntfHandler.java:283` `htIntf.setcallbackinterface(this)` |
| `static boolean init(Context, String, int, String, String, int, String[, byte])` | 连接初始化（7~8 参，**参数语义 OPEN**，见 §D） | （连接屏，语义待对照） |
| `static boolean connectserver()` / `getconnectstate()` | 连服务器 / 查连接态 | javap |
| `static void release()` | 释放 | javap |
| `static int startpaging()` / `stoppaging()` + `newpagingslist()` / `setpaginglistitem(int)` | 寻呼 | `method/MainMethod.java:25,27,31` (startCall→newpagingslist/setpaginglistitem/startpaging) |
| `static int startspeech()` / `stopspeech()` + `newspeechitem()` / `setspeechitem(int)` | 对讲 | `method/MainMethod.java:54,55,56`; `activity/CallOtherActivity.java:99` `htIntf.stopspeech()`; `activity/BeSpeechByOtherActivity.java:167,176` |
| `static int startondemand()` + `newondemandlist()` / `setondemandterminal(int)` / `setondemandmedia(int)` | 点播 | `method/MainMethod.java:35,39,46,49` |
| `static int getterminalid()` | 取本机终端 id | `utils/ArryListUtils.java:73` |
| `static int mp3encodeini(int,int,int,int)` / `mp3encodebuffer(short[],short[],int,byte[])` / `mp3encodebufferflush(byte[])` / `getmp3encodebuffersize(int,int)` / `mp3encoderelease()` | HTIntf 对 MediaCodec native 的薄封装转调 | javap（转入 §C native 面） |

**裁定 LIVE 理由**：方法签名全部可从 AAR javap 逆推；关键控制方法（startspeech/startpaging/startondemand + stop + 列表构造）均有 v3 真实调用点行号佐证。纯 Java，无 native 执行不确定性。

---

## B. CallBackIntf 21 回调 — **LIVE**（双源：AAR + v3 实现）

> 来源（双重）：① `com.example.htapplib.CallBackIntf`（AAR javap，21 个 abstract 方法）；② v3 `service/HTIntfHandler.java:45` `implements CallBackIntf`，**逐回调有真实实现**（行号如下）。这是最强 code-fact——不仅 AAR 定义，v3 已落地消费。

| 回调（签名逆推自 AAR javap） | v3 HTIntfHandler 实现行号 | VoiceTalkAdapter 映射（AR-104） |
|------|:----:|------|
| `void onstartspeech(String)` | :134 | VoiceState.Active（对讲） |
| `void onstopspeech()` | :152 | VoiceState.Ended + close |
| `void onspeechwait()` | :225 | VoiceState.Waiting |
| `void onspeechrefuse()` | :232 | VoiceState.Refused + close |
| `void onspeechrequest(String)` | :276 | VoiceDeviceEvent.IncomingSpeechRequest |
| `void onstartencode()` / `onstopencode()` | :83 / :99 | VoiceState.Active / Ended（mic 编码器） |
| `void onstartondemand(String)` / `onstopondemand()` | :243 / :264 | （点播，非 session 范畴） |
| `void onstartplay(String)` / `onstopplay()` | :109 / :125 | （播放） |
| `void onstartshortcuttask(String)` / `onstopshortcuttask()` | :159 / :173 | （快捷任务） |
| `void onlogin(int)` | :179 | VoiceDeviceEvent.LoggedIn |
| `void onconnect(boolean)` | :291 | VoiceDeviceEvent.Connected |
| `void oncmderror(int)` | :311 | VoiceState.Error + VoiceDeviceEvent.CommandError |
| `void onsetvolume(int)` | :68 | VoiceDeviceEvent.VolumeChanged |
| `void onsetdevicestate(int)` | :56 | （设备态） |
| `void onreboot()` | :60 | （重启） |
| `void onsettime(short,byte,byte,byte,byte,byte)` | :64 | （校时） |
| `void onupdate(String)` | :75 | （更新） |

**计数核对**：AAR javap = 21 abstract；HTIntfHandler 实现 = 21（上表全覆盖）。一致。
**裁定 LIVE 理由**：契约由 AAR 定义且 v3 已逐回调实现——无任何推测成分。DP-4 钉死：适配以此 CallBackIntf 为准，非 NativeTalkListener。

---

## C. MediaCodec MP3 native — **LIVE（结构）/ DRAFT-pending-device（执行）**

> 来源：`com.example.htapplib.MediaCodec`（AAR javap）+ `jni/arm64-v8a/libaudioplay.so`（nm -D）+ SPIKE-AAR64 §2/§4。

**结构面（LIVE）**——4 个 native 方法签名 + JNI 符号一一对应：

| MediaCodec native 方法（javap） | libaudioplay.so 导出 JNI 符号（nm -D） |
|------|------|
| `native int Mp3EncodeInit(int,int,int,int,int)` | `Java_com_example_htapplib_MediaCodec_Mp3EncodeInit` |
| `native int Mp3EncodeBuffer(short[],short[],int,byte[])` | `Java_com_example_htapplib_MediaCodec_Mp3EncodeBuffer` |
| `native int Mp3EncodeBufferFLush(byte[])` | `Java_com_example_htapplib_MediaCodec_Mp3EncodeBufferFLush` |
| `native void Mp3EncodeRelease()` | `Java_com_example_htapplib_MediaCodec_Mp3EncodeRelease` |

- 加载：`MediaCodec.static{}` = `System.loadLibrary("mp3lame")` → `System.loadLibrary("audioplay")`（spike §3.2 字节码）。
- 链接自洽：libaudioplay 的 10 个 lame_* undefined 全由同目录 libmp3lame.so 导出，余 3 个系统符号（spike §4，Critic 已独立复现）。
- arm64 JNI 导出与 armeabi-v7a diff=IDENTICAL（spike §2.1）。

**裁定结构 LIVE**：签名/符号/链接全静态可逆推、Critic 已复核。

**执行面（DRAFT-pending-device）**——真机 loadLibrary 成功 + Mp3EncodeInit/Buffer 实际产出 + 无 SIGSEGV：**无设备源**，候 R-001 真机回执（`.state/realdevice-checklist-handoff.md §7.2`）。R-001 回执前不声称"运行已验"。

---

## D. 明标「无源/待外部」项（DRAFT，不可逆推）

| 项 | 状态 | 待什么 |
|----|------|--------|
| MP3 native 运行时执行 | DRAFT-pending-device | R-001 arm64 真机回执（§C 执行面） |
| `HTIntf.init(...)` 7~8 参数语义（host/port/sn/version?） | DRAFT-OPEN | 对照旧连接屏（SignActivity/ConnectActivity）实读 or 厂商确认；不影响 adapter gate/桥接结构 |

---

## 提议的注册表清单行（§1 表）

`| ICD-VoiceAAR-v2 | htapplib.aar Kotlin 适配层（CallBackIntf 桥接 + MP3 native） | legacy-native | frontend-business | LIVE（控制面+回调+native结构）/ DRAFT-pending-device（native执行+init参数）|`

---

*icd-voiceaar-v2-proposed.md — Legacy-Native · TASK-AR-111 · 待 Critic 核 + PM 串写注册表*
