package com.peanutbutter1001.faceswipe.domain.faceswipe.model

/**
 * 제스처 감지 설정을 외부화한 데이터 클래스.
 *
 * 임계값, 쿨타임, 트리거→액션 매핑을 런타임에 변경할 수 있도록 분리.
 * 추후 DataStore/Room 기반 사용자 설정과 연동 가능.
 *
 * @param triggerThreshold 좌우 회전 트리거 임계값 (도)
 * @param neutralThreshold 중립 복귀 판단 기준 (도)
 * @param cooldownMs 제스처 발동 후 최소 대기 시간 (ms)
 * @param triggerToAction 트리거 → 실행 동작 매핑
 */
data class GestureConfig(
    val triggerThreshold: Float = 20f,
    val neutralThreshold: Float = 8f,
    val cooldownMs: Long = 1500L,
    val triggerToAction: Map<GestureTrigger, GestureAction> = mapOf(
        GestureTrigger.SwipeUp to GestureAction.SwipeVertical(directionUp = true),
        GestureTrigger.SwipeDown to GestureAction.SwipeVertical(directionUp = false)
    )
)
