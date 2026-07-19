package com.flipvehiclewidget.app.data.bluetooth

import com.flipvehiclewidget.app.data.local.VehicleProximityStore
import com.flipvehiclewidget.app.domain.entity.ConnectionState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BluetoothConnectionManagerTest {
    @Test
    fun `reports connected when the proximity store has seen the beacon`() = runTest {
        val proximityStore = mockk<VehicleProximityStore> { every { isVehicleNearby() } returns true }
        val manager = BluetoothConnectionManager(proximityStore)

        assertEquals(ConnectionState.CONNECTED, manager.currentConnectionState())
    }

    @Test
    fun `reports disconnected when the proximity store has not seen the beacon`() = runTest {
        val proximityStore = mockk<VehicleProximityStore> { every { isVehicleNearby() } returns false }
        val manager = BluetoothConnectionManager(proximityStore)

        assertEquals(ConnectionState.DISCONNECTED, manager.currentConnectionState())
    }
}
