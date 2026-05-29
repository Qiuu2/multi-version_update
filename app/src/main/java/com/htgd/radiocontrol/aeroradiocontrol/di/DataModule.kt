package com.htgd.radiocontrol.aeroradiocontrol.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStore
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.AuthStoreImpl
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.KeyValueStore
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.SharedPrefsKeyValueStore
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.TokenRefresher
import com.htgd.radiocontrol.aeroradiocontrol.data.auth.UnsupportedTokenRefresher
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.V3LoginAuthenticator
import com.htgd.radiocontrol.aeroradiocontrol.data.network.HttpStatusSuccessPolicy
import com.htgd.radiocontrol.aeroradiocontrol.data.network.ResponseSuccessPolicy
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.MediaRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.ServerStateRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TaskRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.TerminalRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.V3MediaRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.V3ServerStateRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.V3TaskRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.repository.V3TerminalRepository
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ConstantServerConfig
import com.htgd.radiocontrol.aeroradiocontrol.data.v3bridge.ServerConfig
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.LoginAuthenticator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Data-layer DI: the two key-value stores behind AuthStore, the AuthStore
 * binding, and the (currently stub) token refresher.
 *
 * Split from NetworkModule because these are persistence concerns, not HTTP.
 *
 * @Module(includes = [DataBindings]) wires the interface→impl @Binds; the
 * object body holds the @Provides factories that need construction logic
 * (EncryptedSharedPreferences setup, wrapping prefs in the adapter).
 */
@Module(includes = [DataBindings::class])
@InstallIn(SingletonComponent::class)
object DataModule {

    private const val SECURE_PREFS_FILE = "auth_secure"
    private const val PLAIN_PREFS_FILE = "auth_plain"

    @Provides
    @Singleton
    @SecureStore
    fun provideSecureStore(
        @ApplicationContext context: Context,
    ): KeyValueStore = SharedPrefsKeyValueStore(encryptedPrefs(context))

    @Provides
    @Singleton
    @PlainStore
    fun providePlainStore(
        @ApplicationContext context: Context,
    ): KeyValueStore = SharedPrefsKeyValueStore(
        context.getSharedPreferences(PLAIN_PREFS_FILE, Context.MODE_PRIVATE),
    )

    private fun encryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}

/**
 * Interface→implementation bindings. Separate @Module because @Binds methods
 * are abstract and can't live in an `object`.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindings {

    @Binds
    @Singleton
    abstract fun bindAuthStore(impl: AuthStoreImpl): AuthStore

    /**
     * Token refresher binding.
     *
     * Bound to [UnsupportedTokenRefresher] until INQ-O-1 D-1 is answered: a 401
     * then leads to "re-login required" rather than a silent half-working
     * refresh. Swap this binding once the backend's refresh contract is known
     * (dedicated endpoint vs. credential re-login). TODO(INQ-O-1 D-1).
     */
    @Binds
    @Singleton
    abstract fun bindTokenRefresher(impl: UnsupportedTokenRefresher): TokenRefresher

    /**
     * Response success policy (consumed by Phase 1 Repositories, not by the
     * interceptors). Bound to [HttpStatusSuccessPolicy] (HTTP 2xx == success) as
     * the documented-assumption default. Swap once OPEN(INQ-O-1 Q3) is answered
     * (HTTP status vs. body business code). TODO(INQ-O-1 Q3).
     */
    @Binds
    @Singleton
    abstract fun bindResponseSuccessPolicy(
        impl: HttpStatusSuccessPolicy,
    ): ResponseSuccessPolicy

    /**
     * LoginAuthenticator binding (ICD-LoginAuthenticator-v1).
     *
     * Plan A (CTO D-13/D-14, TASK-PA-01): bound to [V3LoginAuthenticator] (logs in
     * through the v3 stack, stores token in v3 ServerToken). The Retrofit
     * [RetrofitLoginAuthenticator] is now DORMANT (kept on disk as a migration
     * asset, not deleted). Exactly ONE @Binds for LoginAuthenticator app-wide.
     */
    @Binds
    @Singleton
    abstract fun bindLoginAuthenticator(
        impl: V3LoginAuthenticator,
    ): LoginAuthenticator

    /**
     * TerminalRepository binding (TASK-AR-101 interface, TASK-PA-01 impl).
     *
     * Plan A: bound to [V3TerminalRepository] (v3 RequestManger → JSON → existing
     * DTO/Mapper → SSOT). The Retrofit [TerminalRepositoryImpl] is DORMANT (kept as
     * migration asset, not deleted). The interface / domain models / Mapper /
     * consuming ViewModels are unchanged — the seam payoff.
     */
    @Binds
    @Singleton
    abstract fun bindTerminalRepository(
        impl: V3TerminalRepository,
    ): TerminalRepository

    /**
     * TaskRepository binding (ICD-TaskRepository-v1; TASK-PA-03b stub → TASK-PA-10
     * real impl).
     *
     * Plan A: bound to [V3TaskRepository] — now the REAL impl (V3CallbackAdapter →
     * SchemeDto → SchemeMapper group-by-sechename → SSOT; setSchemeActive POSTs
     * /task/sechenableordisable then re-fetches per I-3). The earlier stub was
     * filled in place, so the binding is unchanged. Exactly ONE @Binds for
     * TaskRepository app-wide.
     */
    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: V3TaskRepository,
    ): TaskRepository

    /**
     * ServerStateRepository binding (ICD-ServerStateRepository-v1, TASK-PA-05).
     *
     * Plan A: bound to [V3ServerStateRepository] — a REAL impl (V3CallbackAdapter →
     * ServerStateDto → SSOT), mirroring V3TerminalRepository. The dormant new-stack
     * HealthApiService is NOT used. Exactly ONE @Binds for ServerStateRepository
     * app-wide.
     */
    @Binds
    @Singleton
    abstract fun bindServerStateRepository(
        impl: V3ServerStateRepository,
    ): ServerStateRepository

    /**
     * MediaRepository binding (ICD-MediaRepository-v1, TASK-PA-07, LIST half).
     *
     * Plan A: bound to [V3MediaRepository] — a REAL impl (V3CallbackAdapter →
     * MediaDto/MediaFolderDto → SSOT), mirroring V3TerminalRepository. LIST/selection
     * only; the 点播 CAST action is the HTIntf AAR cast-seam (legacy-native), not
     * this repo. Exactly ONE @Binds for MediaRepository app-wide.
     */
    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        impl: V3MediaRepository,
    ): MediaRepository

    /**
     * ServerConfig binding (TASK-PA-01). Production reads/writes v3
     * `Constant.serveraddress`; the seam exists so data-layer code is unit-testable
     * without loading v3 Constant (Android static-init). v3 Constant is unchanged.
     */
    @Binds
    @Singleton
    abstract fun bindServerConfig(impl: ConstantServerConfig): ServerConfig
}

/** Qualifies the EncryptedSharedPreferences-backed [KeyValueStore]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecureStore

/** Qualifies the plain SharedPreferences-backed [KeyValueStore]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlainStore
