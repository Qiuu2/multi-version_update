package com.htgd.radiocontrol.aeroradiocontrol.di

import com.htgd.radiocontrol.aeroradiocontrol.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

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
