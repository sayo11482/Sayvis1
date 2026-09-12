package com.example.sayvis.model

enum class DeviceType(val labelEn: String, val labelFa: String) {
    ANDROID_PHONE("Android Smartphone", "تلفن هوشمند اندروید"),
    WINDOWS_PC("Windows Workstation", "رایانه ویندوز"),
    SECURE_LAPTOP("Secure Laptop", "لپ‌تاپ امن"),
    TABLET("Tablet", "تبلت"),
    WEB_CLIENT("Web Dashboard", "داشبورد وب"),
    EDGE_GATEWAY("Edge Gateway", "دروازه لبه")
}

data class Device(
    val id: String,
    val name: String,
    val type: DeviceType,
    val publicKeyFingerprint: String,
    val isTrusted: Boolean,
    val isRevoked: Boolean,
    val lastActiveAt: Long,
    val capabilities: List<String> = emptyList()
)
