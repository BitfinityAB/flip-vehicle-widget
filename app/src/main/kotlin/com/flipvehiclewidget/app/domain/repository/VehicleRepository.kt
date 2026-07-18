package com.flipvehiclewidget.app.domain.repository

import com.flipvehiclewidget.app.domain.entity.CommandResult
import com.flipvehiclewidget.app.domain.entity.Vehicle

interface VehicleRepository {
    suspend fun getVehicle(): Result<Vehicle>
    suspend fun toggleTrunk(vehicleId: Long): Result<CommandResult>
    suspend fun toggleFrunk(vehicleId: Long): Result<CommandResult>
    suspend fun toggleClimate(vehicleId: Long): Result<CommandResult>
    suspend fun toggleLocks(vehicleId: Long): Result<CommandResult>
}
