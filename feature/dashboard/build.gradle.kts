plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.topic11.cs426.feature.dashboard"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":domain"))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.circuit.runtime)
    implementation(libs.circuit.runtime.presenter)
    implementation(libs.circuit.runtime.ui)

    testImplementation(project(":core:testing"))
    testImplementation(libs.circuit.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
