package com.flipvehiclewidget.app.domain.usecase

import com.flipvehiclewidget.app.domain.entity.CommandResult
import com.flipvehiclewidget.app.domain.repository.VehicleRepository
import javax.inject.Inject

class ToggleLocksUseCase @Inject constructor(
    private val repository: VehicleRepository,
) : VehicleCommandUseCase {
    override suspend fun invoke(vehicleId: Long): Result<CommandResult> = repository.toggleLocks(vehicleId)
}
