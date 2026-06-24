package com.peanutbutter1001.faceswipe.data.faceswipe.service

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FaceSwipeForegroundService <-> GestureAccessibilityService 간 상태 공유 싱글턴.
 *
 * - isTargetAppActive: 대상 앱(유튜브, 밀리의서재 등) 포그라운드 여부 -> 카메라 파이프라인 활성화 제어
 * - gestureAction: 발생한 제스처 액션 -> 접근성 서비스가 실제 스와이프 디스패치
 */
@Singleton
class AppStateManager @Inject constructor() {

    private val _isTargetAppActive = MutableStateFlow(false)
    val isTargetAppActive: StateFlow<Boolean> = _isTargetAppActive.asStateFlow()

    val gestureAction = MutableSharedFlow<GestureAction>(extraBufferCapacity = 1)

    fun setTargetAppActive(active: Boolean) {
        _isTargetAppActive.value = active
    }
}
