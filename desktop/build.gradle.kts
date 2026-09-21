plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.jetbrains.compose)
}

dependencies {
  implementation(compose.foundation)
  implementation(compose.material3)
  implementation(compose.ui)
  implementation(compose.desktop.currentOs)
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
  implementation("com.squareup.okhttp3:okhttp:4.10.0")
  implementation("org.json:json:20240303")
  implementation("com.google.zxing:core:3.5.3")
}

kotlin {
  jvmToolchain(17)
}

compose.desktop.application {
  mainClass = "desktop.MainKt"

  nativeDistributions {
    packageName = "SAYVIS"
    packageVersion = "1.0.0"
  }
}
