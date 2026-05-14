package com.htgd.radiocontrol.aeroradiocontrol.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.htgd.radiocontrol.aeroradiocontrol.data.api.HealthApiService
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
 *   This project's server address is dynamic — it's set at login time
 *   into Constant.serveraddress and varies per customer's LAN setup.
 *   Retrofit requires a non-null baseUrl at construction time, so we
 *   pass a placeholder. Every API call uses @Url so the real address
 *   is supplied per-request at call time (see HealthApiService).
 *
 *   When Phase 1 introduces real endpoints, we'll add a request
 *   interceptor that injects Constant.serveraddress automatically,
 *   removing the per-call @Url requirement.
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
}
