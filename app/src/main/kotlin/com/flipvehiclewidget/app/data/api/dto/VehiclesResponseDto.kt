package com.flipvehiclewidget.app.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehiclesResponseDto(
    @SerialName("response") val response: List<VehicleDto>,
    @SerialName("count") val count: Int,
)
