package com.musornibak.pocketvpn.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musornibak.pocketvpn.data.Region
import com.musornibak.pocketvpn.data.Regions
import com.musornibak.pocketvpn.data.SettingsRepo
import com.musornibak.pocketvpn.data.VpnState
import com.musornibak.pocketvpn.vpn.SpeedTester
import com.musornibak.pocketvpn.vpn.WarpEngine
import com.musornibak.pocketvpn.vpn.singbox.SingBoxEngine
import com.musornibak.pocketvpn.vpn.singbox.VlessParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class Backend { Warp, Custom }

class VpnViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepo(app)
    private val warpEngine = WarpEngine(app)
    private val singBoxEngine = SingBoxEngine(app)
    private val speedTester = SpeedTester()

    private val _backend = MutableStateFlow(Backend.Warp)
    val backend: StateFlow<Backend> = _backend.asStateFlow()

    val state: StateFlow<VpnState> = combine(
        warpEngine.state, singBoxEngine.state, _backend
    ) { warp, sb, b ->
        if (b == Backend.Custom) sb else warp
    }.let { flow ->
        val mf = MutableStateFlow(VpnState.Disconnected)
        viewModelScope.launch { flow.collect { mf.value = it } }
        mf.asStateFlow()
    }

    private val _bootstrap = MutableStateFlow(0)
    val bootstrap: StateFlow<Int> = _bootstrap.asStateFlow()

    private val _region = MutableStateFlow(Regions.WARP_AUTO)
    val region: StateFlow<Region> = _region.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _speedMbps = MutableStateFlow<Double?>(null)
    val speedMbps: StateFlow<Double?> = _speedMbps.asStateFlow()

    private val _speedTesting = MutableStateFlow(false)
    val speedTesting: StateFlow<Boolean> = _speedTesting.asStateFlow()

    private val _customUrl = MutableStateFlow("")
    val customUrl: StateFlow<String> = _customUrl.asStateFlow()

    val singBoxLogs: StateFlow<List<String>> = singBoxEngine.logs

    val killSwitch = settings.killSwitch
    val autoConnect = settings.autoConnect

    init {
        viewModelScope.launch {
            settings.regionCode.collectLatest { code ->
                val r = Regions.byCode(code)
                _region.value = r
                warpEngine.setRegion(r)
            }
        }
        viewModelScope.launch {
            settings.backend.collectLatest { b ->
                _backend.value = if (b == "custom") Backend.Custom else Backend.Warp
            }
        }
        viewModelScope.launch {
            settings.customUrl.collectLatest { _customUrl.value = it }
        }
        viewModelScope.launch {
            singBoxEngine.error.collectLatest { if (it != null) _error.value = it }
        }
    }

    fun toggle() {
        when (state.value) {
            VpnState.Disconnected, VpnState.Error -> connect()
            VpnState.Connecting -> disconnect()
            VpnState.Connected -> disconnect()
        }
    }

    private fun connect() {
        viewModelScope.launch {
            _bootstrap.value = 10
            try {
                _bootstrap.value = 35
                when (_backend.value) {
                    Backend.Warp -> warpEngine.connect(_region.value)
                    Backend.Custom -> {
                        val url = _customUrl.value.trim()
                        if (url.isEmpty()) throw IllegalStateException("Paste a vless:// URL first")
                        val cfg = VlessParser.toSingBoxConfig(url)
                        singBoxEngine.start(cfg)
                    }
                }
                _bootstrap.value = 100
                _error.value = null
            } catch (t: Throwable) {
                _error.value = t.message ?: "Connection failed"
                _bootstrap.value = 0
            }
        }
    }

    private fun disconnect() {
        when (_backend.value) {
            Backend.Warp -> warpEngine.disconnect()
            Backend.Custom -> singBoxEngine.stop()
        }
        _bootstrap.value = 0
        _speedMbps.value = null
    }

    fun selectRegion(region: Region) {
        _region.value = region
        warpEngine.setRegion(region)
        _speedMbps.value = null
        viewModelScope.launch { settings.setRegion(region.code) }
    }

    fun setBackend(b: Backend) {
        viewModelScope.launch {
            disconnect()
            settings.setBackend(if (b == Backend.Custom) "custom" else "warp")
        }
    }

    fun setCustomUrl(url: String) {
        _customUrl.value = url
        viewModelScope.launch { settings.setCustomUrl(url) }
    }

    fun runSpeedTest() {
        if (_speedTesting.value) return
        if (state.value != VpnState.Connected) return
        viewModelScope.launch {
            _speedTesting.value = true
            try {
                val mbps = speedTester.measureMbps()
                _speedMbps.value = if (mbps > 0) mbps else null
            } finally {
                _speedTesting.value = false
            }
        }
    }

    fun setKillSwitch(v: Boolean) = viewModelScope.launch { settings.setKillSwitch(v) }
    fun setAutoConnect(v: Boolean) = viewModelScope.launch { settings.setAutoConnect(v) }
}
