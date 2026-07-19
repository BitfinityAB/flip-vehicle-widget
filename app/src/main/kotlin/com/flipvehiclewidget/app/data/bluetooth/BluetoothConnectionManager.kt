package com.flipvehiclewidget.app.data.bluetooth

import com.flipvehiclewidget.app.data.local.VehicleProximityStore
import com.flipvehiclewidget.app.domain.entity.ConnectionState
import javax.inject.Inject

class BluetoothConnectionManager @Inject constructor(
    private val proximityStore: VehicleProximityStore,
) {
    suspend fun currentConnectionState(): ConnectionState =
        if (proximityStore.isVehicleNearby()) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
}
