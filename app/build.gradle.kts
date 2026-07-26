plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.topic11.cs426"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.topic11.cs426"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        // Robolectric needs the merged resources and manifest to boot; the pending-sync drain test
        // runs the real Room database rather than a fake so the wiring is covered end to end.
        unitTests.isIncludeAndroidResources = true
        // Espresso and Compose both wait on the animation clock; leaving system animations on is the
        // usual source of emulator-only flakes in the navigation smoke test.
        animationsDisabled = true
        managedDevices {
            localDevices {
                // The device CI runs the instrumented suite on. Declaring it here rather than in the
                // workflow keeps `./gradlew :app:ciDeviceDebugAndroidTest` identical locally and on CI.
                //
                // ATD is the stripped-down image Google publishes for instrumented tests: no Play
                // services, no wallpaper, no GPU, so it boots in a fraction of the time.
                //
                // API 35 and not 36, which would match targetSdk, for two measured reasons. The ATD
                // image only reaches the stable SDK channel up to 35 — at 36 it exists solely on the
                // dev channel, which AGP will not resolve. And on the API 36 emulator image the two
                // Espresso.pressBack() tests fail headless with RootViewWithoutFocusException: the
                // app window never takes focus, so the back key is never delivered. That is an
                // emulator-image quirk, not an app defect — a real back-handling regression surfaces
                // as NoActivityResumedException or a failed assertion, not as a missing window focus.
                //
                // The cost is that behaviour changes specific to targeting API 36 go uncovered here.
                // Worth revisiting once ATD 36 reaches the stable channel.
                create("ciDevice") {
                    device = "Pixel 6"
                    apiLevel = 35
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:navigation"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":feature:assets"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:inspection"))
    implementation(project(":feature:issues"))
    implementation(project(":feature:locations"))
    implementation(project(":feature:reports"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:templates"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.circuit.foundation)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
