plugins {
    id("com.android.application")
}

fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val configuredDeveloperHash = providers.gradleProperty("TORNFCA_DEV_PASSWORD_SHA256").orNull
    ?.trim()?.takeIf { it.isNotEmpty() }
    ?: System.getenv("TORNFCA_DEV_PASSWORD_SHA256")?.trim()?.takeIf { it.isNotEmpty() }
    ?: "8AC1118BA3EAA1A258BF399E88EEB2A32683C1993A2373C44B939F4CEF5C0012"

val factionBackendUrl = providers.gradleProperty("TORNFCA_FACTION_BACKEND_URL").orNull
    ?.trim()?.takeIf { it.isNotEmpty() }
    ?: System.getenv("TORNFCA_FACTION_BACKEND_URL")?.trim()?.takeIf { it.isNotEmpty() }
    ?: ""

val premiumBackendUrl = providers.gradleProperty("TORNFCA_PREMIUM_BACKEND_URL").orNull
    ?.trim()?.takeIf { it.isNotEmpty() }
    ?: System.getenv("TORNFCA_PREMIUM_BACKEND_URL")?.trim()?.takeIf { it.isNotEmpty() }
    ?: ""

android {
    namespace = "com.matthayesego.duckforcetoolkit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.matthayesego.duckforcetoolkit"
        minSdk = 24
        targetSdk = 36
        versionCode = 27
        versionName = "0.9.12"
        manifestPlaceholders["appLabel"] = "TornFCA"
        buildConfigField("String", "DEVELOPER_ACCESS_SHA256", configuredDeveloperHash.asBuildConfigString())
        buildConfigField("String", "FACTION_BACKEND_URL", factionBackendUrl.asBuildConfigString())
        buildConfigField("String", "PREMIUM_BACKEND_URL", premiumBackendUrl.asBuildConfigString())
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".internal"
            versionNameSuffix = "-internal"
            manifestPlaceholders["appLabel"] = "TornFCA INTERNAL"
        }
        create("beta") {
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
            manifestPlaceholders["appLabel"] = "TornFCA BETA"
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.16.0")
}
