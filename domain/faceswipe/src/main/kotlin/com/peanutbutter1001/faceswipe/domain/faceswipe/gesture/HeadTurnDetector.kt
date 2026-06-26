package com.peanutbutter1001.faceswipe.domain.faceswipe.gesture

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.FaceData
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureConfig
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import javax.inject.Inject
import kotlin.math.abs

/**
 * eulerY(좌우 회전각) 기반 고개 돌림 감지기.
 *
 * 알고리즘:
 *  1. eulerY > +triggerThreshold → 오른쪽 회전 → HeadRight
 *  2. eulerY < -triggerThreshold → 왼쪽 회전 → HeadLeft
 *  3. 트리거 후 중립 구간(|eulerY| < neutralThreshold)으로 돌아와야 다음 제스처 가능
 *  4. 쿨타임(cooldownMs) 동안 추가 트리거 무시
 */
class HeadTurnDetector @Inject constructor(
    private val config: GestureConfig
) : GestureDetector {

    private var isTriggered = false
    private var lastTriggerTime = 0L

    override fun detect(faceData: FaceData, timestampMs: Long): GestureTrigger? {
        val eulerY = faceData.eulerY

        // 중립 복귀 체크
        if (isTriggered && abs(eulerY) < config.neutralThreshold) {
            isTriggered = false
        }

        if (isTriggered || timestampMs - lastTriggerTime < config.cooldownMs) return null

        return when {
            eulerY > config.triggerThreshold -> {
                isTriggered = true
                lastTriggerTime = timestampMs
                GestureTrigger.HeadRight
            }
            eulerY < -config.triggerThreshold -> {
                isTriggered = true
                lastTriggerTime = timestampMs
                GestureTrigger.HeadLeft
            }
            else -> null
        }
    }
}
