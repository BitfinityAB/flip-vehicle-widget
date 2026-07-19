package com.flipvehiclewidget.app.domain.usecase

import com.flipvehiclewidget.app.domain.entity.CommandResult
import com.flipvehiclewidget.app.domain.entity.Vehicle

interface VehicleCommandUseCase {
    suspend operator fun invoke(vehicle: Vehicle): Result<CommandResult>
}
