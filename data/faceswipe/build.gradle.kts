plugins {
    id("my.android.library")
    id("my.android.hilt")
}

android {
    namespace = "com.peanutbutter1001.faceswipe.data.faceswipe"
}

dependencies {
    implementation(project(":domain:faceswipe"))

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ML Kit Face Detection (on-device)
    implementation(libs.mlkit.face.detection)

    // Lifecycle Service (LifecycleService for ForegroundService)
    implementation(libs.lifecycle.service)

    // DataStore (앱별 제스처 매핑 설정 영속화)
    implementation(libs.androidx.datastore.preferences)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
}
