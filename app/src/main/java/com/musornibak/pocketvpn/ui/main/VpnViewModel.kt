package com.musornibak.pocketvpn.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musornibak.pocketvpn.data.Region
import com.musornibak.pocketvpn.data.Regions
import com.musornibak.pocketvpn.data.SettingsRepo
import com.musornibak.pocketvpn.data.VpnState
import com.musornibak.pocketvpn.tor.TorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VpnViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepo(app)
    private val tor = TorEngine(app)

    private val _state = MutableStateFlow(VpnState.Disconnected)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    private val _bootstrap = MutableStateFlow(0)
    val bootstrap: StateFlow<Int> = _bootstrap.asStateFlow()

    private val _region = MutableStateFlow(Regions.AUTO)
    val region: StateFlow<Region> = _region.asStateFlow()

    val killSwitch = settings.killSwitch
    val autoConnect = settings.autoConnect
    val conflux = settings.conflux
    val persistent = settings.persistent
    val bridgeMode = settings.bridgeMode
    val bridgeWifiOnly = settings.bridgeWifiOnly
    val bridgeBatteryGuard = settings.bridgeBatteryGuard

    init {
        viewModelScope.launch {
            val code = settings.regionCode.first()
            _region.value = resolveRegion(code)
            tor.conflux = settings.conflux.first()
            tor.persistent = settings.persistent.first()
        }
        viewModelScope.launch {
            tor.bootstrapPercent.collect { _bootstrap.value = it }
        }
    }

    fun toggle() {
        when (_state.value) {
            VpnState.Disconnected, VpnState.Error -> connect()
            VpnState.Connecting -> disconnect()
            VpnState.Connected -> disconnect()
        }
    }

    private fun connect() {
        _state.value = VpnState.Connecting
        viewModelScope.launch {
            tor.start(_region.value)
            _state.value = VpnState.Connected
        }
    }

    private fun disconnect() {
        tor.stop()
        _state.value = VpnState.Disconnected
        _bootstrap.value = 0
    }

    fun selectRegion(region: Region) {
        _region.value = region
        viewModelScope.launch { settings.setRegion(region.code) }
        if (_state.value == VpnState.Connected) {
            // restart with new pin
            disconnect()
            connect()
        }
    }

    fun setConflux(v: Boolean) {
        tor.conflux = v
        viewModelScope.launch { settings.setConflux(v) }
    }

    fun setPersistent(v: Boolean) {
        tor.persistent = v
        viewModelScope.launch { settings.setPersistent(v) }
    }

    fun setKillSwitch(v: Boolean) = viewModelScope.launch { settings.setKillSwitch(v) }
    fun setAutoConnect(v: Boolean) = viewModelScope.launch { settings.setAutoConnect(v) }
    fun setBridge(v: Boolean) = viewModelScope.launch { settings.setBridge(v) }
    fun setBridgeWifiOnly(v: Boolean) = viewModelScope.launch { settings.setBridgeWifiOnly(v) }
    fun setBridgeBatteryGuard(v: Boolean) = viewModelScope.launch { settings.setBridgeBatteryGuard(v) }

    private fun resolveRegion(code: String): Region =
        Regions.PRESETS.firstOrNull { it.code == code }
            ?: Regions.COUNTRIES.firstOrNull { it.code == code }
            ?: Regions.AUTO
}
