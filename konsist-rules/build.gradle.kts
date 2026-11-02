plugins {
    // No version: the Kotlin plugin is on the classpath via the root build's apply-false block, so the
    // version is inherited (declaring one here triggers a classpath-conflict error).
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    // KoScope and the declaration types appear in the public signatures.
    api(libs.konsist)

    testImplementation(libs.junit.api)
    testImplementation(libs.kotest)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
