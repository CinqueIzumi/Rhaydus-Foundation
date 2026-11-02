plugins {
    id("rhaydus.kmp.library")
    id("rhaydus.kmp.compose")
}

kotlin {
    androidLibrary {
        namespace = "nl.rhaydus.designsystem.image"
    }
    // Opt-in async-image components built on Coil. Isolated here so apps that never show a remote image
    // do not inherit the Coil dependency from designsystem-core. `coil-compose` only - the network fetcher
    // (okhttp/ktor) is an app choice, supplied through the app's Coil ImageLoader. Both deps are
    // `implementation`: no Coil or core type appears in the public API (models are passed as `Any?`), so
    // nothing needs to be on a consumer's compile classpath.
    sourceSets {
        commonMain.dependencies {
            implementation(project(":designsystem-core"))
            implementation(libs.coil.compose)
        }
    }
}
