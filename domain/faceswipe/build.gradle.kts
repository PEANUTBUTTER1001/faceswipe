plugins {
    id("my.kotlin.library")
}

dependencies {
    // DI - @Inject 어노테이션 (순수 Kotlin 모듈이므로 javax.inject 사용)
    implementation(libs.javax.inject)

    // Coroutines - Flow (Repository 인터페이스 반환 타입)
    implementation(libs.kotlinx.coroutines.core)

    // 로컬 단위 테스트 (제스처 감지기·유스케이스, 현재 임계값 기준 회귀 방지)
    testImplementation(libs.bundles.unit.test)
}
