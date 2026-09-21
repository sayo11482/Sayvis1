# ODIN v1.0.21 - ProGuard rules - Meta-level security - REAL ONLY
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# Keep ODIN models - needed for trading
-keep class com.odin.agent.models.** { *; }
-keep class com.odin.agent.trading.** { *; }
-keep class com.odin.agent.aware.** { *; }
-keep class com.odin.agent.mt5.** { *; }
-keep class com.odin.agent.nobitex.** { *; }
-keep class com.odin.agent.risk.** { *; }
-keep class com.odin.agent.indicators.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-keep class com.odin.agent.ui.** { *; }

# Keep Moshi / Room
-keep class org.json.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation class * {
  @com.squareup.moshi.Json *;
}

# WebView JS interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# OkHttp
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
