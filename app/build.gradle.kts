plugins {
    id("com.android.application")
}

import java.security.MessageDigest
import java.util.Base64
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

val generatedIconResDir = layout.buildDirectory.dir("generated/tornfcaIconRes")
val launcherIconParts = fileTree("icon-assets") { include("tornfca_launcher_final.webp.b64.part.*") }
val prepareTornFcaLauncherIcon by tasks.registering {
    val outputIcon = generatedIconResDir.map { it.file("drawable-nodpi/tornfca_launcher_final.webp") }
    inputs.files(launcherIconParts)
    outputs.file(outputIcon)
    doLast {
        val parts = launcherIconParts.files.sortedBy { it.name }
        require(parts.size == 5) { "Expected 5 TornFCA launcher icon chunks, found ${parts.size}." }
        val encoded = parts.joinToString("") { it.readText().replace("\\s".toRegex(), "") }
        val bytes = Base64.getDecoder().decode(encoded)
        require(bytes.size == 23948) { "Unexpected TornFCA launcher icon size: ${bytes.size}." }
        require(String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP") { "TornFCA launcher icon is not a valid WebP payload." }
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        require(sha256 == "2e6186478635ff866e777b758e4bb26599f9672c6dbfcbcb46b1a6799c26bd57") { "TornFCA launcher icon checksum mismatch." }
        val target = outputIcon.get().asFile
        target.parentFile.mkdirs()
        target.writeBytes(bytes)
    }
}

android {
    namespace = "com.matthayesego.duckforcetoolkit"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.matthayesego.duckforcetoolkit"
        minSdk = 24
        targetSdk = 36
        versionCode = 73
        versionName = "0.10.18"
        manifestPlaceholders["appLabel"] = "TornFCA"

        val factionBackend = localOrEnv("TORNFCA_FACTION_BACKEND_URL", "https://script.google.com/macros/s/AKfycbzR8zjFVYaDoIZaxFdJV4yBaM4mLSv-iqZL8146HCzq6vRzJgeYr1Y0TKkPGrdASjQj/exec")
        val premiumBackend = localOrEnv("TORNFCA_PREMIUM_BACKEND_URL", "https://script.google.com/macros/s/AKfycbxGp-l6UYQb6MzSSWrhHMo9QNuYlsJfvw1c2qYxdoPnFr-G5TxeUOFOgr1ziOZHpWi7/exec")
        val communityBackend = localOrEnv("TORNFCA_COMMUNITY_BACKEND_URL", "https://script.google.com/macros/s/AKfycbwphLmR-N82GJUChzwZlxQ8DGKVoaN6VCNiXvbpKfgJV6JUxlcwPOYVKgbI1h9tmjp-ig/exec")
        val developerBackend = localOrEnv("TORNFCA_DEVELOPER_BACKEND_URL", "https://script.google.com/macros/s/AKfycbwQeLGPlVpbHgqq2ZG9b73r1PUNvroWQ1CQoy-DXNYCw4W-bCX9VhDkUM7yy_ydw_rjrQ/exec")
        val warPayBackend = localOrEnv("TORNFCA_WARPAY_BACKEND_URL", "https://script.google.com/macros/s/AKfycbzb2RKPNS_Q4LsAK7nIC2oId8UqHXEd-1t93lyyY8bNz3S-7eMJCP1BqP1bnrNq4JiTGQ/exec")
        val feedbackBackend = localOrEnv("TORNFCA_FEEDBACK_BACKEND_URL", "https://script.google.com/macros/s/AKfycbyAFt3NNQJWOrbWOuYsH-1MZjf0gxGYZ9l4oSTFtN4unBaXpS2u_Go5xOdP-tIAWi2X/exec")
        val firebaseAppId = localOrEnv("TORNFCA_FIREBASE_APP_ID")
        val firebaseApiKey = localOrEnv("TORNFCA_FIREBASE_API_KEY")
        val firebaseProjectId = localOrEnv("TORNFCA_FIREBASE_PROJECT_ID")
        val firebaseSenderId = localOrEnv("TORNFCA_FIREBASE_SENDER_ID")
        buildConfigField("int", "DEVELOPER_PLAYER_ID", "3987363")
        buildConfigField("String", "FACTION_BACKEND_URL", quotedBuildValue(factionBackend))
        buildConfigField("String", "PREMIUM_BACKEND_URL", quotedBuildValue(premiumBackend))
        buildConfigField("String", "COMMUNITY_BACKEND_URL", quotedBuildValue(communityBackend))
        buildConfigField("String", "DEVELOPER_BACKEND_URL", quotedBuildValue(developerBackend))
        buildConfigField("String", "WARPAY_BACKEND_URL", quotedBuildValue(warPayBackend))
        buildConfigField("String", "FEEDBACK_BACKEND_URL", quotedBuildValue(feedbackBackend))
        buildConfigField("String", "FIREBASE_APP_ID", quotedBuildValue(firebaseAppId))
        buildConfigField("String", "FIREBASE_API_KEY", quotedBuildValue(firebaseApiKey))
        buildConfigField("String", "FIREBASE_PROJECT_ID", quotedBuildValue(firebaseProjectId))
        buildConfigField("String", "FIREBASE_SENDER_ID", quotedBuildValue(firebaseSenderId))
    }

    buildFeatures { buildConfig = true }
    sourceSets.getByName("main").res.srcDir(generatedIconResDir)

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

    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

tasks.named("preBuild").configure { dependsOn(prepareTornFcaLauncherIcon) }

dependencies {
    implementation("androidx.webkit:webkit:1.14.0")
    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.firebase:firebase-messaging")
}
