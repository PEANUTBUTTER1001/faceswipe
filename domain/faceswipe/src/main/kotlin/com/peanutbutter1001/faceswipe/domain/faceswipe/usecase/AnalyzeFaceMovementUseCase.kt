package com.peanutbutter1001.faceswipe.domain.faceswipe.usecase

import com.peanutbutter1001.faceswipe.domain.faceswipe.gesture.GestureDetector
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.FaceTrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject

/**
 * 얼굴 데이터를 분석하여 감지된 raw [GestureTrigger] 스트림을 발행하는 유즈케이스.
 *
 * 전략 패턴을 사용하여 여러 GestureDetector에 감지를 위임한다.
 * 새로운 제스처를 추가하려면 GestureDetector 구현체만 만들고
 * Hilt @IntoSet으로 등록하면 된다.
 *
 * 주의: trigger → action 매핑은 의도적으로 이 유즈케이스에서 분리했다.
 * 감지(연속/stateful)와 매핑(앱별/교체)을 분리하면:
 *  - 앱 전환 시 감지 파이프라인을 재시작하지 않아 detector 상태 손상이 없고
 *  - 전환 과도기에 이전 앱 매핑이 누출되는 race가 발생하지 않으며
 *  - 프레임 누락이 없다.
 * 매핑 변환은 호출부(FaceSwipeForegroundService)에서 처리한다.
 */
class AnalyzeFaceMovementUseCase @Inject constructor(
    private val repository: FaceTrackingRepository,
    private val detectors: Set<@JvmSuppressWildcards GestureDetector>,
) {
    operator fun invoke(): Flow<GestureTrigger> = channelFlow {
        repository.faceDataFlow.collect { faceData ->
            val now = System.currentTimeMillis()
            for (detector in detectors) {
                val trigger = detector.detect(faceData, now) ?: continue
                send(trigger)
                break // 한 프레임에 하나의 제스처만 처리
            }
        }
    }
}
