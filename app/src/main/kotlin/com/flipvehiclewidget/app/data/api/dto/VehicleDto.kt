package com.flipvehiclewidget.app.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleDto(
    @SerialName("id") val id: Long,
    @SerialName("vin") val vin: String,
    @SerialName("display_name") val displayName: String,
)
