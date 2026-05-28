package com.musornibak.pocketvpn.vpn

import android.content.Context
import com.musornibak.pocketvpn.data.Region
import com.musornibak.pocketvpn.data.Regions
import com.musornibak.pocketvpn.data.VpnState
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Owns the wireguard-go tunnel. Ensures an anonymous Cloudflare WARP account
 * exists, then brings the tunnel UP/DOWN through GoBackend.
 *
 * v0.3.0 added per-region endpoint override (different WARP IP:port biases
 * routing PoP). The network-change watcher introduced in v0.3.0 was removed
 * in v0.3.1 — it raced with WireGuard's own roaming code and silently broke
 * traffic. WG already handles Wi-Fi↔mobile via persistent-keepalive=25.
 */
class WarpEngine(private val context: Context) {

    private val backend = GoBackend(context.applicationContext)
    private val store = WarpStore(context.applicationContext)
    private val client = WarpClient()

    private val _state = MutableStateFlow(VpnState.Disconnected)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    private val tunnel = object : Tunnel {
        override fun getName() = "pocketvpn"
        override fun onStateChange(newState: Tunnel.State) {
            _state.value = when (newState) {
                Tunnel.State.UP -> VpnState.Connected
                Tunnel.State.DOWN -> VpnState.Disconnected
                Tunnel.State.TOGGLE -> VpnState.Connecting
            }
        }
    }

    private var cachedConfig: Config? = null
    private var currentRegion: Region = Regions.WARP_AUTO

    fun setRegion(region: Region) {
        if (region.code == currentRegion.code) return
        currentRegion = region
        cachedConfig = null
    }

    /** Throws on failure; caller (ViewModel) handles UI Error state. */
    suspend fun connect(region: Region = currentRegion) {
        if (region.code != currentRegion.code) {
            currentRegion = region
            cachedConfig = null
        }
        _state.value = VpnState.Connecting
        val account = ensureAccount()
        val config = cachedConfig ?: buildConfig(account, region).also { cachedConfig = it }
        withContext(Dispatchers.IO) {
            backend.setState(tunnel, Tunnel.State.UP, config)
        }
        _state.value = VpnState.Connected
    }

    fun disconnect() {
        runCatching { backend.setState(tunnel, Tunnel.State.DOWN, null) }
        _state.value = VpnState.Disconnected
    }

    private suspend fun ensureAccount(): WarpAccount =
        store.load() ?: client.register().also {
            store.save(it)
            runCatching { client.enableWarp(it) }
        }

    private fun buildConfig(account: WarpAccount, region: Region): Config {
        val iface = Interface.Builder()
            .parsePrivateKey(account.privateKey)
            .parseAddresses("${account.addressV4}/32, ${account.addressV6}/128")
            .parseDnsServers("1.1.1.1, 1.0.0.1")
            .parseMtu("1280")
            .build()

        val endpointStr = region.endpoint ?: account.endpoint

        val peer = Peer.Builder()
            .setPublicKey(Key.fromBase64(account.peerPublicKey))
            .parseEndpoint(endpointStr)
            .parseAllowedIPs("0.0.0.0/0, ::/0")
            .parsePersistentKeepalive("25")
            .build()

        return Config.Builder()
            .setInterface(iface)
            .addPeer(peer)
            .build()
    }
}
