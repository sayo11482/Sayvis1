package com.odin.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.odin.agent.ui.screens.SimpleTradingSuiteScreen
import com.odin.agent.ui.theme.OdinDeepSpace
import com.odin.agent.ui.theme.OdinTheme

/**
 * ODIN.TRADE PRO - MAIN APPLICATION
 * دیزاین ساده، سریع و متمرکز:
 * ۱. چارت زنده و تریدینگ‌ویو (با لاگین گوگل)
 * ۲. شناسایی و تست نقطه ورود جهت ترید واقعی
 * ۳. اتصال متاتریدر ۵ و درگاه وب‌تریدر
 * ۴. مدیریت پوزیشن‌های فعال ECN
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OdinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = OdinDeepSpace
                ) {
                    SimpleTradingSuiteScreen(isPersian = true)
                }
            }
        }
    }
}
