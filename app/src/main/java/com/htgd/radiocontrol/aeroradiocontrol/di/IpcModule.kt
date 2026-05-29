package com.htgd.radiocontrol.aeroradiocontrol.di

import com.htgd.radiocontrol.aeroradiocontrol.data.ipc.LocalSocketClient
import com.htgd.radiocontrol.aeroradiocontrol.data.ipc.LocalSocketClientImpl
import com.htgd.radiocontrol.aeroradiocontrol.data.ipc.SocketConnectionFactory
import com.htgd.radiocontrol.aeroradiocontrol.data.ipc.TcpSocketConnection
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DI for the local IPC socket (127.0.0.1:4521), ICD-IPCSocket-v1.
 *
 * Kept separate from DataModule/NetworkModule because this is on-device IPC, not
 * HTTP or persistence. Provides the production socket transport and binds the
 * LocalSocketClient interface to its implementation.
 *
 * Note: nothing injects [LocalSocketClient] yet — it ships as forward-looking
 * infrastructure (the legacy SocketClient was already orphaned; AR-007 scan).
 * The binding is in place so a Phase 1 screen can @Inject it without further DI
 * work.
 */
@Module(includes = [IpcBindings::class])
@InstallIn(SingletonComponent::class)
object IpcModule {

    /** Production transport over a real TCP socket. Faked in unit tests. */
    @Provides
    @Singleton
    fun provideSocketConnectionFactory(): SocketConnectionFactory = TcpSocketConnection.Factory
}

@Module
@InstallIn(SingletonComponent::class)
abstract class IpcBindings {

    @Binds
    @Singleton
    abstract fun bindLocalSocketClient(impl: LocalSocketClientImpl): LocalSocketClient
}
