// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // ★★★ 請加入底下這一行 (Firebase 必備) ★★★
    id("com.google.gms.google-services") version "4.4.2" apply false
}