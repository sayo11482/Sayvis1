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

    val sectionLanguage get() = t("زبان و نمایش", "Language & Appearance")
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

    // ---------------------------------------------- live screen translation (PUR)
    val toolScreenTranslate get() = t("مترجم زندهٔ صفحه", "Live Screen Translator")
    val toolScreenTranslateHint get() = t(
        "متن انگلیسی روی صفحه، در لحظه سر جای خودش فارسی می‌شود",
        "English text on screen is replaced with Persian in place, live"
    )
    val stTitle get() = t("مترجم زندهٔ صفحه", "Live Screen Translator")
    val stSubtitle get() = t(
        "هر متن انگلیسی که روی صفحه می‌بینید، در همان نقطه از تصویر با فارسی جایگزین می‌شود — بدون کپی‌کردن، بدون انتظار.",
        "Every piece of English on the screen is replaced with Persian exactly where it sits — nothing to copy, nothing to wait for."
    )
    val stStart get() = t("روشن کردن ترجمهٔ زنده", "Turn on live translation")
    val stStop get() = t("پایان ترجمه", "Stop translation")
    val stPause get() = t("توقف موقت", "Pause")
    val stResume get() = t("ادامه", "Resume")
    val stPhaseLabel get() = t("وضعیت سرویس", "Service state")
    val stPermissionsTitle get() = t("دسترسی‌های لازم", "Required permissions")
    val stOverlayPermission get() = t("نمایش روی برنامه‌های دیگر", "Display over other apps")
    val stOverlayPermissionHint get() = t(
        "تا فارسی را دقیقاً روی متن انگلیسی بنویسد، سایویس باید روی صفحهٔ برنامه‌های دیگر ترسیم کند.",
        "SAYVIS draws over other apps so the Persian lands exactly on the English it replaces."
    )
    val stCapturePermission get() = t("ضبط صفحه", "Screen capture")
    val stCapturePermissionHint get() = t(
        "برای خواندن متن روی صفحه. اندروید اجازهٔ ضبط را برای هر جلسه از شما می‌پرسد؛ سایویس تصویر صفحه را جایی نمی‌فرستد.",
        "Needed to read the text on the screen. Android asks for this consent every session; SAYVIS never uploads the picture."
    )
    val stNotificationPermission get() = t("اعلان‌ها", "Notifications")
    val stNotificationPermissionHint get() = t(
        "یک ترجمهٔ دائمی باید همیشه در اعلان‌ها دیده شود تا بتوانید هر لحظه متوقفش کنید.",
        "A permanent translator must stay visible in the notification shade so you can stop it any time."
    )
    val stGrant get() = t("اعطای دسترسی", "Grant")
    val stGranted get() = t("داده شده", "Granted")
    val stOpenSystemSettings get() = t("تنظیمات سیستم", "System settings")
    val stNeedAllPermissions get() = t(
        "برای شروع، «نمایش روی برنامه‌های دیگر» و اجازهٔ ضبط صفحه لازم است.",
        "To start, the overlay permission and screen-capture consent are both required."
    )
    val stAppearanceTitle get() = t("ظاهر ترجمه", "Translation appearance")
    val stModeLabel get() = t("شیوهٔ نمایش", "Display mode")
    val stGranularityLabel get() = t("واحد ترجمه", "Translation unit")
    val stPlateStyle get() = t("پلاک پشت نوشته", "Plate behind the text")
    val stPlateOpacity get() = t("شفافیت پلاک (٪)", "Plate opacity (%)")
    val stTextColor get() = t("رنگ نوشته", "Text colour")
    val stTextScale get() = t("اندازهٔ نوشته (٪)", "Text scale (%)")
    val stPersianNumbers get() = t("اعداد فارسی در ترجمه", "Persian digits inside translations")
    val stPerformanceTitle get() = t("کارایی و باتری", "Performance & battery")
    val stPollIntervalLabel get() = t("فاصلهٔ خواندن صفحه (میلی‌ثانیه)", "Screen read interval (ms)")
    val stPollIntervalHint get() = t(
        "کمتر = به‌روزرسانی سریع‌تر، بیشتر = مصرف کمتر باتری",
        "Lower = snappier, higher = cooler and lighter on the battery"
    )
    val stMaxPerFrame get() = t("بیشینهٔ عبارت در هر تصویر", "Max phrases per frame")
    val stMaxPerRequest get() = t("بیشینهٔ عبارت در هر درخواست هوش مصنوعی", "Max phrases per AI request")
    val stMinWordLength get() = t("کوتاه‌ترین واژهٔ قابل ترجمه (نویسه)", "Shortest translatable word (characters)")
    val stBubble get() = t("حباب کنترل شناور", "Floating control bubble")
    val stBubbleHint get() = t(
        "یک ضربه: توقف/ادامه • کشیدن: جابه‌جایی • نگه‌داشتن: پایان ترجمه",
        "Tap: pause/resume • Drag: move • Hold: stop translating"
    )
    val stBubbleOpacity get() = t("شفافیت حباب (٪)", "Bubble opacity (%)")
    val stKeepAwake get() = t("روشن نگه داشتن صفحه", "Keep the screen awake")
    val stResumeBoot get() = t("یادآوری پس از راه‌اندازی دوبارهٔ دستگاه", "Remind me after a restart")
    val stResumeBootHint get() = t(
        "اندروید اجازهٔ ضبط را در هر جلسه دوباره می‌پرسد؛ این گزینه فقط یادآوری می‌کند.",
        "Android always asks for capture consent again; this only reminds you."
    )
    val stDictionaryOnly get() = t("فقط واژه‌نامهٔ آفلاین (بدون اینترنت)", "Offline dictionary only (no internet)")
    val stDictionaryOnlyHint get() = t(
        "هیچ متنی به سرویس هوش مصنوعی فرستاده نمی‌شود؛ ترجمه فقط از واژه‌نامهٔ روی دستگاه می‌آید.",
        "No sentence ever leaves the device; translations come from the on-device glossary only."
    )
    val stCacheTranslations get() = t("ذخیرهٔ ترجمه‌ها برای دفعهٔ بعد", "Reuse translations next time")
    val stSingleAppCapture get() = t("اجازهٔ انتخاب یک برنامهٔ مشخص (اندروید ۱۴+)", "Let me pick a single app (Android 14+)")
    val stSkipOwnApp get() = t("نادیده گرفتن صفحهٔ خود سایویس", "Skip SAYVIS's own screens")
    val stStatsTitle get() = t("آمار زندهٔ جلسه", "Live session statistics")
    val stStatFrames get() = t("تصویر خوانده‌شده", "Frames read")
    val stStatSkipped get() = t("تصویر ردشده (بدون تغییر)", "Frames skipped (unchanged)")
    val stStatSegments get() = t("عبارت نوشته‌شده روی صفحه", "Phrases written on screen")
    val stStatOcr get() = t("زمان تشخیص متن", "Text recognition time")
    val stStatDictionary get() = t("واژه‌های واژه‌نامهٔ آفلاین", "Offline dictionary entries")
    val stStatCache get() = t("جمله‌های آموخته‌شده", "Learned sentences")
    val stStatOnline get() = t("سرویس هوش مصنوعی", "AI provider")
    val stOnlineReady get() = t("آماده (متن آزاد با هوش مصنوعی)", "Ready (free text via AI)")
    val stOnlineOffline get() = t("فقط واژه‌نامهٔ آفلاین", "Offline dictionary only")
    val stRecentTitle get() = t("آخرین چیزهایی که روی صفحه ترجمه شد", "What was translated most recently")
    val stNothingTranslated get() = t(
        "هنوز چیزی ترجمه نشده است؛ ترجمهٔ زنده را روشن کنید و صفحه را عوض کنید.",
        "Nothing translated yet — turn on live translation and change the screen."
    )
    val stProtectedContent get() = t(
        "این تصویر محافظت‌شده است (برنامهٔ بانکی یا مشابه)؛ اندروید اجازهٔ خواندن آن را نمی‌دهد.",
        "This screen is protected (a bank or similar app); Android does not allow reading it."
    )
    val stTesterTitle get() = t("آزمودن مترجم", "Try the translator")
    val stTesterHint get() = t(
        "یک متن انگلیسی بنویسید تا همین حالا ببینید سایویس چه می‌کند و ترجمه از کجا آمده است.",
        "Type any English text to see what SAYVIS does with it — and which tier produced it."
    )
    val stTestMethod get() = t("ترجمه کن", "Translate")
    val stSourceText get() = t("متن اصلی", "Source text")
    val stResultText get() = t("ترجمهٔ فارسی", "Persian translation")
    val stTierLabel get() = t("منبع ترجمه", "Produced by")
    val stTierDictionary get() = t("واژه‌نامهٔ آفلاین", "Offline dictionary")
    val stTierCache get() = t("حافظهٔ ترجمه", "Translation cache")
    val stTierMachine get() = t("سرویس هوش مصنوعی", "AI provider")
    val stTierNone get() = t("ترجمه نشد", "No translation")
    val stPrivacyTitle get() = t("حریم خصوصی و امنیت", "Privacy & security")
    val stPrivacyBody get() = t(
        "تصویر صفحه فقط روی همین دستگاه پردازش می‌شود و هرگز جایی فرستاده نمی‌شود. " +
            "فقط متنِ شناسایی‌شده و تنها در صورتی که «فقط واژه‌نامهٔ آفلاین» و «حالت آفلاین» خاموش باشند، " +
            "برای ترجمهٔ جمله‌های تازه به سرویس هوش مصنوعی خودتان می‌رود. " +
            "با فعال شدن قفل اضطراری، ترجمهٔ زنده فوراً متوقف و مسدود می‌شود و همهٔ رخدادها در زنجیرهٔ ممیزی ثبت می‌گردد.",
        "The screen image is processed on this device only and is never uploaded. " +
            "Only recognised text — and only when both \"offline dictionary only\" and \"force offline\" are off — " +
            "is sent to the AI provider you configured, to translate sentences the dictionary does not know. " +
            "Engaging the emergency lock stops and blocks live translation immediately, and every transition is written to the audit chain."
    )
    val stEmergencyBlocked get() = t(
        "قفل اضطراری سایویس فعال است؛ ترجمهٔ زندهٔ صفحه مسدود شده است.",
        "The SAYVIS emergency lock is engaged; live screen translation is blocked."
    )
    val stConsentDenied get() = t(
        "اجازهٔ ضبط صفحه داده نشد. برای فعال‌سازی، دوباره تلاش کنید و «شروع ضبط» را بپذیرید.",
        "Screen-capture consent was not granted. Try again and accept the capture prompt."
    )

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
