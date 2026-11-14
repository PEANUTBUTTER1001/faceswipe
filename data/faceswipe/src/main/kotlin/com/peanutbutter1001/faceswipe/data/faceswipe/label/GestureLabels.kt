package com.peanutbutter1001.faceswipe.data.faceswipe.label

import androidx.annotation.StringRes
import com.peanutbutter1001.faceswipe.data.faceswipe.R
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp

/**
 * 도메인 모델 → 표시 문자열 리소스 ID.
 *
 * domain은 Android 리소스를 알 수 없으므로 data에 둔다.
 * 오버레이(접근성 서비스)와 화면(feature)이 같은 이름을 쓰도록 한 곳에서 관리한다.
 */
object GestureLabels {

    @StringRes
    fun trigger(trigger: GestureTrigger): Int = when (trigger) {
        GestureTrigger.HeadLeft -> R.string.trigger_head_left
        GestureTrigger.HeadRight -> R.string.trigger_head_right
        GestureTrigger.MouthOpen -> R.string.trigger_mouth_open
    }

    /** null은 "선택 안 함" */
    @StringRes
    fun action(action: GestureAction?): Int = when (action) {
        is GestureAction.SwipeVertical ->
            if (action.directionUp) R.string.action_swipe_up else R.string.action_swipe_down
        is GestureAction.SwipeHorizontal ->
            if (action.directionLeft) R.string.action_swipe_left else R.string.action_swipe_right
        GestureAction.Tap -> R.string.action_tap_bottom_quarter
        GestureAction.Pause -> R.string.action_pause
        null -> R.string.action_none
    }

    @StringRes
    fun app(app: TargetApp): Int = when (app) {
        TargetApp.YOUTUBE -> R.string.app_youtube
        TargetApp.MILLIE -> R.string.app_millie
    }
}
