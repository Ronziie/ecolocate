package com.ecolocate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ecolocate.Config
import com.ecolocate.R

/**
 * Minimal foreground service to keep the process alive and trigger immediate work on start.
 */
class TelemetryForegroundService : Service() {
    private lateinit var config: Config

    override fun onCreate() {
        super.onCreate()
        config = Config(this)
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Increment restart counter when service is (re)started
        config.serviceRestartCount = config.serviceRestartCount + 1
        // Kick off a one-time immediate telemetry upload
        val work = OneTimeWorkRequestBuilder<com.ecolocate.worker.TelemetryWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork("telemetry-immediate", ExistingWorkPolicy.REPLACE, work)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "telemetry_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Telemetry", NotificationManager.IMPORTANCE_MIN)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Telemetry running")
            .setContentText("ecolocate background service active")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}


