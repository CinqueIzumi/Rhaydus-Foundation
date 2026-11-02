plugins {
    id("rhaydus.kmp.library")
    id("rhaydus.kmp.compose")
}

kotlin {
    androidLibrary {
        namespace = "nl.rhaydus.designsystem.editorial"
    }
    sourceSets {
        commonMain.dependencies {
            // `api`: editorial public APIs expose core types (the `MaterialTheme.editorialTypography`
            // extension, components taking core models), so consumers need core on their compile classpath.
            api(project(":designsystem-core"))
        }
    }
}
