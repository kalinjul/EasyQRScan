import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.compose.internal.utils.getLocalProperty
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.android.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.mavenPublish)
    signing
}

description = "Compose Multiplatform QR Code Scanner for Android/iOS"
group = "io.github.kalinjul.easyqrscan"
version = "0.4.0"
project.version = version

kotlin {
    jvm()
    androidTarget {
        publishLibraryVariants("release", "debug")

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.accompanist.permissions)
                implementation(libs.androidx.camera.camera2)
                implementation(libs.androidx.camera.lifecycle)
                implementation(libs.androidx.camera.view)
                implementation(libs.mlkit.barcode.scanning)
            }
        }
    }
}

android {
    namespace = "org.publicvalue.multiplatform.mobilecapture.${project.name.replace("-", ".")}"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

fun projectHasSignatureProperties(): Boolean {
    val hasLocalProperties = getLocalProperty("SIGNING_KEY_ID") != null && getLocalProperty("SIGNING_KEY") != null && getLocalProperty("SIGNING_KEY_PASSWORD") != null
    val hasEnvProperties = System.getenv("SIGNING_KEY_ID") != null && System.getenv("SIGNING_KEY") != null && System.getenv("SIGNING_KEY_PASSWORD") != null
    return hasLocalProperties || hasEnvProperties
}

if (projectHasSignatureProperties()) {
    signing {
        useInMemoryPgpKeys(
            getLocalProperty("SIGNING_KEY_ID") ?: System.getenv("SIGNING_KEY_ID"),
            getLocalProperty("SIGNING_KEY") ?: System.getenv("SIGNING_KEY"),
            getLocalProperty("SIGNING_KEY_PASSWORD") ?: System.getenv("SIGNING_KEY_PASSWORD"),
        )
        sign(publishing.publications)
    }
}

mavenPublishing {
    if (projectHasSignatureProperties()) {
        signAllPublications()
    }

    coordinates(group.toString(), "library", version.toString())

    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaHtml"),
        )
    )

    pom {
        name.set(project.name)
        val pom = this
        project.afterEvaluate {
            // description seems to be only available after evaluation
            pom.description.set(project.description)
        }
        url.set("https://github.com/kalinjul/EasyQRScan")
        licenses {
            license {
                name.set("Apache-2.0 License")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
            developers {
                developer {
                    id.set("kalinjul")
                    name.set("Julian Kalinowski")
                    email.set("julakali@gmail.com")
                }
                developer {
                    id.set("ch4rl3x")
                    name.set("Alexander Karkossa")
                    email.set("alexander.karkossa@googlemail.com")
                }
            }
            scm {
                connection.set("scm:git:github.com/kalinjul/EasyQRScan.git")
                developerConnection.set("scm:git:ssh://github.com/kalinjul/EasyQRScan.git")
                url.set("https://github.com/kalinjul/EasyQRScan/tree/main")
            }
        }
    }
}
