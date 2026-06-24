package com.peanutbutter1001.faceswipe.domain.faceswipe.gesture

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.FaceData
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger

/**
 * 제스처 감지 전략 인터페이스.
 *
 * 각 구현체는 FaceData의 특정 축/특성을 분석하여 제스처를 감지한다.
 * 쿨타임/중립 복귀 등 상태 관리는 각 구현체 내부에서 처리한다.
 *
 * 주의: 구현체는 스레드 안전하지 않으므로 단일 코루틴에서 순차 호출해야 한다.
 */
interface GestureDetector {
    /**
     * FaceData를 분석하여 제스처를 감지하면 GestureTrigger 반환.
     * 감지되지 않았으면 null 반환.
     *
     * @param faceData 현재 프레임의 얼굴 데이터
     * @param timestampMs 현재 시각 (밀리초)
     * @return 감지된 제스처 또는 null
     */
    fun detect(faceData: FaceData, timestampMs: Long): GestureTrigger?
}
