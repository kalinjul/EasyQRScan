package org.publicvalue.convention.config

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

fun Project.configureKotlinAndroid(extension: CommonExtension) {
    val libs = the<LibrariesForLibs>()

    if (extension is ApplicationExtension) {
        extension.apply {
            defaultConfig {
                targetSdk = libs.versions.targetSdk.get().toInt()
            }
        }
    }

    extension.compileSdk = libs.versions.compileSdk.get().toInt()

    with(extension.defaultConfig) {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Can remove this once https://issuetracker.google.com/issues/260059413 is fixed.
    // See https://kotlinlang.org/docs/gradle-configure-project.html#gradle-java-toolchains-support
    with(extension.compileOptions) {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    }
}
