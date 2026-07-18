package com.flipvehiclewidget.app.data.repository

import com.flipvehiclewidget.app.data.api.VehicleApiService
import com.flipvehiclewidget.app.data.api.VehicleCommandApiService
import com.flipvehiclewidget.app.data.api.dto.ActuateTrunkRequestDto
import com.flipvehiclewidget.app.data.api.dto.CommandResponseDto
import com.flipvehiclewidget.app.domain.entity.CommandResult
import com.flipvehiclewidget.app.domain.entity.Vehicle
import com.flipvehiclewidget.app.domain.repository.VehicleRepository
import javax.inject.Inject

class VehicleRepositoryImpl @Inject constructor(
    private val vehicleApiService: VehicleApiService,
    private val commandApiService: VehicleCommandApiService,
) : VehicleRepository {

    override suspend fun getVehicle(): Result<Vehicle> = runCatching {
        val response = vehicleApiService.getVehicles()
        val dto = response.response.firstOrNull() ?: error("No vehicles found on account")
        Vehicle(id = dto.id, vin = dto.vin, displayName = dto.displayName)
    }

    override suspend fun toggleTrunk(vehicleId: Long): Result<CommandResult> =
        executeCommand { commandApiService.actuateTrunk(vehicleId, ActuateTrunkRequestDto(whichTrunk = "rear")) }

    override suspend fun toggleFrunk(vehicleId: Long): Result<CommandResult> =
        executeCommand { commandApiService.actuateTrunk(vehicleId, ActuateTrunkRequestDto(whichTrunk = "front")) }

    override suspend fun toggleClimate(vehicleId: Long): Result<CommandResult> = runCatching {
        val currentlyOn = vehicleApiService.getVehicleData(vehicleId).response.climateState.isClimateOn
        val response = if (currentlyOn) commandApiService.stopClimate(vehicleId) else commandApiService.startClimate(vehicleId)
        CommandResult(success = response.response.result, reason = response.response.reason)
    }

    override suspend fun toggleLocks(vehicleId: Long): Result<CommandResult> = runCatching {
        val currentlyLocked = vehicleApiService.getVehicleData(vehicleId).response.vehicleState.locked
        val response = if (currentlyLocked) commandApiService.unlockDoors(vehicleId) else commandApiService.lockDoors(vehicleId)
        CommandResult(success = response.response.result, reason = response.response.reason)
    }

    private suspend fun executeCommand(call: suspend () -> CommandResponseDto): Result<CommandResult> = runCatching {
        val response = call()
        CommandResult(success = response.response.result, reason = response.response.reason)
    }
}
