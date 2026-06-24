plugins {
    id("my.android.library")
    id("my.android.compose")
}

android {
    namespace = "com.peanutbutter1001.faceswipe.core.ui"
}

dependencies {
    // Compose (BOM은 convention plugin에서 제공)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
