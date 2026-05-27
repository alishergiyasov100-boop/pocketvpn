package com.musornibak.pocketvpn.data

data class Region(
    val code: String,
    val name: String,
    val flag: String,
    val subtitle: String = ""
)

object Regions {
    val WARP_AUTO = Region(
        code = "warp",
        name = "Cloudflare WARP",
        flag = "⚡",
        subtitle = "Auto · routes via nearest edge"
    )

    val OPTIONS = listOf(WARP_AUTO)
}
