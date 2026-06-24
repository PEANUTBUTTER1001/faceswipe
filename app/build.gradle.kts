plugins {
    id("my.android.application")
    id("my.android.compose")
    id("my.android.hilt")
}

android {
    namespace = "com.peanutbutter1001.faceswipe"

    defaultConfig {
        applicationId = "com.peanutbutter1001.faceswipe"
        versionCode = 1
        versionName = "1.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Android Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(libs.bundles.compose.core)
    debugImplementation(libs.bundles.compose.debug)

    // Hilt Navigation Compose
    implementation(libs.hilt.navigation.compose)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ML Kit Face Detection (on-device)
    implementation(libs.mlkit.face.detection)

    // Lifecycle Service (LifecycleService for ForegroundService)
    implementation(libs.lifecycle.service)
}
