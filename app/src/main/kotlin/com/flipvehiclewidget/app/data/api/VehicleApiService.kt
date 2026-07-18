package com.flipvehiclewidget.app.data.api

import com.flipvehiclewidget.app.data.api.dto.VehicleDataResponseDto
import com.flipvehiclewidget.app.data.api.dto.VehiclesResponseDto
import retrofit2.http.GET
import retrofit2.http.Path

interface VehicleApiService {
    @GET("api/1/vehicles")
    suspend fun getVehicles(): VehiclesResponseDto

    @GET("api/1/vehicles/{id}/vehicle_data")
    suspend fun getVehicleData(@Path("id") vehicleId: Long): VehicleDataResponseDto
}
