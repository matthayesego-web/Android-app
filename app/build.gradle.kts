plugins {
    id("com.android.application")
}

val configuredDeveloperHash = providers.gradleProperty("TORNFCA_DEV_PASSWORD_SHA256").orNull
    ?.trim()?.takeIf { it.isNotEmpty() }
    ?: System.getenv("TORNFCA_DEV_PASSWORD_SHA256")?.trim()?.takeIf { it.isNotEmpty() }
    ?: "8AC1118BA3EAA1A258BF399E88EEB2A32683C1993A2373C44B939F4CEF5C0012"

android {
    namespace = "com.matthayesego.duckforcetoolkit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.matthayesego.duckforcetoolkit"
        minSdk = 24
        targetSdk = 36
        versionCode = 26
        versionName = "0.9.11"
        manifestPlaceholders["appLabel"] = "TornFCA"
        buildConfigField("String", "DEVELOPER_ACCESS_SHA256", "\"$configuredDeveloperHash\"")
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
