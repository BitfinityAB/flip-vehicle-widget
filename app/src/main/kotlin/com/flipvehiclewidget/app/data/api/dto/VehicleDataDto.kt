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
    // Tesla's real field names -- 0 = closed, nonzero = open (partially or fully).
    @SerialName("ft") val frontTrunkState: Int = 0,
    @SerialName("rt") val rearTrunkState: Int = 0,
)

@Serializable
data class ClimateStateDto(
    @SerialName("is_climate_on") val isClimateOn: Boolean,
)
