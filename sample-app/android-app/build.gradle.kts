plugins {
    id("org.publicvalue.convention.android.application")
    id("org.publicvalue.convention.compose.multiplatform")
}

dependencies {
    implementation(projects.sampleApp.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
}

android {
    namespace = "org.publicvalue.multiplatform.qrcode.sample"

    defaultConfig {
        applicationId = "org.publicvalue.multiplatform.qrcode.sample"
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}
