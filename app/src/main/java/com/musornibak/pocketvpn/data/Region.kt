package com.musornibak.pocketvpn.data

data class Region(
    val code: String,
    val name: String,
    val flag: String,
    val subtitle: String = "",
    val endpointHost: String? = null,
    val endpointPort: Int = 2408
) {
    val isAuto: Boolean get() = endpointHost == null
    val endpoint: String? get() = endpointHost?.let { "$it:$endpointPort" }
}

// WARP advertises a single anycast address (engage.cloudflareclient.com:2408)
// but the actual /24s it covers — 162.159.192.0/24, 162.159.193.0/24,
// 188.114.96.0/24 .. 188.114.99.0/24 — and the open UDP ports 2408/500/1701/
// 4500/5279/7559/8854/3476 are advertised in BGP across many PoPs.
// Picking a specific IP+port biases routing toward a particular metro, but
// it is *not* a hard guarantee: Cloudflare can reroute under load.
object Regions {
    val WARP_AUTO = Region(
        code = "auto",
        name = "Auto · Nearest edge",
        flag = "⚡",
        subtitle = "Cloudflare picks the closest PoP"
    )

    val FRANKFURT = Region(
        code = "fra",
        name = "Frankfurt",
        flag = "\uD83C\uDDE9\uD83C\uDDEA",
        subtitle = "Germany · 162.159.192.10:2408",
        endpointHost = "162.159.192.10",
        endpointPort = 2408
    )

    val AMSTERDAM = Region(
        code = "ams",
        name = "Amsterdam",
        flag = "\uD83C\uDDF3\uD83C\uDDF1",
        subtitle = "Netherlands · 162.159.192.40:500",
        endpointHost = "162.159.192.40",
        endpointPort = 500
    )

    val LONDON = Region(
        code = "lhr",
        name = "London",
        flag = "\uD83C\uDDEC\uD83C\uDDE7",
        subtitle = "United Kingdom · 162.159.193.10:1701",
        endpointHost = "162.159.193.10",
        endpointPort = 1701
    )

    val WARSAW = Region(
        code = "waw",
        name = "Warsaw",
        flag = "\uD83C\uDDF5\uD83C\uDDF1",
        subtitle = "Poland · 162.159.193.40:4500",
        endpointHost = "162.159.193.40",
        endpointPort = 4500
    )

    val SINGAPORE = Region(
        code = "sin",
        name = "Singapore",
        flag = "\uD83C\uDDF8\uD83C\uDDEC",
        subtitle = "Singapore · 188.114.96.10:5279",
        endpointHost = "188.114.96.10",
        endpointPort = 5279
    )

    val TOKYO = Region(
        code = "nrt",
        name = "Tokyo",
        flag = "\uD83C\uDDEF\uD83C\uDDF5",
        subtitle = "Japan · 188.114.97.10:7559",
        endpointHost = "188.114.97.10",
        endpointPort = 7559
    )

    val SYDNEY = Region(
        code = "syd",
        name = "Sydney",
        flag = "\uD83C\uDDE6\uD83C\uDDFA",
        subtitle = "Australia · 188.114.97.40:8854",
        endpointHost = "188.114.97.40",
        endpointPort = 8854
    )

    val LOS_ANGELES = Region(
        code = "lax",
        name = "Los Angeles",
        flag = "\uD83C\uDDFA\uD83C\uDDF8",
        subtitle = "United States · 188.114.98.10:2408",
        endpointHost = "188.114.98.10",
        endpointPort = 2408
    )

    val MIAMI = Region(
        code = "mia",
        name = "Miami",
        flag = "\uD83C\uDDFA\uD83C\uDDF8",
        subtitle = "United States · 188.114.98.40:3476",
        endpointHost = "188.114.98.40",
        endpointPort = 3476
    )

    val SAO_PAULO = Region(
        code = "gru",
        name = "São Paulo",
        flag = "\uD83C\uDDE7\uD83C\uDDF7",
        subtitle = "Brazil · 188.114.99.10:1701",
        endpointHost = "188.114.99.10",
        endpointPort = 1701
    )

    val OPTIONS = listOf(
        WARP_AUTO,
        FRANKFURT,
        AMSTERDAM,
        LONDON,
        WARSAW,
        SINGAPORE,
        TOKYO,
        SYDNEY,
        LOS_ANGELES,
        MIAMI,
        SAO_PAULO
    )

    fun byCode(code: String): Region = OPTIONS.firstOrNull { it.code == code } ?: WARP_AUTO
}
