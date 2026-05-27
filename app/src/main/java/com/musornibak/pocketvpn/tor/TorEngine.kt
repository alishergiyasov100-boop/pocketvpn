package com.musornibak.pocketvpn.tor

import android.content.Context
import com.musornibak.pocketvpn.data.Region
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wraps the embedded tor process. v0.1.0 is a stub that simulates bootstrap timing
 * so the UI state machine can be exercised. Real implementation: bundle
 * `info.guardianproject:tor-android` + libtor.so + write torrc with
 *   ConfluxEnabled 1
 *   ExitNodes {<code>}
 *   StrictNodes 1
 *   MaxCircuitDirtiness 600
 *   NumEntryGuards 3
 * then start via ProcessBuilder and listen on the control port for BOOTSTRAP events.
 */
class TorEngine(private val context: Context) {

    private val _bootstrapPercent = MutableStateFlow(0)
    val bootstrapPercent: StateFlow<Int> = _bootstrapPercent

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    var conflux: Boolean = true
    var persistent: Boolean = true

    suspend fun start(region: Region) {
        _running.value = true
        _bootstrapPercent.value = 0
        // Simulated bootstrap. Real tor reports BOOTSTRAP events on control port.
        for (p in listOf(5, 15, 30, 50, 75, 90, 100)) {
            delay(450)
            _bootstrapPercent.value = p
        }
    }

    fun stop() {
        _running.value = false
        _bootstrapPercent.value = 0
    }

    /**
     * Returns the torrc that *would* be written for the given region/settings.
     * Useful for debug & for the about screen ("show torrc").
     */
    fun buildTorrc(region: Region): String = buildString {
        appendLine("# pocketvpn torrc")
        appendLine("SocksPort 127.0.0.1:9050")
        appendLine("ControlPort 127.0.0.1:9051")
        appendLine("CookieAuthentication 1")
        if (conflux) appendLine("ConfluxEnabled 1")
        if (persistent) {
            appendLine("MaxCircuitDirtiness 600")
            appendLine("NumEntryGuards 3")
        }
        when (region.code) {
            "auto" -> { /* no pinning */ }
            "fast_eu" -> {
                appendLine("ExitNodes {de},{nl},{se}")
                appendLine("StrictNodes 1")
            }
            "avoid_14" -> {
                appendLine("ExcludeExitNodes {us},{gb},{ca},{au},{nz},{dk},{fr},{nl},{no},{be},{de},{it},{es},{se}")
            }
            else -> {
                appendLine("ExitNodes {${region.code}}")
                appendLine("StrictNodes 1")
            }
        }
    }
}
