package com.flipvehiclewidget.app.integration

import com.flipvehiclewidget.app.data.api.VehicleApiService
import com.flipvehiclewidget.app.data.api.VehicleCommandApiService
import com.flipvehiclewidget.app.data.repository.VehicleRepositoryImpl
import com.flipvehiclewidget.app.domain.usecase.ToggleTrunkUseCase
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class CommandFlowIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var toggleTrunkUseCase: ToggleTrunkUseCase

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/api/1/vehicles" -> MockResponse().setResponseCode(200).setBody(
                    """{"response":[{"id":7,"vin":"5YJ3E1EA1PF000001","display_name":"Aziz's Model 3"}],"count":1}"""
                )
                request.path == "/api/1/vehicles/7/command/actuate_trunk" -> MockResponse().setResponseCode(200).setBody(
                    """{"response":{"result":true,"reason":null}}"""
                )
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()

        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val vehicleApiService = retrofit.create(VehicleApiService::class.java)
        val commandApiService = retrofit.create(VehicleCommandApiService::class.java)
        val repository = VehicleRepositoryImpl(vehicleApiService, commandApiService)
        toggleTrunkUseCase = ToggleTrunkUseCase(repository)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `resolving the vehicle then toggling the trunk succeeds end to end`() = runTest {
        val repository = VehicleRepositoryImpl(
            retrofitService(),
            retrofitCommandService(),
        )
        val vehicle = repository.getVehicle().getOrThrow()
        assertEquals(7L, vehicle.id)

        val result = toggleTrunkUseCase(vehicle.id)

        assertTrue(result.isSuccess)
        assertEquals(true, result.getOrThrow().success)
    }

    private fun retrofitService(): VehicleApiService = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(VehicleApiService::class.java)

    private fun retrofitCommandService(): VehicleCommandApiService = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(VehicleCommandApiService::class.java)
}
