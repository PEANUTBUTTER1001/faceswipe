package com.peanutbutter1001.faceswipe.data.faceswipe.model

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.FaceData

/** data 레이어의 FaceDataEntity를 domain 모델로 변환한다. */
fun FaceDataEntity.toDomain(): FaceData = FaceData(
    eulerX = eulerX,
    eulerY = eulerY,
    eulerZ = eulerZ
)
