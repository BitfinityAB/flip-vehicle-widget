package com.flipvehiclewidget.app.di

import com.flipvehiclewidget.app.domain.entity.VehicleCommand
import com.flipvehiclewidget.app.domain.usecase.ToggleClimateUseCase
import com.flipvehiclewidget.app.domain.usecase.ToggleFrunkUseCase
import com.flipvehiclewidget.app.domain.usecase.ToggleLocksUseCase
import com.flipvehiclewidget.app.domain.usecase.ToggleTrunkUseCase
import com.flipvehiclewidget.app.domain.usecase.VehicleCommandUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    fun provideVehicleCommandUseCases(
        toggleTrunk: ToggleTrunkUseCase,
        toggleFrunk: ToggleFrunkUseCase,
        toggleClimate: ToggleClimateUseCase,
        toggleLocks: ToggleLocksUseCase,
    ): Map<VehicleCommand, VehicleCommandUseCase> = mapOf(
        VehicleCommand.TOGGLE_TRUNK to toggleTrunk,
        VehicleCommand.TOGGLE_FRUNK to toggleFrunk,
        VehicleCommand.TOGGLE_CLIMATE to toggleClimate,
        VehicleCommand.TOGGLE_LOCKS to toggleLocks,
    )
}
