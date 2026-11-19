plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kover)
    alias(libs.plugins.sonarqube)
}

sonar {
    properties {
        property("sonar.projectKey", "pluscontable-cliente-app")
        property("sonar.host.url", "http://localhost:9000")
        property("sonar.sources", "src/commonMain")
        property("sonar.tests", "src/commonTest,src/androidHostTest")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/kover/report.xml")
        // Solo generado: el .sq es la fuente de SQLDelight, no código Kotlin propio.
        property("sonar.exclusions", "**/build/**,**/generated/**,**/*.sq")
    }
}

kotlin {
    android {
        namespace = "pe.pluscontable.cliente.shared"
        compileSdk = 37
        minSdk = 26
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.serialization.json)
            api(libs.koin.core)
            implementation(libs.koin.compose.viewmodel)
            api(libs.settings)
            api(libs.sqldelight.runtime)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
            implementation(libs.coroutines.test)
            implementation(libs.settings.test)
        }
        getByName("androidHostTest").dependencies { implementation(libs.sqldelight.jvm) }
    }
}

sqldelight {
    databases { create("ClienteDb") { packageName.set("pe.pluscontable.cliente.db") } }
}
