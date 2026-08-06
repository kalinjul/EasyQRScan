import org.publicvalue.convention.config.configureIosTargets

plugins {
    id("org.publicvalue.convention.kotlin.multiplatform.mobile")
    id("org.publicvalue.convention.android.library")
    id("org.publicvalue.convention.centralPublish")
    id("org.publicvalue.convention.compose.multiplatform")
}

description = "Compose Multiplatform Camera Utilities for Android/iOS"

kotlin {
    android {
        namespace = "org.publicvalue.multiplatform.easyqrscan.camerautils"
    }

    configureIosTargets()
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.camera.camera2)
            }
        }

        jvmMain {
            dependencies {
            }
        }
    }
}
