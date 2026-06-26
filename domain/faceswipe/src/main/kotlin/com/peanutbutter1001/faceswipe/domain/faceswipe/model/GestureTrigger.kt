package com.peanutbutter1001.faceswipe.domain.faceswipe.model

/**
 * 감지 가능한 제스처 트리거.
 *
 * 각 detector가 얼굴 데이터를 분석해 해당 트리거를 emit하며,
 * 트리거 → 액션 매핑은 앱별로 GestureMappingRepository(DataStore)에서 관리한다.
 */
sealed interface GestureTrigger {
    /** 고개 왼쪽 회전 */
    data object HeadLeft : GestureTrigger

    /** 고개 오른쪽 회전 */
    data object HeadRight : GestureTrigger

    /** 왼쪽 눈 윙크 */
    data object WinkLeft : GestureTrigger

    /** 오른쪽 눈 윙크 */
    data object WinkRight : GestureTrigger

    /** 입 벌렸다 다물기 */
    data object MouthOpen : GestureTrigger

    companion object {
        /** 설정 화면에 노출되는 제스처 목록 */
        val settingsEntries: List<GestureTrigger> =
            listOf(HeadLeft, HeadRight, WinkLeft, WinkRight, MouthOpen)
    }
}
