package com.flipvehiclewidget.app.domain.repository

import com.flipvehiclewidget.app.domain.entity.CommandResult
import com.flipvehiclewidget.app.domain.entity.Vehicle
import com.flipvehiclewidget.app.domain.entity.VehicleStatus

interface VehicleRepository {
    suspend fun getVehicle(): Result<Vehicle>
    suspend fun getVehicleStatus(vehicle: Vehicle): Result<VehicleStatus>
    suspend fun toggleTrunk(vehicle: Vehicle): Result<CommandResult>
    suspend fun toggleFrunk(vehicle: Vehicle): Result<CommandResult>
    suspend fun toggleClimate(vehicle: Vehicle): Result<CommandResult>
    suspend fun toggleLocks(vehicle: Vehicle): Result<CommandResult>
}
