package com.flipvehiclewidget.app.presentation.widget

import com.flipvehiclewidget.app.domain.entity.VehicleCommand

enum class CommandButtonState {
    IDLE,
    LOADING,
    ERROR,
}

sealed interface WidgetState {
    data object Disconnected : WidgetState

    data class Connected(
        val commandStates: Map<VehicleCommand, CommandButtonState> =
            VehicleCommand.entries.associateWith { CommandButtonState.IDLE },
    ) : WidgetState
}
