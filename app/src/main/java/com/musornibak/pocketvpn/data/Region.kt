package com.musornibak.pocketvpn.data

data class Region(
    val code: String,
    val name: String,
    val flag: String,
    val approxRelays: Int = 0,
    val isPreset: Boolean = false,
    val isFast: Boolean = false
)

object Regions {
    val AUTO = Region("auto", "Auto", "🌐", isPreset = true)
    val FAST_EU = Region("fast_eu", "Fast Europe", "⚡", isPreset = true, isFast = true)
    val AVOID_14EYES = Region("avoid_14", "Outside 14-eyes", "🛡", isPreset = true)

    val PRESETS = listOf(AUTO, FAST_EU, AVOID_14EYES)

    val COUNTRIES = listOf(
        Region("de", "Germany",       "🇩🇪", approxRelays = 1850, isFast = true),
        Region("nl", "Netherlands",   "🇳🇱", approxRelays = 870,  isFast = true),
        Region("se", "Sweden",        "🇸🇪", approxRelays = 230,  isFast = true),
        Region("fr", "France",        "🇫🇷", approxRelays = 760),
        Region("us", "United States", "🇺🇸", approxRelays = 1900),
        Region("ch", "Switzerland",   "🇨🇭", approxRelays = 320),
        Region("at", "Austria",       "🇦🇹", approxRelays = 180),
        Region("fi", "Finland",       "🇫🇮", approxRelays = 110),
        Region("no", "Norway",        "🇳🇴", approxRelays = 95),
        Region("dk", "Denmark",       "🇩🇰", approxRelays = 70),
        Region("ee", "Estonia",       "🇪🇪", approxRelays = 35),
        Region("is", "Iceland",       "🇮🇸", approxRelays = 25),
        Region("ca", "Canada",        "🇨🇦", approxRelays = 410),
        Region("gb", "United Kingdom","🇬🇧", approxRelays = 720),
        Region("ie", "Ireland",       "🇮🇪", approxRelays = 60),
        Region("es", "Spain",         "🇪🇸", approxRelays = 290),
        Region("pt", "Portugal",      "🇵🇹", approxRelays = 80),
        Region("it", "Italy",         "🇮🇹", approxRelays = 220),
        Region("pl", "Poland",        "🇵🇱", approxRelays = 240),
        Region("cz", "Czechia",       "🇨🇿", approxRelays = 130),
        Region("ro", "Romania",       "🇷🇴", approxRelays = 270),
        Region("bg", "Bulgaria",      "🇧🇬", approxRelays = 60),
        Region("ua", "Ukraine",       "🇺🇦", approxRelays = 70),
        Region("md", "Moldova",       "🇲🇩", approxRelays = 40),
        Region("lv", "Latvia",        "🇱🇻", approxRelays = 30),
        Region("lt", "Lithuania",     "🇱🇹", approxRelays = 30),
        Region("jp", "Japan",         "🇯🇵", approxRelays = 110),
        Region("sg", "Singapore",     "🇸🇬", approxRelays = 80),
        Region("hk", "Hong Kong",     "🇭🇰", approxRelays = 50),
        Region("au", "Australia",     "🇦🇺", approxRelays = 60),
        Region("nz", "New Zealand",   "🇳🇿", approxRelays = 20),
        Region("br", "Brazil",        "🇧🇷", approxRelays = 50),
        Region("ar", "Argentina",     "🇦🇷", approxRelays = 25),
        Region("za", "South Africa",  "🇿🇦", approxRelays = 20),
        Region("il", "Israel",        "🇮🇱", approxRelays = 35),
        Region("tr", "Turkey",        "🇹🇷", approxRelays = 25),
        Region("ge", "Georgia",       "🇬🇪", approxRelays = 20),
        Region("am", "Armenia",       "🇦🇲", approxRelays = 15),
        Region("hr", "Croatia",       "🇭🇷", approxRelays = 25),
        Region("rs", "Serbia",        "🇷🇸", approxRelays = 30),
        Region("hu", "Hungary",       "🇭🇺", approxRelays = 90),
        Region("sk", "Slovakia",      "🇸🇰", approxRelays = 40),
        Region("be", "Belgium",       "🇧🇪", approxRelays = 110),
        Region("lu", "Luxembourg",    "🇱🇺", approxRelays = 35),
        Region("by", "Belarus",       "🇧🇾", approxRelays = 10),
        Region("kz", "Kazakhstan",    "🇰🇿", approxRelays = 10),
        Region("mx", "Mexico",        "🇲🇽", approxRelays = 30),
        Region("cl", "Chile",         "🇨🇱", approxRelays = 20),
        Region("th", "Thailand",      "🇹🇭", approxRelays = 30),
        Region("id", "Indonesia",     "🇮🇩", approxRelays = 25)
    ).sortedBy { it.name }
}
