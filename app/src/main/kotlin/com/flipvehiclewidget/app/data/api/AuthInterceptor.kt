package com.flipvehiclewidget.app.data.api

import com.flipvehiclewidget.app.data.local.TokenManager
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = try {
            runBlocking { tokenManager.getValidAccessToken() }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException("Failed to obtain access token", e)
        }
        val authorizedRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authorizedRequest)
    }
}
