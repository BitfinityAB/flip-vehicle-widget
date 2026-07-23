package com.flipvehiclewidget.app.domain.entity

data class VehicleStatus(
    val locked: Boolean,
    val climateOn: Boolean,
    val frontTrunkOpen: Boolean,
    val rearTrunkOpen: Boolean,
)
