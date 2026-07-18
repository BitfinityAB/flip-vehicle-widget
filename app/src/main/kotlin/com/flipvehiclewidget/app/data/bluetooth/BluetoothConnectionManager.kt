package com.flipvehiclewidget.app.data.bluetooth

import com.flipvehiclewidget.app.di.VehicleBluetoothName
import com.flipvehiclewidget.app.domain.entity.ConnectionState
import javax.inject.Inject

class BluetoothConnectionManager @Inject constructor(
    private val gateway: BluetoothConnectivityGateway,
    @VehicleBluetoothName private val vehicleBluetoothName: String,
) {
    suspend fun currentConnectionState(): ConnectionState {
        val matches = gateway.connectedDeviceNames()
            .any { it.contains(vehicleBluetoothName, ignoreCase = true) }
        return if (matches) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
    }
}
