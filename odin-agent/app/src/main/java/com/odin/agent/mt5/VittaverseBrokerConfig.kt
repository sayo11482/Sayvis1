package com.odin.agent.mt5

/**
 * ODIN v1.0.14 - Vittaverse Broker Config - Real Broker Integration
 * بروکر ویتاورس - 60+ جفت فارکس، MT5، cTrader
 */

object VittaverseBrokerConfig {
    const val BROKER_NAME = "Vittaverse"
    const val BROKER_NAME_FA = "ویتاورس"
    const val WEBSITE = "https://vittaverse.com"
    const val FOUNDED = 2022
    const val MIN_DEPOSIT = 15.0 // USD
    const val MAX_LEVERAGE = 500
    const val SPREAD_FROM = 0.0
    const val COMMISSION = "From $0"
    const val REGULATED = false
    const val HEADQUARTERS = "Saint Vincent and the Grenadines"

    // MT5 Servers
    val mt5Servers = listOf(
        "Vittaverse-Real",
        "Vittaverse-Demo",
        "Vittaverse-Real-2",
        "Vittaverse-ECN"
    )

    // Account Types
    enum class AccountType(val labelEn: String, val labelFa: String, val minDeposit: Double, val leverage: Int, val spread: String) {
        STANDARD("Standard", "استاندارد", 15.0, 500, "From 1.0 pip"),
        ECN("ECN", "ای سی ان", 100.0, 200, "From 0.0 pip + $3 commission"),
        PRO("Pro", "حرفه‌ای", 500.0, 200, "From 0.0 pip + $2 commission")
    }

    // Supported platforms
    val platforms = listOf("MT5", "cTrader", "TradingView")

    // Deposit methods
    val depositMethods = listOf(
        "Bitcoin", "Ethereum", "USDT", "Bank Wire", "Credit Card", "Skrill", "Neteller"
    )

    // Trading instruments count
    const val FOREX_PAIRS = 103
    const val CRYPTO_PAIRS = 85
    const val STOCK_CFDS = 140
    const val METALS = 12
    const val ENERGIES = 3
    const val INDICES = 13

    // Special notes for Iranian users
    const val IRAN_NOTE_EN = "Vittaverse does NOT officially accept clients from Iran due to sanctions, but many Iranian traders use it via VPN and crypto deposits. IRR pairs are synthetic custom symbols for tracking Iranian market."
    const val IRAN_NOTE_FA = "ویتاورس به دلیل تحریم‌ها رسما از ایران مشتری نمی‌پذیرد، اما بسیاری از تریدرهای ایرانی با VPN و واریز کریپتو از آن استفاده می‌کنند. جفت‌های ریالی به صورت نماد مصنوعی برای ردیابی بازار ایران نمایش داده می‌شود."

    // Real trading disclaimer
    const val REAL_TRADING_WARNING_EN = "Real trading in forex market involves high risk. You can lose more than your initial deposit. Trade only with money you can afford to lose. This app connects to real MT5 account and places REAL orders with REAL money on Vittaverse broker."
    const val REAL_TRADING_WARNING_FA = "معامله واقعی در بازار فارکس ریسک بالایی دارد. ممکن است بیشتر از سپرده اولیه خود را از دست بدهید. فقط با پولی که توان از دست دادنش را دارید معامله کنید. این اپ به حساب واقعی MT5 متصل شده و سفارشات واقعی با پول واقعی در بروکر ویتاورس ثبت می‌کند."

    fun getServerInfo(server: String): MT5Server? {
        return MT5ConnectionManager().getServers().find { it.name == server }
    }

    fun getAccountTypeInfo(type: AccountType): String {
        return "${type.labelEn} - Min $${type.minDeposit} - Leverage 1:${type.leverage} - Spread ${type.spread}"
    }
}
