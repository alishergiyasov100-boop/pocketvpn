package com.musornibak.pocketvpn.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Measures download throughput through the *current* tunnel by streaming a
 * small chunk from speed.cloudflare.com. Cloudflare's speed endpoint always
 * resolves to the closest WARP edge, so the measurement reflects what real
 * traffic would see for this region.
 *
 * Returns Mbps (megabits / second), -1.0 on failure.
 */
class SpeedTester {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun measureMbps(bytes: Int = TEST_BYTES): Double = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://speed.cloudflare.com/__down?bytes=$bytes")
                .header("User-Agent", "pocketvpn/0.3.0")
                .build()
            val start = System.nanoTime()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext -1.0
                val body = resp.body ?: return@withContext -1.0
                val src = body.source()
                val buf = ByteArray(8 * 1024)
                var total = 0L
                val sink = src.inputStream()
                while (true) {
                    val n = sink.read(buf)
                    if (n <= 0) break
                    total += n
                }
                val elapsedSec = (System.nanoTime() - start) / 1_000_000_000.0
                if (elapsedSec <= 0.0) return@withContext -1.0
                (total * 8.0) / 1_000_000.0 / elapsedSec
            }
        }.getOrDefault(-1.0)
    }

    companion object {
        // 10 MB — long enough to escape TCP slow start, short enough for ~1s test
        const val TEST_BYTES = 10_000_000
    }
}
