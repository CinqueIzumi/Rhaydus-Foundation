plugins {
    // No version: the Kotlin plugin is on the classpath via the root build's apply-false block, so the
    // version is inherited (declaring one here triggers a classpath-conflict error).
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(libs.ktlint.ruleEngine)
    api(libs.ktlint.ruleEngineCore)
    implementation(libs.ktlint.rulesetStandard)

    // ktlint's rule-engine logs via slf4j; provide the api + a no-op binding at runtime
    implementation("org.slf4j:slf4j-api:2.0.16")
    runtimeOnly("org.slf4j:slf4j-nop:2.0.16")

    testImplementation(libs.ktlint.test)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
