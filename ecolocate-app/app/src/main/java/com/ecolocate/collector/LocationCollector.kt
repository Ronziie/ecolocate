package com.ecolocate.collector

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.ecolocate.model.LocationInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationCollector(context: Context) {
    private val fused: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getLocation(): LocationInfo? {
        val last = runCatching { fused.lastLocation.awaitNullable() }.getOrNull()
        if (last != null) return last.toInfo()

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000).setMaxUpdates(1).build()
        val single = runCatching { fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).awaitNullable() }.getOrNull()
        return single?.toInfo()
    }

    private fun Location.toInfo(): LocationInfo = LocationInfo(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        altitude = if (hasAltitude()) altitude else null,
        speed = if (hasSpeed()) speed else null,
        bearing = if (hasBearing()) bearing else null,
        provider = provider
    )
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitNullable(): T? = suspendCancellableCoroutine { cont ->
    addOnCompleteListener {
        if (it.isSuccessful) cont.resume(it.result)
        else cont.resume(null)
    }
}


