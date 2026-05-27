package com.musornibak.pocketvpn.tor

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.musornibak.pocketvpn.MainActivity
import com.musornibak.pocketvpn.PocketVpnApp
import com.musornibak.pocketvpn.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TorService : LifecycleService() {

    private val engine by lazy { TorEngine(applicationContext) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIF_ID, buildNotif("Tor idle"))
        lifecycleScope.launch {
            engine.bootstrapPercent.collectLatest { p ->
                if (p in 1..99) updateNotif("Bootstrapping $p%")
                if (p == 100) updateNotif("Tor ready")
            }
        }
        return START_STICKY
    }

    private fun buildNotif(text: String): Notification {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, PocketVpnApp.CHANNEL_TOR)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(tap)
            .build()
    }

    private fun updateNotif(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotif(text))
    }

    companion object { const val NOTIF_ID = 0x70637401 }
}
