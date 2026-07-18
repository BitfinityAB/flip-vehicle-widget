package com.flipvehiclewidget.app.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActuateTrunkRequestDto(
    @SerialName("which_trunk") val whichTrunk: String,
)

@Serializable
data class CommandResultDto(
    @SerialName("result") val result: Boolean,
    @SerialName("reason") val reason: String? = null,
)

@Serializable
data class CommandResponseDto(
    @SerialName("response") val response: CommandResultDto,
)
