package com.flipvehiclewidget.app.domain.usecase

import com.flipvehiclewidget.app.domain.entity.CommandResult
import com.flipvehiclewidget.app.domain.entity.Vehicle
import com.flipvehiclewidget.app.domain.repository.VehicleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeVehicleRepository : VehicleRepository {
    var lastVehicleId: Long? = null

    override suspend fun getVehicle(): Result<Vehicle> =
        Result.success(Vehicle(id = 7L, vin = "vin", displayName = "Car"))

    override suspend fun toggleTrunk(vehicleId: Long): Result<CommandResult> {
        lastVehicleId = vehicleId
        return Result.success(CommandResult(success = true, reason = null))
    }

    override suspend fun toggleFrunk(vehicleId: Long) = toggleTrunk(vehicleId)
    override suspend fun toggleClimate(vehicleId: Long) = toggleTrunk(vehicleId)
    override suspend fun toggleLocks(vehicleId: Long) = toggleTrunk(vehicleId)
}

class VehicleCommandUseCasesTest {
    @Test
    fun `toggle trunk use case delegates to repository with vehicle id`() = runTest {
        val repository = FakeVehicleRepository()
        val useCase = ToggleTrunkUseCase(repository)

        val result = useCase(vehicleId = 7L)

        assertEquals(7L, repository.lastVehicleId)
        assertEquals(true, result.getOrThrow().success)
    }
}
