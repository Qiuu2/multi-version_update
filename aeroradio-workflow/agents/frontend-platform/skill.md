---
name: frontend-platform-skill
description: Frontend-Platform Agent 的技能配置 — WS / Theme / AdaptiveScaffold / Notification / 错误处理。
version: 1.0.0
---

# Frontend-Platform Agent — Skill

## 1. AeroTheme 实现

### 1.1 文件结构

```
app/src/main/kotlin/com/aeroradio/ui/theme/
├── AeroColors.kt        # design-system-spec.md §1
├── AeroShapes.kt        # §2
├── AeroSpacing.kt       # §3
├── AeroType.kt          # §4
├── AeroElevation.kt     # §5
├── AeroMotion.kt        # §6
└── AeroTheme.kt         # 组合
```

### 1.2 关键代码模板

```kotlin
// AeroColors.kt
object AeroColors {
    // Neutral
    val Background = Color(0xFFF4F5F7)
    val Surface = Color(0xFFFFFFFF)
    val Surface2 = Color(0xFFF8FAFB)
    val Surface3 = Color(0xFFEEF0F3)
    val Ink = Color(0xFF0D1117)
    val Ink2 = Color(0xFF4A5260)
    val Ink3 = Color(0xFF8A929F)
    val Ink4 = Color(0xFFB8BEC8)

    // Brand & Status
    val Primary = Color(0xFF0E7C70)
    val PrimaryInk = Color(0xFF095C54)
    val PrimarySoft = Color(0xFFE6F4F2)
    val StatusOnline = Color(0xFF16A34A)
    val StatusOffline = Color(0xFF8A929F)
    val StatusFault = Color(0xFFDC2626)
    val StatusPlaying = Color(0xFF2563EB)
    val StatusPaging = Color(0xFFEA580C)

    // Tab colors
    val TabTerminal = Color(0xFF0E7C70)
    val TabBroadcast = Color(0xFFEA580C)
    val TabAI = Color(0xFF14B8A6)
    val TabTask = Color(0xFF7C3AED)
    val TabService = Color(0xFF2563EB)

    // Broadcast inner modes
    val ModePage = Color(0xFFEA580C)
    val ModeTalk = Color(0xFF2563EB)
    val ModeCast = Color(0xFF0E7C70)

    // Special
    val BgBeige = Color(0xFFF0EEE9)
}
```

```kotlin
// AeroType.kt
object AeroType {
    private val SansSC = FontFamily(Font(R.font.noto_sans_sc))
    private val JBMono = FontFamily(Font(R.font.jetbrains_mono))

    val Display = TextStyle(
        fontFamily = SansSC,
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp
    )

    val TopBarTitle = TextStyle(
        fontFamily = SansSC,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    )

    val SectionTitle = TextStyle(
        fontFamily = SansSC,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    )

    val BodyMedium = TextStyle(
        fontFamily = SansSC,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
    )

    val Body = TextStyle(
        fontFamily = SansSC,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    )

    val Secondary = TextStyle(
        fontFamily = SansSC,
        fontSize = 13.sp,
        color = AeroColors.Ink2
    )

    val Label = TextStyle(
        fontFamily = JBMono,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp,
        color = AeroColors.Ink3
    )

    val MetricNum = TextStyle(
        fontFamily = JBMono,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = AeroColors.PrimaryInk,
        fontFeatureSettings = "tnum"
    )
}
```

```kotlin
// AeroTheme.kt
@Composable
fun AeroTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AeroColors.Primary,
            onPrimary = Color.White,
            background = AeroColors.Background,
            surface = AeroColors.Surface,
            error = AeroColors.StatusFault
        ),
        typography = Typography(
            displayLarge = AeroType.Display,
            titleLarge = AeroType.TopBarTitle,
            titleMedium = AeroType.SectionTitle,
            bodyLarge = AeroType.BodyMedium,
            bodyMedium = AeroType.Body,
            labelSmall = AeroType.Label
        ),
        shapes = Shapes(
            small = AeroShapes.Chip,
            medium = AeroShapes.Card,
            large = AeroShapes.Sheet
        ),
        content = content
    )
}
```

## 2. RealtimeClient（WebSocket + Polling Fallback）

### 2.1 公开 API

```kotlin
// ui/platform/RealtimeClient.kt
interface RealtimeClient {
    val connectionState: StateFlow<ConnectionState>
    val terminalStateChanges: Flow<BroadcastWSMessage.TerminalStateChange>
    val taskProgressChanges: Flow<BroadcastWSMessage.TaskProgress>

    suspend fun connect()
    suspend fun disconnect()
}

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED,
    POLLING_FALLBACK,   // WS 断 → 10s 轮询中
    ERROR
}
```

### 2.2 实现要点

```kotlin
@Singleton
class RealtimeClientImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val authStore: AuthStore,
    private val terminalApi: TerminalApi
) : RealtimeClient {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState = _connectionState.asStateFlow()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var pollingJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun connect() {
        scope.launch { connectInternal() }
    }

    private suspend fun connectInternal(attempt: Int = 0) {
        val serverAddr = authStore.serverAddressFlow.value ?: return
        val jwt = authStore.jwtFlow.value ?: return

        _connectionState.value = ConnectionState.CONNECTING

        val request = Request.Builder()
            .url("ws://${serverAddr.host}:${serverAddr.port}/ws?token=$jwt")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _connectionState.value = ConnectionState.CONNECTED
                pollingJob?.cancel()
                startHeartbeat()
            }
            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(text)
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.ERROR
                scheduleReconnect(attempt + 1)
                startPollingFallback()
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
                scheduleReconnect(attempt + 1)
                startPollingFallback()
            }
        })
    }

    private fun scheduleReconnect(attempt: Int) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delayMs = minOf((1L shl attempt) * 1000, 60_000)
            delay(delayMs)
            connectInternal(attempt)
        }
    }

    private fun startPollingFallback() {
        if (pollingJob?.isActive == true) return
        _connectionState.value = ConnectionState.POLLING_FALLBACK
        pollingJob = scope.launch {
            while (isActive) {
                runCatching {
                    terminalApi.getAllTerminalState()
                }.onSuccess { /* 推到 _terminalStateChanges */ }
                delay(10_000)
            }
        }
    }

    private fun startHeartbeat() {
        scope.launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                webSocket?.send("""{"type":"ping","timestamp":${System.currentTimeMillis()}}""")
                delay(15_000)
            }
        }
    }
}
```

## 3. AdaptiveScaffold

### 3.1 公开 API

```kotlin
// ui/platform/AdaptiveScaffold.kt
@Composable
fun AdaptiveScaffold(
    railContent: (@Composable () -> Unit)? = null,
    listContent: @Composable () -> Unit,
    detailContent: (@Composable () -> Unit)? = null
)
```

### 3.2 实现要点

```kotlin
@Composable
fun AdaptiveScaffold(
    railContent: (@Composable () -> Unit)? = null,
    listContent: @Composable () -> Unit,
    detailContent: (@Composable () -> Unit)? = null
) {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass

    when (windowSize.widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            // 手机：单栏
            listContent()
        }
        WindowWidthSizeClass.MEDIUM -> {
            // 中型平板：两栏（List + Detail）
            Row {
                Box(Modifier.weight(1f)) { listContent() }
                Box(Modifier.weight(2f)) { detailContent?.invoke() }
            }
        }
        WindowWidthSizeClass.EXPANDED -> {
            // 大平板：三栏（Rail + List + Detail）
            Row {
                railContent?.let { Box(Modifier.width(80.dp)) { it() } }
                Box(Modifier.weight(1f)) { listContent() }
                Box(Modifier.weight(2f)) { detailContent?.invoke() }
            }
        }
    }
}
```

## 4. ConnectionBanner

```kotlin
@Composable
fun ConnectionBanner(connectionState: ConnectionState) {
    AnimatedVisibility(
        visible = connectionState in listOf(
            ConnectionState.POLLING_FALLBACK, ConnectionState.ERROR
        ),
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it })
    ) {
        val (color, text) = when (connectionState) {
            ConnectionState.POLLING_FALLBACK -> Color(0xFFFEF3C7) to "实时已断开，轮询中"
            ConnectionState.ERROR -> Color(0xFFFEE2E2) to "连接失败"
            else -> AeroColors.Surface to ""
        }
        Surface(
            color = color,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text(text, style = AeroType.Body, modifier = Modifier.padding(8.dp))
        }
    }
}
```

## 5. 通知 / FCM

```kotlin
// ui/platform/NotificationManager.kt
@Singleton
class AeroNotificationManager @Inject constructor(@ApplicationContext ctx: Context) {

    fun setupChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "广播通知", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "终端状态与任务进度"
            }
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun requestNotificationPermissionIfNeeded(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 请求 POST_NOTIFICATIONS
        }
    }

    companion object {
        const val CHANNEL_ID = "aero_broadcast"
    }
}
```

## 6. 错误处理框架

### 6.1 4 层错误处理

```kotlin
// ui/platform/ErrorHandler.kt
sealed class AeroError {
    data class Fatal(val message: String) : AeroError()           // 顶部红 banner
    data class RequestFailed(val message: String) : AeroError()   // Toast 3s
    data class PartialFailure(val failedItems: List<String>) : AeroError() // 失败列表 + 重试
    data class FormValidation(val field: String, val message: String) : AeroError() // 表单
}

@Composable
fun GlobalErrorBanner(error: AeroError?) {
    when (error) {
        is AeroError.Fatal -> RedBanner(error.message)
        is AeroError.RequestFailed -> { /* show toast via SideEffect */ }
        else -> { /* handled inline */ }
    }
}
```

## 7. 提交流程

```yaml
submission_workflow:
  before_submit:
    - "AeroTheme 编译通过 + Preview 正常"
    - "RealtimeClient 单元测试覆盖：connect / disconnect / reconnect / heartbeat / fallback"
    - "AdaptiveScaffold 在手机 + 中型平板 + 大平板下渲染验证"
    - "ICD 更新到 references/icd-contracts.md"
    - "Breaking change 已广播 ICD_UPDATE"
```

---

*Frontend-Platform Agent Skill v1.0.0*
