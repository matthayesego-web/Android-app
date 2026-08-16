plugins {
    id("com.android.application")
}

android {
    namespace = "com.matthayesego.duckforcetoolkit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.matthayesego.duckforcetoolkit"
        minSdk = 24
        targetSdk = 36
        versionCode = 24
        versionName = "0.9.9"
        manifestPlaceholders["appLabel"] = "TornFCA"
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
