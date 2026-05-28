package com.musornibak.pocketvpn.vpn.singbox

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

/**
 * Parses `vless://uuid@host:port?security=reality&pbk=...&sid=...&sni=...&fp=chrome#name`
 * into a sing-box JSON config string with a `tun` inbound + `vless` outbound.
 *
 * Supports Reality (the only TLS variant that reliably bypasses UZ DPI, since
 * it mimics handshakes to legitimate sites). TLS-only and Vision-only fallbacks
 * are also handled.
 */
object VlessParser {

    data class VlessParams(
        val uuid: String,
        val host: String,
        val port: Int,
        val name: String,
        val security: String,   // "reality" | "tls" | "none"
        val sni: String?,
        val publicKey: String?, // Reality public key (pbk)
        val shortId: String?,   // Reality short id (sid)
        val fingerprint: String?,
        val flow: String?,
        val transport: String   // "tcp" | "ws" | "grpc"
    )

    fun parse(url: String): VlessParams {
        require(url.startsWith("vless://", ignoreCase = true)) {
            "URL must start with vless://"
        }
        val stripped = url.substring(8)

        val (rest, nameRaw) = stripped.split("#", limit = 2).let {
            if (it.size == 2) it[0] to URLDecoder.decode(it[1], "UTF-8")
            else it[0] to "Custom"
        }
        val (authPart, queryPart) = rest.split("?", limit = 2).let {
            if (it.size == 2) it[0] to it[1] else it[0] to ""
        }

        val atIdx = authPart.lastIndexOf('@')
        require(atIdx > 0) { "vless URL missing @ separator" }
        val uuid = authPart.substring(0, atIdx)
        val hostPort = authPart.substring(atIdx + 1)
        val colonIdx = hostPort.lastIndexOf(':')
        require(colonIdx > 0) { "vless URL missing :port" }
        val host = hostPort.substring(0, colonIdx).trim('[', ']')
        val port = hostPort.substring(colonIdx + 1).toInt()

        val q = mutableMapOf<String, String>()
        if (queryPart.isNotEmpty()) {
            queryPart.split("&").forEach { kv ->
                val eq = kv.indexOf('=')
                if (eq > 0) {
                    q[kv.substring(0, eq)] = URLDecoder.decode(kv.substring(eq + 1), "UTF-8")
                }
            }
        }

        return VlessParams(
            uuid = uuid,
            host = host,
            port = port,
            name = nameRaw,
            security = q["security"] ?: "none",
            sni = q["sni"] ?: q["host"],
            publicKey = q["pbk"],
            shortId = q["sid"],
            fingerprint = q["fp"] ?: "chrome",
            flow = q["flow"],
            transport = q["type"] ?: "tcp"
        )
    }

    /**
     * Builds a sing-box JSON config for the given VLESS URL.
     * TUN inbound is provided by the platform interface (openTun callback).
     */
    fun toSingBoxConfig(url: String): String {
        val p = parse(url)

        val vless = JSONObject().apply {
            put("type", "vless")
            put("tag", "proxy")
            put("server", p.host)
            put("server_port", p.port)
            put("uuid", p.uuid)
            p.flow?.let { if (it.isNotBlank()) put("flow", it) }
            put("packet_encoding", "xudp")

            if (p.security == "reality" || p.security == "tls") {
                val tls = JSONObject().apply {
                    put("enabled", true)
                    put("server_name", p.sni ?: p.host)
                    put("utls", JSONObject().apply {
                        put("enabled", true)
                        put("fingerprint", p.fingerprint ?: "chrome")
                    })
                    if (p.security == "reality") {
                        put("reality", JSONObject().apply {
                            put("enabled", true)
                            put("public_key", p.publicKey ?: "")
                            put("short_id", p.shortId ?: "")
                        })
                    }
                }
                put("tls", tls)
            }

            if (p.transport == "ws") {
                put("transport", JSONObject().apply { put("type", "ws") })
            } else if (p.transport == "grpc") {
                put("transport", JSONObject().apply {
                    put("type", "grpc")
                    put("service_name", "grpc")
                })
            }
        }

        val tunIn = JSONObject().apply {
            put("type", "tun")
            put("tag", "tun-in")
            put("interface_name", "tun0")
            put("address", JSONArray(listOf("172.19.0.1/30", "fdfe:dcba:9876::1/126")))
            put("mtu", 1500)
            put("auto_route", true)
            put("strict_route", false)
            put("stack", "system")
            put("sniff", true)
        }

        val dns = JSONObject().apply {
            put("servers", JSONArray(listOf(
                JSONObject().apply {
                    put("tag", "remote")
                    put("address", "tls://1.1.1.1")
                    put("detour", "proxy")
                },
                JSONObject().apply {
                    put("tag", "local")
                    put("address", "8.8.8.8")
                    put("detour", "direct")
                }
            )))
            put("rules", JSONArray(listOf(
                JSONObject().apply { put("outbound", "any"); put("server", "local") }
            )))
            put("strategy", "prefer_ipv4")
            put("independent_cache", true)
        }

        val outbounds = JSONArray(listOf(
            vless,
            JSONObject().apply { put("type", "direct"); put("tag", "direct") },
            JSONObject().apply { put("type", "block"); put("tag", "block") },
            JSONObject().apply { put("type", "dns"); put("tag", "dns-out") }
        ))

        val route = JSONObject().apply {
            put("rules", JSONArray(listOf(
                JSONObject().apply { put("protocol", "dns"); put("outbound", "dns-out") },
                JSONObject().apply { put("ip_is_private", true); put("outbound", "direct") }
            )))
            put("auto_detect_interface", true)
            put("final", "proxy")
        }

        return JSONObject().apply {
            put("log", JSONObject().apply { put("level", "warn") })
            put("dns", dns)
            put("inbounds", JSONArray(listOf(tunIn)))
            put("outbounds", outbounds)
            put("route", route)
        }.toString()
    }
}
