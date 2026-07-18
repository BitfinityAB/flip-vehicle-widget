package com.flipvehiclewidget.app.data.api

import com.flipvehiclewidget.app.data.api.dto.ActuateTrunkRequestDto
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

class VehicleCommandApiServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: VehicleCommandApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        service = retrofit.create(VehicleCommandApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `actuateTrunk posts which_trunk body and parses result`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"response":{"result":true,"reason":null}}"""))

        val result = service.actuateTrunk(vehicleId = 7L, body = ActuateTrunkRequestDto(whichTrunk = "rear"))

        assertEquals(true, result.response.result)
        val recorded = server.takeRequest()
        assertEquals("/api/1/vehicles/7/command/actuate_trunk", recorded.path)
        assertEquals("""{"which_trunk":"rear"}""", recorded.body.readUtf8())
    }

    @Test
    fun `stopClimate posts to auto_conditioning_stop`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"response":{"result":true,"reason":null}}"""))

        val result = service.stopClimate(vehicleId = 7L)

        assertEquals(true, result.response.result)
        val recorded = server.takeRequest()
        assertEquals("/api/1/vehicles/7/command/auto_conditioning_stop", recorded.path)
    }
}
