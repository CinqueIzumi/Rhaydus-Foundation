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
    compileOnly(libs.dependency.analysis.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.stability.analyzer.gradlePlugin)

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.kotest)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// Module-shape conventions shared across the apps built on this foundation. App-specific tech (Room,
// Apollo) is NOT here on purpose — those conventions stay in the app that uses them. Add them here only
// if more than one app adopts the same tech.
gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "rhaydus.android.library"
            implementationClass = "nl.rhaydus.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("kmpLibrary") {
            id = "rhaydus.kmp.library"
            implementationClass = "nl.rhaydus.buildlogic.KmpLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "rhaydus.android.compose"
            implementationClass = "nl.rhaydus.buildlogic.AndroidComposeConventionPlugin"
        }
        register("kmpCompose") {
            id = "rhaydus.kmp.compose"
            implementationClass = "nl.rhaydus.buildlogic.KmpComposeConventionPlugin"
        }
        register("docBudgets") {
            id = "rhaydus.doc-budgets"
            implementationClass = "nl.rhaydus.buildlogic.DocBudgetsConventionPlugin"
        }
        register("moduleGraph") {
            id = "rhaydus.module-graph"
            implementationClass = "nl.rhaydus.buildlogic.ModuleGraphConventionPlugin"
        }
        register("style") {
            id = "rhaydus.style"
            implementationClass = "nl.rhaydus.buildlogic.StyleConventionPlugin"
        }
        register("konsist") {
            id = "rhaydus.konsist"
            implementationClass = "nl.rhaydus.buildlogic.KonsistConventionPlugin"
        }
        register("resourcePackaging") {
            id = "rhaydus.resource-packaging"
            implementationClass = "nl.rhaydus.buildlogic.ResourcePackagingConventionPlugin"
        }
        register("androidR8KeepContract") {
            id = "rhaydus.android.r8-keep-contract"
            implementationClass = "nl.rhaydus.buildlogic.AndroidR8KeepContractConventionPlugin"
        }
        register("dependencyAnalysis") {
            id = "rhaydus.dependency-analysis"
            implementationClass = "nl.rhaydus.buildlogic.DependencyAnalysisConventionPlugin"
        }
    }
}
