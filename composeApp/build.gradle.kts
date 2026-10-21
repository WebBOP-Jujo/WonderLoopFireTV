import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Secrets live in the git-ignored local.properties (or env vars on CI), never in source control
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val wonderLoopAppKey: String = localProperties.getProperty("wonderloop.appKey")
    ?: System.getenv("WONDERLOOP_APP_KEY")
    ?: ""
// Public HTTPS backend on Amazon EC2. Override with wonderloop.backendUrl (e.g. http://192.168.0.35:3000 for a LAN backend)
val wonderLoopBackendUrl: String = (localProperties.getProperty("wonderloop.backendUrl")
    ?: System.getenv("WONDERLOOP_BACKEND_URL")
    ?: "https://13-217-171-0.sslip.io").trimEnd('/')

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.revenuecat.purchases)
            // Compose for TV (Google): focus-aware Surface/Card/Button tuned for D-pad remotes (used via ui/tv primitives)
            implementation(libs.androidx.tv.material)
            implementation("com.google.zxing:core:3.5.3")
        }
    }
}

android {
    namespace = "org.wonderloop.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.wonderloop.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "WONDERLOOP_APP_KEY", "\"$wonderLoopAppKey\"")
        buildConfigField("String", "WONDERLOOP_BACKEND_URL", "\"$wonderLoopBackendUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
