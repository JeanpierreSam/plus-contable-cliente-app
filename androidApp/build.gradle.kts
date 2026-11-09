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
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.activity.compose)
}
