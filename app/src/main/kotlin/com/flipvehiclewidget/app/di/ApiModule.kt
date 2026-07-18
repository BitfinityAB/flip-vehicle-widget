package com.flipvehiclewidget.app.di

import com.flipvehiclewidget.app.BuildConfig
import com.flipvehiclewidget.app.data.api.AuthInterceptor
import com.flipvehiclewidget.app.data.api.VehicleApiService
import com.flipvehiclewidget.app.data.api.VehicleCommandApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

private const val FLEET_API_BASE_URL = "https://fleet-api.prd.na.vehicle-command.tesla.com/"

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(authInterceptor).build()

    @Provides
    @Singleton
    @ReadApi
    fun provideReadRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(FLEET_API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    @CommandApi
    fun provideCommandRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.PROXY_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideVehicleApiService(@ReadApi retrofit: Retrofit): VehicleApiService =
        retrofit.create(VehicleApiService::class.java)

    @Provides
    @Singleton
    fun provideVehicleCommandApiService(@CommandApi retrofit: Retrofit): VehicleCommandApiService =
        retrofit.create(VehicleCommandApiService::class.java)
}
