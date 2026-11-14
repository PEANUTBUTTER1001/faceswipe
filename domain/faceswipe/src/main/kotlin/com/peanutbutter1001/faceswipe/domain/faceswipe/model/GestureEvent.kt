package com.peanutbutter1001.faceswipe.domain.faceswipe.model

/**
 * 감지된 제스처 1건과 그 시점 대상 앱에 연결된 동작.
 *
 * 동작이 연결되지 않은 제스처도 발행해, 오버레이가 "연결된 동작이 없어요"를 알릴 수 있게 한다.
 *
 * @param action null이면 연결된 동작 없음 (실행하지 않음)
 */
data class GestureEvent(
    val trigger: GestureTrigger,
    val action: GestureAction?,
)
