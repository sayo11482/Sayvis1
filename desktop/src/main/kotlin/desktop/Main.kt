package desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.awt.FileDialog
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SAYVIS for Windows (v5.2.0) — the desktop edition of the Android app:
 * same atomic-gold identity, same Google-account identity hub, and a REAL
 * QR/LAN pairing loop:
 *
 *  1. Windows renders its pairing QR (account + device + pin + LAN address);
 *  2. the Android app scans it → links the same Google account → performs an
 *     HTTP handshake back to this machine (port 8765) → the device appears in
 *     the paired list below;
 *  3. provider keys / configs can arrive as QR content too (paste field).
 *
 * The assistant talks to Gemini with the same fall-forward model chain as the
 * phone, and the live forex strip uses the same public sources.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SAYVIS — Personal AI Platform",
        state = rememberWindowState(width = 520.dp, height = 940.dp)
    ) {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = ArenaTheme.Gold)) {
            Box(modifier = Modifier.fillMaxSize().background(ArenaTheme.Black)) {
                SayvisDesktopApp()
            }
        }
    }
}

private data class ChatLine(val role: String, val text: String)

@Composable
private fun SayvisDesktopApp() {
    val scope = rememberCoroutineScope()

    var config by remember { mutableStateOf(ConfigStore.load()) }
    var listenerState by remember { mutableStateOf("راه‌اندازی شنونده…") }
    var lanAddress by remember { mutableStateOf(DeviceInfo.lanAddress()) }
    var verdict by remember { mutableStateOf("در حال بررسی شبکه…") }

    var emailInput by remember { mutableStateOf(config.accountEmail) }
    var keyInput by remember { mutableStateOf(config.geminiKey) }
    var importText by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf("") }

    var chatBusy by remember { mutableStateOf(false) }
    val chat = remember { mutableStateListOf(ChatLine("SAYVIS", "سلام! من سایو هستم — دستیار سایویس روی ویندوز. بپرسید.")) }
    var chatInput by remember { mutableStateOf("") }

    var marketRows by remember { mutableStateOf(listOf<QuoteRow>()) }
    var marketBusy by remember { mutableStateOf(false) }

    fun persist(next: DesktopConfig) {
        config = next
        ConfigStore.save(next)
    }

    // -------- startup effects: probe + market
    LaunchedEffect(Unit) {
        verdict = Probe.verdict()
        marketBusy = true
        marketRows = MarketClient.fetch()
        marketBusy = false
    }

    // -------- the LAN pairing listener (port 8765)
    LaunchedEffect(Unit) {
        val server = runCatching {
            val server = HttpServer.create(InetSocketAddress(8765), 0)
            server.executor = Executors.newSingleThreadExecutor()
            server.createContext("/") { exchange -> handleRoot(exchange, config.accountEmail, config.pin, config.devices.size, lanAddress) }
            server.createContext("/pair") { exchange ->
                val body = exchange.requestBody.readBytes().decodeToString()
                val json = runCatching { org.json.JSONObject(body) }.getOrNull()
                val device = json?.optString("device").orEmpty().ifBlank { "Android device" }
                val account = json?.optString("account").orEmpty()
                val pin = json?.optString("pin").orEmpty()
                val source = json?.optString("source", "android").orEmpty().ifBlank { "android" }
                if (json != null && device.isNotBlank()) {
                    scope.launch(Dispatchers.Main) {
                        val next = DesktopConfig(
                            accountEmail = config.accountEmail,
                            geminiKey = config.geminiKey,
                            geminiModel = config.geminiModel,
                            pin = config.pin,
                            devices = (config.devices.filterNot { it.name == device && it.source == source } +
                                PairedDevice(device, account, pin, source, System.currentTimeMillis()))
                                .takeLast(20)
                        )
                        persist(next)
                    }
                    exchange.sendResponseHeaders(200, -1)
                } else {
                    exchange.sendResponseHeaders(400, -1)
                }
                exchange.close()
            }
            server.start()
            server
        }.getOrNull()
        listenerState = if (server != null) "فعال روی پورت 8765 ✅" else "پورت 8765 در دسترس نیست (فایروال؟)"
    }

    val payload = remember(config, lanAddress) {
        val host = lanAddress?.let { "$it:8765" }
        Codec.buildPayload(
            account = config.accountEmail.ifBlank { "owner@sayvis.local" },
            device = DeviceInfo.hostname(),
            pin = config.pin,
            createdAt = System.currentTimeMillis(),
            host = host
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---------------------------------------------------------- header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AtomicCore(size = 130.dp, busy = chatBusy)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "سایو · پلتفرم هوش مصنوعی شخصی",
                fontSize = 11.sp,
                color = ArenaTheme.Gold,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "SAYVIS — Windows",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ArenaTheme.TextLight
            )
            Text(
                text = verdict,
                fontSize = 11.sp,
                color = ArenaTheme.SilverMuted
            )
        }

        // -------------------------------------------------- google account
        SectionCard(title = "اکانت گوگل — مرکز هویت") {
            Text(
                text = "همان هویتی که روی گوشی شماست؛ با اسکن QR پایین، هر دو دستگاه زیر یک اکانت می‌روند.",
                fontSize = 11.sp,
                color = ArenaTheme.SilverMuted
            )
            LabeledField(
                label = "ایمیل اکانت گوگل",
                value = emailInput,
                onValueChange = { emailInput = it },
                hint = "name@gmail.com"
            )
            GoldButton(
                label = "ذخیره هویت",
                enabled = emailInput.contains("@")
            ) {
                persist(
                    DesktopConfig(
                        accountEmail = emailInput.trim().lowercase(),
                        geminiKey = config.geminiKey,
                        geminiModel = config.geminiModel,
                        pin = ConfigStore.ensurePin(config.pin),
                        devices = config.devices
                    )
                )
                importMessage = "هویت ذخیره شد ✅"
            }
            if (config.accountLinked) {
                Text(
                    text = "✅ " + config.accountEmail + "  ·  کد جفت‌سازی: " + config.pin,
                    fontSize = 11.sp,
                    color = ArenaTheme.Green
                )
            }
        }

        // ------------------------------------------------- QR device pairing
        SectionCard(title = "اتصال دستگاه‌ها — QR") {
            Text(
                text = "این QR را با اسکنر داخل اپ اندروید بزنید؛ گوشی همان اکانت را لینک می‌کند و خودکار به این کامپیوتر دست می‌دهد.",
                fontSize = 11.sp,
                color = ArenaTheme.SilverMuted
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QrCanvas(payload = payload, side = 190.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "شنوندهٔ جفت‌سازی: " + listenerState + (lanAddress?.let { "  ·  $it:8765" } ?: ""),
                    fontSize = 10.5.sp,
                    color = ArenaTheme.Silver
                )
            }
            LabeledField(
                label = "چسباندن محتوای QR / کلید / کانفیگ",
                value = importText,
                onValueChange = { importText = it },
                hint = "sayvis://link?… یا کلید AIza… یا {\"provider\":…}"
            )
            GoldButton(label = "ثبت محتوای اسکن") {
                when (val imp = Codec.classify(importText)) {
                    is Codec.Import.PairingLink -> {
                        importMessage = "جفت‌سازی از دستگاه «" + imp.pairing.device + "» ثبت شد — روی گوشی، اسکن QR این صفحه اتصال را کامل می‌کند."
                        persist(
                            config.copy(
                                devices = (config.devices + PairedDevice(
                                    name = imp.pairing.device.ifBlank { "paired-device" },
                                    account = imp.pairing.account,
                                    pin = imp.pairing.pin,
                                    source = "qr",
                                    at = imp.pairing.createdAt
                                )).distinctBy { it.name + it.source }
                            )
                        )
                    }
                    is Codec.Import.ProviderKey -> {
                        importMessage = "کلید " + imp.provider + " ذخیره شد ✅"
                        persist(config.copy(geminiKey = if (imp.provider == "gemini") imp.apiKey else config.geminiKey))
                    }
                    is Codec.Import.Config -> {
                        importMessage = "کانفیگ ثبت شد ✅ (provider=" + (imp.provider ?: "?") + ")"
                        persist(
                            config.copy(
                                geminiKey = if (imp.provider == "gemini" || imp.provider.isNullOrBlank()) imp.apiKey else config.geminiKey,
                                geminiModel = imp.model ?: config.geminiModel
                            )
                        )
                    }
                    is Codec.Import.Link -> importMessage = "لینک: " + imp.url
                    is Codec.Import.Plain -> importMessage = if (imp.text.isBlank()) "خالی بود." else "متن: " + imp.text.take(60)
                }
                importText = ""
            }
            if (importMessage.isNotBlank()) {
                Text(text = importMessage, fontSize = 11.sp, color = ArenaTheme.Silver)
            }

            Text(
                text = "دستگاه‌های متصل (" + config.devices.size + ")",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ArenaTheme.Silver
            )
            if (config.devices.isEmpty()) {
                Text(text = "هنوز دستگاهی جفت نشده — QR بالا را با اپ اندروید اسکن کنید.", fontSize = 10.5.sp, color = ArenaTheme.SilverMuted)
            }
            config.devices.take(8).forEach { d ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ArenaTheme.SurfaceVariant)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = d.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ArenaTheme.Silver)
                        Text(
                            text = d.account.ifBlank { "—" } + "  ·  " + d.source + "  ·  " + d.pin,
                            fontSize = 10.sp,
                            color = ArenaTheme.SilverMuted
                        )
                    }
                    Text(text = "●", fontSize = 14.sp, color = ArenaTheme.Green)
                }
            }
            GoldButton(label = "ذخیرهٔ تصویر QR (PNG)") {
                val bytes = qrPngBytes(payload, 640)
                val dialog = FileDialog(null as java.awt.Frame?, "ذخیرهٔ QR", FileDialog.SAVE)
                dialog.file = "sayvis-qr.png"
                dialog.isVisible = true
                val target = dialog.directory + (dialog.file ?: "sayvis-qr.png")
                if (bytes != null) java.io.File(target).writeBytes(bytes)
                importMessage = "QR ذخیره شد: " + target
            }
        }

        // ------------------------------------------------- assistant (SAYO)
        SectionCard(title = "دستیار سایو (Gemini)") {
            if (config.geminiKey.isBlank()) {
                Text(
                    text = "برای گفتگو، کلید Gemini را بچسبانید (همان کلید AI Studio گوشی — یا از QR/پست کپی):",
                    fontSize = 11.sp,
                    color = ArenaTheme.SilverMuted
                )
                LabeledField(
                    label = "کلید Gemini",
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    hint = "AIza…"
                )
                GoldButton(label = "ذخیرهٔ کلید", enabled = keyInput.length > 20) {
                    persist(config.copy(geminiKey = keyInput.trim()))
                    importMessage = "کلید ذخیره شد ✅"
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ArenaTheme.Surface)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chat.takeLast(8).forEach { line ->
                    Text(
                        text = (if (line.role == "USER") "شما: " else "سایو: ") + line.text,
                        fontSize = 11.5.sp,
                        color = if (line.role == "USER") ArenaTheme.SilverMuted else ArenaTheme.TextLight
                    )
                }
            }
            LabeledField(
                label = "پیام به سایو",
                value = chatInput,
                onValueChange = { chatInput = it },
                hint = "سؤال یا فرمان…"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoldButton(
                    label = if (chatBusy) "در حال فکر…" else "ارسال",
                    enabled = !chatBusy && chatInput.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    val prompt = chatInput.trim()
                    chatInput = ""
                    chat.add(ChatLine("USER", prompt))
                    chatBusy = true
                    scope.launch {
                        val result = Gemini.chat(
                            apiKey = config.geminiKey,
                            model = config.geminiModel,
                            prompt = prompt,
                            history = chat.dropLast(1).map { (role, text) ->
                                (if (role == "USER") "USER" else "MODEL") to text
                            }
                        )
                        chat.add(
                            result.fold(
                                onSuccess = { ChatLine("SAYVIS", it) },
                                onFailure = { ChatLine("SAYVIS", "⚠️ " + it.message.orEmpty()) }
                            )
                        )
                        chatBusy = false
                    }
                }
            }
        }

        // ------------------------------------------------------- live market
        SectionCard(title = "مارکت لایو — فارکس و فلزات") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (marketBusy) "در حال دریافت…" else "نمادهای زندهٔ واقعی",
                    fontSize = 11.sp,
                    color = ArenaTheme.SilverMuted,
                    modifier = Modifier.weight(1f)
                )
                GoldButton(label = "بروزرسانی", enabled = !marketBusy) {
                    marketBusy = true
                    scope.launch {
                        marketRows = MarketClient.fetch()
                        marketBusy = false
                    }
                }
            }
            if (marketRows.isEmpty() && !marketBusy) {
                Text(text = "هنوز داده‌ای نیست؛ بروزرسانی را بزنید.", fontSize = 10.5.sp, color = ArenaTheme.SilverMuted)
            }
            marketRows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = row.label, fontSize = 11.5.sp, color = ArenaTheme.Silver, modifier = Modifier.weight(1f))
                    Text(text = row.price, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = ArenaTheme.TextLight)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = row.change?.let { (if (it >= 0) "+" else "") + "%.2f%%".format(it) } ?: "—",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            row.change == null -> ArenaTheme.SilverMuted
                            row.change >= 0 -> ArenaTheme.Green
                            else -> ArenaTheme.Red
                        }
                    )
                }
            }
        }

        Text(
            text = "SAYVIS 5.2.0 · ویندوز — تنظیمات در " + System.getProperty("user.home") + "\\.sayvis",
            fontSize = 9.5.sp,
            color = ArenaTheme.SilverMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

/** Processes the root path — a tiny status page for browser verification. */
private fun handleRoot(exchange: HttpExchange, account: String, pin: String, devices: Int, lan: String?) {
    runCatching {
        val html = "<html dir=\"rtl\"><body style=\"background:#0A0B0E;color:#F2F4F7;font-family:sans-serif\">" +
            "<h2 style=\"color:#D4AF37\">SAYVIS Desktop — فعال ✅</h2>" +
            "<p>اکانت: " + (account.ifBlank { "—" }) + "<br/>کد جفت‌سازی: " + pin + "<br/>دستگاه‌های جفت‌شده: " + devices + "</p>" +
            "<p>آدرس LAN: " + (lan ?: "نامشخص") + ":8765</p>" +
            "</body></html>"
        val bytes = html.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
    exchange.close()
}

@Composable
private fun SectionCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ArenaTheme.Surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ArenaTheme.Gold)
        content()
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String
) {
    Text(text = label, fontSize = 11.sp, color = ArenaTheme.Silver)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(hint, fontSize = 11.sp, color = ArenaTheme.SilverMuted) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ArenaTheme.Gold,
            unfocusedBorderColor = ArenaTheme.Border,
            cursorColor = ArenaTheme.Gold,
            focusedTextColor = ArenaTheme.TextLight,
            unfocusedTextColor = ArenaTheme.TextLight
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GoldButton(
    label: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = ArenaTheme.Gold,
            contentColor = ArenaTheme.Black,
            disabledContainerColor = ArenaTheme.SurfaceVariant,
            disabledContentColor = ArenaTheme.SilverMuted
        ),
        modifier = modifier
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
