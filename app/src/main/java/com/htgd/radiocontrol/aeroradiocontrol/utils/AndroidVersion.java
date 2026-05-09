package com.htgd.radiocontrol.aeroradiocontrol.utils;

import android.os.Build;

/**
 * Centralized Android OS version checks for the multi-version upgrade.
 *
 * Calling code should ask semantic questions ("does this device require the
 * notification runtime permission?") rather than scattering raw SDK_INT
 * comparisons across the codebase. That way, if Google reshuffles a behavior
 * gate between versions, we change one method here instead of grepping the
 * whole project.
 *
 * Raw integer constants are used instead of {@link Build.VERSION_CODES} so
 * this file stays compilable even when compileSdk is bumped in stages.
 */
public final class AndroidVersion {

    public static final int ANDROID_10 = 29;
    public static final int ANDROID_11 = 30;
    public static final int ANDROID_12 = 31;
    public static final int ANDROID_12L = 32;
    public static final int ANDROID_13 = 33;
    public static final int ANDROID_14 = 34;
    public static final int ANDROID_15 = 35;
    public static final int ANDROID_16 = 36;

    private AndroidVersion() {
    }

    public static boolean atLeast(int sdk) {
        return Build.VERSION.SDK_INT >= sdk;
    }

    public static int current() {
        return Build.VERSION.SDK_INT;
    }

    // ---- Semantic gates, named after the user-visible behavior they control. ----

    /** Android 12+ requires every component with an intent-filter to declare android:exported. */
    public static boolean requiresExplicitExported() {
        return atLeast(ANDROID_12);
    }

    /** Android 12+ requires PendingIntent flags to be explicitly mutable or immutable. */
    public static boolean requiresExplicitPendingIntentFlag() {
        return atLeast(ANDROID_12);
    }

    /** Android 12+ replaced legacy BLUETOOTH/BLUETOOTH_ADMIN with BLUETOOTH_CONNECT/SCAN runtime perms. */
    public static boolean requiresNewBluetoothPermissions() {
        return atLeast(ANDROID_12);
    }

    /** Android 13+ requires the POST_NOTIFICATIONS runtime permission to show notifications. */
    public static boolean requiresNotificationPermission() {
        return atLeast(ANDROID_13);
    }

    /** Android 13+ split READ_EXTERNAL_STORAGE into READ_MEDIA_IMAGES / VIDEO / AUDIO. */
    public static boolean requiresGranularMediaPermissions() {
        return atLeast(ANDROID_13);
    }

    /** Android 14+ requires foregroundServiceType in the manifest plus a matching FOREGROUND_SERVICE_* permission. */
    public static boolean requiresForegroundServiceType() {
        return atLeast(ANDROID_14);
    }

    /** Android 14+ adds READ_MEDIA_VISUAL_USER_SELECTED for partial photo/video access. */
    public static boolean supportsPartialMediaPermission() {
        return atLeast(ANDROID_14);
    }

    /** Android 11+ enforces scoped storage; legacy direct file access on /sdcard is gone. */
    public static boolean requiresScopedStorage() {
        return atLeast(ANDROID_11);
    }

    /** Android 15+ tightens edge-to-edge: apps targeting SDK 35+ get edge-to-edge by default. */
    public static boolean defaultsToEdgeToEdge() {
        return atLeast(ANDROID_15);
    }
}
