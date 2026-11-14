package com.peanutbutter1001.faceswipe.domain.faceswipe.gesture

import com.peanutbutter1001.faceswipe.domain.faceswipe.model.FaceData
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureConfig
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 제스처 감지기 단위 테스트.
 *
 * 기준값은 현재 GestureConfig 기본값(사용자 조정값, 커밋 8ee38a9)을 그대로 사용한다.
 * 임계값을 바꿀 때 이 테스트가 바뀐 동작을 드러내도록 한다.
 */
class GestureDetectorsTest {

    private val config = GestureConfig()

    private fun face(
        eulerY: Float = 0f,
        mouth: Float? = 0.0f,
    ) = FaceData(eulerX = 0f, eulerY = eulerY, eulerZ = 0f, mouthOpenRatio = mouth)

    // ---------------- HeadTurnDetector ----------------

    @Test
    fun `고개를 임계값 넘게 돌리면 방향에 맞는 트리거`() {
        val d = HeadTurnDetector(config)
        assertEquals(GestureTrigger.HeadRight, d.detect(face(eulerY = 20f), 1_000))
        val d2 = HeadTurnDetector(config)
        assertEquals(GestureTrigger.HeadLeft, d2.detect(face(eulerY = -20f), 1_000))
    }

    @Test
    fun `임계값 이하 회전은 무시`() {
        assertNull(HeadTurnDetector(config).detect(face(eulerY = 10f), 1_000))
    }

    @Test
    fun `중립으로 돌아오기 전에는 다시 트리거하지 않음`() {
        val d = HeadTurnDetector(config)
        d.detect(face(eulerY = 20f), 1_000)
        // 쿨다운이 지나도 계속 돌린 상태면 무시
        assertNull(d.detect(face(eulerY = 20f), 3_000))
        // 중립 복귀 후 다시 돌리면 트리거
        assertNull(d.detect(face(eulerY = 0f), 3_100))
        assertEquals(GestureTrigger.HeadRight, d.detect(face(eulerY = 20f), 3_200))
    }

    @Test
    fun `쿨다운 안에서는 중립 복귀 후에도 무시`() {
        val d = HeadTurnDetector(config)
        d.detect(face(eulerY = 20f), 1_000)
        d.detect(face(eulerY = 0f), 1_100)
        assertNull(d.detect(face(eulerY = -20f), 1_000 + config.cooldownMs - 1))
    }

    // ---------------- MouthOpenDetector ----------------

    @Test
    fun `입을 유지 시간 이상 벌렸다 다물면 트리거`() {
        val d = MouthOpenDetector(config)
        d.detect(face(mouth = 0.1f), 1_000)
        d.detect(face(mouth = 0.1f), 1_000 + config.mouthMinHoldMs)
        assertEquals(GestureTrigger.MouthOpen, d.detect(face(mouth = 0.0f), 1_000 + config.mouthMinHoldMs + 30))
    }

    @Test
    fun `유지 시간보다 짧게 벌리면 무시`() {
        val d = MouthOpenDetector(config)
        d.detect(face(mouth = 0.1f), 1_000)
        d.detect(face(mouth = 0.1f), 1_000 + config.mouthMinHoldMs - 1)
        assertNull(d.detect(face(mouth = 0.0f), 1_000 + config.mouthMinHoldMs))
    }

    @Test
    fun `입 비율이 없으면 판단하지 않음`() {
        assertNull(MouthOpenDetector(config).detect(face(mouth = null), 1_000))
    }

    @Test
    fun `reset 후에는 벌린 상태가 남지 않아 다물어도 트리거 없음`() {
        val d = MouthOpenDetector(config)
        d.detect(face(mouth = 0.1f), 1_000)
        d.detect(face(mouth = 0.1f), 1_000 + config.mouthMinHoldMs)
        d.reset()
        assertNull(d.detect(face(mouth = 0.0f), 2_000))
    }
}
