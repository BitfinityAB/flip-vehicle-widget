package com.flipvehiclewidget.app.data.bluetooth

interface BluetoothConnectivityGateway {
    suspend fun connectedDeviceNames(): List<String>
}
