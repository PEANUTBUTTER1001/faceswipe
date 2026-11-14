package com.peanutbutter1001.faceswipe.domain.faceswipe.model

/**
 * 제스처 감지 설정을 외부화한 데이터 클래스.
 *
 * 감지 임계값과 쿨타임 등을 런타임에 변경할 수 있도록 분리.
 *
 * 주의: 트리거 → 액션 매핑은 더 이상 여기서 관리하지 않는다.
 * 앱별 매핑은 GestureMappingRepository(DataStore)에서 관리하며,
 * 사용자가 설정 화면에서 변경한다.
 *
 * @param triggerThreshold 좌우 회전 트리거 임계값 (도)
 * @param neutralThreshold 중립 복귀 판단 기준 (도)
 * @param cooldownMs 제스처 발동 후 최소 대기 시간 (ms)
 * @param mouthOpenThreshold 입 벌림 판단 임계값 (mouthOpenRatio가 이 값 이상이면 벌린 것으로 판단)
 * @param mouthClosedThreshold 입 다물기 판단 임계값 (mouthOpenRatio가 이 값 이하이면 다문 것으로 판단)
 * @param mouthMinHoldMs 입 벌림 최소 유지 시간 (ms). 말하기/하품과 구분
 */
data class GestureConfig(
    val triggerThreshold: Float = 15f,
    val neutralThreshold: Float = 7f,
    val cooldownMs: Long = 800L,
    val mouthOpenThreshold: Float = 0.04f,
    val mouthClosedThreshold: Float = 0.04f,
    val mouthMinHoldMs: Long = 100L,
)
