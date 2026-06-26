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

/**
 * TYPE_WINDOW_STATE_CHANGED 이벤트를 발생시키지만 실제 앱 전환이 아닌
 * 시스템 UI 패키지 목록.
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
 * 두 가지 역할의 접근성 서비스:
 * 1. [배터리 방어] 포그라운드 앱 실시간 감시 -> 대상 앱 외 전환 시 카메라 Pause.
 * 2. [제스처 발생] gestureAction 구독 -> GestureDispatcher로 dispatchGesture.
 *
 * 또한 포그라운드 서비스 (재)시작 시 resyncRequest를 받아 rootInActiveWindow로
 * 현재 포그라운드 앱을 강제로 다시 읽어 상태를 재동기화한다.
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
        observeResyncRequests()
        mainHandler.post { syncCurrentForegroundApp() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName in SYSTEM_OVERLAY_PACKAGES) return
        if (packageName == lastForegroundPackage) return

        lastForegroundPackage = packageName
        appStateManager.updateForegroundApp(packageName)
    }

    private fun observeGestureActions() {
        appStateManager.gestureAction
            .onEach { action ->
                val toastMsg = buildToastMessage(action)
                mainHandler.post {
                    Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
                }
                val gesture = gestureDispatcher.buildGesture(action, resources.displayMetrics)
                gesture?.let { dispatchGesture(it, null, null) }
            }
            .launchIn(serviceScope)
    }

    private fun observeResyncRequests() {
        appStateManager.resyncRequest
            .onEach { mainHandler.post { syncCurrentForegroundApp() } }
            .launchIn(serviceScope)
    }

    /**
     * rootInActiveWindow로 현재 포그라운드 창의 패키지명을 읽어 상태 강제 갱신.
     * (accessibility_service_config.xml의 canRetrieveWindowContent="true" 필요)
     */
    private fun syncCurrentForegroundApp() {
        val pkg = rootInActiveWindow?.packageName?.toString()
        if (pkg == null || pkg in SYSTEM_OVERLAY_PACKAGES) return
        lastForegroundPackage = pkg
        appStateManager.updateForegroundApp(pkg)
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
        appStateManager.setTargetAppActive(false)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        appStateManager.setTargetAppActive(false)
        super.onDestroy()
    }
}
