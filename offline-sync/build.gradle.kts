plugins {
    id("rhaydus.kmp.library")
}

kotlin {
    androidLibrary {
        namespace = "nl.rhaydus.offlinesync"
    }

    sourceSets {
        commonMain.dependencies {
            // NetworkAvailabilityProvider (drain-on-return) + AppDispatchers / runCatching* appear on the
            // public drainer surface, so api. core-platform re-exports core-common via api.
            api(project(":core-platform"))
        }
    }
}
