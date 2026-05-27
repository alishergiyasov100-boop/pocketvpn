package com.musornibak.pocketvpn.ui.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musornibak.pocketvpn.ui.main.VpnViewModel

@Composable
fun AppDrawer(vm: VpnViewModel) {
    val killSwitch by vm.killSwitch.collectAsState(initial = false)
    val autoConnect by vm.autoConnect.collectAsState(initial = false)
    val conflux by vm.conflux.collectAsState(initial = true)
    val persistent by vm.persistent.collectAsState(initial = true)
    val bridge by vm.bridgeMode.collectAsState(initial = false)
    val bridgeWifi by vm.bridgeWifiOnly.collectAsState(initial = true)
    val bridgeBattery by vm.bridgeBatteryGuard.collectAsState(initial = true)

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp, bottom = 24.dp)
    ) {
        Text(
            "PocketVPN",
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp
        )
        Text(
            "free · no ads · no servers",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp
        )

        Spacer(modifier = Modifier.height(20.dp))
        Section("Protection")
        ToggleRow("Kill switch", "Block all traffic if Tor drops", killSwitch, vm::setKillSwitch)
        ToggleRow("Auto-connect on untrusted Wi-Fi", "Public hotspots, cafe Wi-Fi", autoConnect, vm::setAutoConnect)

        Spacer(modifier = Modifier.height(12.dp))
        Section("Speed")
        ToggleRow("Conflux (multi-path)", "Split traffic across 2 circuits (~2× faster)", conflux, vm::setConflux)
        ToggleRow("Keep circuits warm", "Lower latency on new connections", persistent, vm::setPersistent)

        Spacer(modifier = Modifier.height(12.dp))
        Section("Bridge mode")
        Text(
            "Let your phone help users behind firewalls. Anonymous, low-impact, runs in the background.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
        ToggleRow("Be a bridge", "Snowflake proxy for censored users", bridge, vm::setBridge)
        ToggleRow("Only over Wi-Fi", "Don't use cellular data", bridgeWifi, vm::setBridgeWifiOnly)
        ToggleRow("Pause below 30% battery", "Auto-stop on low charge", bridgeBattery, vm::setBridgeBatteryGuard)

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "v0.1.0 · built on Tor",
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun Section(label: String) {
    Text(
        label.uppercase(),
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ToggleRow(
    title: String,
    sub: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
            Text(
                sub,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
