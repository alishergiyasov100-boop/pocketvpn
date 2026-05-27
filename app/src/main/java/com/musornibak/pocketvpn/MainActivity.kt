package com.musornibak.pocketvpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.musornibak.pocketvpn.ui.main.MainScreen
import com.musornibak.pocketvpn.ui.main.VpnViewModel
import com.musornibak.pocketvpn.ui.theme.PocketVpnTheme

class MainActivity : ComponentActivity() {

    private val vpnViewModel: VpnViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PocketVpnTheme {
                Surface(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    MainScreen(vm = vpnViewModel)
                }
            }
        }
    }
}
