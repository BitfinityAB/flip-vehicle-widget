package com.flipvehiclewidget.app.presentation.widget

import com.flipvehiclewidget.app.domain.entity.VehicleCommand
import com.flipvehiclewidget.app.domain.entity.VehicleStatus

enum class CommandButtonState {
    IDLE,
    LOADING,
    ERROR,
}

sealed interface WidgetState {
    data class Disconnected(val checking: Boolean = false) : WidgetState

    data class Connected(
        val commandStates: Map<VehicleCommand, CommandButtonState> =
            VehicleCommand.entries.associateWith { CommandButtonState.IDLE },
        // Null when a fresh fetch wasn't attempted (e.g. a single-command loading/error render) --
        // callers should fall back to VehicleStatusStore.lastKnown() rather than pass null through
        // when a real snapshot is unavailable, so the other 3 icons don't reset to a default look.
        val status: VehicleStatus? = null,
    ) : WidgetState
}
