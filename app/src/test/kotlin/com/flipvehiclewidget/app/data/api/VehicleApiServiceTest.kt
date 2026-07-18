package com.flipvehiclewidget.app.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class VehicleApiServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: VehicleApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = retrofit.create(VehicleApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getVehicles parses response into VehiclesResponseDto`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"response":[{"id":7,"vin":"5YJ3E1EA1PF000001","display_name":"Aziz's Model 3"}],"count":1}"""
            )
        )

        val result = service.getVehicles()

        assertEquals(1, result.count)
        assertEquals(7L, result.response.first().id)
        assertEquals("Aziz's Model 3", result.response.first().displayName)
    }

    @Test
    fun `getVehicleData parses locked and climate state`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"response":{"vehicle_state":{"locked":true},"climate_state":{"is_climate_on":false}}}"""
            )
        )

        val result = service.getVehicleData(vehicleId = 7L)

        assertEquals(true, result.response.vehicleState.locked)
        assertEquals(false, result.response.climateState.isClimateOn)
        val recorded = server.takeRequest()
        assertEquals("/api/1/vehicles/7/vehicle_data", recorded.path)
    }
}
