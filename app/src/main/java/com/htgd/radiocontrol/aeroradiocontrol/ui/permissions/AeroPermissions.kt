package com.htgd.radiocontrol.aeroradiocontrol.ui.permissions

import android.Manifest
import com.htgd.radiocontrol.aeroradiocontrol.utils.AndroidVersion

/**
 * Single source of truth for the v4 cold-start runtime-permission set.
 *
 * Background (TASK-AR-010): before the entry-point consolidation (TASK-AR-006),
 * the only runtime-permission gate was the legacy `SignActivity.getThePermission()`.
 * V4Activity is now the sole LAUNCHER and never routes through SignActivity, so a
 * v4-native gate has to request these on first launch — otherwise RECORD_AUDIO is
 * missing and legacy-native paging/intercom (and the terminal map's location) break.
 *
 * The list mirrors design-system-spec.md §8.4 `requiredPermissions`, version-gated
 * the same way the legacy gate was (via [AndroidVersion] semantic checks so the OS
 * branches live in exactly one place). Install-time permissions (INTERNET,
 * ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE) are granted automatically from the
 * manifest and are intentionally NOT requested at runtime.
 *
 * Criticality split drives the denial UX in [RequiredPermissionsGate]:
 *   - [critical] — the app's core actions (paging/intercom) cannot work without it,
 *     so denial shows a blocking-but-recoverable rationale. RECORD_AUDIO only.
 *   - [optional] — degrades a feature but the app stays usable (location → map,
 *     notifications → push, media → file picker), so denial just disables that
 *     feature later, no up-front block.
 *
 * NOTE (cross-domain, pending legacy-native sign-off via PM): RECORD_AUDIO is
 * listed [critical] and requested up-front to match the legacy behavior. Whether
 * voice additionally needs it re-checked at the moment startTalk/startPaging runs
 * (and how FOREGROUND_SERVICE_MICROPHONE interacts on Android 14+) is legacy-native's
 * call; this object only governs the cold-start request.
 */
object AeroPermissions {

    /**
     * RECORD_AUDIO — required by 寻呼/对讲 (paging/intercom), which is the app's
     * reason to exist. Denial is surfaced prominently but never crashes.
     */
    val critical: List<String> = listOf(
        Manifest.permission.RECORD_AUDIO,
    )

    /**
     * Permissions whose absence degrades one feature but leaves the app usable.
     * Built per OS version because storage/bluetooth/notification perms changed
     * shape across releases — same branches the legacy gate used.
     */
    val optional: List<String>
        get() = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)   // terminal map
            add(Manifest.permission.READ_PHONE_STATE)       // legacy device-id usage

            if (AndroidVersion.requiresGranularMediaPermissions()) {
                // Android 13+: READ_EXTERNAL_STORAGE no longer grants media access.
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }

            if (AndroidVersion.requiresNewBluetoothPermissions()) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }

            if (AndroidVersion.requiresNotificationPermission()) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

    /** Everything to request in one batch on cold start (critical first). */
    val all: List<String>
        get() = critical + optional

    /** True iff every [critical] permission appears granted in [grantResult]. */
    fun criticalGranted(grantResult: Map<String, Boolean>): Boolean =
        critical.all { grantResult[it] == true }
}
