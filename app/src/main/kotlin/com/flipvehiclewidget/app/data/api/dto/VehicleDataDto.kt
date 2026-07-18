package com.flipvehiclewidget.app.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleDataResponseDto(
    @SerialName("response") val response: VehicleDataDto,
)

@Serializable
data class VehicleDataDto(
    @SerialName("vehicle_state") val vehicleState: VehicleStateDto,
    @SerialName("climate_state") val climateState: ClimateStateDto,
)

@Serializable
data class VehicleStateDto(
    @SerialName("locked") val locked: Boolean,
)

@Serializable
data class ClimateStateDto(
    @SerialName("is_climate_on") val isClimateOn: Boolean,
)
