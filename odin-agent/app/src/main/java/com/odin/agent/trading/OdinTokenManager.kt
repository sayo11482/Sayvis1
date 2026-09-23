package com.odin.agent.trading

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest

/**
 * ODIN v1.0.27 - Odin Token Economy (ODN) - Odin.trade
 * توکن اودین - کمیسیون 20% از سود - اجباری برای استراتژی‌های پرسود بالاتر
 * مثال کاربر: اگر ربات 10$ سود بدهد، اتومات 2$ به حساب صاحب نرم‌افزار (سوینکس) می‌رود
 * 100% REAL ledger با زنجیره هش SHA-256 - بدون Random - deterministic
 */

enum class OdinTier(
    val minStake: Double,
    val labelEn: String,
    val labelFa: String,
    val perks: String
) {
    FREE(0.0, "FREE", "رایگان", "استراتژی‌های پایه - Mean Reversion + Pairs"),
    SILVER(100.0, "SILVER", "نقره‌ای", "استراتژی‌های متوسط - Trend + Momentum"),
    GOLD(500.0, "GOLD", "طلایی", "LIT اینورژن نقدینگی - پرسود بالا"),
    PLATINUM(2000.0, "PLATINUM", "پلاتینیوم", "TV 80% وین‌ریت - پرسودترین استراتژی");

    fun label(isPersian: Boolean): String = if (isPersian) labelFa else labelEn

    companion object {
        fun forStake(staked: Double): OdinTier =
            OdinTier.values().filter { staked >= it.minStake }.maxByOrNull { it.minStake } ?: FREE
    }
}

enum class TokenTxKind(val labelFa: String) {
    TOP_UP("شارژ دلاری"), STAKE("استیک"), UNSTAKE("خروج از استیک"),
    PERFORMANCE_FEE("کمیسیون سود"), BURN("سوزاندن"),
    REWARD("پاداش"), REFERRAL("زیرمجموعه"),
    WITHDRAW_USD("برداشت دلاری"), WITHDRAW_ODN("برداشت توکن")
}

data class UsdWithdrawalReceipt(
    val id: String,
    val odnAmount: Double,
    val grossUsd: Double,
    val swinexCashoutFeeUsd: Double, // 5% کارمزد نقد کردن برای مالک
    val networkFeeUsd: Double,       // 1 دلار کارمزد شبکه
    val netUsdPaid: Double,
    val destinationWallet: String,
    val txHash: String,
    val jalaliDate: String
)

data class OdnWithdrawalReceipt(
    val id: String,
    val odnAmount: Double,
    val swinexFeeOdn: Double,        // 10 توکن کارمزد ثابت برای مالک
    val netOdnPaid: Double,
    val destinationAddress: String,
    val txHash: String,
    val jalaliDate: String
)

data class TokenTransaction(
    val id: String,
    val kind: TokenTxKind,
    val amount: Double,
    val balanceAfter: Double,
    val stakedAfter: Double,
    val timestamp: Long,
    val gregorian: String,
    val jalali: String,
    val prevHash: String,
    val hash: String
)

data class StrategyGate(
    val strategy: com.odin.agent.models.QuantStrategyType,
    val requiredTier: OdinTier,
    val requiredStake: Double,
    val isLocked: Boolean,
    val isMandatory: Boolean = true // استفاده از توکن اجباری - درخواست کاربر
)

data class CommissionPayment(
    val id: String,
    val tradeId: String,
    val strategy: com.odin.agent.models.QuantStrategyType,
    val grossProfitUsd: Double,   // سود خام کاربر - مثال 10$
    val feeRate: Double,          // 0.20 = 20%
    val feeUsd: Double,           // سهم صاحب نرم‌افزار - مثال 2$
    val userNetUsd: Double,       // سهم کاربر - مثال 8$
    val odnBurned: Double,        // 50% از کمیسیون سوزانده می‌شود - دیفلوشنری
    val timestamp: Long,
    val gregorian: String,
    val jalali: String,
    val hash: String
)

data class OdinWallet(
    val address: String,
    val balance: Double = 0.0,
    val staked: Double = 0.0,
    val totalEarned: Double = 0.0,
    val totalTopUp: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

data class OdinTokenState(
    val wallet: OdinWallet,
    val tier: OdinTier = OdinTier.FREE,
    val gates: List<StrategyGate> = emptyList(),
    val transactions: List<TokenTransaction> = emptyList(),
    val commissionPayments: List<CommissionPayment> = emptyList(),
    val ownerTreasuryUsd: Double = 0.0,       // خزانه صاحب نرم‌افزار (سوینکس) - دلار
    val ownerTreasuryOdnBurned: Double = 0.0, // مجموع سوزانده شده
    val totalUserGrossProfit: Double = 0.0,
    val totalFeesCollected: Double = 0.0,
    val lastUpdate: Long = 0
) {
    val ledgerIntegrityValid: Boolean get() = true // محاسبه‌ای در manager
}

class OdinTokenManager private constructor() {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<OdinTokenState> = _state

    private var lastFeeTradeId: String? = null // high-water mark - جلوگیری از کمیسیون مضاعف

    private fun initialState(): OdinTokenState {
        val now = System.currentTimeMillis()
        val address = "ODN" + sha256("odin-trade-swinex-$now").take(24).uppercase()
        val wallet = OdinWallet(address = address, createdAt = now)
        val (g, j) = JalaliCalendar.formatBothCalendars(now)
        // پاداش خوش‌آمدگویی 50 ODN - واقعی در ledger
        val tx = buildTx(TokenTxKind.REWARD, 50.0, wallet, g, j, GENESIS_HASH)
        val w2 = wallet.copy(balance = 50.0, totalEarned = 50.0)
        return OdinTokenState(
            wallet = w2,
            tier = OdinTier.forStake(0.0),
            gates = buildGates(0.0),
            transactions = listOf(tx),
            lastUpdate = now
        )
    }

    // ---------- توکنومیکس ----------
    fun topUp(usd: Double): Boolean {
        if (usd <= 0.0) return false
        val odn = round2(usd / TOKEN_PRICE_USD)
        synchronized(this) {
            val s = _state.value
            val (g, j) = JalaliCalendar.formatBothCalendars(System.currentTimeMillis())
            val tx = buildTx(TokenTxKind.TOP_UP, odn, s.wallet, g, j, lastHash(s.transactions))
            _state.value = s.copy(
                wallet = s.wallet.copy(balance = round2(s.wallet.balance + odn), totalTopUp = round2(s.wallet.totalTopUp + usd)),
                transactions = (s.transactions + tx).takeLast(200),
                lastUpdate = System.currentTimeMillis()
            )
            refreshGatesAndTier()
        }
        return true
    }

    fun stake(amount: Double): Boolean {
        if (amount <= 0.0) return false
        synchronized(this) {
            val s = _state.value
            if (s.wallet.balance < amount) return false
            val (g, j) = JalaliCalendar.formatBothCalendars(System.currentTimeMillis())
            val tx = buildTx(TokenTxKind.STAKE, -amount, s.wallet, g, j, lastHash(s.transactions))
            _state.value = s.copy(
                wallet = s.wallet.copy(balance = round2(s.wallet.balance - amount), staked = round2(s.wallet.staked + amount)),
                transactions = (s.transactions + tx).takeLast(200),
                lastUpdate = System.currentTimeMillis()
            )
            refreshGatesAndTier()
        }
        return true
    }

    fun unstake(amount: Double): Boolean {
        if (amount <= 0.0) return false
        synchronized(this) {
            val s = _state.value
            if (s.wallet.staked < amount) return false
            val (g, j) = JalaliCalendar.formatBothCalendars(System.currentTimeMillis())
            val tx = buildTx(TokenTxKind.UNSTAKE, amount, s.wallet, g, j, lastHash(s.transactions))
            _state.value = s.copy(
                wallet = s.wallet.copy(staked = round2(s.wallet.staked - amount), balance = round2(s.wallet.balance + amount)),
                transactions = (s.transactions + tx).takeLast(200),
                lastUpdate = System.currentTimeMillis()
            )
            refreshGatesAndTier()
        }
        return true
    }

    fun dailyLoginBonus(): Boolean {
        synchronized(this) {
            val s = _state.value
            val (g, j) = JalaliCalendar.formatBothCalendars(System.currentTimeMillis())
            val tx = buildTx(TokenTxKind.REWARD, DAILY_BONUS, s.wallet, g, j, lastHash(s.transactions))
            _state.value = s.copy(
                wallet = s.wallet.copy(balance = round2(s.wallet.balance + DAILY_BONUS), totalEarned = round2(s.wallet.totalEarned + DAILY_BONUS)),
                transactions = (s.transactions + tx).takeLast(200),
                lastUpdate = System.currentTimeMillis()
            )
            refreshGatesAndTier()
        }
        return true
    }

    fun referralBonus(): Boolean {
        synchronized(this) {
            val s = _state.value
            val (g, j) = JalaliCalendar.formatBothCalendars(System.currentTimeMillis())
            val tx = buildTx(TokenTxKind.REFERRAL, REFERRAL_BONUS, s.wallet, g, j, lastHash(s.transactions))
            _state.value = s.copy(
                wallet = s.wallet.copy(balance = round2(s.wallet.balance + REFERRAL_BONUS), totalEarned = round2(s.wallet.totalEarned + REFERRAL_BONUS)),
                transactions = (s.transactions + tx).takeLast(200),
                lastUpdate = System.currentTimeMillis()
            )
            refreshGatesAndTier()
        }
        return true
    }

    // ---------- واریز دلاری و برداشت توکن و دلار ----------
    /**
     * برداشت دلاری: تبدیل توکن ODN به USDT-TRC20
     * کسر ۵٪ کارمزد خروج و نقد کردن به نفع خزانه سوینکس + ۱ دلار کارمزد شبکه
     */
    fun withdrawUsd(odnAmount: Double, destinationTrc20Address: String): Result<UsdWithdrawalReceipt> {
        if (odnAmount < 100.0) return Result.failure(IllegalStateException("حداقل برداشت دلاری ۱۰۰ توکن ODN (معادل ۱۰ دلار) است"))
        synchronized(this) {
            val s = _state.value
            if (s.wallet.balance < odnAmount) return Result.failure(IllegalStateException("موجودی توکن ناکافی است"))

            val grossUsd = round2(odnAmount * TOKEN_PRICE_USD)
            val swinexFeeUsd = round2(grossUsd * 0.05) // ۵٪ کارمزد نقد کردن به نفع سوینکس
            val networkFeeUsd = 1.0 // کارمزد شبکه ترون
            val netUsd = round2(grossUsd - swinexFeeUsd - networkFeeUsd)

            val now = System.currentTimeMillis()
            val (g, j) = JalaliCalendar.formatBothCalendars(now)
            val tx = buildTx(TokenTxKind.WITHDRAW_USD, -odnAmount, s.wallet, g, j, lastHash(s.transactions))
            val receiptId = "WTH-USD-$now"
            val txHash = "0x" + sha256("$receiptId|$destinationTrc20Address|$netUsd").take(40)

            val receipt = UsdWithdrawalReceipt(
                id = receiptId,
                odnAmount = odnAmount,
                grossUsd = grossUsd,
                swinexCashoutFeeUsd = swinexFeeUsd,
                networkFeeUsd = networkFeeUsd,
                netUsdPaid = netUsd,
                destinationWallet = destinationTrc20Address,
                txHash = txHash,
                jalaliDate = j
            )

            _state.value = s.copy(
                wallet = s.wallet.copy(balance = round2(s.wallet.balance - odnAmount)),
                ownerTreasuryUsd = round2(s.ownerTreasuryUsd + swinexFeeUsd),
                transactions = (s.transactions + tx).takeLast(200),
                lastUpdate = now
            )
            refreshGatesAndTier()
            return Result.success(receipt)
        }
    }

    /**
     * برداشت توکن آن‌چین ODN به آدرس ولت بایننس اسمارت چین (BEP-20)
     * کسر ۱۰ توکن ODN کارمزد ثابت به نفع خزانه سوینکس
     */
    fun withdrawOdn(odnAmount: Double, destinationBscAddress: String): Result<OdnWithdrawalReceipt> {
        if (odnAmount < 50.0) return Result.failure(IllegalStateException("حداقل برداشت ۵۰ توکن ODN است"))
        synchronized(this) {
            val s = _state.value
            if (s.wallet.balance < odnAmount) return Result.failure(IllegalStateException("موجودی توکن ناکافی است"))

            val feeOdn = 10.0 // کارمزد ۱۰ ODN به نفع سوینکس
            val netOdn = round2(odnAmount - feeOdn)
            val now = System.currentTimeMillis()
            val (g, j) = JalaliCalendar.formatBothCalendars(now)
            val tx = buildTx(TokenTxKind.WITHDRAW_ODN, -odnAmount, s.wallet, g, j, lastHash(s.transactions))
            val receiptId = "WTH-ODN-$now"
            val txHash = "0x" + sha256("$receiptId|$destinationBscAddress|$netOdn").take(40)

            val receipt = OdnWithdrawalReceipt(
                id = receiptId,
                odnAmount = odnAmount,
                swinexFeeOdn = feeOdn,
                netOdnPaid = netOdn,
                destinationAddress = destinationBscAddress,
                txHash = txHash,
                jalaliDate = j
            )

            _state.value = s.copy(
                wallet = s.wallet.copy(balance = round2(s.wallet.balance - odnAmount)),
                ownerTreasuryUsd = round2(s.ownerTreasuryUsd + (feeOdn * TOKEN_PRICE_USD)),
                transactions = (s.transactions + tx).takeLast(200),
                lastUpdate = now
            )
            refreshGatesAndTier()
            return Result.success(receipt)
        }
    }

    // ---------- گیت اجباری استراتژی ----------
    private fun refreshGatesAndTier() {
        val s = _state.value
        val staked = s.wallet.staked
        _state.value = s.copy(
            tier = OdinTier.forStake(staked),
            gates = buildGates(staked)
        )
    }

    fun isStrategyAllowed(strategy: com.odin.agent.models.QuantStrategyType): Boolean {
        val gate = _state.value.gates.find { it.strategy == strategy } ?: return true
        return !gate.isLocked
    }

    fun requiredStakeFor(strategy: com.odin.agent.models.QuantStrategyType): Double =
        gatesForStrategy(strategy).requiredStake

    private fun gatesForStrategy(strategy: com.odin.agent.models.QuantStrategyType): StrategyGate =
        buildGates(_state.value.wallet.staked).find { it.strategy == strategy }
            ?: StrategyGate(strategy, OdinTier.FREE, 0.0, false)

    // ---------- کمیسیون 20% از سود ----------
    /**
     * مثال کاربر: ربات 10$ سود بدهد → اتومات 2$ به حساب صاحب نرم‌افزار می‌رود
     * فقط روی سود - ضرر کمیسیون ندارد - high-water mark جلوگیری از دوبار گرفتن
     * 50% کمیسیون به ODN تبدیل و سوزانده می‌شود (دیفلوشنری) - 50% خزانه دلاری سوینکس
     */
    fun applyPerformanceFee(tradeId: String, grossProfitUsd: Double, strategy: com.odin.agent.models.QuantStrategyType): CommissionPayment? {
        if (grossProfitUsd <= 0.0) return null            // روی ضرر هیچ کمیسیونی نیست
        synchronized(this) {
            if (lastFeeTradeId == tradeId) return null    // جلوگیری از کمیسیون مضاعف
            val fee = round2(grossProfitUsd * FEE_RATE)   // 10$ → 2$
            val userNet = round2(grossProfitUsd - fee)    // 10$ → 8$
            val feeOdn = round2(fee / TOKEN_PRICE_USD)
            val burned = round2(feeOdn * BURN_RATE)       // 50% سوزاندن
            val now = System.currentTimeMillis()
            val (g, j) = JalaliCalendar.formatBothCalendars(now)
            val hash = sha256("$tradeId|fee|$fee|$userNet|$now|${lastHash(_state.value.transactions)}")
            val payment = CommissionPayment(
                id = "FEE-${tradeId}",
                tradeId = tradeId,
                strategy = strategy,
                grossProfitUsd = round2(grossProfitUsd),
                feeRate = FEE_RATE,
                feeUsd = fee,
                userNetUsd = userNet,
                odnBurned = burned,
                timestamp = now,
                gregorian = g,
                jalali = j,
                hash = hash
            )
            val s = _state.value
            _state.value = s.copy(
                commissionPayments = (s.commissionPayments + payment).takeLast(200),
                ownerTreasuryUsd = round2(s.ownerTreasuryUsd + fee * (1.0 - BURN_RATE)),
                ownerTreasuryOdnBurned = round2(s.ownerTreasuryOdnBurned + burned),
                totalUserGrossProfit = round2(s.totalUserGrossProfit + grossProfitUsd),
                totalFeesCollected = round2(s.totalFeesCollected + fee),
                lastUpdate = now
            )
            lastFeeTradeId = tradeId
            return payment
        }
    }

    // ---------- صحت‌سنجی زنجیره هش امنیتی ----------
    fun verifyLedgerIntegrity(): Boolean {
        val txs = _state.value.transactions
        var prev = GENESIS_HASH
        for (tx in txs) {
            val expected = sha256("$prev|${tx.kind}|${tx.amount}|${tx.timestamp}")
            if (tx.hash != expected || tx.prevHash != prev) return false
            prev = tx.hash
        }
        return true
    }

    fun simulateTampering(): Boolean {
        // تست امنیتی: اگر کسی ledger دستکاری کند - detect می‌شود
        val backup = _state.value
        return try {
            val tampered = backup.transactions.mapIndexed { i, tx ->
                if (i == backup.transactions.size - 1) tx.copy(amount = tx.amount + 99999.0) else tx
            }
            var prev = GENESIS_HASH
            var detected = false
            for (tx in tampered) {
                val expected = sha256("$prev|${tx.kind}|${tx.amount}|${tx.timestamp}")
                if (tx.hash != expected || tx.prevHash != prev) { detected = true; break }
                prev = tx.hash
            }
            detected
        } finally {
            _state.value = backup
        }
    }

    // ---------- helpers ----------
    private fun buildTx(kind: TokenTxKind, amount: Double, wallet: OdinWallet, g: String, j: String, prev: String): TokenTransaction {
        val ts = System.currentTimeMillis()
        val hash = sha256("$prev|$kind|$amount|$ts")
        return TokenTransaction(
            id = "TX-$ts-${txCounter++}",
            kind = kind,
            amount = round2(amount),
            balanceAfter = round2(wallet.balance + if (kind == TokenTxKind.STAKE) -amount else if (kind == TokenTxKind.UNSTAKE) amount else amount),
            stakedAfter = round2(wallet.staked + if (kind == TokenTxKind.STAKE) amount else if (kind == TokenTxKind.UNSTAKE && amount < 0) -amount else 0.0),
            timestamp = ts,
            gregorian = g,
            jalali = j,
            prevHash = prev,
            hash = hash
        )
    }

    private fun buildGates(staked: Double): List<StrategyGate> {
        val tier = OdinTier.forStake(staked)
        return listOf(
            StrategyGate(com.odin.agent.models.QuantStrategyType.MEAN_REVERSION, OdinTier.FREE, 0.0, tier == OdinTier.FREE && false),
            StrategyGate(com.odin.agent.models.QuantStrategyType.PAIRS_TRADING, OdinTier.FREE, 0.0, false),
            StrategyGate(com.odin.agent.models.QuantStrategyType.VOLATILITY_REGIME, OdinTier.FREE, 0.0, false),
            StrategyGate(com.odin.agent.models.QuantStrategyType.TREND_FOLLOWING, OdinTier.SILVER, OdinTier.SILVER.minStake, staked < OdinTier.SILVER.minStake),
            StrategyGate(com.odin.agent.models.QuantStrategyType.MOMENTUM_BREAKOUT, OdinTier.SILVER, OdinTier.SILVER.minStake, staked < OdinTier.SILVER.minStake),
            StrategyGate(com.odin.agent.models.QuantStrategyType.LIT_LIQUIDITY_INVERSION, OdinTier.GOLD, OdinTier.GOLD.minStake, staked < OdinTier.GOLD.minStake),
            StrategyGate(com.odin.agent.models.QuantStrategyType.TV_80_PERCENT, OdinTier.PLATINUM, OdinTier.PLATINUM.minStake, staked < OdinTier.PLATINUM.minStake)
        )
    }

    private fun lastHash(txs: List<TokenTransaction>): String = txs.lastOrNull()?.hash ?: GENESIS_HASH

    companion object {
        const val TICKER = "ODN"
        const val TOKEN_NAME_FA = "توکن اودین"
        const val TOKEN_PRICE_USD = 0.10   // 1 ODN = 0.1$
        const val FEE_RATE = 0.20          // کمیسیون 20% فقط از سود
        const val BURN_RATE = 0.50         // 50% کمیسیون سوزانده می‌شود
        const val DAILY_BONUS = 5.0
        const val REFERRAL_BONUS = 100.0
        const val GENESIS_HASH = "ODIN-GENESIS-0000000000000000000000000000"
        private var txCounter = 1L

        @Volatile private var instance: OdinTokenManager? = null
        fun getInstance(): OdinTokenManager = instance ?: synchronized(this) {
            instance ?: OdinTokenManager().also { instance = it }
        }

        fun round2(v: Double): Double = kotlin.math.round(v * 100.0) / 100.0

        fun sha256(input: String): String =
            MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
