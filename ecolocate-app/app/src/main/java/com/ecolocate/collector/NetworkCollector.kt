package com.ecolocate.collector

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import com.ecolocate.model.NetworkInfo

class NetworkCollector(private val context: Context) {
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun getNetwork(): NetworkInfo? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return NetworkInfo(null, null, null, null)

        val type = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            else -> "OTHER"
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ssid = try { wifiManager.connectionInfo?.ssid?.trim('"') } catch (_: Exception) { null }

        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val carrier = telephony.networkOperatorName

        var signalDbm: Int? = null
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                signalDbm = telephony.signalStrength?.cellSignalStrengths?.firstOrNull()?.dbm
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        signalDbm = try { signalStrength.level } catch (_: Exception) { null }
                        super.onSignalStrengthsChanged(signalStrength)
                    }
                }
                @Suppress("DEPRECATION")
                telephony.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                telephony.listen(listener, PhoneStateListener.LISTEN_NONE)
            }
        } catch (_: Exception) { }

        val wifiSsid = if (type == "WIFI") ssid else null
        return NetworkInfo(type, signalDbm, wifiSsid, carrier)
    }
}


