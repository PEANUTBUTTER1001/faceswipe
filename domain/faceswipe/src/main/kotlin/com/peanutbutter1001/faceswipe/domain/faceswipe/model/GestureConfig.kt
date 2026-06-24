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
 * @param winkClosedThreshold 눈 감김 판단 임계값 (이 값 미만이면 감은 것으로 판단)
 * @param winkOpenThreshold 눈 뜸 판단 임계값 (이 값 초과이면 뜬 것으로 판단)
 * @param winkMinHoldMs 윙크 최소 유지 시간 (ms). 일반 깜빡임과 구분하기 위해 한쪽 눈만 감은 상태가 이 시간 이상 지속되어야 윙크로 판정
 * @param mouthOpenThreshold 입 벌림 판단 임계값 (mouthOpenRatio가 이 값 이상이면 벌린 것으로 판단)
 * @param mouthClosedThreshold 입 다물기 판단 임계값 (mouthOpenRatio가 이 값 이하이면 다문 것으로 판단)
 * @param mouthMinHoldMs 입 벌림 최소 유지 시간 (ms). 말하기/하품과 구분
 * @param triggerToAction 트리거 → 실행 동작 매핑
 */
data class GestureConfig(
    val triggerThreshold: Float = 20f,
    val neutralThreshold: Float = 8f,
    val cooldownMs: Long = 1500L,
    val winkClosedThreshold: Float = 0.4f,
    val winkOpenThreshold: Float = 0.5f,
    val winkMinHoldMs: Long = 150L,
    val mouthOpenThreshold: Float = 0.08f,
    val mouthClosedThreshold: Float = 0.03f,
    val mouthMinHoldMs: Long = 200L,
    val triggerToAction: Map<GestureTrigger, GestureAction> = mapOf(
        GestureTrigger.SwipeUp to GestureAction.SwipeVertical(directionUp = true),
        GestureTrigger.SwipeDown to GestureAction.SwipeVertical(directionUp = false),
        GestureTrigger.Wink to GestureAction.SwipeVertical(directionUp = true),
        GestureTrigger.MouthOpen to GestureAction.Tap
    )
)
