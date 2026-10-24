pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "rhaydus-foundation"

include(":ktlint-rules")
include(":detekt-rules")
include(":catalog")
include(":toad")
include(":core-common")
include(":core-platform")
include(":offline-sync")
include(":designsystem-core")
include(":designsystem-editorial")
include(":designsystem-image")
