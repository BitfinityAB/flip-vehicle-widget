package com.flipvehiclewidget.app.data.api

import com.flipvehiclewidget.app.data.api.dto.ActuateTrunkRequestDto
import com.flipvehiclewidget.app.data.api.dto.CommandResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface VehicleCommandApiService {
    @POST("api/1/vehicles/{id}/command/door_lock")
    suspend fun lockDoors(@Path("id") vehicleId: Long): CommandResponseDto

    @POST("api/1/vehicles/{id}/command/door_unlock")
    suspend fun unlockDoors(@Path("id") vehicleId: Long): CommandResponseDto

    @POST("api/1/vehicles/{id}/command/actuate_trunk")
    suspend fun actuateTrunk(@Path("id") vehicleId: Long, @Body body: ActuateTrunkRequestDto): CommandResponseDto

    @POST("api/1/vehicles/{id}/command/auto_conditioning_start")
    suspend fun startClimate(@Path("id") vehicleId: Long): CommandResponseDto

    @POST("api/1/vehicles/{id}/command/auto_conditioning_stop")
    suspend fun stopClimate(@Path("id") vehicleId: Long): CommandResponseDto
}
