plugins {
    id("rhaydus.kmp.library")
    id("rhaydus.kmp.compose")
}

kotlin {
    androidLibrary {
        namespace = "nl.rhaydus.designsystem"
    }
    // Compose deps (runtime/foundation/animation/ui/material3-expressive/preview) come from the
    // rhaydus.kmp.compose convention. No brand tokens or app domain modules - that boundary is what
    // keeps this reusable.
    sourceSets {
        commonMain.dependencies {
            // ObserveAsEvents needs LocalLifecycleOwner + repeatOnLifecycle.
            implementation(libs.androidx.lifecycle.runtime.compose)
            // RhaydusDatePickerDialog reports its pick as a LocalDate.
            api(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            // ShareCardCapture (share/): FileProvider + ContextCompat for the Android save/share path,
            // activity-compose for the gallery-write permission launcher.
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
        }
    }
}
