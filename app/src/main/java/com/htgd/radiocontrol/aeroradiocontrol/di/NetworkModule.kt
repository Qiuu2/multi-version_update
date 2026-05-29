package com.htgd.radiocontrol.aeroradiocontrol.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.htgd.radiocontrol.aeroradiocontrol.data.api.AuthApi
import com.htgd.radiocontrol.aeroradiocontrol.data.api.HealthApiService
import com.htgd.radiocontrol.aeroradiocontrol.data.api.TerminalApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Network-layer DI module: Retrofit, Gson, and API service interfaces.
 *
 * Why split from AppModule?
 *   AppModule owns infrastructure shared by all features (OkHttp,
 *   dispatchers). NetworkModule owns network-layer specifics (Retrofit
 *   instance, JSON converter, API service interfaces). Keeping them
 *   separate means a unit test that only needs OkHttp can install
 *   AppModule alone without dragging in API stubs.
 *
 * Base URL note:
 *   This project's server address is dynamic — each campus LAN has its own
 *   host:port, set at login time. Retrofit requires a non-null baseUrl at
 *   construction, so we pass [PLACEHOLDER_BASE_URL]. DynamicBaseUrlInterceptor
 *   (AR-002, wired into AppModule's OkHttpClient) swaps the placeholder host for
 *   the real address from AuthStore and prepends the `/api` base segment, so new
 *   ApiService interfaces can use relative paths (`@POST("/authorizations")`)
 *   matching the legacy Constant.java endpoints — no per-call @Url needed.
 *
 *   The legacy HealthApiService still uses @Url for its standalone reachability
 *   probe (it runs before login, before any server address is stored).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val PLACEHOLDER_BASE_URL = "http://placeholder.invalid/"

    @Provides
    @Singleton
    fun provideGson(): Gson =
        GsonBuilder()
            .setLenient()
            .create()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(PLACEHOLDER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideHealthApiService(retrofit: Retrofit): HealthApiService =
        retrofit.create(HealthApiService::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideTerminalApi(retrofit: Retrofit): TerminalApi =
        retrofit.create(TerminalApi::class.java)
}
