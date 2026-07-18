package com.flipvehiclewidget.app.presentation.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flipvehiclewidget.app.BuildConfig
import com.flipvehiclewidget.app.data.local.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import javax.inject.Inject

object OAuthConfig {
    const val SCOPE = "openid offline_access vehicle_device_data vehicle_cmds"

    private val serviceConfiguration = AuthorizationServiceConfiguration(
        Uri.parse("https://auth.tesla.com/oauth2/v3/authorize"),
        Uri.parse("https://auth.tesla.com/oauth2/v3/token"),
    )

    fun buildAuthorizationRequest(): AuthorizationRequest =
        AuthorizationRequest.Builder(
            serviceConfiguration,
            BuildConfig.TESLA_CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(BuildConfig.OAUTH_REDIRECT_URI),
        )
            .setScope(SCOPE)
            .build()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    private lateinit var authService: AuthorizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = AuthorizationService(this)

        setContent {
            var isAuthenticated by remember { mutableStateOf(tokenManager.isAuthenticated()) }

            val authLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { activityResult ->
                val data = activityResult.data ?: return@rememberLauncherForActivityResult
                val response = AuthorizationResponse.fromIntent(data)
                val exception = AuthorizationException.fromIntent(data)
                if (response != null) {
                    val authState = AuthState(response, exception)
                    authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, tokenException ->
                        authState.update(tokenResponse, tokenException)
                        tokenManager.saveAuthState(authState)
                        isAuthenticated = authState.isAuthorized
                    }
                } else if (exception != null) {
                    isAuthenticated = false
                }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConnectScreen(
                        isAuthenticated = isAuthenticated,
                        onConnectClick = {
                            val intent = authService.getAuthorizationRequestIntent(OAuthConfig.buildAuthorizationRequest())
                            authLauncher.launch(intent)
                        },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        authService.dispose()
        super.onDestroy()
    }
}

@Composable
private fun ConnectScreen(isAuthenticated: Boolean, onConnectClick: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(if (isAuthenticated) "Connected to vehicle" else "Not connected")
        Button(onClick = onConnectClick, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (isAuthenticated) "Reconnect vehicle" else "Connect vehicle")
        }
    }
}
