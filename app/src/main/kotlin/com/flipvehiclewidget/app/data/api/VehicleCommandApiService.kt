package com.flipvehiclewidget.app.data.api

import com.flipvehiclewidget.app.data.api.dto.ActuateTrunkRequestDto
import com.flipvehiclewidget.app.data.api.dto.CommandResponseDto
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

// Unlike VehicleApiService (reads, hit Tesla's real Fleet API directly, which takes the
// numeric account-scoped vehicle id), these go through our own tesla-http-proxy signing proxy,
// which requires the vehicle's 17-character VIN in the path and rejects the numeric id with a
// 404 ("expected 17-character VIN in path (do not use Fleet API ID)") -- see
// pkg/proxy/proxy.go in teslamotors/vehicle-command.
interface VehicleCommandApiService {
    @POST("api/1/vehicles/{vin}/command/door_lock")
    suspend fun lockDoors(@Path("vin") vin: String): CommandResponseDto

    @POST("api/1/vehicles/{vin}/command/door_unlock")
    suspend fun unlockDoors(@Path("vin") vin: String): CommandResponseDto

    @POST("api/1/vehicles/{vin}/command/actuate_trunk")
    suspend fun actuateTrunk(@Path("vin") vin: String, @Body body: ActuateTrunkRequestDto): CommandResponseDto

    @POST("api/1/vehicles/{vin}/command/auto_conditioning_start")
    suspend fun startClimate(@Path("vin") vin: String): CommandResponseDto

    @POST("api/1/vehicles/{vin}/command/auto_conditioning_stop")
    suspend fun stopClimate(@Path("vin") vin: String): CommandResponseDto
}
