plugins {
    id("rhaydus.kmp.library")
}

kotlin {
    androidLibrary {
        namespace = "nl.rhaydus.platform"
    }

    sourceSets {
        commonMain.dependencies {
            // AppDispatchers / AppLog / runCatchingCancellable appear in the public seam surface, so api.
            api(project(":core-common"))
        }

        jvmMain.dependencies {
            // KSafe backs the desktop SecureStorage actual (OS secret store custody).
            implementation(libs.ksafe)
        }
    }
}
