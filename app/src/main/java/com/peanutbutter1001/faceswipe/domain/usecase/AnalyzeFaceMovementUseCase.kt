package com.peanutbutter1001.faceswipe.domain.usecase

import com.peanutbutter1001.faceswipe.domain.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.repository.FaceTrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject
import kotlin.math.abs

/**
 * eulerY(좌우 회전각) 절대 임계값 기반 제스처 감지 유즈케이스.
 *
 * 알고리즘:
 *  1. eulerY > +TURN_THRESHOLD  → 오른쪽 회전 → SwipeUp  (다음 쇼츠)
 *  2. eulerY < -TURN_THRESHOLD  → 왼쪽 회전  → SwipeDown (이전 쇼츠)
 *  3. 트리거 후 중립 구간(|eulerY| < NEUTRAL_THRESHOLD)으로 돌아와야 다음 제스처 가능
 *  4. 쿨타임(COOLDOWN_MS) 동안 추가 트리거 무시
 *
 * delta 방식 대신 절대 임계값 방식을 사용하는 이유:
 *  - 좌우 회전은 끄덕임과 달리 각도를 '유지'하는 동작이므로 delta가 0에 가까워짐
 *  - 절대값으로 비교하면 천천히 돌려도, 빠르게 돌려도 동일하게 인식
 */
private const val TURN_THRESHOLD = 20f    // 좌우 회전 트리거 임계값 (도)
private const val NEUTRAL_THRESHOLD = 8f  // 중립 복귀 판단 기준 (도) — 이 안에 들어와야 재발동 가능
private const val COOLDOWN_MS = 1500L     // 제스처 발동 후 최소 대기 시간 (ms)

class AnalyzeFaceMovementUseCase @Inject constructor(
    private val repository: FaceTrackingRepository
) {
    operator fun invoke(): Flow<GestureTrigger> = channelFlow {
        var isTriggered = false   // 현재 제스처가 발동된 상태인지
        var lastTriggerTime = 0L

        repository.faceDataFlow.collect { faceData ->
            val eulerY = faceData.eulerY
            val now = System.currentTimeMillis()

            // 중립 구간 복귀 체크 — 트리거 상태 해제
            if (isTriggered && abs(eulerY) < NEUTRAL_THRESHOLD) {
                isTriggered = false
            }

            // 쿨타임 중이거나 이미 트리거된 상태면 무시
            if (isTriggered || now - lastTriggerTime < COOLDOWN_MS) return@collect

            when {
                eulerY > TURN_THRESHOLD -> {
                    // 오른쪽으로 돌림 → 다음 쇼츠
                    isTriggered = true
                    lastTriggerTime = now
                    send(GestureTrigger.SwipeUp)
                }
                eulerY < -TURN_THRESHOLD -> {
                    // 왼쪽으로 돌림 → 이전 쇼츠
                    isTriggered = true
                    lastTriggerTime = now
                    send(GestureTrigger.SwipeDown)
                }
            }
        }
    }
}
