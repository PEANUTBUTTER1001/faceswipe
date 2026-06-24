package com.peanutbutter1001.faceswipe.data.faceswipe.model

/**
 * ML Kit에서 추출한 raw 얼굴 데이터.
 *
 * data 레이어 전용 모델. domain에서는 FaceData를 사용한다.
 */
data class FaceDataEntity(
    val eulerX: Float,
    val eulerY: Float,
    val eulerZ: Float,
    val leftEyeOpenProbability: Float? = null,
    val rightEyeOpenProbability: Float? = null,
    val mouthOpenRatio: Float? = null
)
