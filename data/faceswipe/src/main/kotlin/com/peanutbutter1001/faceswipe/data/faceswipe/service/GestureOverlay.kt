package com.peanutbutter1001.faceswipe.data.faceswipe.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * 제스처 인식 결과를 화면 상단에 잠깐 띄우는 알림("고개 오른쪽 돌리기 → 위로 스와이프").
 *
 * - TYPE_ACCESSIBILITY_OVERLAY: 접근성 서비스 context로만 추가 가능하며
 *   "다른 앱 위에 그리기" 권한이 필요 없다.
 * - FLAG_NOT_TOUCHABLE | FLAG_NOT_FOCUSABLE: 터치·포커스를 가로채지 않아
 *   바로 이어서 실행되는 dispatchGesture와 사용자 조작에 영향이 없다.
 * - View 하나를 재사용하고, 새 알림이 오면 문구만 바꾸고 숨김 타이머를 다시 시작한다.
 *
 * 메인 스레드에서만 호출한다.
 */
internal class GestureOverlay(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hide() }
    private var pill: TextView? = null

    private fun dp(value: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, service.resources.displayMetrics).toInt()

    /**
     * @param text 표시 문구
     * @param mapped 연결된 동작이 있으면 true(초록 점), 없으면 false(주황 점)
     */
    fun show(text: String, mapped: Boolean) {
        val view = pill ?: createPill().also { pill = it }
        view.text = text
        view.setCompoundDrawablesRelative(dot(if (mapped) DOT_MAPPED else DOT_UNMAPPED), null, null, null)
        if (!view.isAttachedToWindow) {
            try {
                windowManager.addView(view, layoutParams())
            } catch (e: RuntimeException) {
                // 창 추가 실패(서비스 종료 직전 등)는 피드백만 생략하고 제스처 실행에는 영향을 주지 않는다.
                Log.w(TAG, "overlay addView failed", e)
                return
            }
        }
        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, SHOW_MILLIS)
    }

    fun hide() {
        handler.removeCallbacks(hideRunnable)
        val view = pill ?: return
        if (view.isAttachedToWindow) {
            runCatching { windowManager.removeViewImmediate(view) }
        }
    }

    private fun createPill() = TextView(service).apply {
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setPadding(dp(16f), dp(9f), dp(16f), dp(9f))
        compoundDrawablePadding = dp(8f)
        maxLines = 2
        background = GradientDrawable().apply {
            cornerRadius = dp(999f).toFloat()
            setColor(PILL_COLOR)
        }
        // 제스처는 이미 실행되므로 TalkBack이 매번 읽지 않도록 접근성 트리에서 제외
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun dot(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setBounds(0, 0, dp(8f), dp(8f))
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = dp(48f) // 상태 표시줄 아래
    }

    private companion object {
        const val TAG = "GestureOverlay"
        const val SHOW_MILLIS = 1_600L
        const val PILL_COLOR = 0xEE1E2026.toInt()
        const val DOT_MAPPED = 0xFF2DBE5A.toInt()
        const val DOT_UNMAPPED = 0xFFF2B84B.toInt()
    }
}
