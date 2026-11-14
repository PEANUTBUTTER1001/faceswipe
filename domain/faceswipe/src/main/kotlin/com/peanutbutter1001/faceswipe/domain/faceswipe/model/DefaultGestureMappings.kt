package com.peanutbutter1001.faceswipe.domain.faceswipe.model

/**
 * 새로 설치했을 때(저장된 값이 없는 제스처)의 기본 동작.
 *
 * 모든 대상 앱 공통 (명세 결정 A안):
 *  - 고개 오른쪽 → 위로 스와이프 (다음 쇼츠·다음 페이지)
 *  - 고개 왼쪽 → 아래로 스와이프 (이전)
 *  - 입 벌리기 → 선택 안 함 (오인식 위험이 있어 사용자가 직접 켜도록)
 *
 * 사용자가 "선택 안 함"을 고르거나 전체 해제한 값은 저장소에 명시적으로 기록되므로
 * 기본값으로 덮이지 않는다.
 */
object DefaultGestureMappings {

    private val defaults: Map<GestureTrigger, GestureAction?> = mapOf(
        GestureTrigger.HeadRight to GestureAction.SwipeVertical(directionUp = true),
        GestureTrigger.HeadLeft to GestureAction.SwipeVertical(directionUp = false),
    )

    /** 저장된 값이 없을 때 쓸 기본 동작. null이면 "선택 안 함". */
    fun forTrigger(trigger: GestureTrigger): GestureAction? = defaults[trigger]
}
