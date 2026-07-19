package com.flipvehiclewidget.app.domain.usecase

import com.flipvehiclewidget.app.domain.entity.CommandResult
import com.flipvehiclewidget.app.domain.entity.Vehicle
import com.flipvehiclewidget.app.domain.repository.VehicleRepository
import javax.inject.Inject

class ToggleTrunkUseCase @Inject constructor(
    private val repository: VehicleRepository,
) : VehicleCommandUseCase {
    override suspend fun invoke(vehicle: Vehicle): Result<CommandResult> = repository.toggleTrunk(vehicle)
}
