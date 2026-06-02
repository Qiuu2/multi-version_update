---
name: legacy-native-skill
description: Legacy-Native Agent 的技能配置 — TCP socket 协程封装 / 语音 AAR 适配 / 32 位 ABI / 百度地图 / Native 安全。
version: 1.0.0
---

# Legacy-Native Agent — Skill

## 1. TCP Socket 4521 Kotlin 协程封装

### 1.1 公开 API（ICD-IPCSocket-v1）

```kotlin
// data/ipc/LocalSocketClient.kt
interface LocalSocketClient {
    val connectionStateFlow: StateFlow<ConnectionState>
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun sendCommand(cmd: ShellCommand): Result<String>
}

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

sealed class ShellCommand(val raw: String) {
    object StartPaging : ShellCommand("paging start")
    data class StopPaging(val sessionId: String) : ShellCommand("paging stop $sessionId")
    data class SetVolume(val vol: Int) : ShellCommand("volume set $vol")
    data class PlayMedia(val mediaPath: String) : ShellCommand("play $mediaPath")
    object StopMedia : ShellCommand("stop")
    object QueryStatus : ShellCommand("status")
    // 更多命令待从 utils/SocketClient.java 旧实现提炼
}
```

### 1.2 实现要点

```kotlin
@Singleton
class LocalSocketClientImpl @Inject constructor(
    @ApplicationContext private val ctx: Context
) : LocalSocketClient {

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionStateFlow = _state.asStateFlow()

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private val ioMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectAttempt = 0

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            if (_state.value == ConnectionState.CONNECTED) return@withLock Result.success(Unit)
            _state.value = ConnectionState.CONNECTING
            runCatching {
                val s = Socket()
                s.connect(InetSocketAddress("127.0.0.1", 4521), 5_000)
                socket = s
                reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                writer = PrintWriter(OutputStreamWriter(s.getOutputStream(), Charsets.UTF_8), true)
                _state.value = ConnectionState.CONNECTED
                reconnectAttempt = 0
            }.onFailure {
                _state.value = ConnectionState.ERROR
                scheduleReconnect()
            }
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        ioMutex.withLock {
            runCatching {
                writer?.close()
                reader?.close()
                socket?.close()
            }
            socket = null
            reader = null
            writer = null
            _state.value = ConnectionState.DISCONNECTED
        }
    }

    override suspend fun sendCommand(cmd: ShellCommand): Result<String> =
        withContext(Dispatchers.IO) {
            ioMutex.withLock {
                runCatching {
                    val w = writer ?: error("Socket not connected")
                    val r = reader ?: error("Socket not connected")
                    w.println(cmd.raw)
                    w.flush()
                    // 简化：单行响应；实际需根据协议补完
                    r.readLine() ?: error("Connection closed")
                }
            }
        }

    private fun scheduleReconnect() {
        if (reconnectAttempt >= 3) return
        reconnectAttempt++
        scope.launch {
            delay(2_000L * reconnectAttempt)
            connect()
        }
    }
}
```

### 1.3 使用约束

- **仅前台 Activity 时连接**：节省资源、避免 ANR
- **5s 连接超时**：避免无限等
- **命令幂等性**：同一命令重发不应产生副作用
- **自动重连 3 次**：超过则等下次主动 connect()

---

## 2. libs/htapplib.aar 语音对讲适配

### 2.1 接口逆向流程

```yaml
reverse_engineering_workflow:
  step_1_extract:
    - "解压 AAR：unzip libs/htapplib.aar -d /tmp/htapp"
    - "查看 classes.jar 内容：jar tf /tmp/htapp/classes.jar"
    - "用 jadx-gui 反编译 classes.jar"
    - "查看 jni/* 下的 .so 文件列出 ABI"

  step_2_document:
    - "记录每个 public class 的方法签名"
    - "推测每个方法的作用（参数 + 返回值）"
    - "标注哪些方法可能阻塞"
    - "记录到 memory.md 的 aar_reverse_engineering 节"

  step_3_adapter:
    - "设计 Kotlin idiomatic 接口（VoiceTalkAdapter）"
    - "包装 native 调用，全部 try-catch"
    - "暴露 Flow 状态而非回调"

  step_4_verify:
    - "在 32 位设备上跑通"
    - "在 64 位设备上确认降级"
```

### 2.2 公开 API（ICD-VoiceAAR-v1）

```kotlin
// data/voice/VoiceTalkAdapter.kt
interface VoiceTalkAdapter {
    fun isAvailable(): Boolean
    suspend fun startTalk(targetIds: List<String>): Result<TalkSession>
    suspend fun endTalk(session: TalkSession)
    fun observeTalkState(session: TalkSession): Flow<TalkState>
}

data class TalkSession(val sessionId: String, val startedAt: Long)

sealed class TalkState {
    object Connecting : TalkState()
    object Talking : TalkState()
    data class Error(val cause: Throwable) : TalkState()
    object Ended : TalkState()
}
```

### 2.3 实现要点

```kotlin
@Singleton
class VoiceTalkAdapterImpl @Inject constructor(
    @ApplicationContext private val ctx: Context
) : VoiceTalkAdapter {

    private val available: Boolean by lazy { detectAvailability() }

    private fun detectAvailability(): Boolean {
        // 1. 检查设备是否支持 32 位 ABI
        if (Build.SUPPORTED_32_BIT_ABIS.isEmpty()) return false
        // 2. 尝试加载 AAR 内的 native 库
        return try {
            System.loadLibrary("htapp")  // 实际库名以 AAR 内容为准
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    override fun isAvailable(): Boolean = available

    override suspend fun startTalk(targetIds: List<String>): Result<TalkSession> =
        withContext(Dispatchers.IO) {
            if (!available) return@withContext Result.failure(NativeUnavailableException())
            runCatching {
                // 调用 AAR 提供的 native 方法（待逆向确认接口名）
                val sessionId = NativeBridge.startTalk(targetIds.toTypedArray())
                TalkSession(sessionId, System.currentTimeMillis())
            }
        }

    override suspend fun endTalk(session: TalkSession) = withContext(Dispatchers.IO) {
        runCatching { NativeBridge.endTalk(session.sessionId) }
        Unit
    }

    override fun observeTalkState(session: TalkSession): Flow<TalkState> = callbackFlow {
        if (!available) {
            trySend(TalkState.Error(NativeUnavailableException()))
            close()
            return@callbackFlow
        }
        val listener = object : NativeTalkListener {
            override fun onConnecting() { trySend(TalkState.Connecting) }
            override fun onTalking() { trySend(TalkState.Talking) }
            override fun onError(t: Throwable) { trySend(TalkState.Error(t)) }
            override fun onEnded() { trySend(TalkState.Ended); close() }
        }
        try {
            NativeBridge.registerListener(session.sessionId, listener)
        } catch (e: Throwable) {
            trySend(TalkState.Error(e))
            close()
        }
        awaitClose {
            runCatching { NativeBridge.unregisterListener(session.sessionId) }
        }
    }
}

class NativeUnavailableException :
    RuntimeException("Native library not available on this device")
```

> **注**：`NativeBridge` 是对 AAR 实际 API 的占位封装，确切方法名需逆向后填入。

---

## 3. 32 位 ABI 配置

### 3.1 build.gradle.kts

```kotlin
android {
    defaultConfig {
        // 限制只打包 32 位 ABI（与 htapplib.aar 一致）
        ndk {
            abiFilters += listOf("armeabi-v7a", "x86")
        }
    }

    // 同时为 64 位设备发布单独 split 可选
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "x86")
            isUniversalApk = true
        }
    }

    // 打包配置
    packaging {
        jniLibs {
            useLegacyPackaging = false
            // 排除 64 位库（即使依赖里有）
            excludes += listOf("**/arm64-v8a/**", "**/x86_64/**")
        }
    }
}
```

### 3.2 64 位-only 设备降级

```kotlin
// 在 Frontend-Business 的对讲屏消费
val voiceTalkAdapter: VoiceTalkAdapter = hiltViewModel().voiceTalkAdapter
if (!voiceTalkAdapter.isAvailable()) {
    // 显示降级 UI
    FallbackScreen(
        title = "对讲不可用",
        subtitle = "此设备不支持 32 位库",
        details = "您可以使用：寻呼 / 点播 / 任务"
    )
}
```

### 3.3 Google Play 上架策略

```yaml
play_store_strategy:
  challenge: "Google Play 要求 APK 包含 64 位库"
  options:
    option_A:
      name: "向厂商索要 64 位 AAR"
      best_case: true
      escalate_to: "CTO + 厂商"

    option_B:
      name: "Play Store 上架 64 位包，去掉对讲；32 位包通过其他渠道分发"
      tradeoff: "Play Store 用户无对讲"

    option_C:
      name: "服务端中转方案（不再用 native）"
      tradeoff: "需要后端配合 + 重新设计协议"

  decision_owner: "CTO"
  trigger_for_decision: "Phase 0 结束前必须有答复"
```

---

## 4. 百度地图 SDK 集成

### 4.1 合规接入流程

```kotlin
// Application.onCreate
class AeroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 不要在这里直接初始化百度 SDK
        // 必须先获得用户合规同意
    }
}

// 首次启动时弹合规弹窗
@Composable
fun ComplianceConsentDialog(onAccept: () -> Unit, onReject: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* 不允许外部 dismiss */ },
        title = { Text("隐私合规", style = AeroType.SectionTitle) },
        text = { Text("应用使用百度地图 SDK 提供定位与地图功能，需收集设备识别码与位置信息...") },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("同意") }
        },
        dismissButton = {
            TextButton(onClick = onReject) { Text("拒绝") }
        }
    )
}

// 同意后才初始化
fun initBaiduMapSDK(ctx: Context) {
    SDKInitializer.setAgreePrivacy(ctx, true)
    SDKInitializer.initialize(ctx)
}
```

### 4.2 MapClient

```kotlin
// data/map/MapClient.kt
interface MapClient {
    fun isReady(): Boolean
    suspend fun setMarkers(markers: List<TerminalMarker>): Result<Unit>
}

data class TerminalMarker(
    val terminalId: String,
    val lat: Double,
    val lng: Double,
    val state: TerminalState,
    val title: String
)
```

### 4.3 LocationProvider（ICD-MapLocation-v1）

```kotlin
// data/map/LocationProvider.kt
interface LocationProvider {
    suspend fun getCurrentLocation(): Result<LocationDto>
    fun observeLocation(): Flow<LocationDto>
    suspend fun requestPermission(activity: ComponentActivity): Boolean
}

data class LocationDto(
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val timestamp: Long
)
```

### 4.4 权限延迟请求

```kotlin
// 在进入终端地图视图时才请求，而不是 Application 启动时
@Composable
fun TerminalMapView(locationProvider: LocationProvider) {
    val activity = LocalContext.current as ComponentActivity
    var permissionGranted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        permissionGranted = locationProvider.requestPermission(activity)
    }

    if (!permissionGranted) {
        // 显示降级 UI：仅终端列表，无地图
        FallbackToListView()
    } else {
        // 显示百度地图
        BaiduMapComposable()
    }
}
```

### 4.5 API Key 安全

```kotlin
// AndroidManifest.xml
<meta-data
    android:name="com.baidu.lbsapi.API_KEY"
    android:value="${BAIDU_MAP_API_KEY}" />

// build.gradle.kts
android {
    defaultConfig {
        manifestPlaceholders["BAIDU_MAP_API_KEY"] = project.findProperty("baiduMapApiKey") ?: "DEBUG_KEY"
    }
}

// gradle.properties (NOT in git)
// baiduMapApiKey=XXXXXXXXXX
```

---

## 5. Native 安全 / Crashlytics NDK

### 5.1 全局捕获策略

```kotlin
// 所有 native 调用统一封装
suspend fun <T> safeNativeCall(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
    try {
        Result.success(block())
    } catch (e: UnsatisfiedLinkError) {
        FirebaseCrashlytics.getInstance().recordException(e)
        Result.failure(e)
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        Result.failure(e)
    }
    // 注意：SIGSEGV 等 native crash 无法 try-catch，只能靠 Crashlytics NDK 捕获后上报
}
```

### 5.2 Crashlytics NDK 启用

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ndk")
}

android {
    buildTypes {
        debug {
            firebaseCrashlytics {
                nativeSymbolUploadEnabled = true
                unstrippedNativeLibsDir = "build/intermediates/merged_native_libs/debug/out/lib"
            }
        }
        release {
            firebaseCrashlytics {
                nativeSymbolUploadEnabled = true
            }
        }
    }
}
```

---

## 6. 资源生命周期管理

```kotlin
// 示例：socket 随 ViewModel 销毁释放
@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val localSocketClient: LocalSocketClient
) : ViewModel() {
    init {
        viewModelScope.launch { localSocketClient.connect() }
    }
    override fun onCleared() {
        super.onCleared()
        // 在自己的 scope 里 disconnect
        CoroutineScope(Dispatchers.IO).launch {
            localSocketClient.disconnect()
        }
    }
}

// 示例：native 监听器随 Flow collect 取消而注销
fun observeTalkState(session: TalkSession): Flow<TalkState> = callbackFlow {
    val listener = createListener()
    NativeBridge.registerListener(session.sessionId, listener)
    awaitClose {
        // 必须在 awaitClose 里注销，否则 native 内存泄漏
        NativeBridge.unregisterListener(session.sessionId)
    }
}
```

---

## 7. 提交流程

```yaml
submission_workflow:
  before_submit:
    - "32 位真机 + 64 位真机 + 模拟器三端跑通"
    - "Native 调用全部 try-catch（grep System.loadLibrary 后必看包裹）"
    - "Crashlytics NDK 已启用并上传 symbol"
    - "百度地图合规弹窗已实现"
    - "API Key 不在 git（grep BAIDU_MAP_API_KEY 检查 gradle.properties 是否 .gitignore）"
    - "ICD 更新到 references/icd-contracts.md"
    - "AAR 接口逆向记录到 memory.md"
```

---

*Legacy-Native Agent Skill v1.0.0*
