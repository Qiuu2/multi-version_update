package com.htgd.radiocontrol.aeroradiocontrol.di

import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.ActivityLifecycleForegroundState
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.AppForegroundState
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.OkHttpRealtimeConnectionFactory
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.RealtimeClient
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.RealtimeClientImpl
import com.htgd.radiocontrol.aeroradiocontrol.ui.platform.RealtimeConnectionFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI for the platform/realtime layer (ui/platform): the WS [RealtimeClient], its
 * transport factory, and the foreground-state signal.
 *
 * Mirrors [IpcModule]'s shape (interface→impl @Binds in a sibling abstract
 * module). All bindings are @Singleton — one realtime connection per process
 * (RISK-AR-004: multiple instances would each reconnect → a storm).
 *
 * Note (TASK-AR-103): like the IPC client, nothing injects [RealtimeClient] yet
 * — it ships as forward-looking infrastructure for the Phase 1 screens (terminal
 * Tab realtime, broadcast per-terminal results). The connection-lifecycle skeleton
 * is final; message parsing / WS URL / heartbeat frame wait on OPEN INQ-O-2.
 *
 * Startup wiring TODO: [ActivityLifecycleForegroundState.register] must be called
 * once with the Application instance (e.g. in the @HiltAndroidApp Application's
 * onCreate) for foreground tracking to work. Until then `isForeground` stays
 * false and the client arms but does not open — safe, just inert. Left for the
 * task that introduces the Application subclass / wires startup.
 */
@Module(includes = [PlatformBindings::class])
@InstallIn(SingletonComponent::class)
object PlatformModule

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformBindings {

    @Binds
    @Singleton
    abstract fun bindRealtimeClient(impl: RealtimeClientImpl): RealtimeClient

    @Binds
    @Singleton
    abstract fun bindRealtimeConnectionFactory(
        impl: OkHttpRealtimeConnectionFactory,
    ): RealtimeConnectionFactory

    @Binds
    @Singleton
    abstract fun bindAppForegroundState(
        impl: ActivityLifecycleForegroundState,
    ): AppForegroundState
}
