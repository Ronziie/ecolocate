package com.ecolocate.collector

import android.content.Context
import android.os.Build
import com.ecolocate.model.DeviceInfo

class DeviceInfoCollector(private val context: Context) {
    fun getDeviceInfo(): DeviceInfo {
        val model = Build.MODEL ?: "Android"
        val osVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        val appVersion = runCatching {
            val p = context.packageManager
            val info = p.getPackageInfo(context.packageName, 0)
            info.versionName ?: "1.0.0"
        }.getOrElse { "1.0.0" }
        return DeviceInfo(model, osVersion, appVersion)
    }
}


