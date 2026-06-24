package com.peanutbutter1001.faceswipe.domain.faceswipe.gesture

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.FaceData
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureConfig
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import javax.inject.Inject

/**
 * 입 벌렸다 다물기 감지기.
 *
 * 알고리즘:
 *  1. mouthOpenRatio가 openThreshold 이상이면 '입 벌린 상태' 진입
 *  2. 해당 상태가 mouthMinHoldMs 이상 유지되어야 의도적 입 벌리기로 인정
 *  3. 이후 mouthOpenRatio가 closedThreshold 이하로 내려가면(다물기) 트리거 발동
 *  4. 쿨타임(cooldownMs) 동안 추가 트리거 무시
 *
 * 주의:
 *  - 말하기, 하품 등과 구분하기 위해 minHoldMs를 적절히 설정
 *  - mouthOpenRatio가 null이면 컨투어 미감지 → null 반환
 */
class MouthOpenDetector @Inject constructor(
    private val config: GestureConfig
) : GestureDetector {

    private var isMouthOpen = false
    private var mouthOpenStartTime = 0L
    private var holdSatisfied = false
    private var lastTriggerTime = 0L

    override fun detect(faceData: FaceData, timestampMs: Long): GestureTrigger? {
        val ratio = faceData.mouthOpenRatio ?: return null

        val isOpen = ratio >= config.mouthOpenThreshold
        val isClosed = ratio <= config.mouthClosedThreshold

        if (isOpen) {
            if (!isMouthOpen) {
                // 입 벌리기 시작 감지
                isMouthOpen = true
                mouthOpenStartTime = timestampMs
                holdSatisfied = false
            } else if (!holdSatisfied && timestampMs - mouthOpenStartTime >= config.mouthMinHoldMs) {
                // 최소 유지 시간 충족
                holdSatisfied = true
            }
            return null
        }

        // 입 다물기 감지 → 트리거 발동 조건 확인
        if (isMouthOpen && holdSatisfied && isClosed) {
            resetState()

            // 쿨타임 체크
            if (timestampMs - lastTriggerTime < config.cooldownMs) return null

            lastTriggerTime = timestampMs
            return GestureTrigger.MouthOpen
        }

        // 최소 유지 시간 미달 상태에서 입을 다문 경우 → 리셋
        if (isMouthOpen && !holdSatisfied && isClosed) {
            resetState()
        }

        return null
    }

    private fun resetState() {
        isMouthOpen = false
        mouthOpenStartTime = 0L
        holdSatisfied = false
    }
}
