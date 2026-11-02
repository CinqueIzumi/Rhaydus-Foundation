plugins {
    id("rhaydus.kmp.library")
}

kotlin {
    androidLibrary {
        namespace = "nl.rhaydus.toad"
    }

    sourceSets {
        commonMain.dependencies {
            // ToadScreenModel extends Voyager's ScreenModel and exposes it in its API, so `api`.
            // coroutines-core + koin-core come from the rhaydus.kmp.library convention.
            api(libs.voyager.screenModel)
        }
    }
}
