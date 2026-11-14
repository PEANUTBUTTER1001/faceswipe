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

    /** 입 벌렸다 다물기 */
    data object MouthOpen : GestureTrigger

    // 윙크(WinkLeft/WinkRight)는 오인식이 잦아 제거했다 (2026-11-14 실기기 테스트).
    // 이전에 저장된 윙크 매핑 키는 저장소에 남아도 읽히지 않는다.

    companion object {
        /** 설정 화면에 노출되는 제스처 목록 */
        val settingsEntries: List<GestureTrigger> =
            listOf(HeadLeft, HeadRight, MouthOpen)
    }
}
