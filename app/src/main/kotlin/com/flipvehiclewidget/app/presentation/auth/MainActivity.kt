package com.flipvehiclewidget.app.presentation.auth

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.flipvehiclewidget.app.BuildConfig
import com.flipvehiclewidget.app.data.bluetooth.TeslaBleBeaconName
import com.flipvehiclewidget.app.data.bluetooth.VehicleBeaconScanner
import com.flipvehiclewidget.app.data.local.TokenManager
import com.flipvehiclewidget.app.data.local.VehicleVinCache
import com.flipvehiclewidget.app.domain.entity.ConnectionState
import com.flipvehiclewidget.app.domain.repository.VehicleRepository
import com.flipvehiclewidget.app.domain.usecase.CheckBluetoothConnectionUseCase
import com.flipvehiclewidget.app.presentation.widget.VehicleWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    // Token exchange/refresh is routed through our own proxy (oauth-relay), not Tesla's real
    // token endpoint directly: Tesla's Fleet API requires a client_secret on every token
    // request, which must never be embedded in this app (extractable from the APK). The
    // relay holds the secret server-side and forwards to Tesla's actual endpoint
    // (https://fleet-auth.prd.vn.cloud.tesla.com/oauth2/v3/token). The authorize endpoint
    // itself stays on Tesla's real domain since that step is just a browser redirect with
    // no secret involved.
    private val serviceConfiguration = AuthorizationServiceConfiguration(
        Uri.parse("https://auth.tesla.com/oauth2/v3/authorize"),
        Uri.parse(BuildConfig.PROXY_BASE_URL + "oauth/token"),
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

    @Inject
    lateinit var vehicleRepository: VehicleRepository

    @Inject
    lateinit var vehicleVinCache: VehicleVinCache

    @Inject
    lateinit var vehicleBeaconScanner: VehicleBeaconScanner

    @Inject
    lateinit var checkBluetoothConnectionUseCase: CheckBluetoothConnectionUseCase

    private lateinit var authService: AuthorizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authService = AuthorizationService(this)

        setContent {
            var isAuthenticated by remember { mutableStateOf(tokenManager.isAuthenticated()) }
            var vehicleFetchError by remember { mutableStateOf<String?>(null) }
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            // BLUETOOTH_SCAN is a dangerous permission on API 31+: declaring it in the manifest
            // alone never grants it. Without this request VehicleBeaconScanner.startIfPossible()
            // silently no-ops forever, with no crash or error to signal why -- the widget just
            // never detects the car.
            val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                if (granted) {
                    vehicleBeaconScanner.startIfPossible()
                    VehicleWidgetProvider.requestRefresh(context)
                }
            }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    vehicleBeaconScanner.startIfPossible()
                    VehicleWidgetProvider.requestRefresh(context)
                } else {
                    bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
                }
                // Covers the case where the app was already authenticated from a previous
                // session before this launch -- fetchVehicleAndStartScan below covers the
                // "just completed login/reconnect" case, which this LaunchedEffect(Unit) can't
                // (isAuthenticated is already true by the time this composes, so a later
                // true->true transition on reconnect wouldn't otherwise trigger anything).
                if (isAuthenticated) {
                    fetchVehicleAndStartScan(coroutineScope, context) { vehicleFetchError = it }
                }
            }

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
                        if (isAuthenticated) {
                            fetchVehicleAndStartScan(coroutineScope, context) { vehicleFetchError = it }
                        }
                    }
                } else if (exception != null) {
                    isAuthenticated = false
                }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column {
                        ConnectScreen(
                            isAuthenticated = isAuthenticated,
                            onConnectClick = {
                                val intent = authService.getAuthorizationRequestIntent(OAuthConfig.buildAuthorizationRequest())
                                authLauncher.launch(intent)
                            },
                        )
                        if (BuildConfig.DEBUG) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            BluetoothDebugPanel(
                                vehicleVinCache = vehicleVinCache,
                                vehicleFetchError = vehicleFetchError,
                                checkConnection = checkBluetoothConnectionUseCase,
                                onFetchVehicleClick = {
                                    fetchVehicleAndStartScan(coroutineScope, context) { vehicleFetchError = it }
                                },
                                onRestartScanClick = {
                                    vehicleBeaconScanner.startIfPossible()
                                    VehicleWidgetProvider.requestRefresh(context)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Fetches the vehicle (caching its VIN as a side effect, see VehicleRepositoryImpl) and
    // (re)starts the BLE beacon scan now that the VIN is available. Called both on launch (if
    // already authenticated from a previous session) and right after a fresh login/reconnect
    // completes, since a boolean isAuthenticated LaunchedEffect key can't distinguish "just
    // became true" from "was already true" on a reconnect.
    private fun fetchVehicleAndStartScan(
        coroutineScope: CoroutineScope,
        context: android.content.Context,
        onError: (String?) -> Unit,
    ) {
        coroutineScope.launch {
            val result = vehicleRepository.getVehicle()
            onError(result.exceptionOrNull()?.let { "${it::class.simpleName}: ${it.message}" })
            vehicleBeaconScanner.startIfPossible()
            VehicleWidgetProvider.requestRefresh(context)
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

// Debug-build-only (BuildConfig.DEBUG) surface for the question that stalled testing before:
// does the app actually see the car nearby? Shows the cached VIN, the exact BLE beacon name
// derived from it, and the computed proximity state, not just the final yes/no -- so a missing
// permission, an uncached VIN, or a beacon that's simply out of range are all visible directly
// instead of collapsing into one "disconnected" reading.
@Composable
private fun BluetoothDebugPanel(
    vehicleVinCache: VehicleVinCache,
    vehicleFetchError: String?,
    checkConnection: CheckBluetoothConnectionUseCase,
    onFetchVehicleClick: () -> Unit,
    onRestartScanClick: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var cachedVin by remember { mutableStateOf<String?>(null) }
    var connectionState by remember { mutableStateOf<ConnectionState?>(null) }
    var refreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTick) {
        hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED
        cachedVin = vehicleVinCache.get()
        connectionState = checkConnection()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3_000)
            refreshTick++
        }
    }

    val vin = cachedVin
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Bluetooth debug", style = MaterialTheme.typography.titleMedium)
        Text("BLUETOOTH_SCAN permission: " + if (hasPermission) "granted" else "NOT granted")
        Text("Cached VIN: " + (vin ?: "(none yet -- tap Fetch vehicle now below)"))
        Text("Expected BLE beacon name: " + if (vin != null) TeslaBleBeaconName.forVin(vin) else "(unknown, no VIN)")
        Text("Computed state: ${connectionState?.name ?: "checking..."}")
        Text("Last vehicle fetch error: " + (vehicleFetchError ?: "(none)"))
        Button(onClick = { refreshTick++ }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Refresh now")
        }
        Button(onClick = onFetchVehicleClick, modifier = Modifier.padding(top = 8.dp)) {
            Text("Fetch vehicle now")
        }
        Button(onClick = onRestartScanClick, modifier = Modifier.padding(top = 8.dp)) {
            Text("Restart beacon scan")
        }
    }
}
