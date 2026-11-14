package com.peanutbutter1001.faceswipe.feature.faceswipe.setup

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 설정 마법사 순수 판정 함수 테스트.
 * 화면 단계와 카메라 안내 문구는 이 두 함수의 결과로만 결정된다.
 */
class SetupModelsTest {

    // ---------- resolveCameraState ----------

    @Test
    fun `허용되어 있으면 다른 값과 무관하게 Granted`() {
        assertEquals(
            CameraPermissionState.Granted,
            resolveCameraState(granted = true, requested = true, rationale = false, rationaleSeen = true),
        )
    }

    @Test
    fun `한 번도 요청하지 않았으면 NotAsked`() {
        assertEquals(
            CameraPermissionState.NotAsked,
            resolveCameraState(granted = false, requested = false, rationale = false, rationaleSeen = false),
        )
    }

    @Test
    fun `한 번 거부해 rationale이 true면 Denied`() {
        assertEquals(
            CameraPermissionState.Denied,
            resolveCameraState(granted = false, requested = true, rationale = true, rationaleSeen = true),
        )
    }

    @Test
    fun `거부 이력 후 rationale이 false가 되면 Blocked`() {
        assertEquals(
            CameraPermissionState.Blocked,
            resolveCameraState(granted = false, requested = true, rationale = false, rationaleSeen = true),
        )
    }

    @Test
    fun `권한 창을 선택 없이 닫은 경우는 Blocked가 아니라 Denied`() {
        // 요청은 했지만 rationale=true를 본 적이 없다 → 다시 요청할 수 있어야 한다
        assertEquals(
            CameraPermissionState.Denied,
            resolveCameraState(granted = false, requested = true, rationale = false, rationaleSeen = false),
        )
    }

    // ---------- resolveSetupStep ----------

    private fun step(
        entry: SetupEntry = SetupEntry.First,
        a11y: Boolean = true,
        camera: CameraPermissionState = CameraPermissionState.Granted,
        notificationRuntime: Boolean = true,
        notificationsEnabled: Boolean = false,
        notificationHandled: Boolean = false,
    ) = resolveSetupStep(entry, a11y, camera, notificationRuntime, notificationsEnabled, notificationHandled)

    @Test
    fun `접근성이 꺼져 있으면 항상 고지 단계부터`() {
        assertEquals(SetupStep.Disclose, step(a11y = false, camera = CameraPermissionState.NotAsked))
        assertEquals(SetupStep.Disclose, step(entry = SetupEntry.Home, a11y = false))
    }

    @Test
    fun `접근성 다음은 카메라 단계`() {
        assertEquals(SetupStep.Camera, step(camera = CameraPermissionState.Denied))
        assertEquals(SetupStep.Camera, step(camera = CameraPermissionState.Blocked))
    }

    @Test
    fun `Android 13 이상에서 알림이 꺼져 있고 처리 전이면 알림 단계`() {
        assertEquals(SetupStep.Notification, step())
    }

    @Test
    fun `알림을 건너뛰었거나 요청했으면 첫 실행은 완료 단계`() {
        assertEquals(SetupStep.Done, step(notificationHandled = true))
    }

    @Test
    fun `Android 12 이하는 알림 단계 없이 완료`() {
        assertEquals(SetupStep.Done, step(notificationRuntime = false))
    }

    @Test
    fun `홈 진입에서 할 일이 없으면 Finished`() {
        assertEquals(
            SetupStep.Finished,
            step(entry = SetupEntry.Home, notificationsEnabled = true),
        )
    }

    // ---------- wizardSteps ----------

    @Test
    fun `단계 표시는 Android 13 이상 3단계, 그 미만 2단계`() {
        assertEquals(3, wizardSteps(notificationRuntime = true).size)
        assertEquals(
            listOf(SetupStep.Disclose, SetupStep.Camera),
            wizardSteps(notificationRuntime = false),
        )
    }
}
