package com.musornibak.pocketvpn.vpn.singbox

import android.content.Context
import android.content.Intent
import com.musornibak.pocketvpn.data.VpnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Lightweight controller for SingBoxService. Sends start/stop intents and
 * polls the service's static state to publish a StateFlow. We poll instead
 * of binding because SingBoxService can survive across process restarts and
 * a bound connection would die first.
 */
class SingBoxEngine(private val context: Context) {

    private val _state = MutableStateFlow(VpnState.Disconnected)
    val state: StateFlow<VpnState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val pollScope = CoroutineScope(Dispatchers.Default)

    init {
        pollScope.launch {
            while (isActive) {
                val running = SingBoxService.running
                val err = SingBoxService.lastError
                val current = _state.value
                _state.value = when {
                    err != null && !running -> VpnState.Error
                    running -> VpnState.Connected
                    current == VpnState.Connecting -> VpnState.Connecting
                    else -> VpnState.Disconnected
                }
                _error.value = err
                _logs.value = SingBoxService.snapshotLogs()
                delay(500)
            }
        }
    }

    fun start(configJson: String) {
        _state.value = VpnState.Connecting
        _error.value = null
        val intent = Intent(context, SingBoxService::class.java).apply {
            action = SingBoxService.ACTION_START
            putExtra(SingBoxService.EXTRA_CONFIG, configJson)
        }
        context.startForegroundService(intent)
    }

    fun stop() {
        val intent = Intent(context, SingBoxService::class.java).apply {
            action = SingBoxService.ACTION_STOP
        }
        runCatching { context.startService(intent) }
        _state.value = VpnState.Disconnected
    }
}
