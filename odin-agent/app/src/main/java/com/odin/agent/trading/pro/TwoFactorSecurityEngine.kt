package com.odin.agent.trading.pro

import com.odin.agent.trading.OdinTokenManager

/**
 * ODIN PRO v1.0.28 - Two-Factor Authentication (2FA) Security Engine
 * امنیت دو مرحله‌ای برای عملیات‌های حساس (برداشت توکن، خروج از استیک، معامله لایو)
 */

class TwoFactorSecurityEngine(
    private var securityPinHash: String = OdinTokenManager.sha256("123456") // پین پیش‌فرض
) {
    private var isSessionAuthenticated: Boolean = false
    private var lastAuthTime: Long = 0

    fun verifyPin(pin: String): Boolean {
        if (pin.length != 6) return false
        val hashed = OdinTokenManager.sha256(pin)
        val matches = (hashed == securityPinHash)
        if (matches) {
            isSessionAuthenticated = true
            lastAuthTime = System.currentTimeMillis()
        }
        return matches
    }

    fun isAuthorized(maxIdleMinutes: Int = 15): Boolean {
        if (!isSessionAuthenticated) return false
        val elapsed = (System.currentTimeMillis() - lastAuthTime) / 60000L
        return elapsed <= maxIdleMinutes
    }

    fun setPin(newPin: String) {
        if (newPin.length == 6) {
            securityPinHash = OdinTokenManager.sha256(newPin)
        }
    }

    fun logout() {
        isSessionAuthenticated = false
    }
}
