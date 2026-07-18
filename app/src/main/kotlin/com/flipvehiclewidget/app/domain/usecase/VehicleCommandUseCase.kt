package com.flipvehiclewidget.app.domain.usecase

import com.flipvehiclewidget.app.domain.entity.CommandResult

interface VehicleCommandUseCase {
    suspend operator fun invoke(vehicleId: Long): Result<CommandResult>
}
