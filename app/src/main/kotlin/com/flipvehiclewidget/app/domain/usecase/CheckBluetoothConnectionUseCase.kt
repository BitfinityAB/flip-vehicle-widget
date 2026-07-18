package com.flipvehiclewidget.app.domain.usecase

import com.flipvehiclewidget.app.data.bluetooth.BluetoothConnectionManager
import com.flipvehiclewidget.app.domain.entity.ConnectionState
import javax.inject.Inject

class CheckBluetoothConnectionUseCase @Inject constructor(
    private val bluetoothConnectionManager: BluetoothConnectionManager,
) {
    suspend operator fun invoke(): ConnectionState = bluetoothConnectionManager.currentConnectionState()
}
