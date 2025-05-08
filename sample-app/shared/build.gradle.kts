import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kmp)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(projects.scanner)
            }
        }

        val iosMain by getting {
            dependsOn(commonMain)
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        iosX64Main.dependsOn(iosMain)
        iosArm64Main.dependsOn(iosMain)
        iosSimulatorArm64Main.dependsOn(iosMain)
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
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
