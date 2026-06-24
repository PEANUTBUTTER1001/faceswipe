plugins {
    id("my.android.library")
    id("my.android.compose")
    id("my.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.peanutbutter1001.faceswipe.feature.faceswipe"
}

dependencies {
    implementation(project(":domain:faceswipe"))
    implementation(project(":data:faceswipe"))
    implementation(project(":core:ui"))

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Activity Compose
    implementation(libs.androidx.activity.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt Navigation Compose
    implementation(libs.hilt.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Serialization (type-safe navigation)
    implementation(libs.kotlinx.serialization.json)
}
