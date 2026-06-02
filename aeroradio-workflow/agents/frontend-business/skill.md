---
name: frontend-business-skill
description: >
  Frontend-Business Agent 的技能配置文件。
  包含 5 个一级 Tab 的实现规范、ViewModel 模板、Navigation 配置、
  设计 token 引用指南、与其他 Domain 的交互模式。
version: 1.0.0
author: AeroRadio Architecture Team
---

# Frontend-Business Agent — Skill

## 1. Tab 实现规范

### 1.1 Tab 0 · 终端 Tab（TerminalHubScreen）

**职责**：分区列表 + 终端网格 + 地图视图三种 layout 切换

```kotlin
// feature/terminal/TerminalHubScreen.kt
@Composable
fun TerminalHubScreen(
    viewModel: TerminalHubViewModel = hiltViewModel(),
    onNavigateToZoneDetail: (String) -> Unit,
    onNavigateToTerminalDetail: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    AdaptiveScaffold {  // ← 消费 Frontend-Platform 的 AdaptiveScaffold
        TerminalHubContent(
            state = state,
            connectionState = connectionState,
            onLayoutChange = viewModel::switchLayout,  // 列表 / 网格 / 地图
            onZoneClick = onNavigateToZoneDetail,
            onTerminalClick = onNavigateToTerminalDetail,
            onMultiSelect = viewModel::toggleSelection,
            onBroadcastClick = viewModel::onBroadcastReady
        )
    }
}
```

```kotlin
// feature/terminal/TerminalHubViewModel.kt
@HiltViewModel
class TerminalHubViewModel @Inject constructor(
    private val terminalRepository: TerminalRepository,
    private val realtimeClient: RealtimeClient   // ICD-BroadcastWS
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalHubUiState.Loading)
    val uiState: StateFlow<TerminalHubUiState> = _uiState

    val connectionState: StateFlow<ConnectionState> = realtimeClient.connectionState

    init {
        // 合并 Repository（缓存）+ Realtime（实时更新）
        viewModelScope.launch {
            combine(
                terminalRepository.observeZones(),
                terminalRepository.observeTerminals(),
                realtimeClient.terminalStateChanges
            ) { zones, terminals, _ ->
                TerminalHubUiState.Success(zones, terminals)
            }.collect { _uiState.value = it }
        }
    }
}

sealed class TerminalHubUiState {
    object Loading : TerminalHubUiState()
    object Empty : TerminalHubUiState()
    data class Error(val message: String) : TerminalHubUiState()
    @Immutable
    data class Success(
        val zones: List<ZoneDto>,
        val terminals: List<TerminalDto>,
        val layoutMode: LayoutMode = LayoutMode.GRID,
        val selectedTerminalIds: Set<String> = emptySet()
    ) : TerminalHubUiState()
}
```

**TerminalCard 7 态实现要求**：

```kotlin
@Composable
fun TerminalCard(terminal: TerminalDto, selected: Boolean, onClick: () -> Unit) {
    val (borderColor, indicator) = when (terminal.state) {
        TerminalState.ONLINE  -> AeroColors.StatusOnline to OnlineDot()
        TerminalState.OFFLINE -> AeroColors.StatusOffline to OfflineLabel(terminal)
        TerminalState.PAGING  -> AeroColors.ModePage to PagingPulse()
        TerminalState.TALKING -> AeroColors.ModeTalk to TalkingAvatar()
        TerminalState.CASTING -> AeroColors.ModeCast to PlayingWaveform()
        TerminalState.URGENT  -> AeroColors.StatusFault to UrgentBlink()
        TerminalState.ALARM   -> AeroColors.StatusFault to AlarmIcon()
    }
    Card(
        shape = AeroShapes.Card,
        border = BorderStroke(2.dp, if (selected) AeroColors.Primary else borderColor),
        modifier = Modifier
            .background(if (selected) AeroColors.PrimarySoft else AeroColors.Surface)
            .clickable(onClick = onClick)
    ) { /* content */ }
}
```

### 1.2 Tab 1 · 广播 Tab（BroadcastScreen — 三档容器）

**核心**：`BroadcastScreen` 是容器，顶部三档 Segmented Control 在三种 mode 间切换，**目标终端共享**：

```kotlin
// feature/broadcast/BroadcastScreen.kt
@Composable
fun BroadcastScreen(viewModel: BroadcastViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column {
        BroadcastModeSelector(
            currentMode = state.mode,
            onModeChange = viewModel::switchMode  // 不重置已选终端
        )
        when (state.mode) {
            BroadcastMode.PAGE -> PageMode(
                targetTerminals = state.targetTerminals,
                onStartPaging = viewModel::startPaging
            )
            BroadcastMode.TALK -> TalkMode(
                targetTerminals = state.targetTerminals,
                voiceTalkAdapter = viewModel.voiceTalkAdapter,  // ICD-VoiceAAR
                onStartTalk = viewModel::startTalk,
                showFallback = !viewModel.voiceTalkAdapter.isAvailable()
            )
            BroadcastMode.CAST -> CastMode(
                targetTerminals = state.targetTerminals,
                mediaList = state.mediaList,
                onStartCast = viewModel::startCast
            )
        }
    }
}

enum class BroadcastMode { PAGE, TALK, CAST }

// 三档色（design-system-spec.md §1.3）
val BroadcastMode.color: Color
    get() = when (this) {
        BroadcastMode.PAGE -> AeroColors.ModePage     // #EA580C
        BroadcastMode.TALK -> AeroColors.ModeTalk     // #2563EB
        BroadcastMode.CAST -> AeroColors.ModeCast     // #0E7C70
    }
```

**对讲降级**：

```kotlin
@Composable
fun TalkMode(/* ... */, showFallback: Boolean) {
    if (showFallback) {
        Column {
            Icon(Icons.Warning, contentDescription = "不可用")
            Text(
                "此设备不支持对讲功能",
                style = AeroType.Body
            )
            Text(
                "需要 32 位 ABI 支持",
                style = AeroType.Secondary
            )
        }
    } else {
        // 正常对讲 UI
    }
}
```

### 1.3 Tab 2 · AI Tab（占位 + 后续）

Phase 1-2 仅占位，Phase 3 可降级延期：

```kotlin
@Composable
fun AIPlaceholderScreen() {
    EmptyState(
        icon = Icons.AutoAwesome,
        title = "AI 助手",
        subtitle = "待接入 NLU 后开放",
        cta = null   // 无 CTA
    )
}
```

### 1.4 Tab 3 · 任务 Tab（TaskScreen）

主要包含：今日时间轴 + 作息方案列表 + 临时任务入口

```kotlin
@Composable
fun TaskScreen(/* ... */) {
    Column {
        TaskTopBar(/* ... */)
        TodayTimeline(/* 时间轴卡片 */)
        Spacer(12.dp)
        SchedulePlansList(/* 作息方案 */)
        FAB { /* 临时任务入口 */ }
    }
}
```

**作息编辑** 是独立二级页 `ScheduleDetailScreen`，复杂度高，必须有：拖拽时间块、冲突检测、保存草稿。

### 1.5 Tab 4 · 服务 Tab（ServiceHomeScreen）

健康度卡片 + 工单列表 + 系统信息

```kotlin
@Composable
fun ServiceHomeScreen() {
    LazyColumn {
        item { SystemHealthCard(/* /server/serverstate */) }
        item { TicketSection(/* 工单 */) }
        item { SystemInfoCard(/* 服务器地址、版本 */) }
    }
}
```

---

## 2. ViewModel 模板

### 2.1 标准 ViewModel 结构

```kotlin
@HiltViewModel
class XxxViewModel @Inject constructor(
    private val repository: XxxRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // === UI State ===
    private val _uiState = MutableStateFlow<XxxUiState>(XxxUiState.Loading)
    val uiState: StateFlow<XxxUiState> = _uiState.asStateFlow()

    // === One-Shot Events ===
    private val _events = MutableSharedFlow<XxxEvent>()
    val events: SharedFlow<XxxEvent> = _events.asSharedFlow()

    // === Public Methods (UI → ViewModel) ===
    fun onSomething() { /* */ }

    // === Private (Repository → State) ===
    init {
        viewModelScope.launch {
            repository.observe()
                .catch { _uiState.value = XxxUiState.Error(it.message ?: "") }
                .collect { _uiState.value = XxxUiState.Success(it) }
        }
    }
}

sealed class XxxUiState {
    object Loading : XxxUiState()
    object Empty : XxxUiState()
    data class Error(val message: String) : XxxUiState()
    @Immutable
    data class Success(val data: SomeData) : XxxUiState()
}

sealed class XxxEvent {
    data class ShowToast(val message: String) : XxxEvent()
    object NavigateBack : XxxEvent()
}
```

### 2.2 StateFlow vs SharedFlow

| 用 StateFlow | 用 SharedFlow |
|-------------|---------------|
| UI 状态（屏幕能看到的） | 一次性事件（导航、Toast） |
| 有初始值 | 无初始值 |
| 收集时立即拿最新值 | 只收订阅后的发射 |

## 3. Navigation 配置

```kotlin
// navigation/AeroNavGraph.kt
@Composable
fun AeroNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "main" else "login"
    ) {
        composable("login") {
            LoginScreen(onLoginSuccess = { navController.navigate("main") })
        }
        composable("main") {
            MainScreen(navController)   // 含 5 Tab 底部导航
        }
        composable("zone/{zoneId}",
            arguments = listOf(navArgument("zoneId") { type = NavType.StringType })
        ) {
            ZoneDetailScreen(
                zoneId = it.arguments?.getString("zoneId")!!,
                onBack = { navController.popBackStack() }
            )
        }
        composable("terminal/{terminalId}") { /* ... */ }
        composable("schedule/{scheduleId}") { /* ... */ }
        composable("task/edit/{taskId}") { /* ... */ }
        composable("tempfile/broadcast") { /* ... */ }
    }
}
```

## 4. Design Token 引用速查

```kotlin
import com.aeroradio.ui.theme.AeroColors
import com.aeroradio.ui.theme.AeroShapes
import com.aeroradio.ui.theme.AeroSpacing
import com.aeroradio.ui.theme.AeroType
import com.aeroradio.ui.theme.AeroElevation
import com.aeroradio.ui.theme.AeroMotion

// 颜色
Box(modifier = Modifier.background(AeroColors.Primary))

// 圆角
Card(shape = AeroShapes.Card)
Button(shape = AeroShapes.Chip)

// 间距
Column(modifier = Modifier.padding(horizontal = 16.dp))   // page padding
Spacer(modifier = Modifier.height(12.dp))                  // section gap

// 字体
Text("24", style = AeroType.MetricNum)   // 大数字 mono+tnum
Text("分区标题", style = AeroType.SectionTitle)

// 阴影
Card(elevation = CardDefaults.cardElevation(defaultElevation = AeroElevation.Card))

// 动效
AnimatedVisibility(
    visible = visible,
    enter = fadeIn(AeroMotion.TapHover)
)
```

## 5. 跨 Domain 协作模式

### 5.1 消费 Data-Integration

```kotlin
// 通过 Hilt 注入 Repository
@HiltViewModel
class XxxViewModel @Inject constructor(
    private val repository: TerminalRepository  // ← ICD-TerminalDto 消费
) : ViewModel() { /* */ }

// 不允许 import:
// import com.aeroradio.data.network.OkHttpClient   ← 错误
// import com.aeroradio.data.dto.internal.*         ← 错误
```

### 5.2 消费 Frontend-Platform

```kotlin
// 消费 AeroTheme
setContent {
    AeroTheme {
        // 屏
    }
}

// 消费 AdaptiveScaffold
@Composable
fun XxxScreen() {
    AdaptiveScaffold(
        railContent = { /* 平板 navigation rail */ },
        listContent = { /* 列表 */ },
        detailContent = { /* 详情 */ }
    )
}

// 消费 RealtimeClient
@Inject lateinit var realtimeClient: RealtimeClient
realtimeClient.connectionState.collectAsState()
```

### 5.3 消费 Legacy-Native

```kotlin
// 消费 VoiceTalkAdapter (ICD-VoiceAAR)
@HiltViewModel
class TalkViewModel @Inject constructor(
    val voiceTalkAdapter: VoiceTalkAdapter
) : ViewModel() {

    fun startTalk(targetIds: List<String>) {
        if (!voiceTalkAdapter.isAvailable()) {
            _events.tryEmit(TalkEvent.ShowError("此设备不支持对讲"))
            return
        }
        viewModelScope.launch {
            voiceTalkAdapter.startTalk(targetIds)
                .onFailure { _events.emit(TalkEvent.ShowError(it.message ?: "")) }
        }
    }
}
```

## 6. 测试规范

```kotlin
// ViewModel 单元测试
@OptIn(ExperimentalCoroutinesApi::class)
class TerminalHubViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<TerminalRepository>()
    private val realtimeClient = mockk<RealtimeClient>(relaxed = true)

    @Test
    fun `loads empty state when no terminals`() = runTest {
        every { repository.observeTerminals() } returns flowOf(emptyList())
        every { repository.observeZones() } returns flowOf(emptyList())

        val vm = TerminalHubViewModel(repository, realtimeClient)
        vm.uiState.test {
            assertEquals(TerminalHubUiState.Loading, awaitItem())
            // 收集后第一帧
            // ...
        }
    }

    @Test
    fun `selection state persists across mode switches`() { /* ... */ }
}
```

## 7. 提交流程

```yaml
submission_workflow:
  before_submit:
    - "运行 ./gradlew lint"
    - "运行单元测试 ./gradlew testDebugUnitTest"
    - "Compose Preview 全部能跑"
    - "5 态全覆盖（适用屏）"
    - "Design token 引用检查（grep Color\\(0x）"
    - "ICD 引用检查（与 icd-contracts.md 对照）"

  submission_message:
    type: "DELIVERABLE"
    to: "project-manager"
    payload: "见 communication-protocol.md §2.3"

  expected_flow:
    1: "Submit → PM"
    2: "PM → Critic for review"
    3: "Critic → PM with REVIEW_RESULT"
    4: "If PASSED → COMPLETED; if FAILED → revision cycle"
```

---

*Frontend-Business Agent Skill v1.0.0 — Bringing v4 Design to Life*
