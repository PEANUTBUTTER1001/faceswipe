package com.peanutbutter1001.faceswipe.domain.faceswipe.usecase

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.GestureMappingRepository
import javax.inject.Inject

/**
 * 단일 제스처-액션 매핑을 업데이트하는 유스케이스.
 *
 * 변경 즉시 DataStore에 persist되어 앱 재시작 후에도 유지된다.
 */
class UpdateGestureMappingUseCase @Inject constructor(
    private val repository: GestureMappingRepository,
) {
    /**
     * @param targetApp 대상 앱
     * @param trigger 변경할 제스처
     * @param action 매핑할 액션. null이면 "선택 안 함".
     */
    suspend operator fun invoke(
        targetApp: TargetApp,
        trigger: GestureTrigger,
        action: GestureAction?,
    ) {
        repository.updateMapping(targetApp, trigger, action)
    }

    /**
     * 특정 앱의 모든 매핑을 기본값으로 초기화한다.
     */
    suspend fun reset(targetApp: TargetApp) {
        repository.resetMappings(targetApp)
    }
}
