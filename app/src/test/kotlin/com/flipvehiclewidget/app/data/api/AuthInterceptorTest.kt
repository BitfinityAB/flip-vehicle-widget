package com.flipvehiclewidget.app.data.api

import com.flipvehiclewidget.app.data.local.TokenManager
import io.mockk.coEvery
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `attaches bearer token from token manager to outgoing request`() {
        val tokenManager = mockk<TokenManager>()
        coEvery { tokenManager.getValidAccessToken() } returns "test-token"

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()

        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val request = Request.Builder().url(server.url("/api/1/vehicles")).build()
        client.newCall(request).execute().close()

        val recorded = server.takeRequest()
        assertEquals("Bearer test-token", recorded.getHeader("Authorization"))
    }
}
