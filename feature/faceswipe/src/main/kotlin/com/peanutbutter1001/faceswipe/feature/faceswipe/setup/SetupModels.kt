package com.peanutbutter1001.faceswipe.feature.faceswipe.setup

/**
 * 설정 마법사 진입 경로.
 * - First: 첫 실행 소개 화면에서 시작. 단계 표시("n / 전체")와 완료 화면을 보여준다.
 * - Home: 홈의 권한 행·비활성 스위치·타일에서 진입. 부족한 단계만 거치고 홈으로 돌아간다.
 */
enum class SetupEntry { First, Home }

/** 마법사 단계. Finished는 Home 진입에서 더 할 일이 없음을 뜻한다(화면 없음, 즉시 복귀). */
enum class SetupStep { Disclose, Camera, Notification, Done, Finished }

/** 카메라 권한의 화면 표시용 상태 */
enum class CameraPermissionState { NotAsked, Denied, Blocked, Granted }

/**
 * 카메라 권한 상태 판정 (순수 함수, 단위 테스트 대상).
 *
 * Android는 "다시 묻지 않음"과 "한 번도 묻지 않음"을 모두 rationale=false로 돌려주므로
 * 저장된 두 플래그로 구분한다.
 *  - requested=false → NotAsked
 *  - rationale=true → Denied (한 번 거부, 시스템 창을 다시 띄울 수 있음)
 *  - rationale=false이지만 예전에 rationale=true를 본 적 있음 → Blocked (다시 묻지 않음)
 *  - requested=true, rationale를 본 적 없음 → Denied (창을 선택 없이 닫은 경우. 다시 요청 허용)
 */
fun resolveCameraState(
    granted: Boolean,
    requested: Boolean,
    rationale: Boolean,
    rationaleSeen: Boolean,
): CameraPermissionState = when {
    granted -> CameraPermissionState.Granted
    !requested -> CameraPermissionState.NotAsked
    rationale -> CameraPermissionState.Denied
    rationaleSeen -> CameraPermissionState.Blocked
    else -> CameraPermissionState.Denied
}

/**
 * 현재 권한 상태로 보여줄 단계를 결정한다 (순수 함수, 단위 테스트 대상).
 * 단계는 저장하지 않고 매번 계산하므로 회전·프로세스 종료 후에도 올바른 단계로 복원된다.
 *
 * @param notificationRuntime Android 13+ (알림 권한을 런타임에 요청하는 버전)
 * @param notificationHandled 알림 단계를 이미 요청했거나 건너뛰었는지
 */
fun resolveSetupStep(
    entry: SetupEntry,
    accessibilityEnabled: Boolean,
    camera: CameraPermissionState,
    notificationRuntime: Boolean,
    notificationsEnabled: Boolean,
    notificationHandled: Boolean,
): SetupStep = when {
    !accessibilityEnabled -> SetupStep.Disclose
    camera != CameraPermissionState.Granted -> SetupStep.Camera
    notificationRuntime && !notificationsEnabled && !notificationHandled -> SetupStep.Notification
    entry == SetupEntry.First -> SetupStep.Done
    else -> SetupStep.Finished
}

/** 첫 실행 단계 표시("n / 전체")에 쓰는 단계 목록. Android 12 이하는 알림 단계가 없다. */
fun wizardSteps(notificationRuntime: Boolean): List<SetupStep> = buildList {
    add(SetupStep.Disclose)
    add(SetupStep.Camera)
    if (notificationRuntime) add(SetupStep.Notification)
}
