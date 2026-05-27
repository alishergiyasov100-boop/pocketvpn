package com.musornibak.pocketvpn.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pocketvpn_prefs")

class SettingsRepo(private val context: Context) {
    private val REGION = stringPreferencesKey("region")
    private val CONFLUX = booleanPreferencesKey("conflux")
    private val PERSISTENT = booleanPreferencesKey("persistent_circuits")
    private val KILL_SWITCH = booleanPreferencesKey("kill_switch")
    private val AUTO_CONNECT = booleanPreferencesKey("auto_connect_untrusted")
    private val BRIDGE = booleanPreferencesKey("bridge_mode")
    private val BRIDGE_WIFI_ONLY = booleanPreferencesKey("bridge_wifi_only")
    private val BRIDGE_BATTERY_GUARD = booleanPreferencesKey("bridge_battery_guard")

    val regionCode: Flow<String> = context.dataStore.data.map { it[REGION] ?: "auto" }
    val conflux: Flow<Boolean> = context.dataStore.data.map { it[CONFLUX] ?: true }
    val persistent: Flow<Boolean> = context.dataStore.data.map { it[PERSISTENT] ?: true }
    val killSwitch: Flow<Boolean> = context.dataStore.data.map { it[KILL_SWITCH] ?: false }
    val autoConnect: Flow<Boolean> = context.dataStore.data.map { it[AUTO_CONNECT] ?: false }
    val bridgeMode: Flow<Boolean> = context.dataStore.data.map { it[BRIDGE] ?: false }
    val bridgeWifiOnly: Flow<Boolean> = context.dataStore.data.map { it[BRIDGE_WIFI_ONLY] ?: true }
    val bridgeBatteryGuard: Flow<Boolean> = context.dataStore.data.map { it[BRIDGE_BATTERY_GUARD] ?: true }

    suspend fun setRegion(code: String) = context.dataStore.edit { it[REGION] = code }
    suspend fun setConflux(v: Boolean) = context.dataStore.edit { it[CONFLUX] = v }
    suspend fun setPersistent(v: Boolean) = context.dataStore.edit { it[PERSISTENT] = v }
    suspend fun setKillSwitch(v: Boolean) = context.dataStore.edit { it[KILL_SWITCH] = v }
    suspend fun setAutoConnect(v: Boolean) = context.dataStore.edit { it[AUTO_CONNECT] = v }
    suspend fun setBridge(v: Boolean) = context.dataStore.edit { it[BRIDGE] = v }
    suspend fun setBridgeWifiOnly(v: Boolean) = context.dataStore.edit { it[BRIDGE_WIFI_ONLY] = v }
    suspend fun setBridgeBatteryGuard(v: Boolean) = context.dataStore.edit { it[BRIDGE_BATTERY_GUARD] = v }
}
