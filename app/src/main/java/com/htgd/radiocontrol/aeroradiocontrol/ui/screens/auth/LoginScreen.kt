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
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroGradients
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * Login — Handoff.html §05 spec-card #00 (LoginScreen).
 *
 * Layout (top → bottom):
 *   - Brand mark + product name (no surrounding TopBar).
 *   - Username input.
 *   - Password input + visibility toggle.
 *   - "Remember me" switch.
 *   - Server address collapsible (IP + port).
 *   - Login button (filled, primary CTA).
 *   - Scan QR shortcut.
 *
 * The screen is stateless besides local UI fields — actual auth wiring goes
 * into a `LoginViewModel` in a follow-up. Pass [error] from outside to show
 * the error state (red border + caption under the affected fields).
 */
@Composable
fun LoginScreen(
    error: String? = null,
    onLogin: (account: String, password: String, server: String, remember: Boolean) -> Unit = { _, _, _, _ -> },
    onScanClick: () -> Unit = {},
) {
    val colors = AeroTheme.colors

    var account     by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var remember_   by remember { mutableStateOf(true) }
    var serverExpanded by remember { mutableStateOf(false) }
    var serverIp   by remember { mutableStateOf("") }
    var serverPort by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Brand(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp))

        MInput(
            value = account,
            onValueChange = { account = it },
            label = "账号",
            placeholder = "请输入账号",
            keyboardType = KeyboardType.Text,
            leading = { Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = colors.ink3) },
            error = error,
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
            error = error,
            modifier = Modifier.fillMaxWidth(),
        )

        // Remember-me row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("记住我", style = AeroTheme.typography.bodySmall, color = colors.ink2, modifier = Modifier.weight(1f))
            MSwitch(checked = remember_, onCheckedChange = { remember_ = it })
        }

        // Server address collapsible
        ServerAddressSection(
            expanded = serverExpanded,
            onToggle = { serverExpanded = !serverExpanded },
            ip   = serverIp,
            port = serverPort,
            onIpChange   = { serverIp = it },
            onPortChange = { serverPort = it },
        )

        MButton(
            text     = "登 录",
            onClick  = { onLogin(account, password, "$serverIp:$serverPort", remember_) },
            variant  = MButtonVariant.Filled,
            enabled  = account.isNotBlank() && password.isNotBlank(),
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
                color = colors.ink2,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = colors.ink3,
            )
        }

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
                    modifier = Modifier.weight(0.5f),
                )
            }
        }
    }
}
