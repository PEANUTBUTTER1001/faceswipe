package com.peanutbutter1001.faceswipe.domain.faceswipe.model

/**
 * domain 전용 얼굴 데이터 모델.
 *
 * data 레이어의 FaceDataEntity와 분리하여 domain 순수성을 보장한다.
 *
 * @param eulerX 상하 기울기 (끄덕임)
 * @param eulerY 좌우 회전 (고개 돌림)
 * @param eulerZ 좌우 기울기 (갸우뚱)
 * @param leftEyeOpenProbability 왼쪽 눈 열림 확률 (0.0~1.0, null이면 미감지)
 * @param rightEyeOpenProbability 오른쪽 눈 열림 확률 (0.0~1.0, null이면 미감지)
 * @param mouthOpenRatio 입 벌림 비율 (윗입술~아랫입술 거리 / 얼굴 높이, null이면 미감지)
 */
data class FaceData(
    val eulerX: Float,
    val eulerY: Float,
    val eulerZ: Float,
    val leftEyeOpenProbability: Float? = null,
    val rightEyeOpenProbability: Float? = null,
    val mouthOpenRatio: Float? = null
)
