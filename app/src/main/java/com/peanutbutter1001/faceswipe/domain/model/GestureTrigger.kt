package com.peanutbutter1001.faceswipe.domain.model

/**
 * 고개 움직임 분석 결과로 발생하는 제스처 트리거.
 * SwipeUp: 고개를 위로 들었을 때 → 쇼츠 다음 영상
 * SwipeDown: 고개를 아래로 숙였을 때 → 쇼츠 이전 영상
 */
sealed class GestureTrigger {
    data object SwipeUp : GestureTrigger()
    data object SwipeDown : GestureTrigger()
}
