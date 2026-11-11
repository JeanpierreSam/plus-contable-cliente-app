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
        versionName = "0.1.0"
    }
    buildFeatures { buildConfig = true }
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
