package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.peanutbutter1001.faceswipe.data.faceswipe.R
import com.peanutbutter1001.faceswipe.data.faceswipe.gesture.GestureDispatcher
import com.peanutbutter1001.faceswipe.data.faceswipe.label.GestureLabels
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureEvent
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.AppPreferencesRepository
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
 * 시스템 UI 패키지 목록 (알림창, 시스템 대화상자, 키보드).
 *
 * 런처(홈 화면)는 넣지 않는다. 홈으로 나가는 것은 실제 앱 전환이므로 대상 앱 비활성으로
 * 처리해야 한다. 예전에는 삼성·픽셀·샤오미·화웨이 런처가 이 목록에 있어, 유튜브에서 홈으로
 * 나가도 카메라와 제스처가 계속 켜져 있었다 (2026-11-14 실기기 재현).
 */
/** 창 전환 이벤트 후 활성 창을 다시 읽기까지 대기 (전환 직후 활성 창 갱신 지연 대비) */
private const val ACTIVE_WINDOW_SYNC_DELAY_MS = 250L

private val SYSTEM_OVERLAY_PACKAGES = setOf(
    "android",
    "com.android.systemui",
    "com.android.inputmethod.latin",
    "com.google.android.inputmethod.latin",
    "com.samsung.android.honeyboard",
)

/**
 * 두 가지 역할의 접근성 서비스:
 * 1. [배터리 방어] 포그라운드 앱 실시간 감시 -> 대상 앱 외 전환 시 카메라 Pause.
 * 2. [제스처 발생] gestureEvent 구독 -> 동작이 연결돼 있으면 GestureDispatcher로 dispatchGesture,
 *    "인식 알림 표시"가 켜져 있으면 GestureOverlay로 "제스처 → 동작"을 잠깐 표시 (기존 Toast 대체).
 *
 * 또한 포그라운드 서비스 (재)시작 시 resyncRequest를 받아 rootInActiveWindow로
 * 현재 포그라운드 앱을 강제로 다시 읽어 상태를 재동기화한다.
 */
@AndroidEntryPoint
class GestureAccessibilityService : AccessibilityService() {

    @Inject lateinit var appStateManager: AppStateManager
    @Inject lateinit var gestureDispatcher: GestureDispatcher
    @Inject lateinit var appPreferencesRepository: AppPreferencesRepository

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var lastForegroundPackage: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 접근성 오버레이 창 (서비스 context 필요 → onServiceConnected 이후 생성) */
    private var overlay: GestureOverlay? = null

    /** "인식 알림 표시" 설정. 이벤트 처리 스레드와 공유하므로 volatile */
    @Volatile private var showOverlay = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlay = GestureOverlay(this)
        observeOverlayPreference()
        observeGestureEvents()
        observeResyncRequests()
        mainHandler.post { syncCurrentForegroundApp() }
    }

    /** 창 전환 이벤트가 몰릴 때 마지막 한 번만 활성 창을 다시 읽는다 */
    private val activeWindowSync = Runnable { syncCurrentForegroundApp() }

    /**
     * 창 전환 이벤트는 "다시 확인하라"는 신호로만 쓴다.
     *
     * 이벤트의 packageName은 활성 창이 아닐 수 있다. 실기기(One UI 9)에서 유튜브 → 홈으로 나간 뒤
     * 약 0.7초 후 백그라운드 유튜브가 TYPE_WINDOW_STATE_CHANGED(class=android.view.ViewGroup)를
     * 다시 보내, 이벤트 패키지를 그대로 믿으면 홈 화면에서도 대상 앱으로 판정됐다.
     * 그래서 짧게 기다린 뒤 실제 활성 창(rootInActiveWindow)의 패키지로 판정한다.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        mainHandler.removeCallbacks(activeWindowSync)
        mainHandler.postDelayed(activeWindowSync, ACTIVE_WINDOW_SYNC_DELAY_MS)
    }

    private fun observeOverlayPreference() {
        appPreferencesRepository.preferences
            .onEach { prefs ->
                showOverlay = prefs.showGestureOverlay
                if (!prefs.showGestureOverlay) mainHandler.post { overlay?.hide() }
            }
            .launchIn(serviceScope)
    }

    private fun observeGestureEvents() {
        appStateManager.gestureEvent
            .onEach { event ->
                if (showOverlay) {
                    val text = buildOverlayText(event)
                    mainHandler.post { overlay?.show(text, mapped = event.action != null) }
                }
                // 연결된 동작이 있을 때만 실제 제스처 실행
                val action = event.action ?: return@onEach
                val gesture = gestureDispatcher.buildGesture(action, resources.displayMetrics)
                gesture?.let { dispatchGesture(it, null, null) }
            }
            .launchIn(serviceScope)
    }

    /** "고개 오른쪽 돌리기 → 위로 스와이프" 또는 "입 벌리기 · 연결된 동작이 없어요" */
    private fun buildOverlayText(event: GestureEvent): String {
        val trigger = getString(GestureLabels.trigger(event.trigger))
        val action = event.action ?: return getString(R.string.overlay_unmapped, trigger)
        return getString(R.string.overlay_mapped, trigger, getString(GestureLabels.action(action)))
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
        // 알림창·키보드 등이 활성이면 직전 앱 판정을 유지한다.
        if (pkg == null || pkg in SYSTEM_OVERLAY_PACKAGES) return
        if (pkg == lastForegroundPackage) return
        lastForegroundPackage = pkg
        appStateManager.updateForegroundApp(pkg)
    }

    override fun onInterrupt() {
        appStateManager.setTargetAppActive(false)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(activeWindowSync)
        overlay?.hide()
        overlay = null
        serviceScope.cancel()
        appStateManager.setTargetAppActive(false)
        super.onDestroy()
    }
}
