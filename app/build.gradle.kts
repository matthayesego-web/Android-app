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
        versionCode = 49
        versionName = "0.9.32"
        manifestPlaceholders["appLabel"] = "TornFCA"

        // Closed-beta developer gate. The temporary PIN remains hash-only, and the console also
        // requires the verified Torn identity of the app owner before any developer controls open.
        val devHash = "BFB3F43E00B5530E0A2E42343287C792193DB09A9D87515B19A29992E18BAF7D"
        val factionBackend = localOrEnv("TORNFCA_FACTION_BACKEND_URL")
        val premiumBackend = localOrEnv("TORNFCA_PREMIUM_BACKEND_URL")
        val communityBackend = localOrEnv("TORNFCA_COMMUNITY_BACKEND_URL")
        val firebaseAppId = localOrEnv("TORNFCA_FIREBASE_APP_ID")
        val firebaseApiKey = localOrEnv("TORNFCA_FIREBASE_API_KEY")
        val firebaseProjectId = localOrEnv("TORNFCA_FIREBASE_PROJECT_ID")
        val firebaseSenderId = localOrEnv("TORNFCA_FIREBASE_SENDER_ID")
        buildConfigField("String", "DEVELOPER_ACCESS_SHA256", quotedBuildValue(devHash))
        buildConfigField("int", "DEVELOPER_PLAYER_ID", "3987363")
        buildConfigField("String", "FACTION_BACKEND_URL", quotedBuildValue(factionBackend))
        buildConfigField("String", "PREMIUM_BACKEND_URL", quotedBuildValue(premiumBackend))
        buildConfigField("String", "COMMUNITY_BACKEND_URL", quotedBuildValue(communityBackend))
        buildConfigField("String", "FIREBASE_APP_ID", quotedBuildValue(firebaseAppId))
        buildConfigField("String", "FIREBASE_API_KEY", quotedBuildValue(firebaseApiKey))
        buildConfigField("String", "FIREBASE_PROJECT_ID", quotedBuildValue(firebaseProjectId))
        buildConfigField("String", "FIREBASE_SENDER_ID", quotedBuildValue(firebaseSenderId))
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
            manifestPlaceholders["appLabel"] = "Torn FCA Beta"
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
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.firebase:firebase-messaging")
}
