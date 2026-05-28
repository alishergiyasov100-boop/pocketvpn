package com.musornibak.pocketvpn.vpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.musornibak.pocketvpn.data.Region
import com.musornibak.pocketvpn.data.Regions
import com.musornibak.pocketvpn.data.VpnState
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the wireguard-go tunnel. Ensures an anonymous Cloudflare WARP account
 * exists, then brings the tunnel UP/DOWN through GoBackend.
 *
 * v0.3.0 adds:
 *   - per-region endpoint override (different WARP IP:port biases routing PoP)
 *   - underlying-network change detection → automatic tunnel rebuild
 */
class WarpEngine(private val context: Context) {

    private val backend = GoBackend(context.applicationContext)
    private val store = WarpStore(context.applicationContext)
    private val client = WarpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
    private var lastUnderlyingNetworkId: Long = -1L
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

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
        registerNetworkWatcher()
        _state.value = VpnState.Connected
    }

    fun disconnect() {
        unregisterNetworkWatcher()
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

    private fun registerNetworkWatcher() {
        if (networkCallback != null) return
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val id = network.networkHandle
                if (lastUnderlyingNetworkId != -1L && id != lastUnderlyingNetworkId) {
                    scope.launch { rebuild() }
                }
                lastUnderlyingNetworkId = id
            }

            override fun onLost(network: Network) {
                if (network.networkHandle == lastUnderlyingNetworkId) {
                    lastUnderlyingNetworkId = -1L
                }
            }
        }
        runCatching { cm.registerNetworkCallback(request, cb) }
            .onSuccess { networkCallback = cb }
    }

    private fun unregisterNetworkWatcher() {
        val cb = networkCallback ?: return
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        runCatching { cm.unregisterNetworkCallback(cb) }
        networkCallback = null
        lastUnderlyingNetworkId = -1L
    }

    private suspend fun rebuild() {
        if (_state.value != VpnState.Connected) return
        runCatching {
            backend.setState(tunnel, Tunnel.State.DOWN, null)
            val account = ensureAccount()
            val config = cachedConfig ?: buildConfig(account, currentRegion).also { cachedConfig = it }
            backend.setState(tunnel, Tunnel.State.UP, config)
        }
    }
}
