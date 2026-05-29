package com.htgd.radiocontrol.aeroradiocontrol.di

import okhttp3.OkHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

/**
 * Provides the OkHttpClient the LEGACY stack (`httptask.RequestManger`) reuses,
 * so the old and new stacks share one connection pool + dispatcher instead of
 * each running their own (RISK-AUDIT-05: double connection-pool waste).
 *
 * Why a SEPARATE client (not the new-stack singleton verbatim):
 *   The new-stack OkHttpClient (AppModule) carries DynamicBaseUrlInterceptor +
 *   AuthInterceptor. The legacy stack does NOT want those: it hand-builds
 *   absolute URLs (already host:port/api/...) and attaches its own
 *   `Authorization` header from `ServerToken.serverToken`. Feeding legacy
 *   requests through the new interceptors would (a) be a no-op for the baseUrl
 *   one (host != placeholder) but (b) make AuthInterceptor REPLACE the legacy
 *   header with the AuthStore JWT — a behavioural change during the migration
 *   window. So we keep the legacy client's behaviour identical to before.
 *
 * What IS shared (the actual RISK-AUDIT-05 fix):
 *   We build via `newBuilder()` off the new-stack client, which copies its
 *   `Dispatcher` and `ConnectionPool` — the pooled resources whose duplication
 *   was the waste. We then strip the new interceptors and restore the legacy
 *   client's own timeouts + permissive hostname verifier (verbatim from the
 *   original RequestManger constructor) so legacy behaviour is byte-for-byte
 *   unchanged apart from sharing the pool.
 *
 * Migration note: when the last legacy *Method.java caller is gone (Gate 4),
 * this module and the legacy client disappear with RequestManger.
 */
@Module
@InstallIn(SingletonComponent::class)
object LegacyNetworkModule {

    @Provides
    @Singleton
    @LegacyOkHttpClient
    fun provideLegacyOkHttpClient(newStackClient: OkHttpClient): OkHttpClient =
        // newBuilder() shares Dispatcher + ConnectionPool with the new-stack
        // client (the resources RISK-AUDIT-05 is about).
        newStackClient.newBuilder()
            .apply {
                // Drop the new-stack interceptors — legacy must not be rewritten
                // or re-authed (see class doc). interceptors() is the live list.
                interceptors().clear()
                networkInterceptors().clear()
            }
            // Restore the legacy RequestManger's original config verbatim.
            .connectTimeout(20_000L, TimeUnit.MILLISECONDS)
            .readTimeout(20_000L, TimeUnit.MILLISECONDS)
            .hostnameVerifier { _, _ -> true }
            .build()
}

/** Qualifies the OkHttpClient reused by the legacy `RequestManger`. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LegacyOkHttpClient
