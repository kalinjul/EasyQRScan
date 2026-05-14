package org.publicvalue.convention

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * KMP Android library (AGP 9+). Apply [KotlinMultiplatformMobileConventionPlugin] first so
 * `org.jetbrains.kotlin.multiplatform` is on the project before this plugin runs.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                "Apply org.publicvalue.convention.kotlin.multiplatform.mobile before org.publicvalue.convention.android.library"
            }
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            val libs = the<LibrariesForLibs>()
            extensions.configure<KotlinMultiplatformExtension> {
                (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("android") {
                    compileSdk = libs.versions.compileSdk.get().toInt()
                    minSdk = libs.versions.minSdk.get().toInt()
                    androidResources {
                        enable = true
                    }
                }
                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
                    }
                }
            }
        }
    }
}
