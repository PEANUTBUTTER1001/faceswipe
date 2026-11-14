package com.peanutbutter1001.faceswipe.feature.faceswipe.setup

import androidx.annotation.StringRes
import com.peanutbutter1001.faceswipe.feature.faceswipe.R

sealed interface SetupUiState {
    data object Loading : SetupUiState

    /**
     * @param stepNumber 첫 실행 단계 표시 번호. Home 진입·완료 화면이면 null
     * @param message 한 번 보여줄 스낵바 메시지. 표시 후 onMessageShown으로 비운다.
     */
    data class Success(
        val entry: SetupEntry,
        val step: SetupStep,
        val stepNumber: Int?,
        val stepCount: Int,
        val cameraState: CameraPermissionState,
        val tileAdded: Boolean,
        val message: SetupMessage?,
    ) : SetupUiState
}

/** 설정 마법사 스낵바 메시지 */
enum class SetupMessage(@StringRes val textRes: Int) {
    AccessibilityStillOff(R.string.setup_a11y_still_off),
    AccessibilityEnabled(R.string.setup_a11y_enabled),
    CameraGranted(R.string.setup_camera_granted),
    CameraStillDenied(R.string.setup_camera_still_denied),
    NotificationGranted(R.string.setup_notif_granted),
    YoutubeMissing(R.string.setup_youtube_missing),
    TileAdded(R.string.tile_added_message),
}
