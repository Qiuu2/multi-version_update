package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MInput
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MSwitch
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationBanner
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationType
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroGradients
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * Login — Handoff.html §05 spec-card #00 + CTO BLOCKER-2 (2026-05-30) checklist.
 *
 * Rebuilt for visual fidelity (PA-14 Phase B). Layout top → bottom:
 *   1. Optional form-level error banner ([LoginUiState.formError]).
 *   2. **Hero**: a primary-soft decorative ring behind a brand tile that fills with
 *      [AeroGradients.Primary] and carries [Icons.Filled.Speaker]; under it a "欢迎回来"
 *      welcome line + a subtitle.
 *   3. **Server address** (TOP, expanded by default — was bottom + collapsed): IP +
 *      port inputs with placeholders.
 *   4. Account input.
 *   5. Password input + visibility toggle.
 *   6. Remember-me switch.
 *   7. **CTA**: inline gradient button — `Box` filled with [AeroGradients.Primary] +
 *      "登 录" + a right-arrow trailing icon. Baked inline rather than added as a new
 *      `MButtonVariant.Gradient` (PM ruling 2026-05-30, PA-14 Phase B): a single
 *      consumer doesn't justify enlarging the atom contract; we'd promote it later if
 *      another surface needs the same treatment.
 *   8. **Support footer**: "需要协助? 联系您的系统管理员" centred secondary text.
 *   9. Scan-QR shortcut.
 *
 * Spacing tokens (PA-14 DSN-SPACING-01 close): the previous version used 7 raw `.dp`
 * paddings (28 / 48 / 24 / 16 / 8×3 / 12 / 10) that bypassed `AeroTheme.spacing`.
 * Mechanical mapping applied: page horizontal = [AeroTheme.spacing.xl] (24dp),
 * page vertical = [AeroTheme.spacing.xxl] (32dp), section gap = `spacing.lg`
 * (16dp), hero gap = `spacing.md` (12dp), inline rests = `spacing.sm`/`xs`.
 *
 * Field text stays local UI state; everything that the LoginViewModel decides — submit
 * phase, field validation, form-level errors — arrives via [state]. UI-only rebuild
 * (DSN-LOGIN-HERO/SERVERPOS/CTA/FOOTER + DSN-SPACING-01 close); the VM/auth seam is
 * unchanged so LoginViewModelTest / LoginAuthenticatorTest stay green.
 * [validateServer] defaults to [validateServerAddress] (ServerAddress.parse delegate).
 */
@Composable
fun LoginScreen(
    state: LoginUiState = LoginUiState.Idle,
    onLogin: (account: String, password: String, ip: String, port: String, remember: Boolean) -> Unit = { _, _, _, _, _ -> },
    onScanClick: () -> Unit = {},
    onDismissError: () -> Unit = {},
    /** Pre-fill values from a prior session (survive logout); null = blank. */
    initialAccount: String = "",
    initialIp: String = "",
    initialPort: String = "",
    /** Returns an error message for "ip:port", or null when valid. */
    validateServer: (ip: String, port: String) -> String? = ::validateServerAddress,
) {
    val colors = AeroTheme.colors
    val typo = AeroTheme.typography
    val spacing = AeroTheme.spacing

    var account     by remember { mutableStateOf(initialAccount) }
    var password    by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var remember_   by remember { mutableStateOf(true) }
    var serverIp   by remember { mutableStateOf(initialIp) }
    var serverPort by remember { mutableStateOf(initialPort) }
    // Local server-validation error, surfaced inline; merged with any error the host
    // already put in state.fieldErrors.server.
    var localServerError by remember { mutableStateOf<String?>(null) }

    val submitting = state.isSubmitting
    val serverError = localServerError ?: state.fieldErrors.server
    val canSubmit = !submitting && account.isNotBlank() && password.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.xl, vertical = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        if (state.formError != null) {
            NotificationBanner(
                type = NotificationType.Error,
                message = state.formError,
                onDismiss = onDismissError,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Hero(modifier = Modifier.padding(top = spacing.md, bottom = spacing.sm))

        // Server-address inputs — TOP, expanded by default (PA-14 DSN-LOGIN-SERVERPOS).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            MInput(
                value = serverIp,
                onValueChange = { serverIp = it; localServerError = null },
                label = "服务器 IP",
                placeholder = "192.168.1.100",
                keyboardType = KeyboardType.Uri,
                modifier = Modifier.weight(1f),
            )
            MInput(
                value = serverPort,
                onValueChange = { serverPort = it; localServerError = null },
                label = "端口",
                placeholder = "8080",
                keyboardType = KeyboardType.Number,
                error = serverError,
                modifier = Modifier.weight(0.5f),
            )
        }

        MInput(
            value = account,
            onValueChange = { account = it },
            label = "账号",
            placeholder = "请输入账号",
            keyboardType = KeyboardType.Text,
            leading = { Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = colors.ink3) },
            error = state.fieldErrors.account,
            modifier = Modifier.fillMaxWidth(),
        )

        MInput(
            value = password,
            onValueChange = { password = it },
            label = "密码",
            placeholder = "请输入密码",
            isPassword = !passwordVisible,
            keyboardType = KeyboardType.Password,
            leading = { Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.ink3) },
            trailing = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                        tint = colors.ink3,
                    )
                }
            },
            error = state.fieldErrors.password,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "记住我",
                style = typo.bodySmall,
                color = colors.ink2,
                modifier = Modifier.weight(1f),
            )
            MSwitch(checked = remember_, onCheckedChange = { remember_ = it }, enabled = !submitting)
        }

        GradientLoginButton(
            text = if (submitting) "登录中…" else "登 录",
            enabled = canSubmit,
            onClick = {
                val err = validateServer(serverIp, serverPort)
                localServerError = err
                if (err == null) onLogin(account, password, serverIp, serverPort, remember_)
            },
            modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
        )

        // Support footer — PA-14 DSN-LOGIN-FOOTER.
        Text(
            "需要协助? 联系您的系统管理员",
            style = typo.bodySmall,
            color = colors.ink3,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = spacing.xs),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextScanButton(onClick = onScanClick, enabled = !submitting)
        }
    }
}

/**
 * Brand hero per CTO BLOCKER-2 checklist (PA-14 DSN-LOGIN-HERO):
 *   - decorative primary-soft ring behind the brand tile,
 *   - the tile filled with [AeroGradients.Primary] carrying [Icons.Filled.Speaker],
 *   - "欢迎回来" welcome title + subtitle.
 *
 * Sizes (24/72/56/16dp) are intentional visual atoms (icon glyph + tile + ring offset),
 * not page-spacing — they belong with the artwork, not in `AeroTheme.spacing`.
 */
@Composable
private fun Hero(modifier: Modifier = Modifier) {
    val colors = AeroTheme.colors
    val typo = AeroTheme.typography
    val spacing = AeroTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(
            modifier = Modifier.size(112.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Decorative ring — primary-soft, offset down-right for a layered look.
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .offset(x = 16.dp, y = 16.dp)
                    .clip(CircleShape)
                    .background(colors.primarySoft),
            )
            // Brand tile — gradient fill, speaker icon, e2 elevation lift.
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(elevation = AeroTheme.elevation.e2, shape = AeroTheme.shapes.rTile, clip = false)
                    .clip(AeroTheme.shapes.rTile)
                    .background(AeroGradients.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Speaker,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Text("欢迎回来", style = typo.display, color = colors.ink, textAlign = TextAlign.Center)
        Text(
            "登录您的校园广播控制系统",
            style = typo.bodySmall,
            color = colors.ink3,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Inline gradient login button (PA-14 DSN-LOGIN-CTA — inline-bake ruling). A
 * full-width pill filled with [AeroGradients.Primary], centred label, right-arrow
 * trailing. Disabled state dims to ink4 + drops the gradient (a flat surface3 fill)
 * so the visual affordance matches `MButton`'s disabled treatment.
 *
 * NOT promoted into `MButton` as a `Gradient` variant: only one surface needs it
 * today; promoting later is a one-line MButton diff if other surfaces ask.
 */
@Composable
private fun GradientLoginButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AeroTheme.colors
    val typo = AeroTheme.typography
    val spacing = AeroTheme.spacing
    val shape = AeroTheme.shapes.rChip

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .then(
                if (enabled) Modifier.background(AeroGradients.Primary)
                else Modifier.background(colors.surface3)
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val release = event.changes.any { it.changedToUp() }
                        if (release) onClick()
                    }
                }
            }
            .padding(horizontal = spacing.btnPadH, vertical = spacing.btnPadV),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = text,
                style = typo.button,
                color = if (enabled) Color.White else colors.ink4,
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (enabled) Color.White else colors.ink4,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Plain text-button used for the scan-QR shortcut — kept text-only to keep the
 *  visual hierarchy: gradient CTA → scan as a secondary affordance. */
@Composable
private fun TextScanButton(onClick: () -> Unit, enabled: Boolean) {
    val colors = AeroTheme.colors
    val typo = AeroTheme.typography
    val spacing = AeroTheme.spacing
    Row(
        modifier = Modifier
            .clip(AeroTheme.shapes.rChip)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.changedToUp() }) onClick()
                    }
                }
            }
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        Icon(
            Icons.Filled.QrCodeScanner,
            contentDescription = null,
            tint = if (enabled) colors.primary else colors.ink4,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "扫码登录",
            style = typo.button,
            color = if (enabled) colors.primary else colors.ink4,
        )
    }
}
