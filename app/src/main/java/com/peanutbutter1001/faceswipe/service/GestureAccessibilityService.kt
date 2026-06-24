package com.peanutbutter1001.faceswipe.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.peanutbutter1001.faceswipe.domain.model.GestureTrigger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

/**
 * TYPE_WINDOW_STATE_CHANGED 이벤트를 발생시키지만 실제 앱 전환이 아닌
 * 시스템 UI 패키지 목록. 이 패키지에서 온 이벤트는 무시해야 깜빡임이 없음.
 *  - 볼륨바, 알림창, 키보드, 런처 오버레이 등이 YouTube 위에 뜰 때
 *    packageName이 이들 중 하나로 오면서 setYouTubeActive(false) → (true) 를 반복함.
 */
private val SYSTEM_OVERLAY_PACKAGES = setOf(
    "android",
    "com.android.systemui",
    "com.android.inputmethod.latin",
    "com.google.android.inputmethod.latin",
    "com.samsung.android.honeyboard",   // 삼성 키보드
    "com.miui.home",                    // 샤오미 런처
    "com.sec.android.app.launcher",     // 삼성 런처
    "com.huawei.android.launcher",      // 화웨이 런처
    "com.android.launcher",
    "com.google.android.apps.nexuslauncher",
    "com.android.systemui.recents"
)

/**
 * 두 가지 역할을 담당하는 접근성 서비스:
 *
 * 1. [배터리 방어] onAccessibilityEvent를 통해 포그라운드 앱 실시간 감시.
 *    YouTube(com.google.android.youtube)가 아닌 앱으로 전환 시 즉시 카메라 Pause.
 *
 * 2. [제스처 발생] AppStateManager.gestureTrigger를 구독하여 dispatchGesture 호출.
 *    화면 하단 → 상단 스와이프 Path로 YouTube 쇼츠 넘김.
 */
@AndroidEntryPoint
class GestureAccessibilityService : AccessibilityService() {

    @Inject lateinit var appStateManager: AppStateManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** 마지막으로 처리한 포그라운드 패키지 — 중복 이벤트 무시용 */
    private var lastForegroundPackage: String? = null

    /** Toast는 메인 스레드에서만 호출 가능 */
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        observeGestureTriggers()
    }

    /**
     * 포그라운드 앱 전환 감지.
     *
     * 깜빡임 방지 2단계 필터:
     *  1) 시스템 오버레이 패키지(볼륨바·알림창·키보드 등) 이벤트 무시
     *  2) 직전과 동일한 패키지 이벤트 중복 무시
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // 1단계: 시스템 UI / 오버레이 패키지는 앱 전환으로 보지 않음
        if (packageName in SYSTEM_OVERLAY_PACKAGES) return

        // 2단계: 이미 처리한 패키지면 중복 이벤트 — 무시
        if (packageName == lastForegroundPackage) return

        lastForegroundPackage = packageName
        appStateManager.setYouTubeActive(packageName == YOUTUBE_PACKAGE)
    }

    /**
     * 제스처 트리거 구독 → dispatchGesture 호출
     */
    private fun observeGestureTriggers() {
        appStateManager.gestureTrigger
            .onEach { trigger ->
                when (trigger) {
                    is GestureTrigger.SwipeUp -> dispatchSwipe(swipeUp = true)
                    is GestureTrigger.SwipeDown -> dispatchSwipe(swipeUp = false)
                }
            }
            .launchIn(serviceScope)
    }

    /**
     * 화면 중앙 수직 스와이프 제스처 생성 및 디스패치.
     * swipeUp=true: 하단→상단 (쇼츠 다음 영상)
     * swipeUp=false: 상단→하단 (쇼츠 이전 영상)
     */
    private fun dispatchSwipe(swipeUp: Boolean) {
        // 제스처 인식 확인용 Toast (메인 스레드에서 실행)
        val toastMsg = if (swipeUp) "↑ 위로 넘김 인식" else "↓ 아래로 넘김 인식"
        mainHandler.post {
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
        }

        val metrics = resources.displayMetrics
        val centerX = metrics.widthPixels / 2f
        val screenH = metrics.heightPixels

        val (startY, endY) = if (swipeUp) {
            Pair(screenH * 0.80f, screenH * 0.20f)
        } else {
            Pair(screenH * 0.20f, screenH * 0.80f)
        }

        val swipePath = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    swipePath,
                    /* startTime= */ 0L,
                    /* duration= */ 350L
                )
            )
            .build()

        dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {
        appStateManager.setYouTubeActive(false)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        appStateManager.setYouTubeActive(false)
        super.onDestroy()
    }
}
