pluginManagement {
    includeBuild("build-logic")

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
//        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name="EasyQRScan"

include(":scanner")
include(":sample-app:shared")
include(":sample-app:android-app")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
// https://docs.gradle.org/8.3/userguide/configuration_cache.html#config_cache:stable
// enableFeaturePreview("STABLE_CONFIGURATION_CACHE")