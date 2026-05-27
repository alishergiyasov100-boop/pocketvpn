package com.musornibak.pocketvpn.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musornibak.pocketvpn.data.Region
import com.musornibak.pocketvpn.data.Regions
import com.musornibak.pocketvpn.data.SettingsRepo
import com.musornibak.pocketvpn.data.VpnState
import com.musornibak.pocketvpn.vpn.WarpEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VpnViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepo(app)
    private val engine = WarpEngine(app)

    val state: StateFlow<VpnState> = engine.state

    private val _bootstrap = MutableStateFlow(0)
    val bootstrap: StateFlow<Int> = _bootstrap.asStateFlow()

    private val _region = MutableStateFlow(Regions.WARP_AUTO)
    val region: StateFlow<Region> = _region.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val killSwitch = settings.killSwitch
    val autoConnect = settings.autoConnect

    fun toggle() {
        when (state.value) {
            VpnState.Disconnected, VpnState.Error -> connect()
            VpnState.Connecting -> disconnect()
            VpnState.Connected -> disconnect()
        }
    }

    private fun connect() {
        viewModelScope.launch {
            // crude progress hint (WARP handshake is sub-second; this just animates the arc)
            _bootstrap.value = 10
            try {
                _bootstrap.value = 35
                engine.connect()
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
    }

    fun selectRegion(region: Region) {
        _region.value = region
    }

    fun setKillSwitch(v: Boolean) = viewModelScope.launch { settings.setKillSwitch(v) }
    fun setAutoConnect(v: Boolean) = viewModelScope.launch { settings.setAutoConnect(v) }
}
