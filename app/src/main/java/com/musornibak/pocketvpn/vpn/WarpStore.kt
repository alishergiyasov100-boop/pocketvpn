package com.musornibak.pocketvpn.vpn

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.warpStore by preferencesDataStore(name = "warp_account")

class WarpStore(private val context: Context) {
    private val ACCOUNT = stringPreferencesKey("account")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): WarpAccount? = context.warpStore.data
        .map { it[ACCOUNT]?.let { s -> runCatching { json.decodeFromString<WarpAccount>(s) }.getOrNull() } }
        .first()

    suspend fun save(account: WarpAccount) {
        context.warpStore.edit { it[ACCOUNT] = json.encodeToString(WarpAccount.serializer(), account) }
    }

    suspend fun clear() {
        context.warpStore.edit { it.remove(ACCOUNT) }
    }
}
