plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.thomasnorlund.archiveplayer"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.thomasnorlund.archiveplayer"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Every UI test starts from a fresh install (setup not completed).
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    // R80: `full` has Chromecast (GitHub Releases); `foss` has no proprietary libraries (F-Droid).
    flavorDimensions += "distribution"
    productFlavors {
        create("full") { dimension = "distribution" }
        create("foss") { dimension = "distribution" }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui.compose.material3)
    "fullImplementation"(libs.androidx.media3.cast)
    // Cast route dialogs (MediaRouteChooserDialog) extend AppCompat dialogs.
    "fullImplementation"(libs.androidx.appcompat)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.uiautomator)
    // Compose testing pulls espresso-core 3.5, which fails on API 37 (InputManager.getInstance removed).
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.media3.session)
    androidTestUtil(libs.androidx.test.orchestrator)
}

// R80/R81 flavour check: the `foss` APK must not contain Google Play Services, Cast or MediaRouter.
val verifyFossDependencies by tasks.registering {
    group = "verification"
    description = "Fails if the foss runtime classpath contains proprietary Google libraries."
    val forbidden = listOf("com.google.android.gms", "androidx.media3:media3-cast", "androidx.mediarouter", "com.google.firebase")
    val modules = configurations.named("fossReleaseRuntimeClasspath").map { cfg ->
        cfg.incoming.resolutionResult.allComponents.map { it.moduleVersion.toString() }
    }
    inputs.property("modules", modules)
    doLast {
        val offending = modules.get().filter { m -> forbidden.any { m.startsWith(it) } }
        check(offending.isEmpty()) { "foss contains proprietary libraries: $offending" }
    }
}
tasks.named("check") { dependsOn(verifyFossDependencies) }
