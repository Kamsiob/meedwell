import java.util.Properties

// AGP 9 brings its own Kotlin support, so `org.jetbrains.kotlin.android` is
// not applied here and applying it fails the build outright. The Compose
// compiler plugin is still versioned against Kotlin and is still applied.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// The upload key, never the app signing key. Google holds the app signing key
// under Play App Signing, so both the Play install and the GitHub install carry
// one signature and a user moves between them without uninstalling. See
// MASTER_PROMPT.md section 7. The properties file lives outside the repository
// and is covered by .gitignore in case a copy ever lands inside it.
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.kamsiob.meedwell"

    // compileSdk decides which APIs the code may call. targetSdk opts the app
    // into a platform version's runtime behavior and is what Play checks.
    // Play requires API 36 from 31 August 2026, verified against the live
    // requirements page on 15 August 2026.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kamsiob.meedwell"
        // minSdk 29 is set by the marquee feature rather than by taste.
        // Downloads are written as real files through MediaStore using
        // RELATIVE_PATH and IS_PENDING, and neither exists before API 29.
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("upload") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // The device rule is one copy of the app at all times, and it is
            // the current build. No applicationIdSuffix, so a debug install
            // upgrades the release install in place rather than sitting
            // beside it as a second copy.
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProperties.containsKey("storeFile")) {
                signingConfig = signingConfigs.getByName("upload")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // The schema is the app's public data contract, so it is exported to a file
    // and committed rather than living only inside the binary. That is what
    // makes a migration reviewable and what lets a future desktop or web build
    // read the database without reverse engineering it. See ARCHITECTURE.md.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.generateKotlin", "true")
    }
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        // OldTargetApi fires because targetSdk is 36 while compileSdk is 37.
        // That pairing is deliberate and is what Play requires today.
        disable += "OldTargetApi"
    }
}

/**
 * The merged manifest audit.
 *
 * Libraries introduce permissions silently, so the merged manifest is checked
 * after every build rather than reviewed by hand after every dependency
 * addition. `RECORD_AUDIO` is the one that matters most: the waveform is drawn
 * from a custom AudioProcessor tap inside Meedwell's own Media3 pipeline and
 * needs no permission at all, while the Visualizer API would need the
 * microphone. An app whose whole pitch is restraint cannot ship a microphone
 * permission it did not notice arriving.
 *
 * The allowlist is the complete set of permissions this app is allowed to
 * declare. Adding to it is a deliberate act with a one sentence justification
 * recorded in DECISIONS.md, which is exactly the friction intended.
 */
// Some permissions are namespaced to the application id, so the allowlist is
// written against a placeholder and resolved before comparing.
val namespacePlaceholder = "com.kamsiob.meedwell"

val allowedPermissions = setOf(
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.READ_MEDIA_AUDIO",
    "android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE",
    // Added by the Media3 session library for the playback foreground service.
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
    // Added by Media3's ExoPlayer so audio keeps playing with the screen off.
    // A music player that stops when the phone locks is broken, and this grants
    // nothing beyond keeping the CPU awake while something is actually playing.
    "android.permission.WAKE_LOCK",
    // Added by androidx.core, namespaced to this application id, and declared
    // at signature protection level. It exists so that a broadcast receiver
    // registered as not-exported cannot be reached by another app. It grants
    // nothing to anyone and requests nothing from the user. Justified here
    // because the audit found it rather than because it was expected.
    "$namespacePlaceholder.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
)

val forbiddenPermissions = setOf(
    "android.permission.RECORD_AUDIO",
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.READ_CONTACTS",
    "android.permission.CAMERA",
    "android.permission.READ_PHONE_STATE",
)

val auditManifest = tasks.register("auditMergedManifest") {
    group = "verification"
    description = "Fails if the merged manifest declares a permission this app has not justified."
    // Only the manifest that actually ships. The androidTest manifest is
    // excluded because the instrumentation runner adds REORDER_TASKS to the
    // test APK, which never reaches a user's phone. Auditing it would mean
    // either failing every test build or allowlisting a permission the app
    // does not have, and the second would quietly weaken the real check.
    val manifests = fileTree(layout.buildDirectory.dir("intermediates/merged_manifest")) {
        include("**/AndroidManifest.xml")
        exclude("**/*AndroidTest*/**")
    }
    inputs.files(manifests)
    // Captured as plain values here rather than referenced from inside doLast.
    // The configuration cache cannot serialize a reference back into the build
    // script, and a task that breaks the configuration cache slows every build
    // in the project.
    val allowed = allowedPermissions
    val forbiddenSet = forbiddenPermissions
    doLast {
        val declared = manifests.files.flatMap { file ->
            Regex("""uses-permission[^>]*android:name="([^"]+)"""")
                .findAll(file.readText())
                .map { it.groupValues[1] }
                .toList()
        }.toSet()

        val forbidden = declared.intersect(forbiddenSet)
        val unexpected = declared - allowed - forbiddenSet

        if (forbidden.isNotEmpty()) {
            throw GradleException(
                "The merged manifest declares permissions this app promises never to request: " +
                    forbidden.joinToString(", ") +
                    ". Find the dependency that brought it in and remove it or tools:node=\"remove\" it."
            )
        }
        if (unexpected.isNotEmpty()) {
            throw GradleException(
                "The merged manifest declares permissions that are not on the allowlist: " +
                    unexpected.joinToString(", ") +
                    ". If each is genuinely needed, justify it in one sentence in DECISIONS.md and add it to " +
                    "allowedPermissions in app/build.gradle.kts."
            )
        }
        logger.lifecycle("Manifest audit passed. Permissions declared: ${declared.sorted().joinToString(", ")}")
    }
}

tasks.matching { it.name == "assembleDebug" || it.name == "assembleRelease" }.configureEach {
    finalizedBy(auditManifest)
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.security.crypto)
    implementation(libs.okhttp)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.datasource.okhttp)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.palette.ktx)
    // WorkManager is deliberately absent until the feature that needs it
    // exists. It was added here speculatively for automatic backup in Phase 6,
    // and the manifest audit immediately caught it bringing in WAKE_LOCK and
    // RECEIVE_BOOT_COMPLETED. Shipping two permissions for months before the
    // feature that justifies them is exactly what the audit exists to stop.
    // See DECISIONS.md and issue #3.

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
