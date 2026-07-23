package com.flipvehiclewidget.app.data.repository

import com.flipvehiclewidget.app.data.api.VehicleApiService
import com.flipvehiclewidget.app.data.api.VehicleCommandApiService
import com.flipvehiclewidget.app.data.api.dto.ActuateTrunkRequestDto
import com.flipvehiclewidget.app.data.api.dto.CommandResponseDto
import com.flipvehiclewidget.app.data.local.VehicleVinCache
import com.flipvehiclewidget.app.domain.entity.CommandResult
import com.flipvehiclewidget.app.domain.entity.Vehicle
import com.flipvehiclewidget.app.domain.entity.VehicleStatus
import com.flipvehiclewidget.app.domain.repository.VehicleRepository
import javax.inject.Inject

class VehicleRepositoryImpl @Inject constructor(
    private val vehicleApiService: VehicleApiService,
    private val commandApiService: VehicleCommandApiService,
    private val vehicleVinCache: VehicleVinCache,
) : VehicleRepository {

    override suspend fun getVehicle(): Result<Vehicle> = runCatching {
        val response = vehicleApiService.getVehicles()
        val dto = response.response.firstOrNull() ?: error("No vehicles found on account")
        // Cached so VehicleBeaconScanner can compute the vehicle's BLE beacon name (see
        // TeslaBleBeaconName) without needing a network call every time it wants to (re)start
        // scanning.
        vehicleVinCache.save(dto.vin)
        Vehicle(id = dto.id, vin = dto.vin, displayName = dto.displayName)
    }

    override suspend fun getVehicleStatus(vehicle: Vehicle): Result<VehicleStatus> = runCatching {
        val data = vehicleApiService.getVehicleData(vehicle.id).response
        VehicleStatus(
            locked = data.vehicleState.locked,
            climateOn = data.climateState.isClimateOn,
            frontTrunkOpen = data.vehicleState.frontTrunkState != 0,
            rearTrunkOpen = data.vehicleState.rearTrunkState != 0,
        )
    }

    // toggleTrunk/toggleFrunk/toggleClimate/toggleLocks all send commands through
    // commandApiService (our tesla-http-proxy signing proxy), which requires vehicle.vin, not
    // vehicle.id -- vehicle.id is only valid against Tesla's real Fleet API (used by
    // vehicleApiService's reads below).
    override suspend fun toggleTrunk(vehicle: Vehicle): Result<CommandResult> =
        executeCommand { commandApiService.actuateTrunk(vehicle.vin, ActuateTrunkRequestDto(whichTrunk = "rear")) }

    override suspend fun toggleFrunk(vehicle: Vehicle): Result<CommandResult> =
        executeCommand { commandApiService.actuateTrunk(vehicle.vin, ActuateTrunkRequestDto(whichTrunk = "front")) }

    override suspend fun toggleClimate(vehicle: Vehicle): Result<CommandResult> = runCatching {
        val currentlyOn = vehicleApiService.getVehicleData(vehicle.id).response.climateState.isClimateOn
        val response = if (currentlyOn) commandApiService.stopClimate(vehicle.vin) else commandApiService.startClimate(vehicle.vin)
        CommandResult(success = response.response.result, reason = response.response.reason)
    }

    override suspend fun toggleLocks(vehicle: Vehicle): Result<CommandResult> = runCatching {
        val currentlyLocked = vehicleApiService.getVehicleData(vehicle.id).response.vehicleState.locked
        val response = if (currentlyLocked) commandApiService.unlockDoors(vehicle.vin) else commandApiService.lockDoors(vehicle.vin)
        CommandResult(success = response.response.result, reason = response.response.reason)
    }

    private suspend fun executeCommand(call: suspend () -> CommandResponseDto): Result<CommandResult> = runCatching {
        val response = call()
        CommandResult(success = response.response.result, reason = response.response.reason)
    }
}
