# htapplib.aar 内部结构分析

## 为什么有这份文档

`app/libs/htapplib.aar` 是当前对讲功能的核心,但**原作者已离职,源码下落不明**。在
找到源码之前,任何修改/迁移决策都需要基于黑盒分析。本文档记录通过反编译得到的事实,
供后续接手人参考,**避免重复挖掘**。

文档只描述"是什么",不做修改建议——修改方案见 `RISKS.md`。

---

## 1. 来源判断

| 证据 | 结论 |
|---|---|
| 包名 `com.example.htapplib` | `com.example` 是 Android Studio 默认占位包名,几乎不可能是外购商业 SDK |
| `META-INF/.../aar-metadata.properties` 中 `minAndroidGradlePluginVersion=1.0.0` | 编译时 AGP 1.0,即 **2014 年前后** |
| 内部混淆类命名 `a.class` ~ `t.class` | 编译时开启 ProGuard 混淆,源码不在 aar 内 |
| 公开类完全不混淆(HTIntf 等) | 作者保留了 SDK 公开 API 不混淆,符合内部库做法 |

**判断:公司内部老员工自研,约 2014 年,源码外置(可能在公司 Gitlab / 离职同事电脑 / 老备份)**

## 2. 物理结构

```
htapplib.aar (zip)
├── AndroidManifest.xml          声明 EncodeService、DecodeService 两个 Service
├── classes.jar                  Java 字节码
│   ├── com/example/htapplib/    公开 API(5 个类,未混淆)
│   └── a~t.class                内部实现(24 个类,ProGuard 混淆)
├── jni/
│   ├── armeabi/libmp3lame.so          ← 仅 32 位
│   └── armeabi-v7a/
│       ├── libaudioplay.so            ← 仅 32 位
│       ├── liblog.so                  ← 仅 32 位
│       └── libmp3lame.so              ← 仅 32 位
└── res/values/values.xml        资源(空)
```

**没有 arm64-v8a / x86 / x86_64 的 .so —— 这是当前升级到 Android 14+ 的核心阻塞。**

## 3. 公开 API 契约

### 3.1 HTIntf — 主门面(全部 static 方法)

#### 生命周期
```java
boolean init(Context, String ip, int port, String, String, int, String)
boolean init(Context, String ip, int port, String, String, int, String, byte serverversion)
boolean init1(...)               // 与 init 类似,可能是另一种连接模式
void release()
```

#### 配置
```java
void setcallbackinterface(CallBackIntf)   // 注册事件回调
void setversion(String)
void setserverversion(byte)
void setdevicesn(String)
void setdefaultvolume(int)
void setNotification(Notification, Notification)   // ★ 两个 Notification 对应 EncodeService 和 DecodeService 的前台服务通知
```

#### 服务器连接
```java
boolean checkregister()
boolean connectserver()
boolean getconnectstate()
int getterminalid()
int getterminaltype()
byte getworkstate()
```

#### 业务操作(对讲核心动作)

| 操作组 | 方法 | 含义推断 |
|---|---|---|
| 巡呼 paging | `start/stop/newpagingslist/newpagingmorelist/sendpagingmorelist/setpaginglistitem/setpagingterminalvolume` | 一对多广播 |
| 语音 speech | `start/stop/acceptspeechrequest/newspeechitem/setspeechitem` | 双向通话 |
| 快捷任务 shortcuttask | `start/stop/newshortcuttaskitem/setshortcuttaskitem/setshortcuttaskvolume` | 预设任务触发 |
| 点播 ondemand | `start/stop/newondemandlist/setondemandterminal/setondemandmedia/setondemandvolume` | 媒体点播 |
| 快捷巡呼 shortcutpaging | `start/stop/newshortcutpagingitem/setshortcutpagingitem` | 预设巡呼 |

#### MP3 编码(直通 native)
```java
int  mp3encodeini(int, int, int, int)
void mp3encoderelease()
int  mp3encodebuffer(short[] left, short[] right, int samples, byte[] outbuf)
int  getmp3encodebuffersize(int, int)
int  mp3encodebufferflush(byte[] outbuf)
```

### 3.2 EncodeService — 麦克风 → MP3 前台服务

```
继承: android.app.IntentService
关键字段:
  - AudioRecord                  ← 麦克风采集
  - AcousticEchoCanceler         ← 回声消除
  - NoiseSuppressor              ← 噪声抑制
  - AutomaticGainControl         ← 自动增益
  - Notification                 ← 前台服务通知(由 HTIntf.setNotification 注入)
```

**功能:** 实时录音 → 应用音频效果 → MP3 编码 → 推流(具体推流逻辑在混淆类)

### 3.3 DecodeService — 接收 → AudioTrack 播放前台服务

```
继承: android.app.IntentService
关键字段:
  - AudioTrack                   ← PCM 播放(注意:不依赖 native 解码)
  - Notification                 ← 前台服务通知
```

**功能:** 接收音频流 → 解码 → AudioTrack 播放

### 3.4 MediaCodec — JNI 桥(native 绑定都在这)

```java
public native int  Mp3EncodeInit(int, int, int, int, int)
public native void Mp3EncodeRelease()
public native int  Mp3EncodeBuffer(short[], short[], int, byte[])
public native int  Mp3EncodeBufferFLush(byte[])
```

**static 初始化加载**:`libmp3lame` 和 `libaudioplay`(后者无 JNI 绑定,疑似 LAME 传递依赖或副作用加载)

### 3.5 CallBackIntf — 事件回调(app 实现,SDK 调用)

```java
// 连接事件
void onlogin(int)
void onconnect(boolean)
void oncmderror(int)

// 操作生命周期
void onstartencode() / onstopencode()
void onstartplay(String) / onstopplay()
void onstartspeech(String) / onstopspeech()
void onstartshortcuttask(String) / onstopshortcuttask()
void onstartondemand(String) / onstopondemand()
void onspeechwait()
void onspeechrefuse()
void onspeechrequest(String)

// 设备控制(服务器下发)
void onsetvolume(int)
void onsettime(short y, byte m, byte d, byte h, byte min, byte s)
void onreboot()
void onsetdevicestate(int)
void onupdate(String)
```

## 4. Native 库职能推断

| .so | 已知事实 | 推断 |
|---|---|---|
| `libmp3lame.so` | 4 个 JNI 方法均为 MP3 编码 | 开源 LAME MP3 编码器,**有现成 64 位 prebuilt** |
| `libaudioplay.so` | MediaCodec 加载但无 JNI 方法绑定 | 可能是 LAME 的传递依赖,或 native 端某种初始化副作用,**不直接被 Java 调用** |
| `liblog.so` | 未发现任何 Java 引用 | 可能是 native 内部日志输出工具 |

**关键结论:整个 aar 对外暴露的 native 能力只有 4 个 MP3 编码方法。**

## 5. 不变性边界(改造时不能动的)

如果替换/重写 htapplib,以下契约必须保留,否则 app 端代码会编译失败或运行崩溃:

1. **包名和类名**: `com.example.htapplib.{HTIntf, EncodeService, DecodeService, MediaCodec, CallBackIntf}`
2. **HTIntf 的所有 public static 方法签名** —— app 大量直接调用
3. **CallBackIntf 接口的所有方法** —— app 实现这个接口
4. **EncodeService / DecodeService 的 IntentService 继承和 onCreate / onHandleIntent 行为**
5. **AndroidManifest 中两个 Service 的注册名和 intent-filter action**(`audioencode`, `audiodecode`)

只要保留以上契约,内部实现可以自由重写。

## 6. 建议后续动作

详见 `RISKS.md`。简述:

1. **首选**: 公司内部找 htapplib 源码(Gitlab 搜索、IT 离职同事电脑、技术群求助)
2. **次选**: 反编译混淆类 + 重写 → 重新打包为 64 位 aar(仅当源码彻底找不到)
3. **远期**: 评估是否换用现代音频库(Android `MediaCodec` API + WebRTC 等)替代整个 htapplib

---

*本文档由对 `htapplib.aar` 1c4a... 版本的反编译分析得出。aar 文件未做任何修改。*
