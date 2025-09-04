package com.ecolocate.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TelemetryPayload(
    val deviceId: String,
    val timestamp: String,
    val location: LocationInfo?,
    val battery: BatteryInfo?,
    val network: NetworkInfo?,
    val deviceInfo: DeviceInfo,
    val system: SystemInfo,
)

@JsonClass(generateAdapter = true)
data class LocationInfo(
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val altitude: Double?,
    val speed: Float?,
    val bearing: Float?,
    val provider: String?
)

@JsonClass(generateAdapter = true)
data class BatteryInfo(
    val level: Int?,
    val charging: Boolean?,
    val powerSource: String?,
    val temperature: Float?
)

@JsonClass(generateAdapter = true)
data class NetworkInfo(
    val type: String?,
    val signalStrength: Int?,
    val ssid: String?,
    val carrier: String?
)

@JsonClass(generateAdapter = true)
data class DeviceInfo(
    val model: String,
    val osVersion: String,
    val appVersion: String
)

@JsonClass(generateAdapter = true)
data class SystemInfo(
    val uptimeSeconds: Long,
    val serviceRestartCount: Int
)


