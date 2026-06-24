package com.peanutbutter1001.faceswipe.domain.faceswipe.model

/**
 * GestureTrigger(무엇을 감지했나)와 분리된 실제 동작 모델.
 *
 * GestureTrigger → GestureConfig.triggerToAction → GestureAction 흐름으로
 * 감지와 실행을 분리한다. 이를 통해 "고개 오른쪽 = 다음 쇼츠" 매핑을
 * 런타임에 변경할 수 있다.
 */
sealed interface GestureAction {
    data class SwipeVertical(val directionUp: Boolean) : GestureAction
    data class SwipeHorizontal(val directionLeft: Boolean) : GestureAction
    data object Tap : GestureAction
    data object Pause : GestureAction
}
