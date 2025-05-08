import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kmp)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)

            implementation(projects.sampleApp.shared)
        }
    }
}

android {
    namespace = "org.publicvalue.multiplatform.qrcode.sample"

    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        applicationId = "org.publicvalue.multiplatform.qrcode.sample"
        versionCode = 1
        versionName = "1.0"
    }
}
