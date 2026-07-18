package com.flipvehiclewidget.app.data.local

import android.content.SharedPreferences
import com.flipvehiclewidget.app.di.EncryptedPrefs
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import javax.inject.Inject

class TokenManager @Inject constructor(
    @EncryptedPrefs private val preferences: SharedPreferences,
    private val authService: AuthorizationService,
) {
    fun getAuthState(): AuthState {
        val json = preferences.getString(KEY_AUTH_STATE, null) ?: return AuthState()
        return AuthState.jsonDeserialize(json)
    }

    fun saveAuthState(authState: AuthState) {
        preferences.edit().putString(KEY_AUTH_STATE, authState.jsonSerializeString()).apply()
    }

    fun isAuthenticated(): Boolean = getAuthState().isAuthorized

    fun clear() {
        preferences.edit().remove(KEY_AUTH_STATE).apply()
    }

    suspend fun getValidAccessToken(): String = suspendCancellableCoroutine { continuation ->
        val authState = getAuthState()
        authState.performActionWithFreshTokens(authService) { accessToken, _, exception ->
            saveAuthState(authState)
            when {
                exception != null -> continuation.resumeWithException(exception)
                accessToken != null -> continuation.resume(accessToken)
                else -> continuation.resumeWithException(IllegalStateException("No access token available"))
            }
        }
    }

    private companion object {
        const val KEY_AUTH_STATE = "auth_state"
    }
}
