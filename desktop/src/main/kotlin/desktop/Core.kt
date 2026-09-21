package desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.image.BufferedImage
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Arena black-gold palette, shared with the Android edition. */
object ArenaTheme {
    val Black = Color(0xFF0A0B0E)
    val Surface = Color(0xFF14171B)
    val SurfaceVariant = Color(0xFF1C2026)
    val Border = Color(0xFF272C33)
    val Gold = Color(0xFFD4AF37)
    val GoldLight = Color(0xFFF3CA68)
    val Silver = Color(0xFFA6ADB6)
    val SilverMuted = Color(0xFF6E757E)
    val TextLight = Color(0xFFF2F4F7)
    val Green = Color(0xFF10B981)
    val Red = Color(0xFFEF4444)
}

/** Shared HTTP engine with the SAYVIS identity header, mirroring SayvisNet. */
object Net {
    const val USER_AGENT: String = "SAYVIS/5.2 (Windows desktop; personal robot assistant)"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    fun probeClient(): OkHttpClient = client.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()
}

// ================================================================== codec

/**
 * The pairing-QR codec — the exact same wire format as the Android edition
 * (`sayvis://link?v=1&acct=…&dev=…&pin=…&ts=…&h=<lan-host:port>`), so a QR
 * shown on Windows scans cleanly inside the Android app and vice versa.
 */
object Codec {
    const val SCHEME_PREFIX = "sayvis://link?"

    data class Pairing(
        val account: String,
        val device: String,
        val pin: String,
        val createdAt: Long,
        val host: String? = null
    )

    sealed class Import {
        data class PairingLink(val pairing: Pairing) : Import()
        data class ProviderKey(val provider: String, val apiKey: String) : Import()
        data class Config(val provider: String?, val apiKey: String, val baseUrl: String?, val model: String?) : Import()
        data class Link(val url: String) : Import()
        data class Plain(val text: String) : Import()
    }

    fun buildPayload(account: String, device: String, pin: String, createdAt: Long, host: String? = null): String {
        val base = SCHEME_PREFIX +
            "v=1" +
            "&acct=" + urlencode(account) +
            "&dev=" + urlencode(device) +
            "&pin=" + urlencode(pin) +
            "&ts=" + createdAt
        return if (host.isNullOrBlank()) base else base + "&h=" + urlencode(host)
    }

    fun parsePairing(raw: String): Pairing? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith(SCHEME_PREFIX)) return null
        val params = HashMap<String, String>()
        for (piece in trimmed.removePrefix(SCHEME_PREFIX).split("&")) {
            if (piece.isBlank()) continue
            val idx = piece.indexOf('=')
            val key = if (idx < 0) piece else piece.substring(0, idx)
            val value = if (idx < 0) "" else piece.substring(idx + 1)
            params[urldecode(key)] = urldecode(value)
        }
        if (params["v"] != "1") return null
        val account = params["acct"]?.trim().orEmpty()
        val pin = params["pin"]?.trim().orEmpty()
        if (!account.contains('@') || pin.length != 6 || pin.any { it !in '0'..'9' }) return null
        val hostRaw = params["h"]?.trim()
        return Pairing(
            account = account,
            device = params["dev"]?.trim().orEmpty(),
            pin = pin,
            createdAt = params["ts"]?.toLongOrNull() ?: 0L,
            host = hostRaw?.takeIf { it.isNotBlank() }
        )
    }

    fun classify(raw: String): Import {
        val text = raw.trim()
        if (text.isEmpty()) return Import.Plain("")
        parsePairing(text)?.let { return Import.PairingLink(it) }
        configFromJson(text)?.let { return it }
        providerForKey(text)?.let { return Import.ProviderKey(it, text) }
        if (text.startsWith("https://") || text.startsWith("http://")) return Import.Link(text)
        return Import.Plain(text)
    }

    fun providerForKey(value: String): String? {
        val v = value.trim()
        return when {
            v.length >= 39 && v.startsWith("AIza") -> "gemini"
            v.startsWith("AQ.") -> "gemini"
            v.startsWith("sk-or-") -> "openrouter"
            v.startsWith("gsk_") -> "groq"
            v.startsWith("xai-") -> "xai"
            v.startsWith("sk-") -> "openai"
            else -> null
        }
    }

    fun configFromJson(text: String): Import.Config? {
        val t = text.trim()
        if (!t.startsWith("{") || !t.endsWith("}")) return null
        val apiKey = miniString(t, "apiKey") ?: return null
        if (apiKey.isBlank()) return null
        return Import.Config(
            provider = miniString(t, "provider")?.lowercase(),
            apiKey = apiKey,
            baseUrl = miniString(t, "baseUrl"),
            model = miniString(t, "model")
        )
    }

    fun miniString(json: String, field: String): String? {
        val b = 92.toChar()
        val q = 34.toChar()
        val pattern = q + field + q + b + "s*:" + b + "s*" + q + "((?:" + b + b + ".|[^" + q + b + b + "])*)" + q
        val hit = Regex(pattern).find(json) ?: return null
        return unescape(hit.groupValues[1])
    }

    fun unescape(value: String): String {
        if (!value.contains(92.toChar())) return value
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch != 92.toChar() || i + 1 >= value.length) {
                out.append(ch); i++; continue
            }
            when (val next = value[i + 1]) {
                34.toChar() -> out.append(34.toChar())
                92.toChar() -> out.append(92.toChar())
                'n' -> out.append(' ')
                else -> out.append(next)
            }
            i += 2
        }
        return out.toString()
    }

    private fun urlencode(v: String): String =
        java.net.URLEncoder.encode(v, "UTF-8").replace("+", "%20")

    private fun urldecode(v: String): String =
        runCatching { java.net.URLDecoder.decode(v, "UTF-8") }.getOrDefault(v)
}

// ================================================================= config

data class PairedDevice(
    val name: String,
    val account: String,
    val pin: String,
    val source: String,
    val at: Long
)

data class DesktopConfig(
    val accountEmail: String = "",
    val geminiKey: String = "",
    val geminiModel: String = "gemini-3.6-flash",
    val pin: String = "",
    val devices: List<PairedDevice> = emptyList()
) {
    val accountLinked: Boolean get() = accountEmail.isNotBlank()
}

/** JSON persistence under %USERPROFILE%\.sayvis\config.json. */
object ConfigStore {

    private fun file(): File {
        val dir = File(System.getProperty("user.home"), ".sayvis")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "config.json")
    }

    fun load(): DesktopConfig = runCatching {
        val f = file()
        if (!f.exists()) return DesktopConfig()
        val root = org.json.JSONObject(f.readText())
        val devices = mutableListOf<PairedDevice>()
        val arr = root.optJSONArray("devices")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val d = arr.optJSONObject(i) ?: continue
                devices.add(
                    PairedDevice(
                        name = d.optString("name"),
                        account = d.optString("account"),
                        pin = d.optString("pin"),
                        source = d.optString("source", "device"),
                        at = d.optLong("at")
                    )
                )
            }
        }
        DesktopConfig(
            accountEmail = root.optString("accountEmail"),
            geminiKey = root.optString("geminiKey"),
            geminiModel = root.optString("geminiModel", "gemini-3.6-flash").ifBlank { "gemini-3.6-flash" },
            pin = root.optString("pin"),
            devices = devices
        )
    }.getOrDefault(DesktopConfig())

    fun save(config: DesktopConfig) {
        runCatching {
            val root = org.json.JSONObject()
            root.put("accountEmail", config.accountEmail)
            root.put("geminiKey", config.geminiKey)
            root.put("geminiModel", config.geminiModel)
            root.put("pin", config.pin)
            val arr = org.json.JSONArray()
            config.devices.forEach { d ->
                arr.put(
                    org.json.JSONObject()
                        .put("name", d.name)
                        .put("account", d.account)
                        .put("pin", d.pin)
                        .put("source", d.source)
                        .put("at", d.at)
                )
            }
            root.put("devices", arr)
            file().writeText(root.toString(2))
        }
    }

    /** Stable 6-digit pairing pin, generated once per machine. */
    fun ensurePin(existing: String): String =
        existing.takeIf { it.length == 6 } ?: (100000..999999).random().toString()
}

// =================================================================== net

data class QuoteRow(val label: String, val price: String, val change: Double?)

/** Live market rows — the same public sources the phone uses. */
object MarketClient {

    suspend fun fetch(): List<QuoteRow> = withContext(Dispatchers.IO) {
        val rows = mutableListOf<QuoteRow>()
        runCatching {
            val body = get("https://api.coingecko.com/api/v3/simple/price?ids=pax-gold, tether&vs_currencies=usd&include_24hr_change=true".replace(" ", ""))
            val root = org.json.JSONObject(body)
            val gold = root.optJSONObject("pax-gold")
            if (gold != null) rows.add(QuoteRow("طلا (XAU/USD)", fmt(gold.optDouble("usd")), gold.optDouble("usd_24h_change", Double.NaN).takeIf { !it.isNaN() }))
            val tether = root.optJSONObject("tether")
            if (tether != null) rows.add(QuoteRow("تتر (USDT)", fmt(tether.optDouble("usd")), tether.optDouble("usd_24h_change", Double.NaN).takeIf { !it.isNaN() }))
        }
        runCatching {
            val body = get("https://api.frankfurter.app/latest?from=EUR&to=USD")
            val rate = org.json.JSONObject(body).optJSONObject("rates")?.optDouble("USD")
            if (rate != null) rows.add(QuoteRow("یورو/دلار (EUR/USD)", fmt(rate), null))
        }
        runCatching {
            val body = get("https://open.er-api.com/v6/latest/USD")
            val irr = org.json.JSONObject(body).optJSONObject("rates")?.optDouble("IRR")
            if (irr != null) rows.add(QuoteRow("دلار/ریال (USD/IRR)", fmt(irr), null))
        }
        rows
    }

    private fun get(url: String): String =
        Net.client.newCall(Request.Builder().url(url).header("User-Agent", Net.USER_AGENT).get().build())
            .execute().use { r ->
                val body = r.body?.string().orEmpty()
                if (!r.isSuccessful) error("HTTP ${r.code}") else body
            }

    private fun fmt(v: Double): String = when {
        v >= 100000 -> "%.0f".format(v)
        v >= 1000 -> "%.1f".format(v)
        else -> "%.4f".format(v)
    }
}

/** Quick honest connectivity verdict for the header. */
object Probe {
    suspend fun verdict(): String = withContext(Dispatchers.IO) {
        val google = runCatching {
            Net.probeClient().newCall(Request.Builder().url("https://connectivitycheck.gstatic.com/generate_204").head().build())
                .execute().use { it.isSuccessful || it.code == 204 }
        }.getOrDefault(false)
        val transport = runCatching {
            Net.probeClient().newCall(Request.Builder().url("https://cp.cloudflare.com/generate_204").head().build())
                .execute().use { it.isSuccessful || it.code == 204 }
        }.getOrDefault(false)
        when {
            transport && google -> "🟢 آنلاین — اینترنت و گوگل در دسترس‌اند"
            transport -> "🟠 اینترنت وصل است اما گوگل مسدود است (VPN لازم است)"
            else -> "🔴 هیچ اتصال اینترنتی در دسترس نیست"
        }
    }
}

/** Gemini chat with the same fall-forward chain as the phone (v5.0.1 fix). */
object Gemini {

    val FALLBACKS = listOf("gemini-3.6-flash", "gemini-flash-latest", "gemini-2.0-flash")

    fun chain(requested: String): List<String> {
        val head = requested.trim().ifBlank { "gemini-3.6-flash" }
        val out = mutableListOf(head)
        for (f in FALLBACKS) if (out.none { it == f }) out.add(f)
        return out
    }

    fun retiredError(message: String): Boolean {
        val m = message.lowercase()
        return m.contains("no longer available") ||
            m.contains("is not found") ||
            m.contains("not found for api version") ||
            (m.contains("models/gemini") && m.contains("404")) ||
            m.contains("\"code\":404") ||
            (m.contains("http 404") && m.contains("models/"))
    }

    suspend fun chat(apiKey: String, model: String, prompt: String, history: List<Pair<String, String>>): Result<String> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext Result.failure(IllegalStateException("کلید Gemini تنظیم نشده است — از بخش اکانت/کلید اضافه کنید."))
            val contents = org.json.JSONArray()
            history.takeLast(12).forEach { (role, text) ->
                contents.put(
                    org.json.JSONObject()
                        .put("role", if (role == "USER") "user" else "model")
                        .put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", text)))
                )
            }
            contents.put(
                org.json.JSONObject()
                    .put("role", "user")
                    .put("parts", org.json.JSONArray().put(org.json.JSONObject().put("text", prompt)))
            )
            val body = org.json.JSONObject()
                .put("contents", contents)
                .put(
                    "systemInstruction",
                    org.json.JSONObject().put(
                        "parts",
                        org.json.JSONArray().put(
                            org.json.JSONObject().put(
                                "text",
                                "You are SAYO, the SAYVIS personal AI assistant on the owner's Windows PC. " +
                                    "Reply in the owner's language (Persian by default). Be precise, warm and actionable."
                            )
                        )
                    )
                )
                .put("generationConfig", org.json.JSONObject().put("temperature", 0.7).put("maxOutputTokens", 2048))

            var lastError = "no attempt"
            for (model in chain(model)) {
                runCatching {
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                        .header("x-goog-api-key", apiKey.trim())
                        .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                    Net.client.newCall(request).execute().use { r ->
                        val payload = r.body?.string().orEmpty()
                        if (!r.isSuccessful) error("HTTP ${r.code}: " + payload.take(220))
                        val candidate = org.json.JSONObject(payload).optJSONArray("candidates")?.optJSONObject(0)
                        val text = candidate?.optJSONObject("content")?.optJSONArray("parts")?.let { parts ->
                            buildString {
                                for (i in 0 until parts.length()) {
                                    val p = parts.optJSONObject(i) ?: continue
                                    if (!p.optBoolean("thought", false)) append(p.optString("text"))
                                }
                            }
                        }.orEmpty().trim()
                        if (text.isEmpty()) error("پاسخ خالی (finishReason=" + candidate?.optString("finishReason") + ")")
                        text
                    }
                }.onSuccess { return@withContext Result.success(it) }
                    .onFailure { lastError = it.message ?: "error" }
                if (!retiredError(lastError)) return@withContext Result.failure(IllegalStateException(lastError))
            }
            Result.failure(IllegalStateException(lastError))
        }

    private fun String.toRequestBody(media: okhttp3.MediaType): okhttp3.RequestBody =
        okhttp3.RequestBody.create(media, this)

    private fun String.toMediaType(): okhttp3.MediaType =
        with(okhttp3.MediaType.Companion) { this@toMediaType.toMediaType() }
}

// ================================================================== device

object DeviceInfo {
    fun hostname(): String = runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("Windows-PC")

    /** First site-local IPv4 (the LAN address the QR advertises). */
    fun lanAddress(): String? = runCatching {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val nif = interfaces.nextElement()
            val addresses = nif.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (!addr.isLoopbackAddress && addr is java.net.Inet4Address && addr.isSiteLocalAddress) {
                    return@runCatching addr.hostAddress
                }
            }
        }
        null
    }.getOrNull()

    /** Short device fingerprint shown in the pairing list. */
    fun fingerprint(seed: String): String =
        MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
            .take(3).joinToString("") { String.format("%02X", it) }
}

// ================================================================== canvas

/** The breathing golden atomic core — same visual language as the phone. */
@Composable
fun AtomicCore(size: Dp, busy: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "desktop_core")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (busy) 3000 else 9000, easing = LinearEasing)
        ),
        label = "spin"
    )
    val spin2 by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (busy) 4200 else 12600, easing = LinearEasing)
        ),
        label = "spin2"
    )
    val breath by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (busy) 1100 else 2400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    val flare by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(6000, easing = LinearEasing)),
        label = "flare"
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val base = minOf(this.size.width, this.size.height) / 2f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ArenaTheme.Gold.copy(alpha = 0.18f + 0.08f * breath), Color.Transparent),
                center = center,
                radius = base * (0.82f + 0.10f * breath)
            ),
            radius = base * (0.82f + 0.10f * breath),
            center = center
        )
        drawOrbit(center, base * 0.92f, 0.42f, spin, ArenaTheme.Gold, 1.6.dp.toPx())
        drawOrbit(center, base * 0.84f, 0.42f, spin + 60f, ArenaTheme.Gold.copy(alpha = 0.85f), 1.6.dp.toPx())
        drawOrbit(center, base * 0.74f, 0.40f, spin2, ArenaTheme.Gold.copy(alpha = 0.7f), 1.4.dp.toPx())
        drawElectrons(center, base * 0.92f, 0.42f, spin, ArenaTheme.GoldLight, flare, 0)
        drawElectrons(center, base * 0.84f, 0.42f, spin + 60f, ArenaTheme.GoldLight, 1f - flare, 1)

        val nucleus = base * (0.26f + 0.05f * breath)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.98f),
                    ArenaTheme.GoldLight,
                    ArenaTheme.Gold.copy(alpha = 0.55f),
                    Color.Transparent
                ),
                center = center,
                radius = nucleus * 1.5f
            ),
            radius = nucleus * 1.5f,
            center = center
        )
        drawCircle(color = Color.White.copy(alpha = 0.9f), radius = nucleus * 0.34f, center = center)
    }
}

private fun DrawScope.drawOrbit(center: Offset, radius: Float, squash: Float, angle: Float, color: Color, stroke: Float) {
    rotate(degrees = angle, pivot = center) {
        drawOval(
            brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.4f), color, color.copy(alpha = 0.4f))),
            topLeft = Offset(center.x - radius, center.y - radius * squash),
            size = Size(radius * 2f, radius * 2f * squash),
            style = Stroke(width = stroke)
        )
    }
}

private fun DrawScope.drawElectrons(center: Offset, radius: Float, squash: Float, angle: Float, color: Color, flare: Float, seed: Int) {
    for (i in 0 until 3) {
        val degree = angle + i * 120f + seed * 37f
        val rad = Math.toRadians(degree.toDouble())
        val ex = center.x + radius * cos(rad).toFloat()
        val ey = center.y + radius * squash * sin(rad).toFloat()
        val isLeader = (i + seed) % 3 == 0
        val glow = if (isLeader) 3.2.dp.toPx() * (0.8f + 0.6f * flare) else 2.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = if (isLeader) 0.95f else 0.75f), color, Color.Transparent),
                center = Offset(ex, ey),
                radius = glow * 1.6f
            ),
            radius = glow * 1.6f,
            center = Offset(ex, ey)
        )
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = glow * 0.4f, center = Offset(ex, ey))
    }
}

/** QR payload rendered as pure rects (no image conversion APIs). */
@Composable
fun QrCanvas(payload: String, side: Dp, modifier: Modifier = Modifier) {
    val matrix = remember(payload) {
        runCatching {
            com.google.zxing.qrcode.QRCodeWriter().encode(
                payload,
                com.google.zxing.BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
            )
        }.getOrNull()
    }
    Canvas(modifier = modifier.size(side)) {
        val m = matrix ?: return@Canvas
        val w = m.width
        val h = m.height
        val cell = minOf(this.size.width / w, this.size.height / h)
        val offX = (this.size.width - cell * w) / 2f
        val offY = (this.size.height - cell * h) / 2f
        drawRect(color = Color(0xFFF2F4F7), size = Size(this.size.width, this.size.height))
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (m.get(x, y)) {
                    drawRect(
                        color = Color(0xFF0A0B0E),
                        topLeft = Offset(offX + x * cell, offY + y * cell),
                        size = Size(cell + 0.5f, cell + 0.5f)
                    )
                }
            }
        }
    }
}

/** Small PNG export of the QR (for the tray/save feature). */
fun qrPngBytes(payload: String, size: Int): ByteArray? = runCatching {
    val matrix = com.google.zxing.qrcode.QRCodeWriter().encode(
        payload, com.google.zxing.BarcodeFormat.QR_CODE, size, size,
        mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
    )
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val mx = (x.toDouble() / size * matrix.width).toInt().coerceIn(0, matrix.width - 1)
            val my = (y.toDouble() / size * matrix.height).toInt().coerceIn(0, matrix.height - 1)
            img.setRGB(x, y, if (matrix.get(mx, my)) 0x0A0B0E else 0xF2F4F7)
        }
    }
    val out = java.io.ByteArrayOutputStream()
    javax.imageio.ImageIO.write(img, "png", out)
    out.toByteArray()
}.getOrNull()
