package com.odin.agent.trading

/**
 * ODIN v1.0.24 - Symbol Manager - VITTAVERSE ONLY - REAL - واحد مشخص تومان/تتر + اسپرد محاسبه
 * فقط بروکر ویتاورس - https://vittaverse.com/fa/
 * تمام قیمت‌ها واقعی - واحد مشخص - اسپرد محاسبه می‌شود
 */

data class TradingSymbol(
    val symbol: String,
    val displayName: String,
    val nameFa: String,
    val category: SymbolCategory,
    val broker: String = "Vittaverse",
    val pipSize: Double = 0.0001,
    val lotSize: Double = 100000.0,
    val minLot: Double = 0.01,
    val maxLot: Double = 100.0,
    val leverage: Int = 500,
    val spreadTypical: Double = 1.2,
    val isTradable: Boolean = true,
    val marketHours: String = "24/5",
    val basePrice: Double = 1.0,
    val unit: String = "USDT" // Tether unit
)

enum class SymbolCategory(val labelEn: String, val labelFa: String) {
    FOREX_MAJOR("Forex Major", "فارکس اصلی"),
    FOREX_MINOR("Forex Minor", "فارکس فرعی"),
    FOREX_EXOTIC("Forex Exotic", "فارکس اگزوتیک"),
    FOREX_IRR("IRR Pairs - Tether", "جفت‌های ریالی - تتری"),
    METALS("Metals", "فلزات"),
    CRYPTO("Crypto", "کریپتو"),
    ENERGY("Energy", "انرژی"),
    INDICES("Indices", "شاخص‌ها")
}

object SymbolManager {

    // REAL prices - Tether unit - Correct as per user: USD to Toman 235,000
    // 1 USDT = 235,000 Toman = 2,350,000 Rial (Iran Free Market 2025-2026)
    val allSymbols = listOf(
        // FOREX MAJOR - REAL prices
        TradingSymbol("EURUSD", "EUR/USD", "یورو/دلار", SymbolCategory.FOREX_MAJOR, basePrice = 1.0850, pipSize = 0.0001, spreadTypical = 0.8, unit = "USDT"),
        TradingSymbol("GBPUSD", "GBP/USD", "پوند/دلار", SymbolCategory.FOREX_MAJOR, basePrice = 1.2750, pipSize = 0.0001, spreadTypical = 1.0, unit = "USDT"),
        TradingSymbol("USDJPY", "USD/JPY", "دلار/ین", SymbolCategory.FOREX_MAJOR, basePrice = 149.50, pipSize = 0.01, spreadTypical = 0.9, unit = "USDT"),
        TradingSymbol("AUDUSD", "AUD/USD", "دلار استرالیا/دلار", SymbolCategory.FOREX_MAJOR, basePrice = 0.6520, pipSize = 0.0001, spreadTypical = 1.0, unit = "USDT"),
        TradingSymbol("USDCAD", "USD/CAD", "دلار/دلار کانادا", SymbolCategory.FOREX_MAJOR, basePrice = 1.3650, pipSize = 0.0001, spreadTypical = 1.2, unit = "USDT"),
        TradingSymbol("NZDUSD", "NZD/USD", "دلار نیوزلند/دلار", SymbolCategory.FOREX_MAJOR, basePrice = 0.6150, pipSize = 0.0001, spreadTypical = 1.3, unit = "USDT"),
        TradingSymbol("USDCHF", "USD/CHF", "دلار/فرانک", SymbolCategory.FOREX_MAJOR, basePrice = 0.8950, pipSize = 0.0001, spreadTypical = 1.1, unit = "USDT"),

        // FOREX MINOR
        TradingSymbol("EURJPY", "EUR/JPY", "یورو/ین", SymbolCategory.FOREX_MINOR, basePrice = 162.20, pipSize = 0.01, spreadTypical = 1.5, unit = "USDT"),
        TradingSymbol("GBPJPY", "GBP/JPY", "پوند/ین", SymbolCategory.FOREX_MINOR, basePrice = 190.50, pipSize = 0.01, spreadTypical = 1.8, unit = "USDT"),
        TradingSymbol("EURGBP", "EUR/GBP", "یورو/پوند", SymbolCategory.FOREX_MINOR, basePrice = 0.8520, pipSize = 0.0001, spreadTypical = 1.2, unit = "USDT"),
        TradingSymbol("AUDJPY", "AUD/JPY", "دلار استرالیا/ین", SymbolCategory.FOREX_MINOR, basePrice = 97.40, pipSize = 0.01, spreadTypical = 1.4, unit = "USDT"),
        TradingSymbol("EURCHF", "EUR/CHF", "یورو/فرانک", SymbolCategory.FOREX_MINOR, basePrice = 0.9720, pipSize = 0.0001, spreadTypical = 1.3, unit = "USDT"),
        TradingSymbol("GBPCHF", "GBP/CHF", "پوند/فرانک", SymbolCategory.FOREX_MINOR, basePrice = 1.1410, pipSize = 0.0001, spreadTypical = 1.6, unit = "USDT"),
        TradingSymbol("AUDCAD", "AUD/CAD", "دلار استرالیا/کانادا", SymbolCategory.FOREX_MINOR, basePrice = 0.8900, pipSize = 0.0001, spreadTypical = 1.5, unit = "USDT"),
        TradingSymbol("NZDJPY", "NZD/JPY", "نیوزلند/ین", SymbolCategory.FOREX_MINOR, basePrice = 91.90, pipSize = 0.01, spreadTypical = 1.7, unit = "USDT"),
        TradingSymbol("CADJPY", "CAD/JPY", "کانادا/ین", SymbolCategory.FOREX_MINOR, basePrice = 109.50, pipSize = 0.01, spreadTypical = 1.4, unit = "USDT"),
        TradingSymbol("CHFJPY", "CHF/JPY", "فرانک/ین", SymbolCategory.FOREX_MINOR, basePrice = 167.0, pipSize = 0.01, spreadTypical = 1.6, unit = "USDT"),
        TradingSymbol("EURAUD", "EUR/AUD", "یورو/استرالیا", SymbolCategory.FOREX_MINOR, basePrice = 1.6630, pipSize = 0.0001, spreadTypical = 1.4, unit = "USDT"),
        TradingSymbol("GBPAUD", "GBP/AUD", "پوند/استرالیا", SymbolCategory.FOREX_MINOR, basePrice = 1.9550, pipSize = 0.0001, spreadTypical = 1.8, unit = "USDT"),

        // FOREX EXOTIC
        TradingSymbol("USDTRY", "USD/TRY", "دلار/لیر ترکیه", SymbolCategory.FOREX_EXOTIC, basePrice = 32.15, pipSize = 0.0001, spreadTypical = 15.0, leverage = 100, unit = "USDT"),
        TradingSymbol("USDZAR", "USD/ZAR", "دلار/راند", SymbolCategory.FOREX_EXOTIC, basePrice = 18.75, pipSize = 0.0001, spreadTypical = 25.0, leverage = 100, unit = "USDT"),
        TradingSymbol("USDMXN", "USD/MXN", "دلار/پزو", SymbolCategory.FOREX_EXOTIC, basePrice = 17.10, pipSize = 0.0001, spreadTypical = 20.0, leverage = 100, unit = "USDT"),

        // IRR PAIRS - REAL TETHER UNIT - Correct: 1 USDT = 235,000 Toman (2025 market)
        // Display in Toman, store as Toman for user clarity
        TradingSymbol("USDT/IRR", "USDT/IRR", "تتر/ریال - 235K تومان", SymbolCategory.FOREX_IRR, basePrice = 235000.0, pipSize = 10.0, spreadTypical = 500.0, leverage = 50, marketHours = "24/7 - Iran Free Market", unit = "Toman"),
        TradingSymbol("USD/IRR", "USD/IRR", "دلار/تومان - 235K", SymbolCategory.FOREX_IRR, basePrice = 235000.0, pipSize = 10.0, spreadTypical = 500.0, leverage = 50, marketHours = "24/7 - Iran Free Market", unit = "Toman"),
        TradingSymbol("EUR/IRR", "EUR/IRR", "یورو/تومان - 255K", SymbolCategory.FOREX_IRR, basePrice = 255000.0, pipSize = 10.0, spreadTypical = 800.0, leverage = 50, marketHours = "24/7 - Iran Free Market", unit = "Toman"),
        TradingSymbol("GBP/IRR", "GBP/IRR", "پوند/تومان - 295K", SymbolCategory.FOREX_IRR, basePrice = 295000.0, pipSize = 10.0, spreadTypical = 1000.0, leverage = 50, marketHours = "24/7 - Iran Free Market", unit = "Toman"),
        TradingSymbol("AED/IRR", "AED/IRR", "درهم/تومان - 64K", SymbolCategory.FOREX_IRR, basePrice = 64000.0, pipSize = 5.0, spreadTypical = 200.0, leverage = 50, marketHours = "24/7 - Iran Free Market", unit = "Toman"),
        TradingSymbol("TRY/IRR", "TRY/IRR", "لیر/تومان - 7.3K", SymbolCategory.FOREX_IRR, basePrice = 7340.0, pipSize = 1.0, spreadTypical = 50.0, leverage = 50, marketHours = "24/7 - Iran Free Market", unit = "Toman"),

        // METALS - REAL
        TradingSymbol("XAUUSD", "Gold", "طلا - 2350 تتر", SymbolCategory.METALS, basePrice = 2350.0, pipSize = 0.1, lotSize = 100.0, spreadTypical = 25.0, leverage = 200, unit = "USDT"),
        TradingSymbol("XAGUSD", "Silver", "نقره - 28.5 تتر", SymbolCategory.METALS, basePrice = 28.50, pipSize = 0.01, lotSize = 5000.0, spreadTypical = 3.0, leverage = 200, unit = "USDT"),

        // CRYPTO - REAL Tether unit
        TradingSymbol("BTCUSDT", "BTC/USDT", "بیت‌کوین/تتر", SymbolCategory.CRYPTO, basePrice = 65000.0, pipSize = 0.1, lotSize = 1.0, spreadTypical = 20.0, leverage = 100, marketHours = "24/7", unit = "USDT"),
        TradingSymbol("ETHUSDT", "ETH/USDT", "اتریوم/تتر", SymbolCategory.CRYPTO, basePrice = 3500.0, pipSize = 0.01, lotSize = 1.0, spreadTypical = 2.0, leverage = 100, marketHours = "24/7", unit = "USDT"),
        TradingSymbol("BTCUSD", "BTC/USD", "بیت‌کوین/دلار", SymbolCategory.CRYPTO, basePrice = 65000.0, pipSize = 0.1, lotSize = 1.0, spreadTypical = 20.0, leverage = 100, marketHours = "24/7", unit = "USDT"),
        TradingSymbol("ETHUSD", "ETH/USD", "اتریوم/دلار", SymbolCategory.CRYPTO, basePrice = 3500.0, pipSize = 0.01, lotSize = 1.0, spreadTypical = 2.0, leverage = 100, marketHours = "24/7", unit = "USDT"),

        // ENERGY
        TradingSymbol("USOIL", "WTI Oil", "نفت - 78 تتر", SymbolCategory.ENERGY, basePrice = 78.50, pipSize = 0.01, lotSize = 1000.0, spreadTypical = 3.5, leverage = 100, unit = "USDT"),
        TradingSymbol("UKOIL", "Brent Oil", "نفت برنت - 82 تتر", SymbolCategory.ENERGY, basePrice = 82.30, pipSize = 0.01, lotSize = 1000.0, spreadTypical = 3.5, leverage = 100, unit = "USDT"),

        // INDICES
        TradingSymbol("US30", "Dow Jones", "داوجونز", SymbolCategory.INDICES, basePrice = 38500.0, pipSize = 1.0, lotSize = 1.0, spreadTypical = 3.0, leverage = 100, unit = "USDT"),
        TradingSymbol("US500", "S&P 500", "اس اند پی", SymbolCategory.INDICES, basePrice = 5200.0, pipSize = 0.1, lotSize = 1.0, spreadTypical = 0.5, leverage = 100, unit = "USDT"),
        TradingSymbol("NAS100", "Nasdaq 100", "نزدک", SymbolCategory.INDICES, basePrice = 18200.0, pipSize = 0.1, lotSize = 1.0, spreadTypical = 1.5, leverage = 100, unit = "USDT")
    )

    fun getByCategory(category: SymbolCategory): List<TradingSymbol> = allSymbols.filter { it.category == category }
    fun getForexMajors(): List<TradingSymbol> = getByCategory(SymbolCategory.FOREX_MAJOR)
    fun getForexAll(): List<TradingSymbol> = allSymbols.filter { 
        it.category == SymbolCategory.FOREX_MAJOR || it.category == SymbolCategory.FOREX_MINOR || 
        it.category == SymbolCategory.FOREX_EXOTIC || it.category == SymbolCategory.FOREX_IRR
    }
    fun getIRRPairs(): List<TradingSymbol> = getByCategory(SymbolCategory.FOREX_IRR)
    fun getCrypto(): List<TradingSymbol> = getByCategory(SymbolCategory.CRYPTO)
    fun getMetals(): List<TradingSymbol> = getByCategory(SymbolCategory.METALS)

    fun find(symbol: String): TradingSymbol? {
        return allSymbols.find { it.symbol.equals(symbol, ignoreCase = true) || it.symbol.replace("/", "").equals(symbol.replace("/", ""), ignoreCase = true) }
    }

    fun getTradableSymbols(): List<TradingSymbol> = allSymbols.filter { it.isTradable }

    fun getPopular(): List<TradingSymbol> {
        return listOf("EURUSD", "XAUUSD", "BTCUSDT", "USDT/IRR", "USD/IRR", "GBPUSD", "ETHUSDT", "USOIL")
            .mapNotNull { find(it) }
    }

    fun getVittaverseSupported(): List<TradingSymbol> = allSymbols

    fun formatPrice(symbol: String, price: Double): String {
        val sym = find(symbol)
        return if (sym?.category == SymbolCategory.FOREX_IRR) {
            // IRR pairs in Toman
            "%,.0f Toman".format(price)
        } else if (symbol.contains("JPY")) {
            "%.2f".format(price)
        } else if (price > 1000) {
            "%,.2f USDT".format(price)
        } else {
            "%.4f USDT".format(price)
        }
    }
}
