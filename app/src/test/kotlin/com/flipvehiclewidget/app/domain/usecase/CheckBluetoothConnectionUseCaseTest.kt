package com.flipvehiclewidget.app.domain.usecase

import com.flipvehiclewidget.app.data.bluetooth.BluetoothConnectionManager
import com.flipvehiclewidget.app.data.bluetooth.BluetoothConnectivityGateway
import com.flipvehiclewidget.app.domain.entity.ConnectionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeGateway(private val names: List<String>) : BluetoothConnectivityGateway {
    override suspend fun connectedDeviceNames(): List<String> = names
}

class CheckBluetoothConnectionUseCaseTest {
    @Test
    fun `delegates to bluetooth connection manager`() = runTest {
        val manager = BluetoothConnectionManager(FakeGateway(listOf("Model 3")), vehicleBluetoothName = "Model 3")
        val useCase = CheckBluetoothConnectionUseCase(manager)

        assertEquals(ConnectionState.CONNECTED, useCase())
    }
}
