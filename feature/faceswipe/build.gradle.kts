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

    // Activity Compose
    implementation(libs.androidx.activity.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt Navigation Compose
    implementation(libs.hilt.navigation.compose)

    // Hilt ViewModel Compose (hiltViewModel() 신규 패키지)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Serialization (type-safe navigation)
    implementation(libs.kotlinx.serialization.json)
}
