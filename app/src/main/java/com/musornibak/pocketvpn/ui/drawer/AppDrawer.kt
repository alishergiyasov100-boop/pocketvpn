package com.musornibak.pocketvpn.ui.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musornibak.pocketvpn.ui.main.Backend
import com.musornibak.pocketvpn.ui.main.VpnViewModel

@Composable
fun AppDrawer(vm: VpnViewModel) {
    val killSwitch by vm.killSwitch.collectAsState(initial = false)
    val autoConnect by vm.autoConnect.collectAsState(initial = false)
    val backend by vm.backend.collectAsState()
    val customUrl by vm.customUrl.collectAsState()

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
            "free · no ads · no account",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp
        )

        Spacer(modifier = Modifier.height(20.dp))
        Section("Backend")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = backend == Backend.Warp,
                onClick = { vm.setBackend(Backend.Warp) },
                label = { Text("WARP") }
            )
            FilterChip(
                selected = backend == Backend.Custom,
                onClick = { vm.setBackend(Backend.Custom) },
                label = { Text("Custom (VLESS)") }
            )
        }
        Text(
            "WARP works in most countries.  In Uzbekistan / Iran / Russia it's blocked by DPI — paste your own vless://… URL with Reality and switch to Custom.",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            letterSpacing = 0.2.sp
        )

        if (backend == Backend.Custom) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customUrl,
                onValueChange = { vm.setCustomUrl(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                label = { Text("vless:// URL") },
                placeholder = { Text("vless://uuid@host:443?security=reality&pbk=…") },
                singleLine = false,
                maxLines = 3,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Section("Protection")
        ToggleRow(
            "Kill switch",
            "Block all traffic if VPN drops",
            killSwitch,
            vm::setKillSwitch
        )
        ToggleRow(
            "Auto-connect on untrusted Wi-Fi",
            "Public hotspots, café Wi-Fi",
            autoConnect,
            vm::setAutoConnect
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "v0.4.0 · WireGuard+WARP & sing-box (VLESS / Reality)",
            modifier = Modifier.padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Text(
            "DPI bypass for UZ · 11 WARP regions · in-tunnel speed test",
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
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
