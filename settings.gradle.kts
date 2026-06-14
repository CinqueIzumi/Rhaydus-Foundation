pluginManagement {
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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "rhaydus-foundation"

// Modules land here as the phases progress (see MIGRATION.md):
//   phase 1: include(":ktlint-rules")
//   phase 2: includeBuild("build-logic")  + publish the version catalog
//   phase 3: include(":toad")
//   phase 4: include(":designsystem-core")
