package com.peanutbutter1001.faceswipe.domain.faceswipe.usecase

import com.peanutbutter1001.faceswipe.domain.faceswipe.gesture.GestureDetector
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.FaceData
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
 *
 * 공백 초기화: 프레임 간격이 [FACE_GAP_RESET_MS]를 넘으면(얼굴이 사라졌거나 카메라가 멈춤)
 * 감지기 상태를 먼저 버린다. 예: 입을 벌린 채 대상 앱을 나갔다가 다문 채 돌아와도
 * "입 벌리기"로 오인식하지 않는다.
 */
class AnalyzeFaceMovementUseCase @Inject constructor(
    private val repository: FaceTrackingRepository,
    private val detectors: Set<@JvmSuppressWildcards GestureDetector>,
) {
    /** 마지막으로 처리한 프레임 시각. 0이면 아직 없음. */
    private var lastFrameMs = 0L

    operator fun invoke(): Flow<GestureTrigger> = channelFlow {
        repository.faceDataFlow.collect { faceData ->
            process(faceData, System.currentTimeMillis())?.let { send(it) }
        }
    }

    /**
     * 프레임 1장 처리 (단위 테스트에서 시각을 지정해 호출).
     * 한 프레임에서는 처음 감지된 제스처 하나만 반환한다.
     */
    fun process(faceData: FaceData, nowMs: Long): GestureTrigger? {
        if (lastFrameMs != 0L && nowMs - lastFrameMs > FACE_GAP_RESET_MS) reset()
        lastFrameMs = nowMs
        for (detector in detectors) {
            return detector.detect(faceData, nowMs) ?: continue
        }
        return null
    }

    /** 모든 감지기 상태를 버린다 (대상 앱 이탈 시 서비스가 호출). */
    fun reset() {
        detectors.forEach { it.reset() }
        lastFrameMs = 0L
    }

    companion object {
        /** 이 간격보다 오래 프레임이 없으면 얼굴 공백으로 보고 상태 초기화 (분석 간격은 통상 수십 ms) */
        const val FACE_GAP_RESET_MS = 500L
    }
}
