package com.ecolocate

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Application class initializes WorkManager periodic work based on config.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleTelemetry()
    }

    private fun scheduleTelemetry() {
        val config = Config(this)
        if (!config.trackingEnabled) return
        val interval = config.pollIntervalMinutes.toLong().coerceAtLeast(15L)
        val work = PeriodicWorkRequestBuilder<com.ecolocate.worker.TelemetryWorker>(interval, TimeUnit.MINUTES)
            .addTag("telemetry-periodic")
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "telemetry-periodic",
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        )
    }
}


