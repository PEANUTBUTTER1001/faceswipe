package com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp

/**
 * HomeScreen의 UI 상태를 나타내는 sealed interface.
 *
 * SKILLS.md 4.1절 패턴: Loading / Success / Error 3종 기본 세트.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    /**
     * @param mappedGestureCount 대상 앱별로 동작이 연결된 제스처 수 ("앱별 제스처 설정" 행 요약)
     * @param notificationRequested 알림 권한을 이미 요청한 적이 있는지.
     *   요청 이력이 있고 꺼져 있으면 시스템이 다시 묻지 않을 수 있어 앱 알림 설정으로 바로 보낸다.
     */
    data class Success(
        val isServiceRunning: Boolean,
        val isTargetAppActive: Boolean,
        /** 실행 중 대상 앱에서 카메라를 쓸 수 없음 (다른 앱 사용 중 등) */
        val cameraUnavailable: Boolean = false,
        val hasCameraPermission: Boolean,
        val hasAccessibilityPermission: Boolean,
        val hasNotificationPermission: Boolean = true,
        val notificationRequested: Boolean = false,
        val showGestureOverlay: Boolean = true,
        val tileAdded: Boolean = false,
        val mappedGestureCount: Map<TargetApp, Int> = emptyMap(),
        val message: HomeMessage? = null,
    ) : HomeUiState {
        /** 서비스 시작에 필요한 권한이 모두 허용되었는지 (알림은 선택) */
        val canStartService: Boolean
            get() = hasCameraPermission && hasAccessibilityPermission
    }

    data class Error(val message: String) : HomeUiState
}

/** 홈 스낵바 메시지 */
enum class HomeMessage { ServiceStarted, ServiceStopped, TileAdded, TileAlreadyAdded }
