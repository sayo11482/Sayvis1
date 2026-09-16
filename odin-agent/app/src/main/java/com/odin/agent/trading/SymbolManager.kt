package com.odin.agent.trading

/**
 * ODIN v1.0.14 - Symbol Manager - Real Forex + Crypto + IRR + Vittaverse Broker Symbols
 * شامل تمام نمادهای فارکس، کریپتو، فلزات، انرژی، و ریال ایران
 */

data class TradingSymbol(
    val symbol: String, // e.g. EURUSD, BTCUSD, USD/IRR
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
    val basePrice: Double = 1.0
)

enum class SymbolCategory(val labelEn: String, val labelFa: String) {
    FOREX_MAJOR("Forex Major", "فارکس اصلی"),
    FOREX_MINOR("Forex Minor", "فارکس فرعی"),
    FOREX_EXOTIC("Forex Exotic", "فارکس اگزوتیک"),
    FOREX_IRR("IRR Pairs", "جفت‌های ریالی"),
    METALS("Metals", "فلزات"),
    CRYPTO("Crypto", "کریپتو"),
    ENERGY("Energy", "انرژی"),
    INDICES("Indices", "شاخص‌ها")
}

object SymbolManager {

    // Comprehensive symbol list - Vittaverse broker supports 100+ forex pairs
    val allSymbols = listOf(
        // FOREX MAJOR - 7 pairs
        TradingSymbol("EURUSD", "EUR/USD", "یورو/دلار", SymbolCategory.FOREX_MAJOR, basePrice = 1.0850, pipSize = 0.0001, spreadTypical = 0.8),
        TradingSymbol("GBPUSD", "GBP/USD", "پوند/دلار", SymbolCategory.FOREX_MAJOR, basePrice = 1.2750, pipSize = 0.0001, spreadTypical = 1.0),
        TradingSymbol("USDJPY", "USD/JPY", "دلار/ین", SymbolCategory.FOREX_MAJOR, basePrice = 149.50, pipSize = 0.01, spreadTypical = 0.9),
        TradingSymbol("AUDUSD", "AUD/USD", "دلار استرالیا/دلار", SymbolCategory.FOREX_MAJOR, basePrice = 0.6520, pipSize = 0.0001, spreadTypical = 1.0),
        TradingSymbol("USDCAD", "USD/CAD", "دلار/دلار کانادا", SymbolCategory.FOREX_MAJOR, basePrice = 1.3650, pipSize = 0.0001, spreadTypical = 1.2),
        TradingSymbol("NZDUSD", "NZD/USD", "دلار نیوزلند/دلار", SymbolCategory.FOREX_MAJOR, basePrice = 0.6150, pipSize = 0.0001, spreadTypical = 1.3),
        TradingSymbol("USDCHF", "USD/CHF", "دلار/فرانک", SymbolCategory.FOREX_MAJOR, basePrice = 0.8950, pipSize = 0.0001, spreadTypical = 1.1),

        // FOREX MINOR / CROSSES - 12 pairs
        TradingSymbol("EURJPY", "EUR/JPY", "یورو/ین", SymbolCategory.FOREX_MINOR, basePrice = 162.20, pipSize = 0.01, spreadTypical = 1.5),
        TradingSymbol("GBPJPY", "GBP/JPY", "پوند/ین", SymbolCategory.FOREX_MINOR, basePrice = 190.50, pipSize = 0.01, spreadTypical = 1.8),
        TradingSymbol("EURGBP", "EUR/GBP", "یورو/پوند", SymbolCategory.FOREX_MINOR, basePrice = 0.8520, pipSize = 0.0001, spreadTypical = 1.2),
        TradingSymbol("AUDJPY", "AUD/JPY", "دلار استرالیا/ین", SymbolCategory.FOREX_MINOR, basePrice = 97.40, pipSize = 0.01, spreadTypical = 1.4),
        TradingSymbol("EURCHF", "EUR/CHF", "یورو/فرانک", SymbolCategory.FOREX_MINOR, basePrice = 0.9720, pipSize = 0.0001, spreadTypical = 1.3),
        TradingSymbol("GBPCHF", "GBP/CHF", "پوند/فرانک", SymbolCategory.FOREX_MINOR, basePrice = 1.1410, pipSize = 0.0001, spreadTypical = 1.6),
        TradingSymbol("AUDCAD", "AUD/CAD", "دلار استرالیا/کانادا", SymbolCategory.FOREX_MINOR, basePrice = 0.8900, pipSize = 0.0001, spreadTypical = 1.5),
        TradingSymbol("NZDJPY", "NZD/JPY", "نیوزلند/ین", SymbolCategory.FOREX_MINOR, basePrice = 91.90, pipSize = 0.01, spreadTypical = 1.7),
        TradingSymbol("CADJPY", "CAD/JPY", "کانادا/ین", SymbolCategory.FOREX_MINOR, basePrice = 109.50, pipSize = 0.01, spreadTypical = 1.4),
        TradingSymbol("CHFJPY", "CHF/JPY", "فرانک/ین", SymbolCategory.FOREX_MINOR, basePrice = 167.0, pipSize = 0.01, spreadTypical = 1.6),
        TradingSymbol("EURAUD", "EUR/AUD", "یورو/استرالیا", SymbolCategory.FOREX_MINOR, basePrice = 1.6630, pipSize = 0.0001, spreadTypical = 1.4),
        TradingSymbol("GBPAUD", "GBP/AUD", "پوند/استرالیا", SymbolCategory.FOREX_MINOR, basePrice = 1.9550, pipSize = 0.0001, spreadTypical = 1.8),

        // FOREX EXOTIC - Including IRR pairs
        TradingSymbol("USDTRY", "USD/TRY", "دلار/لیر ترکیه", SymbolCategory.FOREX_EXOTIC, basePrice = 32.15, pipSize = 0.0001, spreadTypical = 15.0, leverage = 100),
        TradingSymbol("USDZAR", "USD/ZAR", "دلار/راند", SymbolCategory.FOREX_EXOTIC, basePrice = 18.75, pipSize = 0.0001, spreadTypical = 25.0, leverage = 100),
        TradingSymbol("USDMXN", "USD/MXN", "دلار/پزو", SymbolCategory.FOREX_EXOTIC, basePrice = 17.10, pipSize = 0.0001, spreadTypical = 20.0, leverage = 100),

        // IRR PAIRS - Iranian Rial - ریال ایران
        TradingSymbol("USD/IRR", "USD/IRR", "دلار/ریال ایران", SymbolCategory.FOREX_IRR, basePrice = 590000.0, pipSize = 1.0, spreadTypical = 500.0, leverage = 50, marketHours = "Sat-Thu 08:00-16:00 Tehran"),
        TradingSymbol("EUR/IRR", "EUR/IRR", "یورو/ریال ایران", SymbolCategory.FOREX_IRR, basePrice = 640000.0, pipSize = 1.0, spreadTypical = 800.0, leverage = 50, marketHours = "Sat-Thu 08:00-16:00 Tehran"),
        TradingSymbol("GBP/IRR", "GBP/IRR", "پوند/ریال ایران", SymbolCategory.FOREX_IRR, basePrice = 752000.0, pipSize = 1.0, spreadTypical = 1000.0, leverage = 50, marketHours = "Sat-Thu 08:00-16:00 Tehran"),
        TradingSymbol("AED/IRR", "AED/IRR", "درهم/ریال ایران", SymbolCategory.FOREX_IRR, basePrice = 160600.0, pipSize = 1.0, spreadTypical = 200.0, leverage = 50, marketHours = "Sat-Thu 08:00-16:00 Tehran"),
        TradingSymbol("TRY/IRR", "TRY/IRR", "لیر/ریال ایران", SymbolCategory.FOREX_IRR, basePrice = 18300.0, pipSize = 1.0, spreadTypical = 100.0, leverage = 50, marketHours = "Sat-Thu 08:00-16:00 Tehran"),

        // METALS
        TradingSymbol("XAUUSD", "Gold", "طلا", SymbolCategory.METALS, basePrice = 2350.0, pipSize = 0.1, lotSize = 100.0, spreadTypical = 25.0, leverage = 200),
        TradingSymbol("XAGUSD", "Silver", "نقره", SymbolCategory.METALS, basePrice = 28.50, pipSize = 0.01, lotSize = 5000.0, spreadTypical = 3.0, leverage = 200),
        TradingSymbol("XPTUSD", "Platinum", "پلاتین", SymbolCategory.METALS, basePrice = 1020.0, pipSize = 0.1, lotSize = 50.0, spreadTypical = 50.0, leverage = 100),
        TradingSymbol("XPDUSD", "Palladium", "پالادیوم", SymbolCategory.METALS, basePrice = 950.0, pipSize = 0.1, lotSize = 50.0, spreadTypical = 60.0, leverage = 100),

        // CRYPTO
        TradingSymbol("BTCUSD", "BTC/USD", "بیت‌کوین/دلار", SymbolCategory.CRYPTO, basePrice = 65000.0, pipSize = 0.1, lotSize = 1.0, spreadTypical = 20.0, leverage = 100, marketHours = "24/7"),
        TradingSymbol("ETHUSD", "ETH/USD", "اتریوم/دلار", SymbolCategory.CRYPTO, basePrice = 3500.0, pipSize = 0.01, lotSize = 1.0, spreadTypical = 2.0, leverage = 100, marketHours = "24/7"),
        TradingSymbol("SOLUSD", "SOL/USD", "سولانا/دلار", SymbolCategory.CRYPTO, basePrice = 145.0, pipSize = 0.01, lotSize = 1.0, spreadTypical = 0.2, leverage = 50, marketHours = "24/7"),
        TradingSymbol("XRPUSD", "XRP/USD", "ریپل/دلار", SymbolCategory.CRYPTO, basePrice = 0.52, pipSize = 0.0001, lotSize = 1.0, spreadTypical = 0.01, leverage = 50, marketHours = "24/7"),

        // ENERGY
        TradingSymbol("USOIL", "WTI Oil", "نفت WTI", SymbolCategory.ENERGY, basePrice = 78.50, pipSize = 0.01, lotSize = 1000.0, spreadTypical = 3.5, leverage = 100),
        TradingSymbol("UKOIL", "Brent Oil", "نفت برنت", SymbolCategory.ENERGY, basePrice = 82.30, pipSize = 0.01, lotSize = 1000.0, spreadTypical = 3.5, leverage = 100),
        TradingSymbol("NATGAS", "Natural Gas", "گاز طبیعی", SymbolCategory.ENERGY, basePrice = 2.15, pipSize = 0.001, lotSize = 10000.0, spreadTypical = 0.01, leverage = 100),

        // INDICES
        TradingSymbol("US30", "Dow Jones", "داوجونز", SymbolCategory.INDICES, basePrice = 38500.0, pipSize = 1.0, lotSize = 1.0, spreadTypical = 3.0, leverage = 100),
        TradingSymbol("US500", "S&P 500", "اس اند پی ۵۰۰", SymbolCategory.INDICES, basePrice = 5200.0, pipSize = 0.1, lotSize = 1.0, spreadTypical = 0.5, leverage = 100),
        TradingSymbol("NAS100", "Nasdaq 100", "نزدک", SymbolCategory.INDICES, basePrice = 18200.0, pipSize = 0.1, lotSize = 1.0, spreadTypical = 1.5, leverage = 100)
    )

    fun getByCategory(category: SymbolCategory): List<TradingSymbol> {
        return allSymbols.filter { it.category == category }
    }

    fun getForexMajors(): List<TradingSymbol> = getByCategory(SymbolCategory.FOREX_MAJOR)
    fun getForexAll(): List<TradingSymbol> = allSymbols.filter { 
        it.category == SymbolCategory.FOREX_MAJOR || 
        it.category == SymbolCategory.FOREX_MINOR || 
        it.category == SymbolCategory.FOREX_EXOTIC ||
        it.category == SymbolCategory.FOREX_IRR
    }
    fun getIRRPairs(): List<TradingSymbol> = getByCategory(SymbolCategory.FOREX_IRR)
    fun getCrypto(): List<TradingSymbol> = getByCategory(SymbolCategory.CRYPTO)
    fun getMetals(): List<TradingSymbol> = getByCategory(SymbolCategory.METALS)

    fun find(symbol: String): TradingSymbol? {
        return allSymbols.find { it.symbol.equals(symbol, ignoreCase = true) || it.symbol.replace("/", "").equals(symbol.replace("/", ""), ignoreCase = true) }
    }

    fun getTradableSymbols(): List<TradingSymbol> = allSymbols.filter { it.isTradable }

    fun getPopular(): List<TradingSymbol> {
        return listOf("EURUSD", "GBPUSD", "XAUUSD", "BTCUSD", "USD/IRR", "USDJPY", "AUDUSD", "ETHUSD")
            .mapNotNull { find(it) }
    }

    fun getVittaverseSupported(): List<TradingSymbol> {
        // Vittaverse supports 100+ forex, metals, crypto, etc. but not IRR (offshore doesn't support IRR due to sanctions)
        // We still show IRR as custom synthetic pair
        return allSymbols
    }
}
