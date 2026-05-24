package com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MInput
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MSwitch
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onScanClick: () -> Unit = {},
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var serverExpanded by remember { mutableStateOf(false) }
    var serverAddr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun attemptLogin() {
        error = when {
            username.isBlank() -> "请输入用户名"
            password.isBlank() -> "请输入密码"
            else -> null
        }
        if (error == null) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.base),
    ) {
        Text("登录", style = AeroTheme.typography.display, color = colors.onBackground)
        Text("登录以控制校园广播终端", style = AeroTheme.typography.body, color = colors.onSurfaceVariant)

        MInput(
            value = username,
            onValueChange = { username = it; error = null },
            label = "用户名",
            placeholder = "请输入用户名",
            leadingIcon = Icons.Filled.Person,
            isError = error != null && username.isBlank(),
            modifier = Modifier.padding(top = spacing.sm),
        )
        MInput(
            value = password,
            onValueChange = { password = it; error = null },
            label = "密码",
            placeholder = "请输入密码",
            leadingIcon = Icons.Filled.Lock,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = error != null && password.isBlank(),
        )

        if (error != null) {
            Text(error!!, style = AeroTheme.typography.caption, color = colors.danger)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                MSwitch(checked = rememberMe, onCheckedChange = { rememberMe = it })
                Text("记住我", style = AeroTheme.typography.body, color = colors.onSurface)
            }
            Row(
                modifier = Modifier.clickable(onClick = onScanClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = "扫码", tint = colors.primary)
                Text("扫码登录", style = AeroTheme.typography.body, color = colors.primary)
            }
        }

        ServerConfigSection(
            expanded = serverExpanded,
            onToggle = { serverExpanded = !serverExpanded },
            serverAddr = serverAddr,
            onServerAddrChange = { serverAddr = it },
        )

        MButton(
            text = "登录",
            onClick = { attemptLogin() },
            modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
        )
    }
}

@Composable
private fun ServerConfigSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    serverAddr: String,
    onServerAddrChange: (String) -> Unit,
) {
    val colors = AeroTheme.colors
    val spacing = AeroTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("服务器设置", style = AeroTheme.typography.bodyStrong, color = colors.onSurface)
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            MInput(
                value = serverAddr,
                onValueChange = onServerAddrChange,
                placeholder = "https://server.example.com",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
        }
    }
}
