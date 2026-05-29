package com.htgd.radiocontrol.aeroradiocontrol.di

import com.htgd.radiocontrol.aeroradiocontrol.BuildConfig
import com.htgd.radiocontrol.aeroradiocontrol.data.network.AuthInterceptor
import com.htgd.radiocontrol.aeroradiocontrol.data.network.DynamicBaseUrlInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Qualifier
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

/**
 * Application-scoped DI module.
 *
 * Provides the shared infrastructure singletons that any new feature can
 * @Inject into its Repository / ViewModel / Service:
 *
 *   - OkHttpClient: modern HTTP client (replaces legacy okhttp-3.2.0.jar
 *     for new code; old RequestManger keeps using the local jar).
 *   - IoDispatcher / MainDispatcher: coroutine dispatchers, injected
 *     rather than hardcoded so tests can override with TestDispatcher.
 *
 * Retrofit + ApiService are intentionally NOT provided here yet — they
 * arrive in Phase 0-3 alongside the first Repository sample, so we have
 * a concrete use case to shape the API surface.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        baseUrlInterceptor: DynamicBaseUrlInterceptor,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Interceptor order is load-bearing (see each interceptor's doc):
            //   1. baseUrl — swap placeholder host → real server + /api prefix
            //   2. auth    — attach Bearer header, 401→refresh→retry
            //   3. logging — last, so it prints the real host + header (debug only)
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(authInterceptor)

        // Verbose body logging in debug builds so we can see request /
        // response bodies in Logcat. Release builds keep it off to avoid
        // leaking sensitive payloads (tokens, user data).
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    /**
     * Application-lifetime CoroutineScope (SupervisorJob + IO) for work that must
     * outlive any single screen — e.g. a Repository's shared in-flight refresh
     * (Plan A V3*Repository de-dups concurrent refresh() onto one network fetch
     * launched in this scope, so a caller cancelling does not kill the shared
     * fetch for the others). A child failure does not cancel siblings (Supervisor).
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
}

/**
 * Qualifier for the IO dispatcher. Use this on @Inject sites so Hilt knows
 * which dispatcher to inject (we have multiple CoroutineDispatcher providers).
 *
 * Usage:
 *     class SomeRepository @Inject constructor(
 *         @IoDispatcher private val ioDispatcher: CoroutineDispatcher
 *     ) { ... }
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/** Qualifier for the application-lifetime [kotlinx.coroutines.CoroutineScope]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
