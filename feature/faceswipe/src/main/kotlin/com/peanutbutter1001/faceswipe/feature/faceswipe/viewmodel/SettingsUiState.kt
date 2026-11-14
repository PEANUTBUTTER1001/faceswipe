package com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp

/**
 * 제스처 설정 화면의 UI 상태.
 *
 * ViewModel에서 단일 StateFlow로 노출하며,
 * UI는 이 상태를 구독하여 화면을 렌더링한다.
 */
sealed interface SettingsUiState {

    /** DataStore에서 설정을 로딩 중 */
    data object Loading : SettingsUiState

    /**
     * 설정 로드 완료 상태.
     *
     * @param selectedApp 현재 선택된 대상 앱
     * @param mappings 현재 앱의 제스처-액션 매핑. null 값은 "선택 안 함".
     * @param pendingUndoApp 방금 연결을 모두 해제해 실행 취소할 수 있는 앱. null이면 실행 취소 없음.
     */
    data class Success(
        val selectedApp: TargetApp,
        val mappings: Map<GestureTrigger, GestureAction?>,
        val pendingUndoApp: TargetApp? = null,
    ) : SettingsUiState
}
