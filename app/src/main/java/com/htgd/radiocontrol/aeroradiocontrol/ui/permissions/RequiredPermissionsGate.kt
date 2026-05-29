package com.htgd.radiocontrol.aeroradiocontrol.ui.permissions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButton
import com.htgd.radiocontrol.aeroradiocontrol.ui.components.atoms.MButtonVariant
import com.htgd.radiocontrol.aeroradiocontrol.ui.theme.AeroTheme

/**
 * Cold-start runtime-permission gate for the v4 UI (TASK-AR-010).
 *
 * Wrap the first post-login destination with this. On first composition it
 * requests every permission in [AeroPermissions.all] that is not already
 * granted, in one system dialog batch. Once the user has answered:
 *   - critical granted (or already held)  → renders [content] immediately;
 *   - critical denied                      → renders [criticalDeniedContent],
 *     which explains the lost capability and offers retry / open-settings, but
 *     never crashes (soul: Real-World Robustness).
 *
 * Optional-permission denials are intentionally NOT blocked here — the affected
 * feature (map / notifications / media picker) disables itself at point of use.
 * This mirrors the legacy `SignActivity.getThePermission()` behavior of asking
 * for everything up-front, minus the hard gate on non-essential perms.
 *
 * Timing note (cross-domain, pending legacy-native confirmation via PM): the
 * batch fires when this gate first enters composition — placed at the Main
 * scaffold entry, so it runs right before paging/intercom become reachable.
 */
@Composable
fun RequiredPermissionsGate(
    modifier: Modifier = Modifier,
    criticalDeniedContent: @Composable (onRetry: () -> Unit, onOpenSettings: () -> Unit) -> Unit =
        { onRetry, onOpenSettings -> DefaultMicDeniedContent(onRetry, onOpenSettings) },
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // null = not answered yet; true/false = critical-grant outcome of last batch.
    var criticalGranted by remember { mutableStateOf<Boolean?>(null) }
    // Bump to force a re-request after the user comes back from Settings or taps retry.
    var requestNonce by remember { mutableStateOf(0) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        // The system only returns entries for permissions it actually prompted.
        // Re-derive the full critical verdict from the live package state so
        // already-held perms (absent from `result`) still count as granted.
        criticalGranted = AeroPermissions.critical.all { isGranted(context, it) } ||
            AeroPermissions.criticalGranted(result)
    }

    LaunchedEffect(requestNonce) {
        val missing = AeroPermissions.all.filterNot { isGranted(context, it) }
        if (missing.isEmpty()) {
            criticalGranted = true
        } else {
            criticalGranted = null
            launcher.launch(missing.toTypedArray())
        }
    }

    when (criticalGranted) {
        // Still waiting on the user's answer — keep the host's background, no flash.
        null -> Column(modifier = modifier.fillMaxSize().background(AeroTheme.colors.bg)) {}
        true -> content()
        false -> criticalDeniedContent(
            /* onRetry = */ { requestNonce++ },
            /* onOpenSettings = */ { openAppSettings(context) },
        )
    }
}

/** Default 麦克风权限被拒 degraded screen — explains the lost capability, never crashes. */
@Composable
private fun DefaultMicDeniedContent(
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = AeroTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = AeroTheme.spacing.xl, vertical = AeroTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AeroTheme.spacing.md, Alignment.CenterVertically),
    ) {
        Icon(
            Icons.Filled.MicOff,
            contentDescription = null,
            tint = colors.statusFault,
            modifier = Modifier.size(56.dp),
        )
        Text(
            "麦克风权限未授予",
            style = AeroTheme.typography.sectionTitle,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            "寻呼与对讲需要麦克风权限。其余功能仍可使用，授予后可正常发起语音。",
            style = AeroTheme.typography.body,
            color = colors.ink3,
            textAlign = TextAlign.Center,
        )
        MButton(text = "重新授权", onClick = onRetry, variant = MButtonVariant.Filled)
        MButton(text = "去系统设置", onClick = onOpenSettings, variant = MButtonVariant.Tonal)
    }
}

private fun isGranted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
