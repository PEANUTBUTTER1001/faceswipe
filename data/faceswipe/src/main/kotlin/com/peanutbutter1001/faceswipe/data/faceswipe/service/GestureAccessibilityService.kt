package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.peanutbutter1001.faceswipe.data.faceswipe.gesture.GestureDispatcher
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
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
 */
private val SYSTEM_OVERLAY_PACKAGES = setOf(
    "android",
    "com.android.systemui",
    "com.android.inputmethod.latin",
    "com.google.android.inputmethod.latin",
    "com.samsung.android.honeyboard",
    "com.miui.home",
    "com.sec.android.app.launcher",
    "com.huawei.android.launcher",
    "com.android.launcher",
    "com.google.android.apps.nexuslauncher",
    "com.android.systemui.recents"
)

/**
 * 두 가지 역할을 담당하는 접근성 서비스:
 *
 * 1. [배터리 방어] onAccessibilityEvent를 통해 포그라운드 앱 실시간 감시.
 *    YouTube가 아닌 앱으로 전환 시 즉시 카메라 Pause.
 *
 * 2. [제스처 발생] AppStateManager.gestureAction을 구독하여
 *    GestureDispatcher를 통해 dispatchGesture 호출.
 */
@AndroidEntryPoint
class GestureAccessibilityService : AccessibilityService() {

    @Inject lateinit var appStateManager: AppStateManager
    @Inject lateinit var gestureDispatcher: GestureDispatcher

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastForegroundPackage: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        observeGestureActions()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName in SYSTEM_OVERLAY_PACKAGES) return
        if (packageName == lastForegroundPackage) return

        lastForegroundPackage = packageName
        appStateManager.setYouTubeActive(packageName == YOUTUBE_PACKAGE)
    }

    /**
     * 제스처 액션 구독 -> GestureDispatcher를 통해 dispatchGesture 호출.
     * when 하드코딩 제거, GestureDispatcher에 위임.
     */
    private fun observeGestureActions() {
        appStateManager.gestureAction
            .onEach { action ->
                // Toast (디버그용)
                val toastMsg = buildToastMessage(action)
                mainHandler.post {
                    Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
                }

                val gesture = gestureDispatcher.buildGesture(action, resources.displayMetrics)
                gesture?.let { dispatchGesture(it, null, null) }
            }
            .launchIn(serviceScope)
    }

    private fun buildToastMessage(action: GestureAction): String = when (action) {
        is GestureAction.SwipeVertical ->
            if (action.directionUp) "↑ 위로 넘김 인식" else "↓ 아래로 넘김 인식"
        is GestureAction.SwipeHorizontal ->
            if (action.directionLeft) "← 왼쪽 넘김 인식" else "→ 오른쪽 넘김 인식"
        is GestureAction.Tap -> "탭 인식"
        is GestureAction.Pause -> "일시정지 인식"
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
