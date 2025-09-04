package com.ecolocate.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ecolocate.Config
import com.ecolocate.service.TelemetryForegroundService

/**
 * Starts the foreground service after device boot if tracking is enabled.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val config = Config(context)
        if (!config.trackingEnabled) return
        val serviceIntent = Intent(context, TelemetryForegroundService::class.java)
        context.startForegroundService(serviceIntent)
    }
}


