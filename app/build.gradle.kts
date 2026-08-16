plugins {
    id("com.android.application")
}

val configuredDeveloperHash = providers.gradleProperty("TORNFCA_DEV_PASSWORD_SHA256").orNull
    ?.trim()?.takeIf { it.isNotEmpty() }
    ?: System.getenv("TORNFCA_DEV_PASSWORD_SHA256")?.trim()?.takeIf { it.isNotEmpty() }
    ?: "AD039B0643FE2CD75558E56B90955252ED3F56CE6B2B7AA90CD1ED3BC22AC6AB"

android {
    namespace = "com.matthayesego.duckforcetoolkit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.matthayesego.duckforcetoolkit"
        minSdk = 24
        targetSdk = 36
        versionCode = 25
        versionName = "0.9.10"
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
