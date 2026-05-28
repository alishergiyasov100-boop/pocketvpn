package com.musornibak.pocketvpn.vpn.singbox

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.musornibak.pocketvpn.MainActivity
import com.musornibak.pocketvpn.R
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.SetupOptions
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.SystemProxyStatus
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import java.io.File

/**
 * Android VpnService that drives sing-box via libbox.
 *
 * On START: ensures libbox is set up, opens a CommandServer with this as the
 * PlatformInterface, then asks sing-box to load the JSON config. sing-box calls
 * back into openTun() to get the actual TUN fd from VpnService.Builder.
 *
 * On STOP: closes the service and tears down the TUN.
 *
 * Why we need a custom VpnService rather than reusing GoBackend's: wireguard's
 * GoBackend is hard-wired to its own wireguard-go binary. sing-box is a totally
 * separate Go runtime — it needs its own VpnService.
 */
class SingBoxService : VpnService(), PlatformInterface {

    companion object {
        private const val TAG = "SingBoxService"
        private const val CHANNEL_ID = "pocketvpn_singbox"
        private const val NOTIFICATION_ID = 4242
        const val ACTION_START = "com.musornibak.pocketvpn.singbox.START"
        const val ACTION_STOP = "com.musornibak.pocketvpn.singbox.STOP"
        const val EXTRA_CONFIG = "config_json"

        // Cross-process state shared with the engine (single VPN active at a time).
        @Volatile var running: Boolean = false
            private set
        @Volatile var lastError: String? = null
            private set
    }

    private var commandServer: CommandServer? = null
    private var tunPfd: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSingBox()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val config = intent?.getStringExtra(EXTRA_CONFIG)
                if (config.isNullOrBlank()) {
                    lastError = "Empty config"
                    stopSelf()
                    return START_NOT_STICKY
                }
                startForeground(NOTIFICATION_ID, buildNotification("Connecting…"))
                try {
                    startSingBox(config)
                    running = true
                    lastError = null
                    updateNotification("Protected")
                } catch (t: Throwable) {
                    Log.e(TAG, "start failed", t)
                    lastError = t.message ?: "sing-box start failed"
                    running = false
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopSingBox()
        super.onDestroy()
    }

    override fun onRevoke() {
        // User revoked VPN permission from system Settings.
        stopSingBox()
        stopSelf()
        super.onRevoke()
    }

    private fun startSingBox(configJson: String) {
        val base = File(filesDir, "sing-box").apply { mkdirs() }
        val work = File(base, "work").apply { mkdirs() }
        val tmp = File(cacheDir, "sing-box").apply { mkdirs() }
        val setupOpts = SetupOptions().apply {
            basePath = base.absolutePath
            workingPath = work.absolutePath
            tempPath = tmp.absolutePath
            fixAndroidStack = true
            commandServerListenPort = 0
            debug = false
        }
        Libbox.setup(setupOpts)

        val server = CommandServer(SilentCommandServerHandler(), this)
        server.start()
        server.startOrReloadService(configJson, OverrideOptions())
        commandServer = server
    }

    private fun stopSingBox() {
        running = false
        runCatching { commandServer?.closeService() }
        runCatching { commandServer?.close() }
        commandServer = null
        runCatching { tunPfd?.close() }
        tunPfd = null
    }

    // --- PlatformInterface ---------------------------------------------------

    override fun openTun(options: TunOptions): Int {
        val builder = Builder()
            .setSession("PocketVPN")
            .setMtu(options.mtu.takeIf { it > 0 } ?: 1500)
            .setBlocking(false)
            .addDnsServer("1.1.1.1")
            .addDnsServer("1.0.0.1")

        // Forward IPv4 addresses + default route.
        val v4 = options.inet4Address
        while (v4 != null && v4.hasNext()) {
            val p = v4.next()
            builder.addAddress(p.address(), p.prefix())
        }
        val v6 = options.inet6Address
        while (v6 != null && v6.hasNext()) {
            val p = v6.next()
            builder.addAddress(p.address(), p.prefix())
        }

        // Routes: default to all-traffic (auto_route in config also handles this).
        val r4 = options.inet4RouteAddress
        if (r4 != null && r4.hasNext()) {
            while (r4.hasNext()) {
                val p = r4.next()
                builder.addRoute(p.address(), p.prefix())
            }
        } else {
            builder.addRoute("0.0.0.0", 0)
        }
        val r6 = options.inet6RouteAddress
        if (r6 != null && r6.hasNext()) {
            while (r6.hasNext()) {
                val p = r6.next()
                builder.addRoute(p.address(), p.prefix())
            }
        } else {
            builder.addRoute("::", 0)
        }

        // Exclude our own app traffic so the proxy connection isn't routed through itself.
        runCatching { builder.addDisallowedApplication(packageName) }

        val pfd = builder.establish()
            ?: throw IllegalStateException("VpnService.Builder.establish() returned null")
        tunPfd = pfd
        return pfd.detachFd()
    }

    override fun autoDetectInterfaceControl(fd: Int) {
        // Mark the outbound socket so it bypasses our own TUN — required, else loop.
        if (!protect(fd)) {
            Log.w(TAG, "protect($fd) returned false")
        }
    }

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
    override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    override fun includeAllNetworks(): Boolean = false
    override fun underNetworkExtension(): Boolean = false

    override fun getInterfaces(): NetworkInterfaceIterator {
        val list = mutableListOf<io.nekohasekai.libbox.NetworkInterface>()
        runCatching {
            java.net.NetworkInterface.getNetworkInterfaces()?.toList()
                ?.filter { !it.isLoopback && it.isUp }
                ?.forEach { ni ->
                    val addrs = ni.interfaceAddresses.map { it.address.hostAddress ?: "" }
                        .filter { it.isNotBlank() }
                    list += io.nekohasekai.libbox.NetworkInterface().apply {
                        index = ni.index
                        mtu = runCatching { ni.mtu }.getOrDefault(1500)
                        name = ni.name
                        addresses = StringArrayIterator(addrs)
                        flags = 0
                        type = Libbox.InterfaceTypeOther
                        dnsServer = StringArrayIterator(emptyList())
                        metered = false
                    }
                }
        }
        return NetworkInterfaceListIterator(list)
    }

    override fun localDNSTransport(): LocalDNSTransport? = null
    override fun systemCertificates(): StringIterator? = null
    override fun readWIFIState(): WIFIState? = null
    override fun clearDNSCache() {}

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        // Push a single notify so sing-box considers the underlying network "live".
        // We deliberately don't track Wi-Fi↔mobile transitions yet; sing-box will
        // re-resolve on the next outbound attempt.
        listener.updateDefaultInterface("any", -1, false, false)
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {}

    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String?,
        sourcePort: Int,
        destinationAddress: String?,
        destinationPort: Int
    ): ConnectionOwner = throw UnsupportedOperationException("not implemented")

    override fun sendNotification(notification: io.nekohasekai.libbox.Notification) {}

    // --- Notification --------------------------------------------------------

    private fun ensureNotificationChannel() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL_ID, "VPN", NotificationManager.IMPORTANCE_LOW).apply {
            description = "PocketVPN sing-box session"
            setShowBadge(false)
        }
        mgr.createNotificationChannel(ch)
    }

    private fun buildNotification(text: String): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("PocketVPN")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    private fun updateNotification(text: String) {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private class SilentCommandServerHandler : CommandServerHandler {
        override fun getSystemProxyStatus(): SystemProxyStatus {
            return SystemProxyStatus().apply {
                available = false
                enabled = false
            }
        }
        override fun serviceReload() {}
        override fun serviceStop() {}
        override fun setSystemProxyEnabled(enabled: Boolean) {}
        override fun writeDebugMessage(message: String?) {
            if (message != null) Log.d(TAG, message)
        }
    }
}
