package com.flipvehiclewidget.app.domain.usecase

import com.flipvehiclewidget.app.data.bluetooth.BluetoothConnectionManager
import com.flipvehiclewidget.app.data.local.VehicleProximityStore
import com.flipvehiclewidget.app.domain.entity.ConnectionState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckBluetoothConnectionUseCaseTest {
    @Test
    fun `delegates to bluetooth connection manager`() = runTest {
        val proximityStore = mockk<VehicleProximityStore> { every { isVehicleNearby() } returns true }
        val useCase = CheckBluetoothConnectionUseCase(BluetoothConnectionManager(proximityStore))

        assertEquals(ConnectionState.CONNECTED, useCase())
    }
}
