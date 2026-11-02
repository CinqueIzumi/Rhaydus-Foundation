plugins {
    id("rhaydus.kmp.library")
}

kotlin {
    androidLibrary {
        namespace = "nl.rhaydus.common"
    }

    sourceSets {
        commonMain.dependencies {
            // coroutines-core + koin-core come from the rhaydus.kmp.library convention.
            api(libs.kotlinx.datetime)
            // Kermit backs AppLog; no Kermit type leaks into the public surface, so implementation.
            implementation(libs.kermit)
        }
    }
}
