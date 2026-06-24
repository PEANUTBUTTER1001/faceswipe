package com.peanutbutter1001.faceswipe.domain.faceswipe.repository

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.FaceData
import kotlinx.coroutines.flow.Flow

/**
 * 얼굴 추적 데이터 제공 리포지토리 인터페이스.
 *
 * domain 모듈은 순수 Kotlin이므로 Android 의존성(CameraX, Context 등)을 포함하지 않는다.
 * 구현체(FaceTrackingRepositoryImpl)는 :data:faceswipe 모듈에 위치한다.
 */
interface FaceTrackingRepository {
    /** 얼굴 감지 데이터 스트림. domain 전용 FaceData 모델 사용. */
    val faceDataFlow: Flow<FaceData>

    fun startTracking()
    fun stopTracking()
}
