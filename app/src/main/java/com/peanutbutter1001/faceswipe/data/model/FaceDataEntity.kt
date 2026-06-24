package com.peanutbutter1001.faceswipe.data.model

/**
 * ML Kit FaceDetector가 반환하는 고개 각도 데이터 엔티티.
 *
 * @param eulerX 상하 끄덕임 (양수: 위, 음수: 아래)
 * @param eulerY 좌우 회전  (양수: 오른쪽, 음수: 왼쪽) ← 제스처 인식에 사용
 * @param eulerZ 좌우 기울기 (양수: 오른쪽 기울기, 음수: 왼쪽 기울기)
 */
data class FaceDataEntity(
    val eulerX: Float,
    val eulerY: Float,
    val eulerZ: Float
)
