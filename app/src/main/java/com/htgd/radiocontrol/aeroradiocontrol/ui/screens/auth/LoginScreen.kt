package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.Dvr
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButtonVariant
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MInput
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MSwitch
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationBanner
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.molecules.NotificationType
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroGradients
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * Login — Handoff.html §05 spec-card #00 (LoginScreen).
 *
 * Layout (top → bottom):
 *   - Optional form-level error banner ([LoginUiState.formError]).
 *   - Brand mark + product name (no surrounding TopBar).
 *   - Username input.
 *   - Password input + visibility toggle.
 *   - "Remember me" switch.
 *   - Server address collapsible (IP + port).
 *   - Login button (filled, primary CTA — shows progress while submitting).
 *   - Scan QR shortcut.
 *
 * Field text stays local UI state; everything that a future `LoginViewModel`
 * decides — submit phase, field validation, form-level errors — arrives via
 * [state]. This renders all five login states (idle / submitting / success /
 * field-error / form-error) with NO network/data-layer wiring yet (auth call +
 * AuthStore.saveLogin land when AR-005's data layer is unblocked). [validateServer]
 * defaults to [validateServerAddress], which delegates to data-integration's
 * `ServerAddress.parse()` (ICD-AuthState consumption approved) to produce the
 * 地址校验失败 state — pure UI validation, no network.
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

    var account     by remember { mutableStateOf(initialAccount) }
    var password    by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var remember_   by remember { mutableStateOf(true) }
    var serverExpanded by remember { mutableStateOf(initialIp.isNotBlank() || initialPort.isNotBlank()) }
    var serverIp   by remember { mutableStateOf(initialIp) }
    var serverPort by remember { mutableStateOf(initialPort) }
    // Local server-validation error, surfaced inline; merged with any error the
    // host already put in state.fieldErrors.server.
    var localServerError by remember { mutableStateOf<String?>(null) }

    val submitting = state.isSubmitting
    val serverError = localServerError ?: state.fieldErrors.server

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (state.formError != null) {
            NotificationBanner(
                type = NotificationType.Error,
                message = state.formError,
                onDismiss = onDismissError,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Brand(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))

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

        // Remember-me row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("记住我", style = AeroTheme.typography.bodySmall, color = colors.ink2, modifier = Modifier.weight(1f))
            MSwitch(checked = remember_, onCheckedChange = { remember_ = it }, enabled = !submitting)
        }

        // Server address collapsible
        ServerAddressSection(
            expanded = serverExpanded,
            onToggle = { serverExpanded = !serverExpanded },
            ip   = serverIp,
            port = serverPort,
            onIpChange   = { serverIp = it; localServerError = null },
            onPortChange = { serverPort = it; localServerError = null },
            error = serverError,
        )

        MButton(
            text     = if (submitting) "登录中…" else "登 录",
            onClick  = {
                val err = validateServer(serverIp, serverPort)
                localServerError = err
                if (err == null) onLogin(account, password, serverIp, serverPort, remember_)
            },
            variant  = MButtonVariant.Filled,
            enabled  = !submitting && account.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MButton(
                text    = "扫码登录",
                onClick = onScanClick,
                variant = MButtonVariant.Text,
                enabled = !submitting,
                leading = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
            )
        }
    }
}

@Composable
private fun Brand(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(AeroTheme.shapes.rTile)
                .background(AeroGradients.Primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.Dvr, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Column {
            Text("AeroRadioControl", style = AeroTheme.typography.sectionTitle, color = AeroTheme.colors.ink)
            Text("校园广播控制 · v4", style = AeroTheme.typography.kicker, color = AeroTheme.colors.ink3)
        }
    }
}

@Composable
private fun ServerAddressSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    ip: String,
    port: String,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    error: String? = null,
) {
    val colors = AeroTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AeroTheme.shapes.rInput)
                .clickable(onClick = onToggle)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text  = "服务器地址",
                style = AeroTheme.typography.bodySmall,
                color = if (error != null) colors.statusFault else colors.ink2,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = colors.ink3,
            )
        }

        // When collapsed but invalid, still surface the error so it isn't hidden.
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MInput(
                    value = ip,
                    onValueChange = onIpChange,
                    label = "IP",
                    placeholder = "192.168.1.100",
                    keyboardType = KeyboardType.Uri,
                    modifier = Modifier.weight(1f),
                )
                MInput(
                    value = port,
                    onValueChange = onPortChange,
                    label = "端口",
                    placeholder = "8080",
                    keyboardType = KeyboardType.Number,
                    error = error,
                    modifier = Modifier.weight(0.5f),
                )
            }
        } else if (error != null) {
            Text(
                text = error,
                style = AeroTheme.typography.bodySmall,
                color = colors.statusFault,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}
