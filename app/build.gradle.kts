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
    // Modules
    implementation(project(":feature:faceswipe"))
    implementation(project(":data:faceswipe"))
    implementation(project(":domain:faceswipe"))
    implementation(project(":core:ui"))

    // Android Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)

    // Navigation (app-level NavHost)
    implementation(libs.navigation.compose)

    // Compose (setContent에 필요한 activity-compose만 선언, 나머지는 convention plugin 제공)
    implementation(libs.androidx.activity.compose)
}
