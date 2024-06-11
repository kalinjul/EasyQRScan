plugins {
    id("org.publicvalue.convention.android.application")
    id("org.publicvalue.convention.kotlin.multiplatform.mobile")
    id("org.publicvalue.convention.compose.multiplatform")
}

kotlin {
    androidTarget()
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)

                implementation(projects.sampleApp.shared)
            }
        }
    }
}

android {
    namespace = "org.publicvalue.multiplatform.qrcode.sample"

    defaultConfig {
        applicationId = "org.publicvalue.multiplatform.qrcode.sample"
        versionCode = 1
        versionName = "1.0"
    }
}
