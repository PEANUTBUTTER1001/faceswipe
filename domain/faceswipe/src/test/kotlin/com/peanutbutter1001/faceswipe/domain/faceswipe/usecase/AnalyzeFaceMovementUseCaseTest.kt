package com.peanutbutter1001.faceswipe.domain.faceswipe.usecase

import com.peanutbutter1001.faceswipe.domain.faceswipe.gesture.HeadTurnDetector
import com.peanutbutter1001.faceswipe.domain.faceswipe.gesture.MouthOpenDetector
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.FaceData
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureConfig
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.repository.FaceTrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyzeFaceMovementUseCaseTest {

    private val config = GestureConfig()

    private val fakeRepository = object : FaceTrackingRepository {
        override val faceDataFlow: Flow<FaceData> = emptyFlow()
        override fun startTracking() = Unit
        override fun stopTracking() = Unit
    }

    private fun face(eulerY: Float = 0f, mouth: Float = 0f) =
        FaceData(eulerX = 0f, eulerY = eulerY, eulerZ = 0f, mouthOpenRatio = mouth)

    @Test
    fun `프레임 공백 뒤에는 이전 입 벌림 상태가 남지 않음`() {
        val useCase = AnalyzeFaceMovementUseCase(fakeRepository, setOf(MouthOpenDetector(config)))
        useCase.process(face(mouth = 0.1f), 1_000)
        useCase.process(face(mouth = 0.1f), 1_000 + config.mouthMinHoldMs) // 유지 충족
        // 얼굴 공백(카메라 정지·얼굴 놓침) 후 다문 입으로 복귀
        val afterGap = 1_000 + config.mouthMinHoldMs + AnalyzeFaceMovementUseCase.FACE_GAP_RESET_MS + 1
        assertNull(useCase.process(face(mouth = 0f), afterGap))
    }

    @Test
    fun `공백이 없으면 정상적으로 입 벌리기 트리거`() {
        val useCase = AnalyzeFaceMovementUseCase(fakeRepository, setOf(MouthOpenDetector(config)))
        useCase.process(face(mouth = 0.1f), 1_000)
        useCase.process(face(mouth = 0.1f), 1_000 + config.mouthMinHoldMs)
        assertEquals(GestureTrigger.MouthOpen, useCase.process(face(mouth = 0f), 1_000 + config.mouthMinHoldMs + 30))
    }

    @Test
    fun `한 프레임에는 제스처 하나만 반환`() {
        val useCase = AnalyzeFaceMovementUseCase(
            fakeRepository,
            linkedSetOf(HeadTurnDetector(config), MouthOpenDetector(config)),
        )
        assertEquals(GestureTrigger.HeadRight, useCase.process(face(eulerY = 20f), 1_000))
    }

    @Test
    fun `reset을 호출하면 고개 중립 대기 상태도 풀림`() {
        val useCase = AnalyzeFaceMovementUseCase(fakeRepository, setOf(HeadTurnDetector(config)))
        useCase.process(face(eulerY = 20f), 1_000)
        useCase.reset()
        // reset 후 첫 프레임: 중립 복귀 없이도 다시 트리거 가능
        assertEquals(GestureTrigger.HeadRight, useCase.process(face(eulerY = 20f), 1_050))
    }
}
