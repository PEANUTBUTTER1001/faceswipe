package com.peanutbutter1001.faceswipe.data.faceswipe.service

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FaceSwipeForegroundService <-> GestureAccessibilityService <-> UI(ViewModel) 간 상태 공유 싱글턴.
 *
 * - isServiceRunning: 포그라운드 서비스 실행 여부 단일 소스(실제 서비스가 갱신).
 *   프로세스가 살아있는 채 재시작돼도 UI 버튼 상태와 실제 서비스 상태가 일치.
 * - isTargetAppActive: 대상 앱 포그라운드 여부 -> 카메라 파이프라인 활성화 제어
 * - currentTargetApp: 현재 포그라운드 TargetApp -> 앱별 제스처 매핑 조회
 * - gestureAction: 발생한 제스처 액션 -> 접근성 서비스가 실제 스와이프 디스패치
 * - resyncRequest: 포그라운드 서비스 (재)시작 시 현재 포그라운드 앱 강제 재동기화 요청
 */
@Singleton
class AppStateManager @Inject constructor() {

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _isTargetAppActive = MutableStateFlow(false)
    val isTargetAppActive: StateFlow<Boolean> = _isTargetAppActive.asStateFlow()

    private val _currentTargetApp = MutableStateFlow<TargetApp?>(null)
    val currentTargetApp: StateFlow<TargetApp?> = _currentTargetApp.asStateFlow()

    val gestureAction = MutableSharedFlow<GestureAction>(extraBufferCapacity = 1)

    private val _resyncRequest = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resyncRequest: SharedFlow<Unit> = _resyncRequest.asSharedFlow()

    /** 포그라운드 서비스 실행 상태 갱신. (서비스 start/stop/onTaskRemoved 시점에 호출) */
    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun setTargetAppActive(active: Boolean) {
        _isTargetAppActive.value = active
        if (!active) _currentTargetApp.value = null
    }

    /**
     * 포그라운드 앱 패키지명으로 TargetApp을 찾아 상태 업데이트.
     */
    fun updateForegroundApp(packageName: String) {
        val app = TargetApp.entries.find { it.packageName == packageName }
        _currentTargetApp.value = app
        _isTargetAppActive.value = app != null
    }

    fun requestForegroundResync() {
        _resyncRequest.tryEmit(Unit)
    }
}
