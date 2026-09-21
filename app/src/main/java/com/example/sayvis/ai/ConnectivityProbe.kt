package com.example.sayvis.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import com.example.sayvis.net.SayvisNet

/**
 * Answers the owner's very first question — "does SAYVIS actually have
 * internet?" — with a precise, honest state instead of silence:
 *
 *  - ONLINE            transport AND Google both reachable (cloud AI possible);
 *  - ONLINE_NO_GOOGLE  transport works but Google is unreachable from this
 *                      network (the Iran case: VPN needed for Gemini/AI Studio);
 *  - OFFLINE           nothing reachable.
 *
 * Transport is probed on Bing + Cloudflare (both reachable from Iran-side
 * networks in most cases); Google on gstatic's generate_204.
 */
class ConnectivityProbe {

    enum class NetState { ONLINE, ONLINE_NO_GOOGLE, OFFLINE }

    data class Result(
        val state: NetState,
        val transportOk: Boolean,
        val googleOk: Boolean,
        val checkedAtEpochMs: Long
    ) {
        val isOnline: Boolean get() = state != NetState.OFFLINE

        fun message(fa: Boolean): String = when (state) {
            NetState.ONLINE -> if (fa) "🟢 آنلاین — اینترنت و گوگل در دسترس‌اند"
            else "🟢 Online — internet and Google reachable"
            NetState.ONLINE_NO_GOOGLE -> if (fa) "🟠 اینترنت وصل است اما گوگل از شبکهٔ شما در دسترس نیست (VPN لازم است)"
            else "🟠 Internet works but Google is unreachable on this network (VPN needed)"
            NetState.OFFLINE -> if (fa) "🔴 هیچ اتصال اینترنتی در دسترس نیست"
            else "🔴 No internet connection available"
        }
    }

    /** Pure decision (unit-tested). */
    fun decide(transportOk: Boolean, googleOk: Boolean): NetState = when {
        !transportOk -> NetState.OFFLINE
        googleOk -> NetState.ONLINE
        else -> NetState.ONLINE_NO_GOOGLE
    }

    suspend fun probe(): Result = withContext(Dispatchers.IO) {
        val transport = runCatching { reachable("https://www.bing.com/") }.getOrDefault(false) ||
            runCatching { reachable("https://cp.cloudflare.com/generate_204") }.getOrDefault(false)
        val google = runCatching { reachable("https://connectivitycheck.gstatic.com/generate_204") }.getOrDefault(false)
        Result(decide(transport, google), transport, google, System.currentTimeMillis())
    }

    private fun reachable(url: String): Boolean {
        val client = SayvisNet.probeClient(4, 4)
        val request = Request.Builder().url(url).head().build()
        return client.newCall(request).execute().use { it.isSuccessful || it.code == 204 }
    }
}
