plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// `:core` is pure Kotlin. No Android plugin is applied, which is the whole
// point: an Android import here does not compile, so the boundary fails the
// build rather than eroding quietly. Everything in this module is logic a
// future Linux desktop or web build could reuse unchanged.
//
// Nothing Android may be added to this file. If something needs a platform
// capability, `:core` defines the interface and `:app` implements it.

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
