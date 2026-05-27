package com.musornibak.pocketvpn.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.musornibak.pocketvpn.MainActivity
import com.musornibak.pocketvpn.PocketVpnApp
import com.musornibak.pocketvpn.R

/**
 * VpnService that establishes the TUN and (eventually) forwards TCP packets
 * to the local SOCKS5 proxy provided by TorService. v0.1.0 only sets up the
 * tunnel and leaves the actual packet pump for v0.2.0.
 *
 * Real implementation will spawn hev-socks5-tunnel (or badvpn-tun2socks) against
 * the fd from this Builder.establish().
 */
class PocketVpnService : VpnService() {

    private var tun: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotif(getString(R.string.notif_vpn_connecting)))
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        val builder = Builder()
            .setSession("PocketVPN")
            .setMtu(1500)
            .addAddress("10.7.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("1.1.1.1")
            .addDnsServer("9.9.9.9")
            .setBlocking(true)
        tun = builder.establish()
        // TODO(v0.2.0): start tun2socks worker against tun?.fileDescriptor and 127.0.0.1:9050.
    }

    private fun stopVpn() {
        tun?.close()
        tun = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        tun?.close()
        super.onDestroy()
    }

    private fun buildNotif(text: String): Notification {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, PocketVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, PocketVpnApp.CHANNEL_VPN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(tap)
            .addAction(0, "Stop", stop)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.musornibak.pocketvpn.STOP"
        const val NOTIF_ID = 0x70637402
    }
}
