package com.musornibak.pocketvpn

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class PocketVpnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_VPN, getString(R.string.notif_channel_vpn), NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_TOR, getString(R.string.notif_channel_tor), NotificationManager.IMPORTANCE_MIN)
            )
        }
    }

    companion object {
        const val CHANNEL_VPN = "vpn"
        const val CHANNEL_TOR = "tor"
    }
}
