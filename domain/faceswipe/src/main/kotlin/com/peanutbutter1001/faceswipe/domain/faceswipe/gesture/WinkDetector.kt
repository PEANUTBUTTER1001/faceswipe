package com.peanutbutter1001.faceswipe.domain.faceswipe.gesture

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.FaceData
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureConfig
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import javax.inject.Inject

/**
 * 한쪽 눈 윙크(감았다 뜨기) 감지기.
 *
 * 알고리즘:
 *  1. 한쪽 눈은 감고(< closedThreshold) 다른 쪽은 뜬 상태(> openThreshold) 감지
 *  2. 해당 상태가 winkMinHoldMs 이상 유지되어야 윙크로 인정 (일반 깜빡임 필터링)
 *  3. 이후 양쪽 눈 모두 뜬 상태로 복귀하면 Wink 트리거 발동
 *  4. 쿨타임(cooldownMs) 동안 추가 트리거 무시
 *
 * 주의:
 *  - 양쪽 눈 모두 감은 경우(일반 깜빡임)는 무시하고 상태 리셋
 *  - 일반 깜빡임 시 한쪽 눈이 1-2프레임 먼저 감기는 경우도 minHoldMs로 필터링
 *  - ML Kit Classification이 null을 반환하면 감지 불가 → null 반환
 */
class WinkDetector @Inject constructor(
    private val config: GestureConfig
) : GestureDetector {

    /** 윙크 상태 (한쪽만 감은 상태가 감지됨) */
    private var isWinking = false
    /** 한쪽 눈만 감은 상태가 시작된 시각 */
    private var winkStartTime = 0L
    /** 최소 유지 시간을 충족했는지 여부 */
    private var holdSatisfied = false
    private var lastTriggerTime = 0L

    override fun detect(faceData: FaceData, timestampMs: Long): GestureTrigger? {
        val leftProb = faceData.leftEyeOpenProbability ?: return null
        val rightProb = faceData.rightEyeOpenProbability ?: return null

        val leftClosed = leftProb < config.winkClosedThreshold
        val rightClosed = rightProb < config.winkClosedThreshold
        val leftOpen = leftProb > config.winkOpenThreshold
        val rightOpen = rightProb > config.winkOpenThreshold

        // 양쪽 다 감은 경우 → 일반 깜빡임, 윙크 상태 리셋
        if (leftClosed && rightClosed) {
            resetState()
            return null
        }

        // 한쪽만 감은 상태 감지 (양쪽 다 감으면 위에서 이미 리셋됨)
        val oneEyeWink = (leftClosed && rightOpen) || (rightClosed && leftOpen)

        if (oneEyeWink) {
            if (!isWinking) {
                // 윙크 시작 감지 — 시작 시각 기록
                isWinking = true
                winkStartTime = timestampMs
                holdSatisfied = false
            } else if (!holdSatisfied && timestampMs - winkStartTime >= config.winkMinHoldMs) {
                // 최소 유지 시간 충족
                holdSatisfied = true
            }
            return null
        }

        // 한쪽만 감은 상태가 아님 → 윙크 후 복귀 확인
        if (isWinking && holdSatisfied && leftOpen && rightOpen) {
            // 윙크 후 양쪽 눈 모두 다시 뜸 + 최소 유지 시간 충족 → 트리거 발동
            resetState()

            // 쿨타임 체크
            if (timestampMs - lastTriggerTime < config.cooldownMs) return null

            lastTriggerTime = timestampMs
            return GestureTrigger.Wink
        }

        // 최소 유지 시간 미달 상태에서 눈을 뜬 경우 → 일반 깜빡임으로 판단, 리셋
        if (isWinking && !holdSatisfied) {
            resetState()
        }

        return null
    }

    private fun resetState() {
        isWinking = false
        winkStartTime = 0L
        holdSatisfied = false
    }
}
