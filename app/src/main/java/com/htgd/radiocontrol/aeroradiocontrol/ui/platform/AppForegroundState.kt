package com.htgd.radiocontrol.aeroradiocontrol.ui.platform

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the app is currently in the foreground.
 *
 * [RealtimeClient] observes this to satisfy the soul's "前台 + 网络可用才重连 /
 * 后台暂停重连" rule (RISK-AR-004 reconnect-storm avoidance): a backgrounded app
 * must not burn battery hammering reconnects.
 *
 * Exposed as an interface so RealtimeClient depends on the *signal*, not on any
 * particular lifecycle library. Tests inject a fake that flips the flow; the
 * skeleton needs no `androidx.lifecycle:lifecycle-process` dependency (which is
 * not currently on the classpath — keeping this dependency-free avoids a shared
 * build.gradle change just for the skeleton).
 */
interface AppForegroundState {
    /** true while ≥1 activity is started (app visible), false once all are stopped. */
    val isForeground: StateFlow<Boolean>
}

/**
 * Default [AppForegroundState] backed by [Application.ActivityLifecycleCallbacks]
 * — a framework API, no extra dependency. Counts started activities; foreground
 * iff the count is > 0.
 *
 * Must be registered once at app start (see [register]); call it from the
 * Application's `onCreate`. Registration is intentionally explicit (not done in
 * the constructor) so injecting this type has no side effects — important for
 * tests and for keeping Hilt graph construction pure.
 */
@Singleton
class ActivityLifecycleForegroundState @Inject constructor() :
    AppForegroundState,
    Application.ActivityLifecycleCallbacks {

    private val _isForeground = MutableStateFlow(false)
    override val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    private var startedActivities = 0

    /** Register on the [Application] at startup. Idempotent per Application instance. */
    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
        if (startedActivities > 0) _isForeground.value = true
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities = (startedActivities - 1).coerceAtLeast(0)
        if (startedActivities == 0) _isForeground.value = false
    }

    // Unused lifecycle callbacks — foreground is tracked purely by start/stop.
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
