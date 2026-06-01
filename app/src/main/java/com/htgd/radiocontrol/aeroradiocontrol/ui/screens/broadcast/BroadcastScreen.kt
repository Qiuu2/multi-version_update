package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.broadcast

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButtonVariant
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MChip
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.StatusPill
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.TerminalStatus
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.EmptyState
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.ListSkeleton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationBanner
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationType
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

enum class BroadcastMode(val label: String) { Page("寻呼"), Talk("对讲"), Cast("点播") }

/**
 * Resolves a [BroadcastMode] to its identity color (design-system-spec §1.3 mode-
 * colors + Handoff.html:883-885). Same three-leg-trace pattern as `AeroTab.identityColor`
 * (PA-14 C-1) — pixels in [ModeSegmented] / [VoicePanel] CTA / [CastPanel] CTA all
 * inherit from here, so a token rename / new mode inserts in one place.
 *
 *   spec row                   spec hex     → AeroColors field    (internal value)
 *   ────────────────────────── ──────────── ──────────────────── ───────────────────
 *   寻呼 (Page)  / modePaging  #EA580C      → colors.modePaging    (PageWarm  = #EA580C)
 *   对讲 (Talk)  / modeIntercom #2563EB     → colors.modeIntercom  (TalkBlue  = #2563EB)
 *   点播 (Cast)  / modeCast    #0E7C70      → colors.modeCast      (Primary   = #0E7C70)
 */
private fun BroadcastMode.identityColor(
    colors: com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroColors,
) = when (this) {
    BroadcastMode.Page -> colors.modePaging    // spec §1.3 寻呼 #EA580C
    BroadcastMode.Talk -> colors.modeIntercom  // spec §1.3 对讲 #2563EB
    BroadcastMode.Cast -> colors.modeCast      // spec §1.3 点播 #0E7C70
}

/**
 * Broadcast tab. Segmented switch over three modes; the target-zone selection is
 * shared across all modes (so switching mode does NOT reset the chosen targets).
 *
 * Mode wiring (de-mocked onto real seams):
 *   - 点播 (Cast) → [CastViewModel] (MediaRepository + OnDemandCastAdapter, NO mic).
 *   - 寻呼 (Page) / 对讲 (Talk) → [VoiceViewModel] (VoiceTalkAdapter; mic-gated). The
 *     RECORD_AUDIO prompt is driven HERE on [VoiceEffect.RequestMicPermission] (the
 *     adapter re-checks the grant at call time and fails closed; fe owns the prompt,
 *     never the gate). A device-connection banner is driven from deviceEvents.
 *
 * Mode-color identity (PA-14 Phase C-2, design-system-spec §1.3 + Handoff:883-885):
 *   each of the 3 modes carries its own identity color across the segmented control,
 *   the status line tint, AND the primary CTA — so a glance at any panel says which
 *   mode is active. Pre-C-2 the active mode bg was always `AeroGradients.Primary`
 *   (teal) and the CTA was `MButtonVariant.Success` (green #16A34A), so every mode
 *   rendered the same teal/green regardless of identity (CTO 2026-05-30 real-device
 *   screencap `.state/cto-verify/02-broadcast-tab-huawei-2026-05-30.jpg`).
 */
@Composable
fun BroadcastScreen(
    modifier: Modifier = Modifier,
    targetsViewModel: BroadcastTargetsViewModel = hiltViewModel(),
    castViewModel: CastViewModel = hiltViewModel(),
    voiceViewModel: VoiceViewModel = hiltViewModel(),
) {
    val spacing = AeroTheme.spacing
    val zones by targetsViewModel.zones.collectAsStateWithLifecycle()
    val voiceState by voiceViewModel.uiState.collectAsStateWithLifecycle()
    val connection by voiceViewModel.connection.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(BroadcastMode.Page) }
    // Start with nothing selected; the zone list arrives asynchronously and may
    // be empty, so never index into it (defensive — was zones.first()).
    var selectedZones by remember { mutableStateOf(setOf<String>()) }
    // Selection is shared across modes (hoisted here, not per-panel) so the switch
    // keeps the chosen targets.
    val activeSelection = selectedZones.intersect(zones.map { it.id }.toSet())
    // rememberUpdatedState so the permission-result callback (created once) always
    // retries against the LATEST selection, not a stale snapshot.
    val currentSelection by rememberUpdatedState(activeSelection)

    // The kind to retry after the RECORD_AUDIO prompt resolves (set when the VM asks).
    var pendingMicKind by remember { mutableStateOf<VoiceKind?>(null) }
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pendingMicKind?.let { kind ->
            voiceViewModel.onMicPermissionResult(granted, kind, currentSelection)
            pendingMicKind = null
        }
    }

    // fe drives the mic prompt off the VM's one-shot effect (adapter owns the gate).
    LaunchedEffect(Unit) {
        voiceViewModel.effects.collect { effect ->
            when (effect) {
                is VoiceEffect.RequestMicPermission -> {
                    pendingMicKind = effect.retryKind
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                is VoiceEffect.Message -> { /* targets-empty etc.: panel reflects state; no-op toast hook */ }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AeroTheme.colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(spacing.pageH),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        // Tab-level device-connection banner (寻呼/对讲 only — voice device link).
        if (mode != BroadcastMode.Cast && connection == VoiceConnection.Disconnected) {
            NotificationBanner(type = NotificationType.Warning, message = "语音设备已离线")
        }

        ModeSegmented(selected = mode, onSelect = { mode = it })

        TargetSection(
            zoneLabels = zones.map { it.id to it.name },
            selectedIds = activeSelection,
            onToggle = { id ->
                selectedZones =
                    if (id in selectedZones) selectedZones - id else selectedZones + id
            },
        )

        when (mode) {
            BroadcastMode.Page -> VoicePanel(
                kind = VoiceKind.Page,
                state = voiceState,
                targetCount = activeSelection.size,
                onStart = { voiceViewModel.start(VoiceKind.Page, activeSelection) },
                onStop = voiceViewModel::stop,
                onDismiss = voiceViewModel::dismiss,
            )
            BroadcastMode.Talk -> VoicePanel(
                kind = VoiceKind.Talk,
                state = voiceState,
                targetCount = activeSelection.size,
                onStart = { voiceViewModel.start(VoiceKind.Talk, activeSelection) },
                onStop = voiceViewModel::stop,
                onDismiss = voiceViewModel::dismiss,
            )
            BroadcastMode.Cast -> CastPanel(
                viewModel = castViewModel,
                selectedZoneIds = activeSelection,
            )
        }
    }
}

/**
 * Segmented mode-switcher (寻呼 / 对讲 / 点播) — each segment carries its OWN identity
 * color so the strip reads as 3 distinct modes even at rest (Handoff:883-885 + spec
 * §1.3): selected → solid mode-color bg + white label; resting → transparent + mode-
 * color label dimmed via the secondary `bodySmall` weight. Pre-C-2 the active bg was
 * `AeroGradients.Primary` (teal) for ALL three modes — see class KDoc.
 */
@Composable
private fun ModeSegmented(selected: BroadcastMode, onSelect: (BroadcastMode) -> Unit) {
    val colors = AeroTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.rChip)
            .background(colors.surface3)
            .padding(AeroTheme.spacing.xs),
    ) {
        BroadcastMode.values().forEach { m ->
            val active = m == selected
            val modeColor = m.identityColor(colors)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(AeroTheme.shapes.rChip)
                    .then(if (active) Modifier.background(modeColor) else Modifier)
                    .clickable { onSelect(m) }
                    .padding(vertical = AeroTheme.spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = m.label,
                    style = AeroTheme.typography.bodyLarge,
                    // Selected = white on mode-color; resting = mode-color label so
                    // the chip carries identity even before selection (spec row 12-13).
                    color = if (active) Color.White else modeColor,
                )
            }
        }
    }
}

@Composable
private fun TargetSection(
    zoneLabels: List<Pair<String, String>>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("目标终端", style = AeroTheme.typography.sectionTitle, color = colors.ink, modifier = Modifier.weight(1f))
            Text("已选 ${selectedIds.size} 区", style = AeroTheme.typography.bodySmall, color = colors.ink3)
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            zoneLabels.forEach { (id, name) ->
                MChip(label = name, active = id in selectedIds, onClick = { onToggle(id) })
            }
        }
    }
}

/**
 * Unified 寻呼/对讲 panel — de-mocked onto [VoiceViewModel] (SD2). Renders the
 * [VoiceUiState] for the active [kind]; start is gated by isAvailable() (Unavailable
 * fallback) + non-empty targets, and the mic prompt is driven at the screen root.
 *
 * Mode-color identity (PA-14 C-2): the Idle CTA and the Connecting/Waiting/Active
 * status-line tint follow the active mode's identity color — Page → modePaging
 * orange, Talk → modeIntercom blue (resolved via [BroadcastMode.identityColor]).
 * Terminal states (Refused / Error) stay neutral / fault — they are functional
 * states, not mode identity.
 */
@Composable
private fun VoicePanel(
    kind: VoiceKind,
    state: VoiceUiState,
    targetCount: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val verb = if (kind == VoiceKind.Page) "寻呼" else "对讲"
    // VoiceKind maps 1:1 onto the segmented BroadcastMode that drives this panel.
    val mode = if (kind == VoiceKind.Page) BroadcastMode.Page else BroadcastMode.Talk
    val modeColor = mode.identityColor(colors)

    if (state is VoiceUiState.Unavailable) {
        EmptyState(
            icon = Icons.Filled.MicOff,
            title = "$verb 不可用",
            description = "此设备不支持语音功能（缺少音频原生库或 ABI 不兼容）。",
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        // Status line for the live/terminal states — Connecting/Waiting/Active tint
        // follows the mode identity; Refused/Error stay neutral/fault per spec.
        when (state) {
            is VoiceUiState.Connecting ->
                VoiceStatusLine(TerminalStatus.Paging, "正在连接…", modeColor)
            is VoiceUiState.Waiting ->
                VoiceStatusLine(TerminalStatus.Paging, "等待对方接听…", modeColor)
            is VoiceUiState.Active ->
                VoiceStatusLine(TerminalStatus.Playing, "正在$verb $targetCount 区", modeColor)
            is VoiceUiState.Refused ->
                VoiceStatusLine(TerminalStatus.Offline, "对方已拒绝", colors.ink3)
            is VoiceUiState.Error ->
                VoiceStatusLine(TerminalStatus.Fault, state.message, colors.statusFault)
            else -> Text(
                "选择目标终端后，点击下方按钮开始$verb",
                style = AeroTheme.typography.body,
                color = colors.ink3,
                textAlign = TextAlign.Center,
            )
        }

        when (state) {
            is VoiceUiState.Idle -> ModeCtaButton(
                text = "开始$verb",
                modeColor = modeColor,
                leadingIcon = Icons.Filled.Mic,
                enabled = targetCount > 0,
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            )
            is VoiceUiState.Refused, is VoiceUiState.Error -> MButton(
                text = "好的",
                variant = MButtonVariant.Tonal,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> MButton( // Connecting / Waiting / Active → end the session
                text = "结束$verb",
                variant = MButtonVariant.Danger,
                leading = { Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun VoiceStatusLine(status: TerminalStatus, label: String, labelColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AeroTheme.spacing.sm),
    ) {
        StatusPill(status = status)
        Text(label, style = AeroTheme.typography.bodySmall, color = labelColor)
    }
}

/**
 * Inline mode-tinted CTA button (PA-14 C-2) — Box+Background pattern (same shape as
 * LoginScreen's `GradientLoginButton`, PA-14 Phase B). Bound to a mode-identity color
 * so the 3 broadcast modes have visually distinct CTAs without enlarging the `MButton`
 * variant set. Disabled state drops the mode color → `surface3` + `ink4` (matches the
 * `MButton.disabled` visual treatment).
 */
@Composable
private fun ModeCtaButton(
    text: String,
    modeColor: Color,
    leadingIcon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val shape = AeroTheme.shapes.rChip
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(if (enabled) modeColor else colors.surface3)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = AeroTheme.spacing.btnPadH, vertical = AeroTheme.spacing.btnPadV),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = if (enabled) Color.White else colors.ink4,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = AeroTheme.typography.button,
                color = if (enabled) Color.White else colors.ink4,
            )
        }
    }
}

/**
 * 点播 (Cast) mode — de-mocked onto [CastViewModel] (MediaRepository + OnDemandCast,
 * NO mic). Renders the library states; cast targets the shared [selectedZoneIds].
 */
@Composable
private fun CastPanel(
    viewModel: CastViewModel,
    selectedZoneIds: Set<String>,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val castResult by viewModel.castResult.collectAsStateWithLifecycle()

    var selectedMedia by remember { mutableStateOf(setOf<String>()) }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        castResult?.let { result ->
            val (type, msg) = when (result) {
                is CastResult.Success -> NotificationType.Success to "已开始点播"
                is CastResult.Failure -> NotificationType.Error to result.message
            }
            NotificationBanner(type = type, message = msg)
        }

        when (val s = state) {
            is CastUiState.Loading -> ListSkeleton(rows = 4)

            is CastUiState.Unavailable -> EmptyState(
                icon = Icons.Filled.MusicNote,
                title = "点播不可用",
                description = "此设备不支持点播功能（缺少音频原生库）。",
            )

            is CastUiState.Error -> EmptyState(
                icon = Icons.Filled.MusicNote,
                title = "加载失败",
                description = s.message,
                actionLabel = "重试",
                onAction = viewModel::refresh,
            )

            is CastUiState.Ready -> {
                Text("选择音频", style = AeroTheme.typography.sectionTitle, color = colors.ink)
                if (s.media.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.MusicNote,
                        title = "暂无媒体",
                        description = "媒体库中还没有音频文件。",
                    )
                } else {
                    s.media.forEach { file ->
                        MediaRow(
                            file = file,
                            selected = file.id in selectedMedia,
                            onClick = {
                                selectedMedia =
                                    if (file.id in selectedMedia) selectedMedia - file.id
                                    else selectedMedia + file.id
                            },
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.sm))
                    // Cast CTA tinted with 点播 modeCast identity (PA-14 C-2). Visually
                    // teal — same as primary by spec coincidence — but bound to the
                    // mode role so a future hex change to modeCast propagates here too.
                    ModeCtaButton(
                        text = "开始点播",
                        modeColor = BroadcastMode.Cast.identityColor(colors),
                        leadingIcon = Icons.Filled.PlayArrow,
                        enabled = selectedMedia.isNotEmpty() && selectedZoneIds.isNotEmpty(),
                        onClick = { viewModel.castMedia(selectedMedia, selectedZoneIds) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaRow(file: MediaUi, selected: Boolean, onClick: () -> Unit) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AeroTheme.shapes.rCard)
            .background(if (selected) colors.primarySoft else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.pageH, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = if (selected) colors.primary else colors.ink3,
            modifier = Modifier.size(24.dp),
        )
        Text(file.name, style = AeroTheme.typography.body, color = colors.ink, modifier = Modifier.weight(1f))
        Text(file.duration, style = AeroTheme.typography.bodySmall, color = colors.ink3)
    }
}
