package com.ecolocate.network

import com.ecolocate.model.TelemetryPayload
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface TelemetryApi {
    @POST("/api/devices/{id}/location")
    suspend fun postTelemetry(
        @Path("id") deviceId: String,
        @Header("x-device-id") deviceHeaderId: String,
        @Header("x-api-key") apiKey: String,
        @Body payload: TelemetryPayload
    ): Response<Unit>
}


