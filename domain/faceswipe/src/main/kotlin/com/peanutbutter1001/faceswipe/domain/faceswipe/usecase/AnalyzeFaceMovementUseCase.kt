package com.peanutbutter1001.faceswipe.domain.faceswipe.usecase

import com.peanutbutter1001.faceswipe.domain.faceswipe.gesture.GestureDetector
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureConfig
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.FaceTrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject

/**
 * 얼굴 데이터를 분석하여 제스처 액션을 발행하는 유즈케이스.
 *
 * 전략 패턴을 사용하여 여러 GestureDetector에 감지를 위임한다.
 * 새로운 제스처를 추가하려면 GestureDetector 구현체만 만들고
 * Hilt @IntoSet으로 등록하면 된다.
 */
class AnalyzeFaceMovementUseCase @Inject constructor(
    private val repository: FaceTrackingRepository,
    private val detectors: Set<@JvmSuppressWildcards GestureDetector>,
    private val config: GestureConfig
) {
    operator fun invoke(): Flow<GestureAction> = channelFlow {
        repository.faceDataFlow.collect { faceData ->
            val now = System.currentTimeMillis()
            for (detector in detectors) {
                val trigger = detector.detect(faceData, now) ?: continue
                val action = config.triggerToAction[trigger] ?: continue
                send(action)
                break // 한 프레임에 하나의 제스처만 처리
            }
        }
    }
}
