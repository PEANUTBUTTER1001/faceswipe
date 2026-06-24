package com.peanutbutter1001.faceswipe.domain.repository

import com.peanutbutter1001.faceswipe.data.model.FaceDataEntity
import kotlinx.coroutines.flow.SharedFlow

/**
 * 얼굴 추적 데이터를 제공하는 리포지토리 인터페이스.
 * Data 레이어 구현체(FaceTrackingRepositoryImpl)에 의해 CameraX + ML Kit로 구현됩니다.
 */
interface FaceTrackingRepository {
    /** ML Kit가 감지한 얼굴 Euler 각도 스트림 */
    val faceDataFlow: SharedFlow<FaceDataEntity>

    /** CameraX 분석 파이프라인 시작 (YouTube 포그라운드 진입 시) */
    fun startTracking()

    /** CameraX 분석 파이프라인 중지 (YouTube 백그라운드 진입 시) */
    fun stopTracking()
}
