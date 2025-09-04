package com.ecolocate.worker

import android.content.Context
import android.os.SystemClock
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ecolocate.Config
import com.ecolocate.collector.BatteryCollector
import com.ecolocate.collector.DeviceInfoCollector
import com.ecolocate.collector.LocationCollector
import com.ecolocate.collector.NetworkCollector
import com.ecolocate.model.SystemInfo
import com.ecolocate.model.TelemetryPayload
import com.ecolocate.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter

class TelemetryWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val config = Config(context)
        if (!config.trackingEnabled) return@withContext Result.success()

        val location = LocationCollector(context).getLocation()
        val battery = BatteryCollector(context).getBattery()
        val network = NetworkCollector(context).getNetwork()
        val deviceInfo = DeviceInfoCollector(context).getDeviceInfo()

        val payload = TelemetryPayload(
            deviceId = config.deviceId,
            timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            location = location,
            battery = battery,
            network = network,
            deviceInfo = deviceInfo,
            system = SystemInfo(
                uptimeSeconds = SystemClock.elapsedRealtime() / 1000,
                serviceRestartCount = config.serviceRestartCount
            )
        )

        val api = NetworkClient(context).api
        val response = runCatching {
            api.postTelemetry(config.deviceId, config.deviceId, config.apiKey, payload)
        }.getOrElse { return@withContext Result.retry() }

        if (response.isSuccessful) Result.success() else Result.retry()
    }
}


