package com.peanutbutter1001.faceswipe.domain.faceswipe.model

/**
 * 제스처 감지 대상 앱 목록.
 *
 * @param key DataStore 키에 사용되는 식별자
 * @param packageName AccessibilityService에서 포그라운드 앱 판별에 사용되는 패키지명
 */
enum class TargetApp(val key: String, val packageName: String) {
    YOUTUBE(key = "youtube", packageName = "com.google.android.youtube"),
    MILLIE(key = "millie", packageName = "kr.co.millie.millieshelf"),
}
