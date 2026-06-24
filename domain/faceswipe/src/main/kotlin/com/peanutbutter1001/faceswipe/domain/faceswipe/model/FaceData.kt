package com.peanutbutter1001.faceswipe.domain.faceswipe.model

/**
 * domain 전용 얼굴 데이터 모델.
 *
 * data 레이어의 FaceDataEntity와 분리하여 domain 순수성을 보장한다.
 * ML Kit Classification 필드(눈 깜빡임 등)는 추후 확장 시 Optional로 추가 가능.
 *
 * @param eulerX 상하 기울기 (끄덕임)
 * @param eulerY 좌우 회전 (고개 돌림)
 * @param eulerZ 좌우 기울기 (갸우뚱)
 */
data class FaceData(
    val eulerX: Float,
    val eulerY: Float,
    val eulerZ: Float
)
