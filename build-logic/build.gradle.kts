plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.compose.multiplatform.gradlePlugin)
}

// Module-shape conventions shared across the apps built on this foundation. App-specific tech (Room,
// Apollo) is NOT here on purpose — those conventions stay in the app that uses them. Add them here only
// if more than one app adopts the same tech.
gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "rhaydus.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("kmpLibrary") {
            id = "rhaydus.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "rhaydus.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("kmpCompose") {
            id = "rhaydus.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
    }
}
