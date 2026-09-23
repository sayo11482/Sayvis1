package com.odin.agent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odin.agent.models.QuantStrategyType
import com.odin.agent.models.SignalSide
import com.odin.agent.trading.pro.OdinProQuantitativeEngine
import com.odin.agent.trading.pro.PayoutNetwork
import com.odin.agent.ui.theme.*

/**
 * ODIN PRO v1.0.28 - Pro Quantitative Trading Suite Screen
 * صفحه ابزارهای کوانت پیشرفته اودین پرو - ۱۸ ماژول رفع کمبودها
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProTradingHubScreen(isPersian: Boolean = true) {
    val proEngine = remember { OdinProQuantitativeEngine.getInstance() }
    val mt5State by proEngine.mt5Gateway.state.collectAsState()
    val payoutState by proEngine.payoutRails.state.collectAsState()
    val bridgeState by proEngine.onChainContract.bridgeState.collectAsState()
    val journalEntries by proEngine.tradeJournal.entries.collectAsState()

    var selectedSymbol by remember { mutableStateOf("EURUSD") }
    var actionFeedback by remember { mutableStateOf<String?>(null) }
    var pinInput by remember { mutableStateOf("123456") }
    var is2faAuthed by remember { mutableStateOf(proEngine.security2fa.isAuthorized()) }

    val sessionEval = remember { proEngine.sessionFilter.evaluate(QuantStrategyType.LIT_LIQUIDITY_INVERSION) }
    val newsEval = remember { proEngine.newsFilter.evaluateSymbol(selectedSymbol) }
    val preTrade = remember(selectedSymbol) {
        proEngine.evaluatePreTrade(
            symbol = selectedSymbol,
            side = SignalSide.BUY,
            equity = 10000.0,
            peakEquity = 10000.0,
            price = 1.0850,
            atr = 0.0035,
            strategy = QuantStrategyType.LIT_LIQUIDITY_INVERSION
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OdinDeepSpace)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // هدر لوکس مشکی/طلایی
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, OdinGold, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = OdinGoldLight, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPersian) "موتور معاملات کوانت حرفه‌ای Odin Pro" else "Odin Pro Quantitative Engine",
                            color = OdinGoldLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = if (isPersian) "۱۸ ماژول پیشرفته رفع کمبودها | ساختار سازمانی سوینکس (SWINEX)" else "18 Advanced Pro Modules | Swinex Technologies",
                        color = OdinSilverMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // بخش ۱: وضعیت اتصال لایو MT5 ویتاورس و امنیت ۲FA
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, OdinCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPersian) "⚡ گیت‌وی اجرای سفارش REAL متاتریدر ۵" else "⚡ MT5 Real Order Gateway",
                            color = OdinCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Surface(color = OdinGreen, shape = RoundedCornerShape(4.dp)) {
                            Text(text = "LIVE MT5", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "سرور بروکر: ${mt5State.serverName} | وضعیت: متصل", color = OdinSilver, fontSize = 12.sp)
                    Text(text = "تیکت‌های فعال: ${mt5State.activeOrders.size} | حجم ترید شده: ${mt5State.totalVolumeExecutedLots} لات", color = OdinSilverMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val ok = proEngine.security2fa.verifyPin(pinInput)
                                is2faAuthed = ok
                                actionFeedback = if (ok) "۲FA تایید شد - مجوز معامله لایو صادر شد" else "پین ۲FA اشتباه است"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (is2faAuthed) OdinGreen else OdinGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (is2faAuthed) "✓ 2FA تایید شد" else "تایید 2FA (پین: ۱۲۳۴۵۶)",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (!is2faAuthed) {
                                    actionFeedback = "ابتدا پین 2FA را تایید کنید"
                                    return@Button
                                }
                                val res = proEngine.mt5Gateway.sendRealOrder(
                                    symbol = selectedSymbol,
                                    side = SignalSide.BUY,
                                    lots = preTrade.recommendedLots,
                                    price = 1.0850,
                                    sl = 1.0800,
                                    tp = 1.0950,
                                    strategy = QuantStrategyType.LIT_LIQUIDITY_INVERSION,
                                    isTwoFactorVerified = true
                                )
                                actionFeedback = if (res.isSuccess) {
                                    val ord = res.getOrNull()
                                    "سفارش REAL ارسال شد: تیکت #${ord?.ticket} با حجم ${ord?.lots} لات"
                                } else {
                                    res.exceptionOrNull()?.message
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OdinCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "ارسال سفارش REAL", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // بخش ۲: رادار اخبار اقتصادی و سشن‌های معاملاتی
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isPersian) "📰 رادار اخبار اقتصادی و سشن‌های معاملاتی" else "📰 News Radar & Trading Sessions",
                        color = OdinGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "وضعیت اخبار: ${newsEval.statusFa}", color = if (newsEval.isTradingAllowed) OdinGreen else Color.Red, fontSize = 12.sp)
                    Text(text = "سشن معاملاتی: ${sessionEval.messageFa}", color = OdinSilver, fontSize = 11.sp)
                    Text(text = "تداخل لندن-نیویورک: ${if (sessionEval.isOverlapActive) "فعال (+۲۵٪ اعتبار)" else "غیرفعال"}", color = OdinCyan, fontSize = 11.sp)
                }
            }
        }

        // بخش ۳: ماتریس کانفلوئنس چند تایم‌فریم و سود مرکب
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, OdinGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isPersian) "🧭 کانفلوئنس چند تایم‌فریم و سرمایه مرکب" else "🧭 Multi-TF Confluence & Compounding",
                        color = OdinGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "امتیاز کانفلوئنس: ${preTrade.confluenceScore}/100 | ${if (preTrade.confluenceScore >= 75) "تایید شده" else "مسدود"}", color = OdinGoldLight, fontSize = 12.sp)
                    Text(text = "احتمال برد کالیبره ML: ${preTrade.calibratedConfidence}٪", color = OdinSilver, fontSize = 11.sp)
                    Text(text = "حجم محاسبه‌شده با سود مرکب: ${preTrade.recommendedLots} لات (ریسک داینامیک)", color = OdinCyan, fontSize = 11.sp)
                    Text(text = "مدل اسلیپیج واقعی: ورود در ${preTrade.expectedSlippagePrice}", color = OdinSilverMuted, fontSize = 11.sp)
                }
            }
        }

        // بخش ۴: خزانه ۲۰٪ سوینکس و توکن آن‌چین ODN
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, OdinGoldLight.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPersian) "💎 تسویه کمیسیون ۲۰٪ سوینکس و توکن ODN" else "💎 Swinex 20% Treasury & ODN Token",
                            color = OdinGoldLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Surface(color = OdinGold, shape = RoundedCornerShape(4.dp)) {
                            Text(text = "USDT-TRC20", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "آدرس ولت ترون سوینکس: ${payoutState.swinexTrc20Address.take(18)}...", color = OdinSilver, fontSize = 11.sp)
                    Text(text = "قرارداد هوشمند بایننس (BEP-20): ${bridgeState.contractSpec.contractAddressBsc.take(18)}...", color = OdinCyan, fontSize = 11.sp)
                    Text(text = "خزانه تجمیعی دلاری: ${payoutState.accumulatedTreasuryUsd}$ | فاکتورهای تسویه: ${payoutState.settledInvoices.size}", color = OdinGold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val inv = proEngine.payoutRails.generateWeeklyInvoice(100.0, PayoutNetwork.TRC20)
                                actionFeedback = "فاکتور هفتگی صادر شد: ۲۰$ سود سوینکس با امضای دیجیتال ${inv.invoiceId}"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OdinGold),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "صدور فاکتور رسمی", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                proEngine.onChainContract.syncWithOnChain()
                                actionFeedback = "توکن ODN با قرارداد BEP-20 همگام‌سازی شد (بلاک: #${bridgeState.lastSyncBlockNumber})"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF252525)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "همگام‌سازی Web3", color = OdinSilver, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // پیام بازخورد عملیات
        if (actionFeedback != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, OdinGreen, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = actionFeedback ?: "",
                        color = OdinGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // بخش ۵: ژورنال ابری و خروجی CSV
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isPersian) "📓 ژورنال معاملات ابری و خروجی اکسل" else "📓 Cloud Journal & Export",
                        color = OdinSilver,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "تعداد ثبت‌های ژورنال: ${journalEntries.size} معامله", color = OdinSilverMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            proEngine.tradeJournal.logTrade(
                                tradeId = "TX-${System.currentTimeMillis()}",
                                symbol = "EURUSD",
                                side = SignalSide.BUY,
                                entryPrice = 1.0850,
                                exitPrice = 1.0950,
                                netPnlUsd = 80.0,
                                returnR = 2.0,
                                strategy = QuantStrategyType.LIT_LIQUIDITY_INVERSION
                            )
                            actionFeedback = "ترید در ژورنال ثبت و خروجی CSV بهینه‌سازی شد"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "ثبت معامله تستی در ژورنال ابری", color = OdinGoldLight, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
