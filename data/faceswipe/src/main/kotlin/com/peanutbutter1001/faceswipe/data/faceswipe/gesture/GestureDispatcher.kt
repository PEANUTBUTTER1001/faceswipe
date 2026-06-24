package com.peanutbutter1001.faceswipe.data.faceswipe.gesture

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.DisplayMetrics
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import javax.inject.Inject

/**
 * GestureAction -> AccessibilityService GestureDescription 변환.
 *
 * 새로운 GestureAction 타입이 추가되면 여기에 변환 로직만 추가하면 된다.
 * AccessibilityService의 dispatchGesture()에 전달할 GestureDescription을 생성한다.
 */
class GestureDispatcher @Inject constructor() {

    fun buildGesture(action: GestureAction, displayMetrics: DisplayMetrics): GestureDescription? {
        return when (action) {
            is GestureAction.SwipeVertical -> buildVerticalSwipe(action.directionUp, displayMetrics)
            is GestureAction.SwipeHorizontal -> buildHorizontalSwipe(action.directionLeft, displayMetrics)
            is GestureAction.Tap -> buildTap(displayMetrics)
            is GestureAction.Pause -> null // 일시정지는 제스처가 아닌 다른 메커니즘으로 처리
        }
    }

    private fun buildVerticalSwipe(up: Boolean, metrics: DisplayMetrics): GestureDescription {
        val centerX = metrics.widthPixels / 2f
        val screenH = metrics.heightPixels
        val (startY, endY) = if (up) Pair(screenH * 0.80f, screenH * 0.20f)
        else Pair(screenH * 0.20f, screenH * 0.80f)
        val path = Path().apply { moveTo(centerX, startY); lineTo(centerX, endY) }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 350L))
            .build()
    }

    private fun buildHorizontalSwipe(left: Boolean, metrics: DisplayMetrics): GestureDescription {
        val centerY = metrics.heightPixels / 2f
        val screenW = metrics.widthPixels
        val (startX, endX) = if (left) Pair(screenW * 0.80f, screenW * 0.20f)
        else Pair(screenW * 0.20f, screenW * 0.80f)
        val path = Path().apply { moveTo(startX, centerY); lineTo(endX, centerY) }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 350L))
            .build()
    }

    private fun buildTap(metrics: DisplayMetrics): GestureDescription {
        val centerX = metrics.widthPixels / 2f
        val centerY = metrics.heightPixels / 2f
        val path = Path().apply { moveTo(centerX, centerY) }
        return GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 50L))
            .build()
    }
}
