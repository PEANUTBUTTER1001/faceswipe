package com.peanutbutter1001.faceswipe.domain.faceswipe.model

/**
 * 제스처 매핑 외의 앱 환경설정.
 *
 * @param onboardingDone 첫 실행 소개·설정 마법사를 마쳤거나 건너뛰었는지. false면 시작 화면이 소개 화면.
 * @param showGestureOverlay 제스처 인식 시 화면 위에 "제스처 → 동작" 알림을 띄울지
 * @param tileAdded 빠른 설정 타일이 추가되어 있는지 (TileService 추가·제거 콜백으로 갱신)
 * @param cameraPermissionRequested 카메라 권한을 한 번이라도 요청했는지 (처음 상태와 거부 상태 구분)
 * @param cameraRationaleSeen 카메라 거부 후 rationale=true를 본 적이 있는지.
 *   이후 rationale=false가 되면 "다시 묻지 않음"(차단) 상태로 판정한다.
 * @param notificationPermissionRequested 알림 권한을 요청했거나 사용자가 건너뛰었는지
 */
data class AppPreferences(
    val onboardingDone: Boolean = false,
    val showGestureOverlay: Boolean = true,
    val tileAdded: Boolean = false,
    val cameraPermissionRequested: Boolean = false,
    val cameraRationaleSeen: Boolean = false,
    val notificationPermissionRequested: Boolean = false,
)
