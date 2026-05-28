package com.musornibak.pocketvpn.ui.main

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musornibak.pocketvpn.data.VpnState
import com.musornibak.pocketvpn.ui.drawer.AppDrawer
import com.musornibak.pocketvpn.ui.region.RegionPicker
import kotlinx.coroutines.launch

@Composable
fun MainScreen(vm: VpnViewModel) {
    val state by vm.state.collectAsState()
    val region by vm.region.collectAsState()
    val bootstrap by vm.bootstrap.collectAsState()
    val speed by vm.speedMbps.collectAsState()
    val speedTesting by vm.speedTesting.collectAsState()
    val backend by vm.backend.collectAsState()
    val singBoxLogs by vm.singBoxLogs.collectAsState()
    val error by vm.error.collectAsState()
    val customUrl by vm.customUrl.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showRegionPicker by remember { mutableStateOf(false) }

    val ctx = LocalContext.current
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            vm.toggle()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                AppDrawer(vm)
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopBar(
                    onMenu = { scope.launch { drawerState.open() } }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PowerButton(
                            state = state,
                            bootstrapPercent = bootstrap,
                            onTap = {
                                if (state == VpnState.Disconnected || state == VpnState.Error) {
                                    val intent = VpnService.prepare(ctx)
                                    if (intent != null) vpnPermissionLauncher.launch(intent)
                                    else vm.toggle()
                                } else {
                                    vm.toggle()
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        StateLabel(state = state, bootstrap = bootstrap)
                        if (backend == Backend.Custom && customUrl.isBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Open drawer → paste vless:// URL",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                        if (backend == Backend.Custom && error != null && state != VpnState.Connected) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = error.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                        if (backend == Backend.Custom &&
                            state != VpnState.Connected &&
                            singBoxLogs.isNotEmpty()
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LogPanel(lines = singBoxLogs)
                        }
                        if (state == VpnState.Connected) {
                            Spacer(modifier = Modifier.height(20.dp))
                            SpeedLabel(
                                mbps = speed,
                                running = speedTesting,
                                onTap = { vm.runSpeedTest() }
                            )
                        }
                    }
                }

                if (backend == Backend.Warp) {
                    RegionPill(
                        flag = region.flag,
                        name = region.name,
                        onClick = { showRegionPicker = true }
                    )
                } else {
                    RegionPill(
                        flag = "\uD83D\uDD12",
                        name = "Custom (VLESS / Reality)",
                        onClick = { /* drawer is where the URL is pasted */ }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showRegionPicker) {
        RegionPicker(
            selected = region,
            onDismiss = { showRegionPicker = false },
            onPick = {
                vm.selectRegion(it)
                showRegionPicker = false
            }
        )
    }
}

@Composable
private fun TopBar(onMenu: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "PocketVPN",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun StateLabel(state: VpnState, bootstrap: Int) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (fadeIn(tween(350)) togetherWith fadeOut(tween(250)))
        },
        label = "state-label"
    ) { s ->
        val text = when (s) {
            VpnState.Disconnected -> "Tap to connect"
            VpnState.Connecting -> if (bootstrap in 1..99) "Building circuit · $bootstrap%" else "Building circuit…"
            VpnState.Connected -> "Protected"
            VpnState.Error -> "Connection failed"
        }
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground.copy(
                alpha = if (s == VpnState.Disconnected) 0.7f else 1f
            ),
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SpeedLabel(mbps: Double?, running: Boolean, onTap: () -> Unit) {
    val text = when {
        running -> "Measuring…"
        mbps != null -> "↓ ${"%.1f".format(mbps)} Mbps"
        else -> "Tap to test speed"
    }
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.3.sp
    )
}

@Composable
private fun LogPanel(lines: List<String>) {
    val scrollState = rememberScrollState()
    val tail = lines.takeLast(20)
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 360.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp)
            .heightIn(max = 180.dp)
    ) {
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            tail.forEach { line ->
                Text(
                    text = line,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun RegionPill(flag: String, name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(flag, fontSize = 20.sp)
        Text(
            name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
