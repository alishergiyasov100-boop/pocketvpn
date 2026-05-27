package com.musornibak.pocketvpn.vpn

import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Cloudflare WARP anonymous registration. Mirrors the protocol used by `wgcf`
 * (https://github.com/ViRb3/wgcf): POST a fresh WireGuard public key, get back
 * the assigned tunnel IPs + peer endpoint, then PATCH to enable warp mode.
 *
 * No account, no email, no card.
 */
class WarpClient {

    private val http = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun register(): WarpAccount = withContext(Dispatchers.IO) {
        val kp = KeyPair()
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

        val body = """
            {"key":"${kp.publicKey.toBase64()}","install_id":"","fcm_token":"","tos":"$timestamp","type":"Android","model":"PC","locale":"en_US"}
        """.trimIndent()

        val req = Request.Builder()
            .url("https://api.cloudflareclient.com/v0a2158/reg")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "okhttp/3.12.1")
            .addHeader("CF-Client-Version", "a-6.30-3596")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(req).execute().use {
            if (!it.isSuccessful) error("WARP /reg failed: ${it.code}")
            val text = it.body?.string().orEmpty()
            val root = json.parseToJsonElement(text).jsonObject
            val id = root["id"]!!.jsonPrimitive.content
            val token = root["token"]!!.jsonPrimitive.content
            val config = root["config"]!!.jsonObject
            val iface = config["interface"]!!.jsonObject["addresses"]!!.jsonObject
            val v4 = iface["v4"]!!.jsonPrimitive.content
            val v6 = iface["v6"]!!.jsonPrimitive.content
            val peer = config["peers"]!!.jsonArray[0].jsonObject
            val peerPubKey = peer["public_key"]!!.jsonPrimitive.content
            val endpoint = peer["endpoint"]!!.jsonObject
            val host = endpoint["host"]!!.jsonPrimitive.content

            WarpAccount(
                id = id,
                token = token,
                privateKey = kp.privateKey.toBase64(),
                publicKey = kp.publicKey.toBase64(),
                peerPublicKey = peerPubKey,
                endpoint = host,
                addressV4 = v4,
                addressV6 = v6
            )
        }
    }

    /** Enable warp mode on the freshly registered account. */
    suspend fun enableWarp(account: WarpAccount) = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.cloudflareclient.com/v0a2158/reg/${account.id}")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "okhttp/3.12.1")
            .addHeader("CF-Client-Version", "a-6.30-3596")
            .addHeader("Authorization", "Bearer ${account.token}")
            .patch("""{"warp_enabled":true}""".toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { /* best-effort */ }
        Unit
    }
}

@Serializable
data class WarpAccount(
    val id: String,
    val token: String,
    val privateKey: String,
    val publicKey: String,
    val peerPublicKey: String,
    val endpoint: String,
    val addressV4: String,
    val addressV6: String
)
