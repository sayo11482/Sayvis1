package com.example.sayvis.i18n

import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

/**
 * Complete built-in SAYVIS interface dictionary (Persian ⇄ English).
 *
 * The whole point of this file is that **nothing in the chrome of the app is left in
 * English when the owner picks Persian**: navigation labels, section headers, buttons,
 * hints, statuses, dialogs and error messages are all resolved here, offline, with no
 * dependency on a network model. Free-form text that cannot be enumerated (AI answers,
 * broker notes, user-entered titles) is handled separately by
 * `com.example.sayvis.ai.TranslationService`.
 *
 * Access it inside Compose with `val s = LocalStrings.current`.
 */
class SayvisStrings(val fa: Boolean) {

    /** Core picker: returns the Persian string when [fa], otherwise the English one. */
    fun t(faText: String, enText: String): String = if (fa) faText else enText

    // ---------------------------------------------------------------- branding
    val appName get() = t("سایویس", "SAYVIS")
    val appNameLatin get() = "SAYVIS"
    val appTagline get() = t("سامانهٔ هوش شخصی و حاکمیت داده", "Sovereign Personal AI Platform")
    val assistantName get() = t("دستیار سایو", "SAYO Assistant")

    // ------------------------------------------------------- bottom navigation
    val navHome get() = t("خانه", "Home")
    val navAssistant get() = t("دستیار", "Assistant")
    val navTools get() = t("ابزارها", "Tools")
    val navSettings get() = t("تنظیمات", "Settings")
    val navMore get() = t("بیشتر", "More")
    val backToTools get() = t("بازگشت به ابزارها", "Back to tools")

    // ------------------------------------------------------------ tools screen
    val toolsTitle get() = t("ابزارهای کاربردی", "Practical Tools")
    val toolsSubtitle get() =
        t("همهٔ قابلیت‌ها در یک نگاه؛ روی هر کارت بزنید تا باز شود.", "Every capability at a glance — tap a card to open it.")
    val toolsGroupWork get() = t("کار و برنامه‌ریزی", "Work & Planning")
    val toolsGroupMind get() = t("شناخت و ادراک", "Cognition & Context")
    val toolsGroupMoney get() = t("مالی و معاملات", "Finance & Trading")
    val toolsGroupSystem get() = t("سیستم و توسعه", "System & Development")

    val toolMissions get() = t("مأموریت‌ها و کارها", "Missions & Tasks")
    val toolMissionsHint get() = t("اهداف، وظیفه‌ها، پیشرفت و مهلت‌ها", "Goals, tasks, progress and deadlines")
    val toolUic get() = t("پروندهٔ شناختی من", "My Cognitive Profile")
    val toolUicHint get() = t("ترجیحات، عادت‌ها و اهدافی که سایو از شما می‌داند", "Preferences, habits and goals SAYO knows about you")
    val toolAware get() = t("پیشنهادهای هوشمند", "Smart Suggestions")
    val toolAwareHint get() = t("هشدارها و فرصت‌هایی که سیستم خودش تشخیص می‌دهد", "Alerts and opportunities the system detects on its own")
    val toolTrading get() = t("معاملات و بازار", "Trading & Markets")
    val toolTradingHint get() = t("سیگنال‌ها، پورتفو و اتصال به ترمینال", "Signals, portfolio and terminal connection")
    val toolGateway get() = t("درگاه ترمینال ترید", "Trading Terminal Gateway")
    val toolGatewayHint get() = t("اتصال متاتریدر ۴/۵ به سایویس", "Connect MetaTrader 4/5 to SAYVIS")
    val toolScripts get() = t("اسکریپت و خودکارسازی", "Scripts & Automation")
    val toolScriptsHint get() = t("کدنویسی قانون و ربات شخصی", "Write your own rules and personal bots")
    val toolSecurity get() = t("امنیت و دستگاه‌ها", "Security & Devices")
    val toolSecurityHint get() = t("قفل اضطراری، دستگاه‌های متصل، گزارش رویدادها", "Kill-switch, paired devices, event log")
    val toolSimulation get() = t("شبیه‌سازی تصمیم", "Decision Simulator")
    val toolSimulationHint get() = t("سناریوهای مسیر زندگی و پیامد انتخاب‌ها", "Life-path scenarios and consequence modelling")

    // --------------------------------------------------------- settings screen
    val settingsTitle get() = t("تنظیمات نرم‌افزار", "Application Settings")
    val settingsSubtitle get() =
        t("همهٔ کنترل‌های سیستم اینجا جمع شده‌اند.", "Every system control lives here.")

    val sectionVisual get() = t("نمای گرافیکی هوش", "AI Visual Style")
    val visualSubtitle get() = t(
        "چهار نمای زندهٔ چندبعدی برای آواتار هوش: شکل‌های ژئومتریک، برش‌های استرولوژیک، باران کد دودویی ۰/۱ و هولوگرام — به صدای محیط واکنش نشان می‌دهند.",
        "Four live multidimensional views for the AI avatar: geometric shapes, stereologic slices, 0/1 binary code rain and hologram — all react to ambient sound."
    )
    val roboticSound get() = t("صدای رباتیک پاسخ به ویس", "Robotic voice-reply sound")
    val roboticSoundHint get() = t(
        "وقتی سایویس به ورودی گفتاری شما پاسخ می‌دهد، بیپ رباتیک پخش می‌شود.",
        "A robotic chirp plays whenever SAYVIS answers your voice input."
    )
    val sectionLanguage get() = t("زبان و نمایش", "Language & Appearance")

    // ------------------------------------------------------ permission gate
    val gateTitle get() = t("دسترسی‌های سایویس", "SAYVIS permissions")
    val gateSubtitle get() = t(
        "برای فعال‌شدن کامل قابلیت‌ها، دستگاه از شما اجازه می‌گیرد؛ هر زمان می‌توانید از تنظیمات تغییر دهید.",
        "The device will ask your approval to unlock every capability — changeable anytime in Settings."
    )
    val gateGrantAll get() = t("فعال‌سازی همهٔ دسترسی‌ها", "Enable all permissions")
    val gateContinue get() = t("ورود به سایویس", "Enter SAYVIS")
    val gatePrivacyNote get() = t(
        "حریم خصوصی: پردازش صدا و داده‌ها فقط روی همین دستگاه انجام می‌شود و هیچ چیز بدون اجازهٔ شما به بیرون ارسال نمی‌گردد.",
        "Privacy: audio and data are processed on this device only — nothing leaves without your explicit action."
    )
    val permMic get() = t("میکروفون", "Microphone")
    val permMicDesc get() = t("گفتگو با دستیار، ورودی صوتی و شنیدار آواتار", "Talking to the assistant, voice input and the listening avatar")
    val permCamera get() = t("دوربین", "Camera")
    val permCameraDesc get() = t("اسکن اسناد و قابلیت‌های بینایی آینده", "Document scanning and upcoming vision features")
    val permLocation get() = t("موقعیت مکانی", "Location")
    val permLocationDesc get() = t("پیشنهادهای آگاه از مکان (آفلاین، روی خود دستگاه)", "Location-aware suggestions (offline, on-device only)")
    val permGallery get() = t("گالری (تصاویر)", "Gallery (images)")
    val permGalleryDesc get() = t("خواندن تصاویر انتخابی شما برای تحلیل و پیوست", "Reading images you pick for analysis and attachments")
    val permContacts get() = t("مخاطبین", "Contacts")
    val permContactsDesc get() = t("یافتن مخاطب هنگام اشتراک‌گذاری، فقط با فرمان شما", "Finding a contact when sharing — only on your command")
    val permNotif get() = t("آگاهی‌سازها", "Notifications")
    val permNotifDesc get() = t("نشان شنیدار آواتار و هشدارهای مهم", "The avatar listening badge and critical alerts")
    val permOverlay get() = t("نمایش روی برنامه‌های دیگر", "Display over other apps")
    val permOverlayDesc get() = t("ماندن حبابک آواتار روی هوم و سایر برنامه‌ها", "Keeping the avatar bubble over Home and other apps")
    val sectionPermissions get() = t("دسترسی‌های دستگاه", "Device permissions")
    val permOpenSettings get() = t("تنظیمات دسترسی‌ها", "Open permission settings")
    val sectionAi get() = t("هوش مصنوعی و API", "Artificial Intelligence & API")
    val sectionTrading get() = t("درگاه معاملاتی", "Trading Gateway")
    val sectionAutomation get() = t("خودکارسازی و کدنویسی", "Automation & Scripting")
    val sectionSecurity get() = t("امنیت و حریم خصوصی", "Security & Privacy")
    val sectionData get() = t("داده‌ها", "Data")
    val sectionAbout get() = t("دربارهٔ نرم‌افزار", "About")

    val language get() = t("زبان برنامه", "Application language")
    val languageHint get() = t("کل رابط کاربری بلافاصله تغییر می‌کند", "The whole interface switches immediately")
    val persianDigits get() = t("اعداد فارسی", "Persian digits")
    val persianDigitsHint get() = t("نمایش ۱۲۳ به‌جای 123 در همهٔ برنامه", "Show ۱۲۳ instead of 123 everywhere")
    val autoTranslate get() = t("ترجمهٔ هوشمند متن‌های آزاد", "AI translation of free text")
    val autoTranslateHint get() =
        t("متن‌هایی که در واژه‌نامه نیستند (پاسخ هوش مصنوعی، یادداشت بروکر) آنلاین ترجمه می‌شوند",
            "Text with no dictionary entry (AI answers, broker notes) is translated online")
    val markTranslated get() = t("علامت‌گذاری متن ماشینی", "Mark machine-translated text")
    val forceRtl get() = t("چینش راست‌به‌چپ در فارسی", "Right-to-left layout in Persian")
    val appearance get() = t("حالت نمایش", "Appearance")
    val haptics get() = t("بازخورد لمزی", "Haptic feedback")

    // ------------------------------------------------------------------- AI/API
    val aiProvider get() = t("سرویس هوش مصنوعی", "AI provider")
    val aiProviderHint get() = t("موتوری که پاسخ‌ها را تولید می‌کند", "The engine that produces answers")
    val apiKey get() = t("کلید API", "API key")
    val apiKeyHint get() = t("کلید را از پنل سازندهٔ سرویس بگیرید و اینجا بچسبانید", "Copy the key from the provider console and paste it here")
    val model get() = t("مدل", "Model")
    val baseUrl get() = t("نشانی سرویس (Base URL)", "Service base URL")
    val temperature get() = t("خلاقیت پاسخ (Temperature)", "Response creativity (temperature)")
    val maxTokens get() = t("بیشینهٔ طول پاسخ", "Maximum response length")
    val timeout get() = t("مهلت انتظار (ثانیه)", "Timeout (seconds)")
    val persona get() = t("دستورالعمل شخصی‌سازی", "Custom system instruction")
    val personaHint get() = t("لحن و قوانین اختصاصی خودتان را اینجا بنویسید", "Describe the tone and rules you want")
    val forceResponseLanguage get() = t("پاسخ همیشه به زبان رابط کاربری", "Always answer in the UI language")

    val testConnection get() = t("آزمودن اتصال", "Test connection")
    val testing get() = t("در حال آزمون…", "Testing…")
    val connectionOk get() = t("اتصال برقرار است", "Connection successful")
    val connectionFailed get() = t("اتصال ناموفق", "Connection failed")
    val apiKeyMissing get() = t("کلید API وارد نشده است", "No API key entered")
    val apiKeyMaskedHint get() = t("برای تغییر، کلید جدید را کامل وارد کنید", "Enter a full new key to replace it")
    val showKey get() = t("نمایش", "Show")
    val hideKey get() = t("پنهان", "Hide")
    val clearKey get() = t("پاک کردن کلید", "Clear key")

    // Google account link (sign in with Google -> AI Studio -> Gemini key)
    val googleLinkTitle get() = t("اتصال با حساب گوگل (Gemini)", "Google account link (Gemini)")
    val googleLinkHow get() = t(
        "با حساب گوگل خود وارد Google AI Studio شوید، دکمهٔ «Create API key» را بزنید و کلید را کپی کنید؛ سایویس آن را خودکار تشخیص می‌دهد، آزمایش می‌کند و ذخیره می‌سازد.",
        "Sign in to Google AI Studio with your Google account, tap “Create API key” and copy it; SAYVIS auto-detects, verifies and stores the key."
    )
    val googleLinkSignIn get() = t("ورود با گوگل و دریافت کلید", "Sign in with Google & get key")
    val googleLinkFromClipboard get() = t("اتصال کلید کپی‌شده", "Connect copied key")
    val googleLinkChecking get() = t("در حال بررسی کلید…", "Verifying key…")
    val googleLinkSaved get() = t("کلید ذخیره شد و Gemini انتخاب شد ✅", "Key saved; Gemini selected ✅")
    val googleLinkNoKey get() = t(
        "کلیدی در کلیپ‌بورد پیدا نشد؛ ابتدا در AI Studio کلید بسازید و کپی کنید",
        "No key found on the clipboard; create and copy a key in AI Studio first"
    )
    val googleLinkActiveKey get() = t("کلید فعال", "Active key")
    val googleLinkKindAuth get() = t("کلید نوع Auth (AQ…)", "Auth key (AQ…)")
    val googleLinkKindStandard get() = t("کلید استاندارد (AIza…)", "Standard key (AIza…)")
    val googleLinkKindUnknown get() = t("کلید نوع نامشخص", "Key of unknown kind")
    val googleLinkSelectionHint get() = t(
        "راه میان‌بر: در صفحهٔ AI Studio کلید را با انگشت انتخاب کنید و «ذخیرهٔ کلید در سایویس» را بزنید.",
        "Shortcut: select the key text in AI Studio and tap “Save key in SAYVIS”."
    )
    val captureConnect get() = t("اتصال به سایویس", "Link to SAYVIS")
    val captureCancel get() = t("انصراف", "Cancel")
    val captureNone get() = t(
        "کلیدی در متن اشتراکی پیدا نشد. یک کلید Gemini از Google AI Studio بگیرید.",
        "No key found in the shared text. Get a Gemini key from Google AI Studio."
    )
    val captureOpenStudio get() = t("ورود به Google AI Studio", "Open Google AI Studio")

    // In-app Google sign-in (OAuth) + Google account capabilities
    val googleSignIn get() = t("ورود با گوگل", "Sign in with Google")
    val googleSignOut get() = t("خروج از حساب گوگل", "Sign out of Google")
    val googleWelcome get() = t("ورود با گوگل موفق بود ✅", "Google sign-in succeeded ✅")
    val googleDenied get() = t("اجازهٔ دسترسی داده نشد (access_denied)", "Access was denied (access_denied)")
    val googleErrorGeneric get() = t("جریان ورود گوگل ناتمام ماند؛ دوباره تلاش کنید", "The Google sign-in flow did not complete; try again")
    val googleReturnHint get() = t("در انتظار بازگشت از مرورگر… اگر این صفحه ماند، ورود را از تنظیمات دوباره شروع کنید.", "Waiting for the browser to return… if this page stays, restart sign-in from Settings.")
    val googleConnectedAs get() = t("حساب گوگل متصل شد:", "Google account connected:")
    val googleSectionTitle get() = t("حساب گوگل و دسترسی‌ها", "Google account & capabilities")
    val googleSectionHint get() = t(
        "ورود استاندارد OAuth گوگل با PKCE. توکن تازه‌سازی فقط در امن‌سپر (Keystore) نگه داشته می‌شود و دسترسی‌ها فقط-خواندنی‌اند: Gmail، تقویم، درایو.",
        "Standard Google OAuth with PKCE. The refresh token lives only in the Keystore vault; scopes are read-only: Gmail, Calendar, Drive."
    )
    val googleClientId get() = t("شناسهٔ کلاینت OAuth گوگل", "Google OAuth client ID")
    val googleClientIdHint get() = t(
        "Google Cloud Console → APIs & Services → Credentials → OAuth client (Web) با ریدایرکت sayvis://oauth2 — یک‌بار برای همیشه",
        "Google Cloud Console → APIs & Services → Credentials → OAuth client (Web) with redirect sayvis://oauth2 — one-time setup"
    )
    val googleHowTo get() = t(
        "راهنما: در console.cloud.google.com یک OAuth Client از نوع Web بسازید و Redirect URI را دقیقاً sayvis://oauth2 بگذارید، سپس شناسهٔ …apps.googleusercontent.com را اینجا وارد کنید.",
        "Guide: in console.cloud.google.com create a Web-type OAuth Client with redirect URI exactly sayvis://oauth2, then paste the …apps.googleusercontent.com ID here."
    )
    val googleOpenConsole get() = t("بازکردن Google Cloud Console", "Open Google Cloud Console")
    val googleLockToggle get() = t("ورود به اپ فقط با گوگل", "Require Google sign-in at launch")
    val googleGateTitle get() = t("ورود به سایویس", "Sign in to SAYVIS")
    val googleGateHint get() = t("برای ادامه، با حساب گوگل خود وارد شوید.", "Continue by signing in with your Google account.")
    val googleGateWelcome get() = t("خوش آمدید", "Welcome")
    val googleGateLocal get() = t("ادامهٔ محلی بدون ورود", "Continue locally without signing in")
    val googleConnecting get() = t("در حال تبادل توکن گوگل…", "Exchanging Google tokens…")
    val googleAutoConnecting get() = t("اتصال خودکار به جمینای…", "Auto-connecting to Gemini…")
    val googleOpeningStudio get() = t("بازشدن خودکار AI Studio — کلید را بسازید و کپی کنید", "Opening AI Studio automatically — create and copy a key")
    val googleWaitingCopy get() = t("در انتظار کپی‌کردن کلید…", "Waiting for the key to be copied…")
    val googleKeyVerifying get() = t("آزمایش زندهٔ کلید و اتصال…", "Verifying the key and linking…")
    val googleAutoDone get() = t("اتصال خودکار کامل شد — جمینای متصل است ✅", "Auto-connect complete — Gemini is linked ✅")
    val googleCopyGuide get() = t(
        "در صفحهٔ AI Studio: Create API key → Copy، بعد «بررسی مجدد» را بزنید (یا فقط به اپ برگردید).",
        "In AI Studio: Create API key → Copy, then tap Re-check (or just return to the app)."
    )
    val googleRecheck get() = t("بررسی مجدد کلیپ‌بورد", "Re-check clipboard")

    // Agent hub (five specialists + self-evolution)
    val agentHubHint get() = t("پنج ایجنت تخصصی — مغزِ رایگانِ بهترین، خودکار انتخاب می‌شود", "Five specialist agents — the best free brain is chosen automatically")
    val agentKindResearch get() = t("پژوهش", "Research")
    val agentKindTrade get() = t("ترید", "Trade")
    val agentKindContent get() = t("محتوا", "Content")
    val agentKindWeb get() = t("طراحی سایت", "Web design")
    val agentKindApp get() = t("اپ‌سازی", "App builder")
    val agentAutoBrain get() = t("⚡ انتخاب خودکار بهترین هوش مصنوعی رایگان و کم‌مصرف در هر اجرا", "⚡ Best free, token-lean AI is auto-selected on every run")
    val agentGoalTrade get() = t("مثلاً: تحلیل طلا در هفتهٔ جاری با سناریوها", "e.g. analyse gold this week with scenarios")
    val agentGoalContent get() = t("مثلاً: تقویم محتوای پیج پوشاک برای هفتهٔ آینده", "e.g. content calendar for my fashion page")
    val agentGoalWeb get() = t("مثلاً: لندینگ مینیمال برای استودیو طراحی", "e.g. minimal landing for a design studio")
    val agentGoalApp get() = t("مثلاً: اپ یادداشت با هم‌گام‌سازی محلی", "e.g. a notes app with local sync")
    val evolutionTitle get() = t("خودتکاملی سایویس (گیت‌هاب)", "SAYVIS self-evolution (GitHub)")
    val evolutionHint get() = t(
        "پروژه‌های مشابه را در گیت‌هاب می‌کاود و بهترین الگوها را به‌صورت بک‌لاگ جذب ثبت می‌کند.",
        "Scans GitHub for similar agents and records the best patterns as an adoption backlog."
    )
    val evolutionRun get() = t("اسکن گیت‌هاب", "Scan GitHub")
    val evolutionScanning get() = t("در حال کاوش…", "Scanning…")

    // Digital mirror
    val toolMirror get() = t("آینهٔ دیجیتال سایویس", "SAYVIS Digital Mirror")
    val toolMirrorHint get() = t(
        "با دوربین سلفی صورت شما را می‌بیند و به پرترهٔ رباتیک زنده تبدیل می‌کند — کاملاً روی دستگاه",
        "Sees you through the selfie camera and repaints you as a live robotic portrait — fully on-device"
    )
    val mirrorHint get() = t("صورت‌تان را داخل کادر نگه دارید…", "Hold your face inside the frame…")

    // Live markets + LIT strategy
    val marketsRefresh get() = t("به‌روزرسانی", "Refresh")
    val marketsRefreshing get() = t("در حال دریافت…", "Fetching…")
    val marketsPullHint get() = t("برای دریافت قیمت‌های زنده و تحلیل LIT، «به‌روزرسانی» را بزنید.", "Tap “Refresh” for live quotes and LIT analysis.")
    val marketsAnalysing get() = t("هنوز سری داده‌ای برای این نماد دریافت نشده؛ بعد از به‌روزرسانی، تحلیل LIT اینجا ظاهر می‌شود.", "No series fetched for this symbol yet; LIT analysis appears here after refresh.")
    val marketEstimated get() = t("برآورد رسمی", "official estimate")
    val marketUsdtPanel get() = t("تتر در TradingView نماد استاندارد تومانی ندارد؛ قیمت زندهٔ بازار/برآورد رسمی در کارت بالا نمایش داده می‌شود.", "Tether has no standard Toman symbol on TradingView; the live/free-market or labelled official price stays in the card above.")
    fun litPlan(entry: Double, sl: Double, tp1: Double, tp2: Double, tp3: Double): String {
        val template = t(
            "نقشهٔ معامله: ورود %s | حد ضرر %s | اهداف: %s / %s / %s — ریسک به ریوارد حداقل ۱:۳",
            "Trade plan: entry %s | stop %s | targets %s / %s / %s — risk:reward floor 1:3"
        )
        return String.format(
            template,
            fmtNum(entry), fmtNum(sl), fmtNum(tp1), fmtNum(tp2), fmtNum(tp3)
        )
    }
    val litRiskNote get() = t(
        "⚠️ تحلیل آموزشی است نه سیگنال قطعی. اجرای واقعی فقط از درگاه متصل و با تأییدهای خود گیت‌وی انجام می‌شود؛ حالت پیش‌فرض شبیه‌سازی کاغذی است.",
        "⚠️ Educational analysis, not financial advice. Real routing only through a connected gateway with its own confirmations; paper simulation is the default."
    )
    val litAutoToggle get() = t("ترید خودکار LIT (از طریق درگاه ایمن)", "LIT auto-trade (via the safe gateway)")
    val litExecute get() = t("اجرای نقشهٔ LIT", "Execute the LIT plan")
    fun litTuningLabel(atr: Double, rsiHigh: Double, rsiLow: Double, targets: List<Double>): String {
        val ladder = targets.joinToString("/") { "%.1f".format(it) }
        return t(
            "🧬 تیونینگ موتور: ATR×%s | RSI %s/%s | اهداف %s (کف ۱:۳)",
            "🧬 Engine tuning: ATR×%s | RSI %s/%s | targets %s (floor 1:3)"
        ).let { tpl ->
            String.format(
                tpl,
                "%.1f".format(atr), "%.0f".format(rsiHigh), "%.0f".format(rsiLow), ladder
            )
        }
    }
    val litTuningScan get() = t("ارتقای خودکار از گیت‌هاب (اسکن استراتژی‌ها)", "Auto-evolve from GitHub (scan strategies)")
    val litTuningScanning get() = t("در حال یادگیری از گیت‌هاب…", "Learning from GitHub…")

    // Linked accounts (Instagram + Gmail-login sites)
    val sectionLinked get() = t("حساب‌های متصل", "Linked accounts")
    val linkedHint get() = t(
        "هندل اینستاگرام برای ایجنت محتوا استفاده می‌شود. نشست سایت‌ها فقط روی همین دستگاه در امن‌سپر ذخیره می‌شود. انتشار خودکار در اینستاگرام API رسمی ندارد؛ سایویس در حالت یاری‌گر عمل می‌کند.",
        "The Instagram handle feeds the content agent; pasted sessions stay in this device's Keystore. Instagram has no official auto-posting API for personal accounts — SAYVIS acts as an assisted agent."
    )
    val linkedInstagram get() = t("هندل اینستاگرام (بدون @)", "Instagram handle (without @)")
    val linkedSites get() = t("نشست سایت‌های متصل (JSON)", "Linked site sessions (JSON)")

    // Research agent
    val sectionAgent get() = t("ایجنت پژوهش", "Research agent")
    val toolAgent get() = t("ایجنت وب‌گردی سایویس", "SAYVIS web-browsing agent")
    val toolAgentHint get() = t(
        "جست‌وجو، بازکردن و خواندن صفحات وب و نتیجه‌گیری مستند — به‌علاوهٔ Gmail/تقویم/درایو وقتی وارد شوید",
        "Searches, opens and reads web pages, then answers with citations — plus Gmail/Calendar/Drive when signed in"
    )
    val agentGoalLabel get() = t("هدف پژوهش", "Research goal")
    val agentRun get() = t("اجرای ایجنت", "Run agent")
    val agentRunning get() = t("ایجنت در حال کار…", "Agent working…")
    val agentEmpty get() = t(
        "مثال: «قیمت لحظه‌ای طلا و دلیل رشد آن» یا «ایمیل‌های اخیرم». در دستیار هم با «ایجنت: …» کار می‌کند.",
        "e.g. “current gold price and why it is rising” or “my recent emails”. In the assistant too: “agent: …”."
    )
    val saved get() = t("ذخیره شد", "Saved")
    val vaultHardware get() = t("کلیدها در تراشهٔ امن دستگاه رمزنگاری شده‌اند", "Keys are encrypted inside the device secure hardware")
    val vaultSoftware get() = t("هشدار: تراشهٔ امن در دسترس نیست؛ کلیدها فقط مبهم‌سازی شده‌اند", "Warning: secure hardware unavailable — keys are obfuscated only")
    val aiActive get() = t("هوش مصنوعی فعال است", "AI is active")
    val aiInactive get() = t("هوش مصنوعی غیرفعال است", "AI is inactive")
    val apiStatusReady get() = t("آمادهٔ ارسال درخواست", "Ready to send requests")
    val apiStatusNeedsKey get() = t("نیاز به کلید API دارد", "Needs an API key")
    val getApiKeyGuide get() = t("راهنمای دریافت کلید", "How to get a key")
    val offlineMode get() = t("حالت آفلاین اجباری", "Force offline mode")
    val offlineModeHint get() = t("هیچ درخواستی به اینترنت فرستاده نمی‌شود", "No request will ever reach the internet")

    // --------------------------------------------------------------- trading UI
    val gatewayTitle get() = t("درگاه ترمینال ترید", "Trading Terminal Gateway")
    val gatewaySubtitle get() =
        t("اتصال امن سایویس به متاتریدر ۴ و ۵ از طریق پل ارتباطی", "Secure SAYVIS link to MetaTrader 4/5 through a bridge")
    val bridgeKind get() = t("نوع پل ارتباطی", "Bridge type")
    val terminalVersion get() = t("نسخهٔ ترمینال", "Terminal version")
    val accountType get() = t("نوع حساب", "Account type")
    val broker get() = t("نام بروکر", "Broker name")
    val server get() = t("سرور (مثلاً ICMarkets-Demo)", "Server (e.g. ICMarkets-Demo)")
    val login get() = t("شمارهٔ حساب (Login)", "Account number (login)")
    val password get() = t("رمز عبور", "Password")
    val bridgeToken get() = t("توکن پل / حساب", "Bridge / account token")
    val bridgeUrl get() = t("نشانی پل (REST یا WebSocket)", "Bridge address (REST or WebSocket)")
    val executionMode get() = t("سطح اجازهٔ اجرا", "Execution permission level")
    val executionModeHint get() =
        t("تا وقتی روی «شبیه‌سازی کاغذی» است، هیچ سفارشی به بروکر ارسال نمی‌شود",
            "While set to paper simulation, no order is ever routed to the broker")
    val maxDailyLoss get() = t("سقف زیان روزانه (دلار)", "Daily loss cap (USD)")
    val maxLot get() = t("بیشینهٔ حجم هر سفارش (لات)", "Max volume per order (lots)")
    val autoCloseOnDrawdown get() = t("بستن خودکار در رسیدن به سقف زیان", "Auto-flatten when the loss cap is hit")
    val connect get() = t("اتصال", "Connect")
    val disconnect get() = t("قطع اتصال", "Disconnect")
    val connecting get() = t("در حال اتصال…", "Connecting…")
    val reconnect get() = t("اتصال دوباره", "Reconnect")
    val testGateway get() = t("آزمون درگاه", "Test gateway")
    val statusDisconnected get() = t("قطع", "Disconnected")
    val statusConnecting get() = t("در حال اتصال", "Connecting")
    val statusConnected get() = t("متصل", "Connected")
    val statusError get() = t("خطا", "Error")
    val statusSimulated get() = t("شبیه‌سازی محلی", "Local simulation")

    val accountInfo get() = t("اطلاعات حساب", "Account information")
    val balance get() = t("موجودی", "Balance")
    val equity get() = t("ارزش خالص", "Equity")
    val freeMargin get() = t("مارجین آزاد", "Free margin")
    val margin get() = t("مارجین درگیر", "Used margin")
    val leverage get() = t("اهرم", "Leverage")
    val openPositions get() = t("پوزیشن‌های باز", "Open positions")
    val noPositions get() = t("پوزیشن باز وجود ندارد", "No open positions")
    val marketWatch get() = t("دیدبان بازار", "Market watch")
    val bid get() = t("قیمت فروش", "Bid")
    val ask get() = t("قیمت خرید", "Buy price")
    val symbol get() = t("نماد", "Symbol")
    val volume get() = t("حجم", "Volume")
    val orderType get() = t("نوع سفارش", "Order type")
    val buy get() = t("خرید", "Buy")
    val sell get() = t("فروش", "Sell")
    val closePosition get() = t("بستن پوزیشن", "Close position")
    val openOrder get() = t("ثبت سفارش", "Place order")
    val orderBlockedPaper get() =
        t("ارسال سفارش مسدود است: سطح اجرا روی «شبیه‌سازی کاغذی» تنظیم شده.",
            "Order routing blocked: execution level is set to paper simulation.")
    val orderBlockedLock get() =
        t("ارسال سفارش مسدود است: قفل اضطراری فعال است.", "Order routing blocked: the emergency lock is engaged.")
    val liveWarningTitle get() = t("هشدار اجرای زنده", "Live execution warning")
    val liveWarningBody get() =
        t("اجرای زنده یعنی پول واقعی درگیر می‌شود و خطا برگشت‌ناپذیر است. این سطح فقط با تأیید صریح شما و پس از ثبت در گزارش ممیزی فعال می‌ماند.",
            "Live execution commits real money and mistakes are irreversible. This level stays enabled only with your explicit confirmation and is written to the audit log.")
    val iUnderstand get() = t("می‌فهمم و تأیید می‌کنم", "I understand and confirm")
    val gatewayHowTo get() = t("چطور متصل شوم؟", "How do I connect?")

    // --------------------------------------------------------------- scripting
    val scriptsTitle get() = t("اسکریپت و خودکارسازی", "Scripts & Automation")
    val scriptsSubtitle get() =
        t("قانون‌های خودتان را بنویسید تا سایو به‌صورت خودکار واکنش نشان دهد.",
            "Write your own rules so SAYO reacts automatically.")
    val newScript get() = t("اسکریپت تازه", "New script")
    val editScript get() = t("ویرایش اسکریپت", "Edit script")
    val scriptName get() = t("نام اسکریپت", "Script name")
    val scriptDescription get() = t("توضیح کوتاه", "Short description")
    val scriptSource get() = t("کد", "Code")
    val scriptTrigger get() = t("شرط اجرا", "Trigger")
    val runScript get() = t("اجرا", "Run")
    val saveScript get() = t("ذخیرهٔ اسکریپت", "Save script")
    val deleteScript get() = t("حذف اسکریپت", "Delete script")
    val enableScript get() = t("فعال", "Enabled")
    val scriptOutput get() = t("خروجی اجرا", "Run output")
    val scriptConsole get() = t("کنسول", "Console")
    val scriptReference get() = t("راهنمای زبان اسکریپت", "Scripting language reference")
    val noScripts get() = t("هنوز اسکریپتی نساخته‌اید", "You have not written a script yet")
    val scriptInvalidName get() = t("نام اسکریپت خالی است", "Script name is empty")
    val scriptParseError get() = t("خطای نگارشی در کد", "Syntax error in code")
    val scriptRanOk get() = t("اسکریپت با موفقیت اجرا شد", "Script ran successfully")
    val scriptsDisabled get() = t("اجرای خودکار اسکریپت‌ها در تنظیمات غیرفعال است", "Automatic script execution is disabled in settings")
    val askAiToWrite get() = t("از سایو بخواه بنویسد", "Ask SAYO to write it")

    // ---------------------------------------------------------------- security
    val emergencyLock get() = t("قفل اضطراری", "Emergency lock")
    val emergencyLockOn get() = t("قفل اضطراری فعال است", "Emergency lock engaged")
    val emergencyLockOff get() = t("امنیت عادی", "Normal security")
    val requireHighRiskConfirm get() = t("تأیید اجباری برای اقدامات پرخطر", "Require confirmation for high-risk actions")
    val keepAudit get() = t("نگه‌داشتن گزارش ممیزی روی دستگاه", "Keep the audit log on-device")
    val auditLog get() = t("گزارش رویدادها", "Event log")
    val devices get() = t("دستگاه‌ها", "Devices")
    val revokeDevice get() = t("لغو دسترسی دستگاه", "Revoke device access")
    val trusted get() = t("مورد اعتماد", "Trusted")
    val revoked get() = t("لغوشده", "Revoked")

    // -------------------------------------------------------------- home & chat
    val homeGreetingFa get() = "خوش آمدید"
    val homeGreeting get() = t("خوش آمدید", "Welcome")
    val todayOverview get() = t("نمای امروز", "Today at a glance")
    val activeMissions get() = t("مأموریت فعال", "Active missions")
    val blockedTasks get() = t("وظیفهٔ مسدود", "Blocked tasks")
    val cognitiveLoad get() = t("بار شناختی", "Cognitive load")
    val focusWindow get() = t("پنجرهٔ تمرکز", "Focus window")
    val networkStatus get() = t("وضعیت شبکه", "Network status")
    val online get() = t("متصل به اینترنت", "Online")
    val offline get() = t("آفلاین", "Offline")
    val quickAsk get() = t("سؤال سریع از سایو…", "Quick question for SAYO…")
    val send get() = t("ارسال", "Send")
    val chatPlaceholder get() = t("پیام خود را بنویسید…", "Type your message…")
    val thinking get() = t("در حال فکر کردن…", "Thinking…")
    val you get() = t("شما", "You")
    val suggestions get() = t("پیشنهادها", "Suggestions")
    val approve get() = t("تأیید", "Approve")
    val dismiss get() = t("رد کردن", "Dismiss")
    val retry get() = t("تلاش دوباره", "Retry")
    val runScan get() = t("بررسی محیطی", "Run context scan")

    // ------------------------------------------------------------- common verbs
    val ok get() = t("تأیید", "OK")
    val cancel get() = t("انصراف", "Cancel")
    val close get() = t("بستن", "Close")
    val save get() = t("ذخیره", "Save")
    val edit get() = t("ویرایش", "Edit")
    val delete get() = t("حذف", "Delete")
    val add get() = t("افزودن", "Add")
    val search get() = t("جست‌وجو", "Search")
    val refresh get() = t("به‌روزرسانی", "Refresh")
    val copy get() = t("کپی", "Copy")
    val copied get() = t("کپی شد", "Copied")
    val optional get() = t("اختیاری", "optional")
    val required get() = t("الزامی", "required")
    val none get() = t("هیچ", "None")
    val all get() = t("همه", "All")
    val details get() = t("جزئیات", "Details")
    val apply get() = t("اعمال", "Apply")
    val reset get() = t("بازنشانی", "Reset")
    val resetAll get() = t("بازنشانی همهٔ تنظیمات", "Reset all settings")
    val resetAllWarning get() =
        t("همهٔ تنظیمات، کلیدهای API و پروفایل معاملاتی پاک می‌شوند. گزارش ممیزی حفظ می‌ماند.",
            "All settings, API keys and the trading profile will be cleared. The audit log is kept.")
    val loading get() = t("در حال بارگذاری…", "Loading…")
    val translating get() = t("در حال ترجمه…", "Translating…")
    val machineTranslated get() = t("ترجمهٔ ماشینی", "machine-translated")
    val notTranslated get() = t("ترجمه نشد", "Not translated")
    val errorGeneric get() = t("خطای ناشناخته", "Unknown error")
    val version get() = t("نسخه", "Version")

    // ------------------------------------------------------------------ numbers
    val percent get() = t("٪", "%")
    val usd get() = t("دلار", "USD")
    val seconds get() = t("ثانیه", "s")
    val items get() = t("مورد", "items")

    // --------------------------------------------------- status / risk vocabulary
    fun risk(levelName: String): String = when (levelName) {
        "LOW_RISK" -> t("کم‌خطر", "Low risk")
        "MEDIUM_RISK" -> t("خطر متوسط", "Medium risk")
        "HIGHER_RISK" -> t("پرخطر", "High risk")
        "CRITICAL" -> t("بحرانی", "Critical")
        else -> levelName
    }

    fun cognitive(levelName: String): String = when (levelName) {
        "OPTIMAL" -> t("بهینه", "Optimal")
        "ELEVATED" -> t("بالا", "Elevated")
        "FATIGUE_RISK" -> t("خستگی شناختی", "Fatigue risk")
        else -> levelName
    }

    // -------------------------------------- floating avatar & ambient listening
    val toolRobot get() = t("ربات سایویس (زنده)", "SAYVIS Robot (live)")
    val toolRobotHint get() = t(
        "چهرهٔ زندهٔ رباتِ لوگو: درخشش چشم‌ها، تنفس و رنگ بنفش دهان همراه تفکر عوض می‌شود",
        "The logo robot, live: glowing eyes, breathing and the purple mouth glow shift while thinking"
    )
    val robotTitle get() = t("ربات سایویس — زنده", "SAYVIS Robot — Live")
    val robotHintLive get() = t(
        "رنگ چشم‌ها، تنفس کروم، دهان بنفش و هستهٔ سینه با وضعیت هوش و صدای محیط زنده تغییر می‌کنند.",
        "Eye colours, the chrome breathing, the purple mouth vent and the chest core follow the AI state and ambient sound live."
    )
    val robotPreview get() = t("پیش‌نمایش وضعیت (لمس کنید)", "State preview (tap)")
    val robotAsk get() = t("پرسش آزمایشی از دستیار", "Ask the assistant a test question")
    val robotStateIdle get() = t("آماده", "Idle")
    val robotStateThinking get() = t("در حال تفکر", "Thinking")
    val robotStateSpeaking get() = t("در حال گفتار", "Speaking")
    val robotStateAware get() = t("هوشیار", "Aware")
    val robotStateLocked get() = t("قفل اضطراری", "Locked")
    val robotStateOffline get() = t("آفلاین", "Offline")
    val toolAvatarListen get() = t("آواتار شناور و شنیدار", "Floating Avatar & Listening")
    val toolAvatarListenHint get() =
        t("حبابک همیشگی روی هوم گوشی، میکروفون زنده و شناسایی صدای شما", "A persistent bubble on your Home screen with a live mic that knows your voice")
    val avatarTitle get() = t("آواتار شناور و شنیدار لحظه‌ای", "Floating Avatar & Ambient Listening")
    val avatarSubtitle get() = t(
        "حبابک کوچک سایویس بعد از رفتن به هوم هم روی صفحه می‌مانَد، صدای محیط را می‌شنود و صدای شما را می‌شناسد.",
        "The small SAYVIS bubble stays on screen after you press Home, hears ambient sound and recognises your voice."
    )
    val avatarStatusTitle get() = t("وضعیت شنیدار", "Listening status")
    val avatarModeOff get() = t("غیرفعال", "Off")
    val avatarModeListening get() = t("در حال شنیدن محیط…", "Listening to the surroundings…")
    val avatarModeOwner get() = t("صدای شما شناسایی شد!", "Your voice was recognised!")
    val avatarLevel get() = t("سطح صدای محیط", "Ambient sound level")
    val avatarRunToggle get() = t("آواتار شناور و میکروفون زنده", "Floating avatar & live microphone")
    val avatarRunHint get() = t(
        "با رفتن به هوم، حبابک روی صفحه می‌ماند و به شنیدن ادامه می‌دهد.",
        "The bubble stays on screen over the launcher and keeps listening."
    )
    val avatarTapHint get() = t(
        "لمس = بازشدن سایویس • کشیدن = جابه‌جایی • نگه‌داشتن = بستن",
        "Tap = open SAYVIS • drag = move • hold = stop"
    )
    val avatarPermTitle get() = t("دسترسی‌های لازم", "Required permissions")
    val avatarPermMic get() = t("میکروفون", "Microphone")
    val avatarPermOverlay get() = t("نمایش روی برنامه‌های دیگر", "Display over other apps")
    val avatarPermNotif get() = t("آگاهی‌ساز (نشان شنیدار)", "Notification (listening badge)")
    val avatarPermGrant get() = t("اجازه دادن", "Grant")
    val avatarPermGranted get() = t("داده شد", "Granted")
    val avatarPermMissing get() = t("داده نشده", "Missing")
    val avatarEnrollTitle get() = t("شناسنامهٔ صوتی من", "My voice-print")
    val avatarEnrollHint get() = t(
        "در محیط آرام، سه بار یک جملهٔ ثابت بخوانید (مثلاً: «سایویس، خودت را معرفی کن»). همه‌چیز فقط روی همین دستگاه می‌ماند.",
        "In a quiet room, read the same short phrase three times (e.g. \"SAYVIS, introduce yourself\"). Everything stays on this device only."
    )
    val avatarEnrollStart get() = t("شروع ثبت نمونه", "Start capturing a sample")
    val avatarEnrollNext get() = t("ثبت نمونهٔ بعدی", "Capture the next sample")
    val avatarCapturing get() = t("در حال ضبط… صحبت کنید", "Recording… speak now")
    val avatarEnrollDone get() = t("شناسنامهٔ صوتی شما ثبت شد ✅", "Your voice-print is enrolled ✅")
    val avatarEnrolledAt get() = t("تاریخ ثبت", "Enrolled on")
    val avatarReEnroll get() = t("ثبت دوباره", "Re-enrol")
    val avatarDeletePrint get() = t("حذف شناسنامهٔ صوتی", "Delete voice-print")
    val avatarNoPrintYet get() = t(
        "هنوز شناسنامهٔ صوتی ثبت نشده؛ فقط سطح صدای محیط پایش می‌شود.",
        "No voice-print yet — only the ambient level is being monitored."
    )
    val avatarTestTitle get() = t("آزمون شناسایی", "Recognition test")
    val avatarTestHint get() = t(
        "۶ ثانیه میکروفون باز می‌ماند؛ همان جملهٔ ثبت‌شده را بخوانید.",
        "The mic stays open for 6 seconds; read your enrolled phrase."
    )
    val avatarTestRun get() = t("اجرای آزمون", "Run test")
    val avatarThreshold get() = t("حساسیت شناسایی", "Recognition sensitivity")
    val avatarThresholdLow get() = t("سخت‌گیر", "Strict")
    val avatarThresholdHigh get() = t("آسان‌گیر", "Permissive")
    val avatarHistoryTitle get() = t("شناسایی‌های اخیر", "Recent recognitions")
    val avatarHistoryEmpty get() = t("هنوز صدایی شناسایی نشده است.", "No voice recognised yet.")
    val avatarPrivacyNote get() = t(
        "حریم خصوصی: پردازش صدا کاملاً روی دستگاه است؛ هیچ صدایی ذخیره یا ارسال نمی‌شود و نشانگر میکروفون اندروید همیشه روشن است. برای توقف، آواتار را نگه دارید یا از آگاهی‌ساز استفاده کنید.",
        "Privacy: audio processing is fully on-device — nothing is stored or uploaded, and Android's microphone indicator stays lit. Hold the avatar or use the notification to stop."
    )
    val avatarMicUnavailable get() = t("میکروفون در دسترس نیست.", "Microphone unavailable.")
    val avatarTooNoisy get() = t(
        "صدای واضحی ضبط نشد؛ در محیط آرام‌تر دوباره تلاش کنید.",
        "No clear voice was captured; try again somewhere quieter."
    )
    fun avatarSampleCaptured(n: Int): String =
        t("نمونهٔ شمارهٔ $n ثبت شد؛ نمونهٔ بعدی را ضبط کنید.", "Sample $n captured; record the next one.")
    fun avatarTestOk(percent: String): String =
        t("صدای شما با اطمینان $percent٪ شناخته شد.", "Your voice matched with $percent% confidence.")
    fun avatarTestNo(percent: String): String =
        t("مطابقت کافی نبود ($percent٪). دوباره ثبت کنید یا حساسیت را بالا ببرید.", "Not a confident match ($percent%). Re-enrol or raise the sensitivity.")

    // ---- v4.0.0 connect centre / rebuilt network / QR pairing ----
    val connectHubTitle get() = t("مرکز اتصال سایویس", "SAYVIS Connect Centre")
    val connectHubHint get() = t(
        "یک جا برای همهٔ اتصال‌ها: اکانت گوگل شما مرکز همهٔ ورودها و هوش مصنوعی سایویس است؛ شبکه هم بازسازی شده و همهٔ درخواست‌ها از یک موتور مشترک با هویت اکانت شما می‌روند.",
        "One place for every connection: your Google account is the hub of all logins and of SAYVIS's AI usage, and the network layer is rebuilt around one shared engine carrying your account identity."
    )
    val connectNetChecking get() = t("در حال بررسی اتصال جهانی…", "Checking global connectivity…")
    val connectNetNote get() = t(
        "موتور شبکهٔ بازسازی‌شده: اتصال مشترک، تلاش مجدد خودکار، سرآیند هویت اکانت",
        "Rebuilt network engine: shared client, automatic retries, account identity header"
    )
    val connectNetRetry get() = t("بررسی دوباره", "Re-check")
    val connectAccountTitle get() = t("اکانت گوگل — مرکز هویت", "Google account — identity hub")
    val connectAccountHint get() = t(
        "با یک لمس، اکانت گوگل خود را انتخاب کنید؛ بدون هیچ تنظیمات اضافه‌ای. بعد از آن همهٔ ورودها و هوش مصنوعی زیر همین اکانت می‌ماند.",
        "Pick your Google account with one tap — no extra setup. Every login and all AI usage then runs under this account."
    )
    val connectGoogleButton get() = t("ادامه با اکانت گوگل", "Continue with Google account")
    val connectAccountNote get() = t(
        "مرکز همهٔ ورودها و مصرف هوش مصنوعی سایویس",
        "Hub of all logins and of SAYVIS's AI usage"
    )
    val connectSignOut get() = t("خروج از اکانت", "Sign out")
    val connectQrTitle get() = t("اتصال با QR — اسکن کامپیوتر", "QR connection — computer scan")
    val connectQrHint get() = t(
        "این QR اکانت و دستگاه شماست؛ با اسکنر کامپیوتر یا گوشی دیگر بخوانیدش تا پیوند بخورد. یا برعکس: هر QR ساخته‌شده در کامپیوتر (کلید Gemini، کانفیگ، لینک) را اسکن کنید.",
        "This QR holds your account + device; read it with any computer/phone scanner to pair. The other way round, scan any computer-made QR (Gemini key, config, link)."
    )
    val connectQrNeedAccount get() = t(
        "برای ساخت QR اتصال، اول اکانت گوگل را پیوند دهید.",
        "Link your Google account first to generate the pairing QR."
    )
    val connectPinLabel get() = t("کد جفت‌سازی دستگاه:", "Device pairing pin:")
    val connectScan get() = t("اسکن QR از کامپیوتر", "Scan QR from computer")
    val connectScanHintLive get() = t(
        "QR را روی صفحهٔ کامپیوتر نشان دهید",
        "Show the QR on your computer screen"
    )
    val connectScanPermission get() = t("برای اسکن، دسترسی دوربین لازم است.", "Camera permission is needed to scan.")
    val connectScanGrant get() = t("اجازهٔ دوربین", "Grant camera")
    val connectPasteHint get() = t("محتوای QR را همین‌جا بچسبانید…", "Paste QR content here…")
    val connectImport get() = t("ثبت محتوای QR", "Apply QR content")
    val connectInvalid get() = t("محتوای QR قابل استفاده نبود.", "The QR content could not be used.")
    val connectLater get() = t("فعلاً بعداً", "Maybe later")
    val connectManualTitle get() = t("پیوند اکانت گوگل", "Link your Google account")
    val connectManualHint get() = t(
        "انتخاب‌گر اکانت گوگل در دسترس نبود؛ ایمیل اکانت خود را وارد کنید (همه‌چیز محلی می‌ماند).",
        "The Google account sheet was unavailable; enter your account e-mail (everything stays on-device)."
    )
    val connectManualLink get() = t("پیوند دستی اکانت", "Link manually")

    // ---- v5.0.0 assistant brain / device accounts / MTF / sports / manager ----
    val assistantBrainLabel get() = t("ایجنت/مغز دستیار", "Assistant agent / brain")
    val brainAuto get() = t("خودکار", "Auto")
    val brainActive get() = t("مغز فعال:", "Active brain:")
    val connectDeviceAccountsTitle get() = t("اکانت‌های گوگل این دستگاه", "Google accounts on this device")
    val connectDeviceAccountsHint get() = t(
        "حساب‌هایی که با گوگل روی همین گوشی وارد شده‌اند؛ با یک لمس به‌عنوان هویت سایویس به‌کار گرفته می‌شوند.",
        "Accounts signed in with Google on this phone; one tap adopts one as the SAYVIS identity."
    )
    val connectLinkBtn get() = t("به‌کارگیری", "Use this one")
    val mtfTitle get() = t("نقشهٔ مولتی‌تایم‌فریم و نقطهٔ ورود", "Multi-timeframe map & entry hunt")
    val mtfHint get() = t(
        "چارت در تایم‌فریم‌های ۱۵دقیقه/۱ساعته/۴ساعته/روزانه بررسی می‌شود؛ با همگرایی حداقل دو تایم‌فریم (و مخالفت‌نکردن تایم‌فریم سنگین) نقطهٔ ورود با کف سخت RR≥1:3 پیشنهاد می‌شود.",
        "The chart is examined on 15m/1h/4h/1d; an entry (hard RR≥1:3 floor) needs ≥2 agreeing timeframes and no heavy-TF opposition."
    )
    val mtfScan get() = t("اسکن نقطهٔ ورود", "Scan for entries")
    val mtfScanning get() = t("در حال بررسی چندتایم‌فریمی…", "Scanning timeframes…")
    val mtfNoData get() = t("هنوز دادهٔ کافی نیست؛ دوباره تلاش کنید.", "Not enough data yet; try again.")
    val mtfPlanTitle get() = t("برنامهٔ ورود پیشنهادی", "Proposed entry plan")
    val mtfAgree get() = t("هم‌نظر:", "Agreeing:")
    val mtfOppose get() = t("مخالف:", "Opposing:")
    val sportsTitle get() = t("پیشنهاد ورزشی بر اساس سلیقهٔ شما", "Sports picks from your taste")
    val sportsHint get() = t(
        "بر اساس ۱۰۰ جستجوی اخیر شما در سایویس؛ برای استفاده از تاریخچهٔ گوگل خودتان، متن فعالیت اخیر (myactivity.google.com) را بچسبانید — دسترسی مستقیم گوگل برای هیچ اپی وجود ندارد.",
        "From your last 100 in-SAYVIS searches; to use your own Google history, paste recent activity from myactivity.google.com — no app can read it directly."
    )
    val sportsImport get() = t("چسباندن فعالیت اخیر گوگل (اختیاری)", "Paste recent Google activity (optional)")
    val sportsImportBtn get() = t("شخصی‌سازی", "Personalise")
    val managerTitle get() = t("ایجنت مدیر — دفترچهٔ بیزینس", "Manager agent — business directory")
    val managerSmsHint get() = t(
        "همهٔ پیامک‌های روی همین دستگاه خوانده می‌شوند؛ فرستندگان بیزینس — به‌ویژه تأمین‌کننده‌های مواد اولیه — با نام و فامیل، شرکت، شماره، سایت و آدرس در دسته‌های تأمین/پخش/اداره/تفریح دسته‌بندی می‌شوند. همه‌چیز روی دستگاه می‌ماند.",
        "Every on-device SMS is read; business senders — especially raw-material suppliers — are classified (supplier / distribution / administration / entertainment) with name, company, phone, site and address. Everything stays on-device."
    )
    val managerScan get() = t("اسکن همهٔ پیامک‌ها", "Scan all SMS")
    val managerScanning get() = t("در حال خواندن و دسته‌بندی…", "Reading & classifying…")
    val managerPermission get() = t("برای این کار، دسترسی پیامک را تأیید کنید.", "Grant the SMS permission first.")
    val managerGrant get() = t("اجازهٔ پیامک", "Grant SMS")
    val managerInstaTitle get() = t("تحلیل اینستاگرام (بیو/فالوور/فالوئینگ)", "Instagram analysis (bio / followers / following)")
    val managerInstaHint get() = t(
        "متن بیو و آمار صفحه را بدهید تا بیزینس بودن، دسته و شواهد استخراج شود (API اینستاگرام توکن بیزینسی می‌خواهد؛ این مسیر روی دستگاه است).",
        "Provide the bio text and account stats to extract business type, category and signals (Instagram's API needs a business token; this path is on-device)."
    )
    val managerInstaHandle get() = t("هندل", "Handle")
    val managerInstaBio get() = t("متن بیو/پروفایل", "Bio / profile text")
    val managerInstaCtx get() = t("فالوور/فالوئینگ (اختیاری)", "Followers/following (optional)")
    val managerInstaAdd get() = t("تحلیل و افزودن", "Analyse & add")
    val managerEmpty get() = t("دفترچه خالی است؛ اسکن بزنید.", "The directory is empty; run a scan.")
    val brainKindChipsLabel get() = t("کدام ایجنت پاسخ دهد؟", "Which agent answers?")

    private fun fmtNum(v: Double): String = if (v >= 1000.0) "%.1f".format(v) else "%.4f".format(v)

    companion object {
        val English = SayvisStrings(false)
        val Persian = SayvisStrings(true)
        fun of(isPersian: Boolean): SayvisStrings = if (isPersian) Persian else English

        /** True when the device locale is a Persian-speaking locale. */
        fun deviceIsPersian(): Boolean {
            val code = Locale.getDefault().language.lowercase(Locale.ROOT)
            return code == "fa" || code == "pr" || code == "ps" || code == "tg" || code == "ku"
        }
    }
}

/** CompositionLocal carrying the active dictionary for the whole tree. */
val LocalStrings = compositionLocalOf<SayvisStrings> { SayvisStrings.English }
