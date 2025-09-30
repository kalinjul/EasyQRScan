import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.publicvalue.convention.config.configureIosTargets

plugins {
    id("org.publicvalue.convention.android.library")
    id("org.publicvalue.convention.kotlin.multiplatform.mobile")
    id("org.publicvalue.convention.compose.multiplatform")
}

kotlin {
    configureIosTargets()
    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)

                implementation(projects.scanner)
            }
        }
        androidMain {
            dependencies {
            }
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.withType<Framework> {
            isStatic = true
            baseName = "shared"
//            export("io.github.kalinjul.kotlin.multiplatform:oidc-appsupport")
        }
    }
}

android {
    namespace = "org.publicvalue.multiplatform.qrcode.sample.shared"
}
