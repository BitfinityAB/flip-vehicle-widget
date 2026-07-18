package com.flipvehiclewidget.app.data.repository

import com.flipvehiclewidget.app.data.api.VehicleApiService
import com.flipvehiclewidget.app.data.api.VehicleCommandApiService
import com.flipvehiclewidget.app.data.api.dto.ActuateTrunkRequestDto
import com.flipvehiclewidget.app.data.api.dto.ClimateStateDto
import com.flipvehiclewidget.app.data.api.dto.CommandResponseDto
import com.flipvehiclewidget.app.data.api.dto.CommandResultDto
import com.flipvehiclewidget.app.data.api.dto.VehicleDataDto
import com.flipvehiclewidget.app.data.api.dto.VehicleDataResponseDto
import com.flipvehiclewidget.app.data.api.dto.VehicleDto
import com.flipvehiclewidget.app.data.api.dto.VehicleStateDto
import com.flipvehiclewidget.app.data.api.dto.VehiclesResponseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleRepositoryImplTest {
    private val vehicleApiService = mockk<VehicleApiService>()
    private val commandApiService = mockk<VehicleCommandApiService>()
    private val repository = VehicleRepositoryImpl(vehicleApiService, commandApiService)

    @Test
    fun `getVehicle maps first vehicle from list`() = runTest {
        coEvery { vehicleApiService.getVehicles() } returns VehiclesResponseDto(
            response = listOf(VehicleDto(id = 7L, vin = "vin", displayName = "Car")),
            count = 1,
        )

        val vehicle = repository.getVehicle().getOrThrow()

        assertEquals(7L, vehicle.id)
        assertEquals("Car", vehicle.displayName)
    }

    @Test
    fun `getVehicle fails when account has no vehicles`() = runTest {
        coEvery { vehicleApiService.getVehicles() } returns VehiclesResponseDto(response = emptyList(), count = 0)

        val result = repository.getVehicle()

        assert(result.isFailure)
    }

    @Test
    fun `toggleTrunk requests rear trunk and maps result`() = runTest {
        coEvery {
            commandApiService.actuateTrunk(7L, ActuateTrunkRequestDto(whichTrunk = "rear"))
        } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleTrunk(7L).getOrThrow()

        assertEquals(true, result.success)
    }

    @Test
    fun `toggleFrunk requests front trunk`() = runTest {
        coEvery {
            commandApiService.actuateTrunk(7L, ActuateTrunkRequestDto(whichTrunk = "front"))
        } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleFrunk(7L).getOrThrow()

        assertEquals(true, result.success)
    }

    @Test
    fun `toggleClimate stops climate when currently on`() = runTest {
        coEvery { vehicleApiService.getVehicleData(7L) } returns VehicleDataResponseDto(
            VehicleDataDto(vehicleState = VehicleStateDto(locked = true), climateState = ClimateStateDto(isClimateOn = true)),
        )
        coEvery { commandApiService.stopClimate(7L) } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleClimate(7L).getOrThrow()

        assertEquals(true, result.success)
    }

    @Test
    fun `toggleClimate starts climate when currently off`() = runTest {
        coEvery { vehicleApiService.getVehicleData(7L) } returns VehicleDataResponseDto(
            VehicleDataDto(vehicleState = VehicleStateDto(locked = true), climateState = ClimateStateDto(isClimateOn = false)),
        )
        coEvery { commandApiService.startClimate(7L) } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleClimate(7L).getOrThrow()

        assertEquals(true, result.success)
    }

    @Test
    fun `toggleLocks unlocks when currently locked`() = runTest {
        coEvery { vehicleApiService.getVehicleData(7L) } returns VehicleDataResponseDto(
            VehicleDataDto(vehicleState = VehicleStateDto(locked = true), climateState = ClimateStateDto(isClimateOn = false)),
        )
        coEvery { commandApiService.unlockDoors(7L) } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleLocks(7L).getOrThrow()

        assertEquals(true, result.success)
    }

    @Test
    fun `toggleLocks locks when currently unlocked`() = runTest {
        coEvery { vehicleApiService.getVehicleData(7L) } returns VehicleDataResponseDto(
            VehicleDataDto(vehicleState = VehicleStateDto(locked = false), climateState = ClimateStateDto(isClimateOn = false)),
        )
        coEvery { commandApiService.lockDoors(7L) } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleLocks(7L).getOrThrow()

        assertEquals(true, result.success)
    }
}
