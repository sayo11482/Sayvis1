package com.odin.agent.trading

/**
 * ODIN v1.0.27 - Software Gap Analyzer - Odin.trade
 * تحلیل عملکرد نرم‌افزار - چه چیزهایی کم داره تا نسخه کارآمدتر و سودسازتر بشه
 * ارزیابی واقعی ماژول‌ها - اولویت‌بندی بر اساس تاثیر روی سود
 */

enum class GapStatus(val labelFa: String) {
    EXISTS("موجود"), PARTIAL("نیمه‌کاره"), MISSING("نصب نیست")
}

enum class GapImpact(val labelFa: String, val score: Int) {
    CRITICAL("حیاتی - مستقیم روی سود", 100),
    HIGH("بالا", 70),
    MEDIUM("متوسط", 40),
    LOW("کم", 15)
}

data class SoftwareGap(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val category: String,
    val status: GapStatus,
    val impact: GapImpact,
    val effortDays: Int,
    val descriptionFa: String,
    val solutionFa: String
) {
    val priorityScore: Int get() = impact.score + (if (status == GapStatus.MISSING) 50 else if (status == GapStatus.PARTIAL) 20 else 0) - effortDays
}

class SoftwareGapAnalyzer {

    fun analyze(): List<SoftwareGap> = gaps.sortedByDescending { it.priorityScore }

    val gaps: List<SoftwareGap> = listOf(
        // ---------- اجرای معامله ----------
        SoftwareGap(
            "REAL_ORDER_EXECUTION", "Real MT5 Order Execution Gateway", "اجرای سفارش واقعی MT5",
            "EXECUTION", GapStatus.PARTIAL, GapImpact.CRITICAL, 14,
            "الان معامله دمو و پیپر کامل است اما سفارش REAL به سرور MT5 ویتاورس از داخل اپ ارسال نمی‌شود - بزرگترین فاصله تا سود واقعی",
            "اتصال مستقیم به MT5 WebTrader گیت‌وی یا API کپی‌ترید ویتاورس + ارسال سفارش REAL با تایید دو مرحله‌ای کاربر"
        ),
        SoftwareGap(
            "TRAILING_STOP_BREAKEVEN", "Trailing Stop & Breakeven", "حد ضرر متحرک و سربه‌سر",
            "RISK", GapStatus.MISSING, GapImpact.HIGH, 5,
            "پوزیشن‌های سودده بدون تریلینگ استاپ به محض برگشت سودشان از دست می‌رود - میانگین 15-25% سود اضافه از دست رفته در هر ماه",
            "تریلینگ استاپ ATR + انتقال سربه‌سر در سود 1R + قفل سود پله‌ای"
        ),
        SoftwareGap(
            "PARTIAL_CLOSE_SCALING", "Partial Close & Scale In/Out", "بستن پله‌ای و ورود پله‌ای",
            "EXECUTION", GapStatus.MISSING, GapImpact.MEDIUM, 6,
            "خروج کامل یکجا - بدون قفل سود پله‌ای 50% در TP1 - نسبت سود به ریسک واقعی پایین می‌آید",
            "TP1 بستن 50% + حرکت استاپ به سربه‌سر + TP2 باقی - ورود پله‌ای در پولبک"
        ),
        SoftwareGap(
            "SLIPPAGE_MODEL", "Real Slippage Model", "مدل اسلیپیج واقعی",
            "EXECUTION", GapStatus.MISSING, GapImpact.MEDIUM, 3,
            "بک‌تست و دمو بدون اسلیپیج - در واقعیت 0.1-0.5 pip از هر معامله کم می‌شود - نتایج بک‌تست خوش‌بینانه است",
            "افزودن اسلیپیج داینامیک بر اساس نوسان و حجم به موتور بک‌تست و دمو"
        ),
        SoftwareGap(
            "REAL_SWAP_RATES", "Real Swap/Overnight Rates", "نرخ سواپ شبانه واقعی",
            "EXECUTION", GapStatus.MISSING, GapImpact.LOW, 2,
            "معاملات چندروزه بدون هزینه سواپ - معاملات طولانی سود واقعی‌شان کمتر از نمایش است",
            "جدول سواپ سه‌گانه ویتاورس (Long/Short/Triple) در موتور قیمت"
        ),
        // ---------- تحلیل و سیگنال ----------
        SoftwareGap(
            "NEWS_FILTER", "Economic News Filter", "فیلتر اخبار اقتصادی",
            "ANALYSIS", GapStatus.MISSING, GapImpact.HIGH, 6,
            "ورود در لحظه خبرهای NFP/CPI/FOMC اسپرد را 10 برابر و استاپ‌ها را قتل‌عام می‌کند - بزرگترین دلیل ضررهای ناگهانی",
            "تقویم اقتصادی زنده + بلاک ورود 15 دقیقه قبل/بعد خبر قرمز + هشدار"
        ),
        SoftwareGap(
            "SESSION_FILTER", "Trading Session Filter", "فیلتر سشن معاملاتی",
            "ANALYSIS", GapStatus.PARTIAL, GapImpact.MEDIUM, 3,
            "LIT و نقدینگی در سشن لندن/نیویورک بهترین عملکرد را دارد - الان سشن آسیا هم سیگنال می‌دهد",
            "فعال‌سازی سیگنال فقط در سشن انتخابی + وزن‌دهی کانفلوئنس به سشن"
        ),
        SoftwareGap(
            "MULTI_TF_CONFLUENCE", "Multi-Timeframe Confluence Engine", "موتور کانفلوئنس چند تایم‌فریم",
            "ANALYSIS", GapStatus.PARTIAL, GapImpact.HIGH, 8,
            "سیگنال M15 بدون تایید H4/D1 - وین‌ریت واقعی 10-15% پایین‌تر از پتانسیل",
            "چک همزمان 4 تایم‌فریم + امتیاز کانفلوئنس - ورود فقط با تایید HTF"
        ),
        SoftwareGap(
            "WALK_FORWARD_OPTIMIZER", "Walk-Forward Strategy Optimizer", "اپتیمایزر walk-forward",
            "ANALYSIS", GapStatus.MISSING, GapImpact.HIGH, 10,
            "پارامترهای ثابت استراتژی‌ها - بدون بهینه‌سازی دوره‌ای روی دیتای جدید - افت تدریجی عملکرد",
            "بهینه‌سازی rolling 90 روزه + اعتبارسنجی out-of-sample + رتبه‌بندی اتومات"
        ),
        SoftwareGap(
            "ML_CONFIDENCE_CALIBRATION", "ML Confidence Calibration", "کالیبراسیون ML کانفیدنس",
            "ANALYSIS", GapStatus.MISSING, GapImpact.MEDIUM, 9,
            "کانفیدنس 85% واقعا 85% وین‌ریت ندارد - کالیبره نبودن باعث سایز پوزیشن اشتباه می‌شود",
            "مدل رگرسیون کالیبراسیون روی تاریخچه سیگنال + نمایش وین‌ریت واقعی هر بازه کانفیدنس"
        ),
        // ---------- ریسک ----------
        SoftwareGap(
            "AUTO_COMPOUNDING", "Auto-Compounding Money Management", "مدیریت سرمایه مرکب اتومات",
            "RISK", GapStatus.MISSING, GapImpact.HIGH, 4,
            "سایز پوزیشن ثابت روی سرمایه اولیه - سود مرکب قوی‌ترین موتور سود بلندمدت فعال نیست",
            "سایز داینامیک درصدی از اکوییتی + گزینه مرکب + سقف ریسک دلاری"
        ),
        SoftwareGap(
            "EQUITY_PROTECTOR", "Equity Protector Time-Based", "محافظ اکوییتی زمانی",
            "RISK", GapStatus.PARTIAL, GapImpact.MEDIUM, 4,
            "کیل‌سویچ روزانه هست اما محدودیت ضرر هفتگی/ماهانه و سقف سود روزانه برای قفل سود نیست",
            "سقف ضرر هفتگی 5% + ماهانه 10% + قفل سود ماهانه + استاپ بعد از N باخت متوالی"
        ),
        // ---------- اقتصاد توکن ----------
        SoftwareGap(
            "TOKEN_REAL_PAYOUT", "Commission Real Payout Rails", "راهکار پرداخت واقعی کمیسیون",
            "MONETIZATION", GapStatus.PARTIAL, GapImpact.CRITICAL, 12,
            "کمیسیون 20% الان در خزانه درون‌برنامه‌ای ثبت می‌شود - تسویه واقعی به حساب صاحب نرم‌افزار (USDT/کارت بانکی) اتومات نیست",
            "تسویه هفتگی USDT-TRC20 از سمت سرور + صورتحساب PDF + گزارش مالی سالانه شمسی"
        ),
        SoftwareGap(
            "TOKEN_ONCHAIN", "ODN On-Chain Migration", "مهاجرت ODN به بلاکچین",
            "MONETIZATION", GapStatus.MISSING, GapImpact.HIGH, 20,
            "توکن ODN الان ledger درون‌برنامه با زنجیره هش است - انتقال‌پذیر بین کاربران و صرافی نیست",
            "قرارداد ERC-20/BEP-20 + آدرس قرارداد در اپ + bridging ledger درون‌برنامه به ولت"
        ),
        // ---------- پلتفرم ----------
        SoftwareGap(
            "PUSH_NOTIFICATIONS", "Signal Push Notifications", "نوتیفیکیشن سیگنال",
            "PLATFORM", GapStatus.PARTIAL, GapImpact.HIGH, 5,
            "سیگنال فقط داخل اپ - کاربر از دست دادن سیگنال طلایی باخبر نمی‌شود",
            "FCM/WorkManager آلارم محلی + نوتیف مهم‌ترین سیگنال با کانفیدنس بالای 80%"
        ),
        SoftwareGap(
            "CLOUD_SYNC_JOURNAL", "Cloud Sync Trade Journal", "ژورنال معاملات ابری",
            "PLATFORM", GapStatus.MISSING, GapImpact.MEDIUM, 7,
            "تاریخچه معاملات فقط روی دستگاه - با تعویض گوشی همه آماری از بین می‌رود",
            "سینک Firestore + خروجی CSV + آمار روانشناختی معامله‌گر"
        ),
        SoftwareGap(
            "TWO_FACTOR_AUTH", "Two-Factor Security (2FA)", "امنیت دومرحله‌ای",
            "SECURITY", GapStatus.MISSING, GapImpact.MEDIUM, 4,
            "ولت توکن و حساب دمو فقط با قفل دستگاه - بدون PIN جداگانه درون اپ",
            "PIN 6 رقمی + بایومتریک + تایید زنده برای برداشت/استیک‌گیری"
        ),
        SoftwareGap(
            "LICENSE_ANTIPERACY", "License & Anti-Piracy Binding", "لایسنس و ضد کپی",
            "SECURITY", GapStatus.MISSING, GapImpact.HIGH, 6,
            "APK کپی‌پذیر است - درآمد کمیسیون دور زده می‌شود - گیت توکن بدون سرور قابل دستکاری است",
            "لایسنس متصل به اکانت گوگل + امضای سروری گیت توکن + Integrity API پلی"
        )
    )

    fun summary(): GapSummary {
        val missing = gaps.count { it.status == GapStatus.MISSING }
        val partial = gaps.count { it.status == GapStatus.PARTIAL }
        val exists = gaps.count { it.status == GapStatus.EXISTS }
        val critical = gaps.filter { it.impact == GapImpact.CRITICAL && it.status != GapStatus.EXISTS }
        val totalEffort = gaps.filter { it.status != GapStatus.EXISTS }.sumOf { it.effortDays }
        return GapSummary(
            total = gaps.size, missing = missing, partial = partial, exists = exists,
            criticalIds = critical.map { it.id },
            totalEffortDays = totalEffort,
            topPriorities = analyze().take(5).map { it.id }
        )
    }

    fun roadmapFa(): List<String> = listOf(
        "فاز 1 (هفته 1-2): تریلینگ استاپ + مرکب کردن + فیلتر سشن - بیشترین سود با کمترین کار",
        "فاز 2 (هفته 3-4): فیلتر اخبار اقتصادی + کانفلوئنس چند تایم‌فریم + نوتیفیکیشن سیگنال",
        "فاز 3 (ماه 2): گیت‌وی سفارش REAL ویتاورس + اسلیپیج و سواپ واقعی + 2FA",
        "فاز 4 (ماه 2-3): تسویه واقعی کمیسیون USDT + لایسنس ضد کپی + اپتیمایزر walk-forward",
        "فاز 5 (ماه 3+): مهاجرت ODN به بلاکچین BEP-20 + ژورنال ابری + کالیبراسیون ML"
    )
}

data class GapSummary(
    val total: Int,
    val missing: Int,
    val partial: Int,
    val exists: Int,
    val criticalIds: List<String>,
    val totalEffortDays: Int,
    val topPriorities: List<String>
)
