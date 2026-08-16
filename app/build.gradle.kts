plugins {
    id("com.android.application")
}

import java.util.Properties

fun localOrEnv(name: String, fallback: String = ""): String {
    val fromEnv = System.getenv(name)
    if (!fromEnv.isNullOrBlank()) return fromEnv.trim()
    val props = Properties()
    val local = rootProject.file("local.properties")
    if (local.exists()) local.inputStream().use { props.load(it) }
    return props.getProperty(name, fallback).trim()
}

fun quotedBuildValue(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.matthayesego.duckforcetoolkit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.matthayesego.duckforcetoolkit"
        minSdk = 24
        targetSdk = 36
        versionCode = 32
        versionName = "0.9.17"
        manifestPlaceholders["appLabel"] = "TornFCA"

        // Closed-beta temporary developer gate. Only the one-way SHA-256 hash is shipped; the
        // plaintext password is never embedded in source or the APK. Replace before public release.
        val devHash = "BFB3F43E00B5530E0A2E42343287C792193DB09A9D87515B19A29992E18BAF7D"
        val factionBackend = localOrEnv("TORNFCA_FACTION_BACKEND_URL")
        val premiumBackend = localOrEnv("TORNFCA_PREMIUM_BACKEND_URL")
        buildConfigField("String", "DEVELOPER_ACCESS_SHA256", quotedBuildValue(devHash))
        buildConfigField("String", "FACTION_BACKEND_URL", quotedBuildValue(factionBackend))
        buildConfigField("String", "PREMIUM_BACKEND_URL", quotedBuildValue(premiumBackend))
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".internal"
            versionNameSuffix = "-internal"
            manifestPlaceholders["appLabel"] = "TornFCA Internal"
        }
        create("beta") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            manifestPlaceholders["appLabel"] = "TornFCA Beta"
            isDebuggable = false
            signingConfig = null
        }
        release {
            isMinifyEnabled = false
            signingConfig = null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.14.0")
}
