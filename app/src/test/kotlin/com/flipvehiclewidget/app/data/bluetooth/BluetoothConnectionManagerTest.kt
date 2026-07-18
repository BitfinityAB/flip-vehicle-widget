package com.flipvehiclewidget.app.data.bluetooth

import com.flipvehiclewidget.app.domain.entity.ConnectionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeBluetoothConnectivityGateway(private val names: List<String>) : BluetoothConnectivityGateway {
    override suspend fun connectedDeviceNames(): List<String> = names
}

class BluetoothConnectionManagerTest {
    @Test
    fun `reports connected when a connected device name matches configured vehicle name`() = runTest {
        val manager = BluetoothConnectionManager(
            gateway = FakeBluetoothConnectivityGateway(listOf("Aziz's Model 3")),
            vehicleBluetoothName = "Model 3",
        )

        assertEquals(ConnectionState.CONNECTED, manager.currentConnectionState())
    }

    @Test
    fun `reports disconnected when no connected device name matches`() = runTest {
        val manager = BluetoothConnectionManager(
            gateway = FakeBluetoothConnectivityGateway(listOf("AirPods Pro")),
            vehicleBluetoothName = "Model 3",
        )

        assertEquals(ConnectionState.DISCONNECTED, manager.currentConnectionState())
    }

    @Test
    fun `reports disconnected when nothing is connected`() = runTest {
        val manager = BluetoothConnectionManager(
            gateway = FakeBluetoothConnectivityGateway(emptyList()),
            vehicleBluetoothName = "Model 3",
        )

        assertEquals(ConnectionState.DISCONNECTED, manager.currentConnectionState())
    }
}
