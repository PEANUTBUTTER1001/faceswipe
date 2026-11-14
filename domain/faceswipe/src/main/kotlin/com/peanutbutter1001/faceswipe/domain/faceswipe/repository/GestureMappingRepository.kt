package com.peanutbutter1001.faceswipe.domain.faceswipe.repository

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import kotlinx.coroutines.flow.Flow

/**
 * 앱별 제스처-액션 매핑 설정의 CRUD 인터페이스.
 *
 * 구현체는 data 레이어에서 DataStore 기반으로 제공한다.
 */
interface GestureMappingRepository {

    /**
     * 특정 앱의 전체 제스처 매핑을 관찰한다.
     * 매핑이 변경될 때마다 새로운 값을 emit한다.
     *
     * @param targetApp 대상 앱
     * @return 제스처별 액션 매핑. null 값은 "선택 안 함"을 의미.
     */
    fun getMappings(targetApp: TargetApp): Flow<Map<GestureTrigger, GestureAction?>>

    /**
     * 특정 앱의 단일 제스처-액션 매핑을 업데이트한다.
     *
     * @param targetApp 대상 앱
     * @param trigger 변경할 제스처
     * @param action 매핑할 액션. null이면 "선택 안 함".
     */
    suspend fun updateMapping(
        targetApp: TargetApp,
        trigger: GestureTrigger,
        action: GestureAction?,
    )

    /**
     * 특정 앱의 모든 매핑을 기본값(선택 안 함)으로 초기화한다.
     *
     * @param targetApp 대상 앱
     */
    suspend fun resetMappings(targetApp: TargetApp)

    /**
     * 특정 앱의 매핑 전체를 한 번에 저장한다 (해제 실행 취소 등 일괄 복원용).
     * 전달하지 않은 제스처는 변경하지 않는다.
     */
    suspend fun setMappings(
        targetApp: TargetApp,
        mappings: Map<GestureTrigger, GestureAction?>,
    )
}
