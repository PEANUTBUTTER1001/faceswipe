package com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel

/**
 * HomeScreen의 UI 상태를 나타내는 sealed interface.
 *
 * SKILLS.md 4.1절 패턴: Loading / Success / Error 3종 기본 세트.
 */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val isServiceRunning: Boolean,
        val isTargetAppActive: Boolean,
        val hasCameraPermission: Boolean,
        val hasAccessibilityPermission: Boolean
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}
