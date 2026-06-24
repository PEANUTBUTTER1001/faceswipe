package com.peanutbutter1001.faceswipe.domain.faceswipe.model

/**
 * 얼굴 제스처 감지 결과를 나타내는 sealed interface.
 *
 * 각 구현체는 특정 얼굴 움직임 패턴에 대응한다.
 * 새로운 제스처 타입은 여기에 추가하고, GestureDetector 구현체를 만들면 된다.
 */
sealed interface GestureTrigger {
    /** 오른쪽 회전 감지 → 다음 쇼츠 */
    data object SwipeUp : GestureTrigger

    /** 왼쪽 회전 감지 → 이전 쇼츠 */
    data object SwipeDown : GestureTrigger
}
