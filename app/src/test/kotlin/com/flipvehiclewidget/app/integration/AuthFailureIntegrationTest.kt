package com.flipvehiclewidget.app.integration

import com.flipvehiclewidget.app.data.api.AuthInterceptor
import com.flipvehiclewidget.app.data.api.VehicleApiService
import com.flipvehiclewidget.app.data.api.VehicleCommandApiService
import com.flipvehiclewidget.app.data.local.TokenManager
import com.flipvehiclewidget.app.data.local.VehicleVinCache
import com.flipvehiclewidget.app.data.repository.VehicleRepositoryImpl
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.openid.appauth.AuthorizationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.io.IOException

class AuthFailureIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: VehicleRepositoryImpl

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val tokenManager = mockk<TokenManager>()
        val authFailure = AuthorizationException.GeneralErrors.NETWORK_ERROR
        coEvery { tokenManager.getValidAccessToken() } throws authFailure

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        repository = VehicleRepositoryImpl(
            retrofit.create(VehicleApiService::class.java),
            retrofit.create(VehicleCommandApiService::class.java),
            mockk<VehicleVinCache>(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `a token fetch failure reaches the repository as IOException wrapping the original cause`() = runTest {
        val result = repository.getVehicle()

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is IOException)

        // kotlinx.coroutines transparently recovers the stack trace whenever a thrown exception
        // crosses a coroutine suspension boundary on a different thread than where it originated
        // (here: AuthInterceptor throws on OkHttp's own dispatcher thread while getVehicle()'s
        // continuation is suspended on the test dispatcher). Because IOException happens to expose
        // a (message, cause) constructor, kotlinx.coroutines substitutes a *copy* of it as `error`,
        // chaining the original AuthInterceptor-thrown IOException as its cause -- so the
        // AuthorizationException AuthInterceptor wraps can land one level deeper than `error.cause`.
        // Walk the full chain instead of asserting a fixed depth, so this test verifies the real,
        // always-present contract rather than an artifact of whether recovery fires.
        val causeChain = generateSequence(error as Throwable) { it.cause }.toList()
        assertTrue(
            "expected AuthorizationException.GeneralErrors.NETWORK_ERROR somewhere in the cause chain: $causeChain",
            causeChain.any { it == AuthorizationException.GeneralErrors.NETWORK_ERROR },
        )
    }
}
