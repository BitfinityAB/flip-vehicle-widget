package com.flipvehiclewidget.app.domain.usecase

import com.flipvehiclewidget.app.domain.entity.CommandResult
import com.flipvehiclewidget.app.domain.entity.Vehicle
import com.flipvehiclewidget.app.domain.repository.VehicleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeVehicleRepository : VehicleRepository {
    var lastVehicle: Vehicle? = null

    override suspend fun getVehicle(): Result<Vehicle> =
        Result.success(Vehicle(id = 7L, vin = "5YJ3E1EA1PF000001", displayName = "Car"))

    override suspend fun toggleTrunk(vehicle: Vehicle): Result<CommandResult> {
        lastVehicle = vehicle
        return Result.success(CommandResult(success = true, reason = null))
    }

    override suspend fun toggleFrunk(vehicle: Vehicle) = toggleTrunk(vehicle)
    override suspend fun toggleClimate(vehicle: Vehicle) = toggleTrunk(vehicle)
    override suspend fun toggleLocks(vehicle: Vehicle) = toggleTrunk(vehicle)
}

class VehicleCommandUseCasesTest {
    @Test
    fun `toggle trunk use case delegates to repository with the vehicle (vin, not just id)`() = runTest {
        val repository = FakeVehicleRepository()
        val useCase = ToggleTrunkUseCase(repository)
        val vehicle = Vehicle(id = 7L, vin = "5YJ3E1EA1PF000001", displayName = "Car")

        val result = useCase(vehicle)

        assertEquals(vehicle, repository.lastVehicle)
        assertEquals(true, result.getOrThrow().success)
    }
}
