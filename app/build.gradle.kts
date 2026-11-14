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

    /*
     * 출시 서명: 키스토어 정보는 환경변수로만 주입한다 (저장소·로그에 남기지 않음).
     * 4개가 모두 있을 때만 release에 연결하고, 없으면 로컬 release 빌드는 서명 없이 진행한다.
     * CD에서는 GitHub Secrets로 주입한다 (.github/workflows/cd.yml).
     */
    val keystoreFile = providers.environmentVariable("FACESWIPE_KEYSTORE_FILE").orNull
    val keystorePassword = providers.environmentVariable("FACESWIPE_KEYSTORE_PASSWORD").orNull
    val releaseKeyAlias = providers.environmentVariable("FACESWIPE_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.environmentVariable("FACESWIPE_KEY_PASSWORD").orNull
    val hasReleaseSigning = listOf(keystoreFile, keystorePassword, releaseKeyAlias, releaseKeyPassword)
        .all { !it.isNullOrBlank() }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreFile!!)
                storePassword = keystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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
    // MainActivity: 환경설정 StateFlow 수집(collectAsStateWithLifecycle)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.splashscreen)

    // Navigation (app-level NavHost)
    implementation(libs.navigation.compose)

    // Compose (setContent에 필요한 activity-compose만 선언, 나머지는 convention plugin 제공)
    implementation(libs.androidx.activity.compose)
}
