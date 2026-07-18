package com.flipvehiclewidget.app.data.bluetooth

import com.flipvehiclewidget.app.BuildConfig
import com.flipvehiclewidget.app.domain.entity.ConnectionState
import javax.inject.Inject

class BluetoothConnectionManager @Inject constructor(
    private val gateway: BluetoothConnectivityGateway,
    private val vehicleBluetoothName: String = BuildConfig.VEHICLE_BT_NAME,
) {
    suspend fun currentConnectionState(): ConnectionState {
        val matches = gateway.connectedDeviceNames()
            .any { it.contains(vehicleBluetoothName, ignoreCase = true) }
        return if (matches) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
    }
}
