plugins {
    // No version: the Kotlin plugin is on the classpath via the root build's apply-false block, so the
    // version is inherited (declaring one here triggers a classpath-conflict error).
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    // detekt-api is provided by the host detekt process at runtime; compile against it only.
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.test)
    testImplementation(libs.kotest)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Type resolution in the rule tests needs the coroutines Flow API on the analysis classpath.
    testImplementation(libs.kotlinx.coroutines.core)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
