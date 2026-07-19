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
import com.flipvehiclewidget.app.data.local.VehicleVinCache
import com.flipvehiclewidget.app.domain.entity.Vehicle
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private val TEST_VEHICLE = Vehicle(id = 7L, vin = "5YJ3E1EA1PF000001", displayName = "Car")

class VehicleRepositoryImplTest {
    private val vehicleApiService = mockk<VehicleApiService>()
    private val commandApiService = mockk<VehicleCommandApiService>()
    private val vehicleVinCache = mockk<VehicleVinCache>(relaxed = true)
    private val repository = VehicleRepositoryImpl(vehicleApiService, commandApiService, vehicleVinCache)

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
    fun `getVehicle caches the VIN for beacon scanning`() = runTest {
        coEvery { vehicleApiService.getVehicles() } returns VehiclesResponseDto(
            response = listOf(VehicleDto(id = 7L, vin = "5YJSA1E14FF101183", displayName = "Car")),
            count = 1,
        )

        repository.getVehicle().getOrThrow()

        verify { vehicleVinCache.save("5YJSA1E14FF101183") }
    }

    @Test
    fun `getVehicle fails when account has no vehicles`() = runTest {
        coEvery { vehicleApiService.getVehicles() } returns VehiclesResponseDto(response = emptyList(), count = 0)

        val result = repository.getVehicle()

        assert(result.isFailure)
    }

    @Test
    fun `toggleTrunk requests rear trunk by VIN and maps result`() = runTest {
        coEvery {
            commandApiService.actuateTrunk(TEST_VEHICLE.vin, ActuateTrunkRequestDto(whichTrunk = "rear"))
        } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleTrunk(TEST_VEHICLE).getOrThrow()

        assertEquals(true, result.success)
    }

    @Test
    fun `toggleFrunk requests front trunk by VIN`() = runTest {
        coEvery {
            commandApiService.actuateTrunk(TEST_VEHICLE.vin, ActuateTrunkRequestDto(whichTrunk = "front"))
        } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleFrunk(TEST_VEHICLE).getOrThrow()

        assertEquals(true, result.success)
    }

    @Test
    fun `toggleClimate reads state by id but commands by VIN, stopping when currently on`() = runTest {
        coEvery { vehicleApiService.getVehicleData(TEST_VEHICLE.id) } returns VehicleDataResponseDto(
            VehicleDataDto(vehicleState = VehicleStateDto(locked = true), climateState = ClimateStateDto(isClimateOn = true)),
        )
        coEvery { commandApiService.stopClimate(TEST_VEHICLE.vin) } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleClimate(TEST_VEHICLE).getOrThrow()

        assertEquals(true, result.success)
    }

    @Test
    fun `toggleClimate starts climate when currently off`() = runTest {
        coEvery { vehicleApiService.getVehicleData(TEST_VEHICLE.id) } returns VehicleDataResponseDto(
            VehicleDataDto(vehicleState = VehicleStateDto(locked = true), climateState = ClimateStateDto(isClimateOn = false)),
        )
        coEvery { commandApiService.startClimate(TEST_VEHICLE.vin) } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleClimate(TEST_VEHICLE).getOrThrow()

        assertEquals(true, result.success)
    }

    @Test
    fun `toggleLocks unlocks by VIN when currently locked`() = runTest {
        coEvery { vehicleApiService.getVehicleData(TEST_VEHICLE.id) } returns VehicleDataResponseDto(
            VehicleDataDto(vehicleState = VehicleStateDto(locked = true), climateState = ClimateStateDto(isClimateOn = false)),
        )
        coEvery { commandApiService.unlockDoors(TEST_VEHICLE.vin) } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleLocks(TEST_VEHICLE).getOrThrow()

        assertEquals(true, result.success)
    }

    @Test
    fun `toggleLocks locks by VIN when currently unlocked`() = runTest {
        coEvery { vehicleApiService.getVehicleData(TEST_VEHICLE.id) } returns VehicleDataResponseDto(
            VehicleDataDto(vehicleState = VehicleStateDto(locked = false), climateState = ClimateStateDto(isClimateOn = false)),
        )
        coEvery { commandApiService.lockDoors(TEST_VEHICLE.vin) } returns CommandResponseDto(CommandResultDto(result = true, reason = null))

        val result = repository.toggleLocks(TEST_VEHICLE).getOrThrow()

        assertEquals(true, result.success)
    }
}
