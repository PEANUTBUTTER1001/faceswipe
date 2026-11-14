package com.peanutbutter1001.faceswipe.data.faceswipe.service

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureEvent
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
 * - gestureEvent: 감지된 제스처와 연결된 동작 -> 접근성 서비스가 스와이프 실행·오버레이 표시
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

    /**
     * 감지된 제스처 이벤트. 동작이 연결되지 않은 제스처도 발행해(action=null)
     * 접근성 서비스가 오버레이로 "연결된 동작이 없어요"를 알릴 수 있게 한다.
     */
    val gestureEvent = MutableSharedFlow<GestureEvent>(extraBufferCapacity = 1)

    /**
     * 대상 앱에서 카메라를 쓸 수 없는 상태(바인딩 실패, 다른 앱이 사용 중 등).
     * 홈 실행 설명과 서비스 알림에 표시한다.
     */
    private val _cameraUnavailable = MutableStateFlow(false)
    val cameraUnavailable: StateFlow<Boolean> = _cameraUnavailable.asStateFlow()

    fun setCameraUnavailable(unavailable: Boolean) {
        _cameraUnavailable.value = unavailable
    }

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
