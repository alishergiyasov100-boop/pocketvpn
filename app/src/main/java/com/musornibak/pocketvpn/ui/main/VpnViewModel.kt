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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VpnViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepo(app)
    private val engine = WarpEngine(app)
    private val speedTester = SpeedTester()

    val state: StateFlow<VpnState> = engine.state

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

    val killSwitch = settings.killSwitch
    val autoConnect = settings.autoConnect

    init {
        viewModelScope.launch {
            settings.regionCode.collectLatest { code ->
                val r = Regions.byCode(code)
                _region.value = r
                engine.setRegion(r)
            }
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
                engine.connect(_region.value)
                _bootstrap.value = 100
                _error.value = null
            } catch (t: Throwable) {
                _error.value = t.message ?: "Connection failed"
                _bootstrap.value = 0
            }
        }
    }

    private fun disconnect() {
        engine.disconnect()
        _bootstrap.value = 0
        _speedMbps.value = null
    }

    fun selectRegion(region: Region) {
        _region.value = region
        engine.setRegion(region)
        _speedMbps.value = null
        viewModelScope.launch { settings.setRegion(region.code) }
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
