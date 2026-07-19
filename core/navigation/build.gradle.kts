plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.topic11.cs426.core.navigation"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.circuit.runtime.screen)
}
