package com.peanutbutter1001.faceswipe.domain.faceswipe.usecase

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.GestureMappingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 특정 앱의 제스처 매핑 설정을 조회하는 유스케이스.
 *
 * Flow를 반환하여 설정 변경 시 UI가 자동으로 갱신된다.
 */
class GetGestureMappingUseCase @Inject constructor(
    private val repository: GestureMappingRepository,
) {
    operator fun invoke(
        targetApp: TargetApp,
    ): Flow<Map<GestureTrigger, GestureAction?>> {
        return repository.getMappings(targetApp)
    }
}
