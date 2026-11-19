import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "pe.pluscontable.cliente"
    compileSdk = 37
    defaultConfig {
        applicationId = "pe.pluscontable.cliente"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }
    buildFeatures { buildConfig = true }
    // Credenciales fuera del repo: keystore.properties (ignorado por Git). Sin él, el release sale sin firmar.
    val firma = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    signingConfigs {
        if (firma.isNotEmpty()) create("release") {
            storeFile = rootProject.file(firma.getProperty("storeFile"))
            storePassword = firma.getProperty("storePassword")
            keyAlias = firma.getProperty("keyAlias")
            keyPassword = firma.getProperty("keyPassword")
        }
    }
    buildTypes {
        debug {
            // Emulador -> ms-auth en la PC. Celular real: cambiar por la IP de la PC.
            buildConfigField("String", "AUTH_URL", "\"http://10.0.2.2:8081/\"")
            manifestPlaceholders["cleartext"] = "true"
        }
        release {
            // shortcut: URL de producción sin definir; fijarla (sin subirla al repo) antes del APK final.
            buildConfigField("String", "AUTH_URL", "\"https://cambiar.invalid/\"")
            manifestPlaceholders["cleartext"] = "false"
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.activity.compose)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.security.crypto)
    implementation(libs.sqldelight.android)
}
