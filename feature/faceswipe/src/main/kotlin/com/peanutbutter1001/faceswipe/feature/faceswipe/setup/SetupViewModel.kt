package com.peanutbutter1001.faceswipe.feature.faceswipe.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.peanutbutter1001.faceswipe.data.faceswipe.service.PermissionChecker
import com.peanutbutter1001.faceswipe.data.faceswipe.service.ServiceController
import com.peanutbutter1001.faceswipe.domain.faceswipe.usecase.ObserveAppPreferencesUseCase
import com.peanutbutter1001.faceswipe.domain.faceswipe.usecase.UpdateAppPreferencesUseCase
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.SetupDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 설정 마법사 ViewModel.
 *
 * 상태 원천:
 *  - 저장된 환경설정(권한 요청 이력, 타일 추가 여부)
 *  - OS 권한 스냅샷: Route가 ON_RESUME·권한 결과마다 [refresh]로 갱신
 *    (카메라 rationale은 Activity가 필요하므로 Route가 계산해 전달)
 *  - 세션 상태: 시스템 설정 복귀 대기 플래그, 알림 단계 처리 여부, 스낵바 메시지
 * 세 원천을 combine해 단일 StateFlow<SetupUiState>로 노출한다 (UDF).
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeAppPreferences: ObserveAppPreferencesUseCase,
    private val updateAppPreferences: UpdateAppPreferencesUseCase,
    private val permissionChecker: PermissionChecker,
    private val serviceController: ServiceController,
) : ViewModel() {

    private val destination = savedStateHandle.toRoute<SetupDestination>()
    private val entry = if (destination.fromHome) SetupEntry.Home else SetupEntry.First

    private val snapshot = MutableStateFlow(readSnapshot(cameraRationale = false))
    private val session = MutableStateFlow(SessionState())

    val uiState: StateFlow<SetupUiState> = combine(
        observeAppPreferences(),
        snapshot,
        session,
    ) { prefs, snap, s ->
        val camera = resolveCameraState(
            granted = snap.cameraGranted,
            requested = prefs.cameraPermissionRequested,
            rationale = snap.cameraRationale,
            rationaleSeen = prefs.cameraRationaleSeen,
        )
        // 알림 행에서 들어오면 이전 요청 이력과 무관하게 알림 단계를 보여준다.
        val notificationHandled = if (destination.focusNotification) {
            s.notificationHandled
        } else {
            prefs.notificationPermissionRequested || s.notificationHandled
        }
        val step = resolveSetupStep(
            entry = entry,
            accessibilityEnabled = snap.accessibilityEnabled,
            camera = camera,
            notificationRuntime = permissionChecker.isNotificationPermissionRuntime(),
            notificationsEnabled = snap.notificationsEnabled,
            notificationHandled = notificationHandled,
        )
        val steps = wizardSteps(permissionChecker.isNotificationPermissionRuntime())
        val index = steps.indexOf(step)
        SetupUiState.Success(
            entry = entry,
            step = step,
            // 첫 실행에서만 "n / 전체" 표시
            stepNumber = if (entry == SetupEntry.First && index >= 0) index + 1 else null,
            stepCount = steps.size,
            cameraState = camera,
            tileAdded = prefs.tileAdded,
            message = s.message,
        ) as SetupUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SetupUiState.Loading,
    )

    /**
     * OS 권한 상태를 다시 읽는다 (ON_RESUME, 권한 요청 결과).
     * 시스템 설정에서 돌아온 경우 결과 메시지를 남긴다.
     */
    fun refresh(cameraRationale: Boolean) {
        val snap = readSnapshot(cameraRationale)
        snapshot.value = snap
        if (cameraRationale) {
            // 한 번 거부된 상태를 기록해 두면, 이후 rationale=false를 "차단"으로 판정할 수 있다.
            viewModelScope.launch {
                updateAppPreferences { if (it.cameraRationaleSeen) it else it.copy(cameraRationaleSeen = true) }
            }
        }
        session.update { s ->
            var message = s.message
            if (s.awaitingAccessibility) {
                message = if (snap.accessibilityEnabled) SetupMessage.AccessibilityEnabled else SetupMessage.AccessibilityStillOff
            }
            if (s.awaitingAppInfo) {
                message = if (snap.cameraGranted) SetupMessage.CameraGranted else SetupMessage.CameraStillDenied
            }
            s.copy(awaitingAccessibility = false, awaitingAppInfo = false, message = message)
        }
    }

    /** 고지에 동의하고 시스템 접근성 설정으로 이동하기 직전 */
    fun onOpenAccessibilitySettings() {
        session.update { it.copy(awaitingAccessibility = true) }
    }

    /** 카메라 시스템 권한 창을 띄우기 직전 */
    fun onCameraPermissionRequested() {
        viewModelScope.launch { updateAppPreferences { it.copy(cameraPermissionRequested = true) } }
    }

    fun onCameraPermissionResult(granted: Boolean, cameraRationale: Boolean) {
        refresh(cameraRationale)
        if (granted) session.update { it.copy(message = SetupMessage.CameraGranted) }
    }

    /** 차단 상태에서 앱 정보 화면으로 이동하기 직전 */
    fun onOpenAppInfo() {
        session.update { it.copy(awaitingAppInfo = true) }
    }

    fun onNotificationPermissionRequested() {
        viewModelScope.launch { updateAppPreferences { it.copy(notificationPermissionRequested = true) } }
    }

    fun onNotificationPermissionResult(granted: Boolean, cameraRationale: Boolean) {
        refresh(cameraRationale)
        session.update {
            it.copy(
                notificationHandled = true,
                message = if (granted) SetupMessage.NotificationGranted else it.message,
            )
        }
    }

    fun onSkipNotification() {
        session.update { it.copy(notificationHandled = true) }
        viewModelScope.launch { updateAppPreferences { it.copy(notificationPermissionRequested = true) } }
    }

    /**
     * 첫 실행을 마치거나 건너뜀. 권한이 모두 있으면 서비스를 켠다.
     */
    fun completeFirstRun(startService: Boolean) {
        viewModelScope.launch { updateAppPreferences { it.copy(onboardingDone = true) } }
        if (startService && permissionChecker.hasAccessibilityPermission() && permissionChecker.hasCameraPermission()) {
            serviceController.start()
        }
    }

    fun onTileAdded() {
        viewModelScope.launch { updateAppPreferences { it.copy(tileAdded = true) } }
    }

    fun showMessage(message: SetupMessage) {
        session.update { it.copy(message = message) }
    }

    fun onMessageShown() {
        session.update { it.copy(message = null) }
    }

    private fun readSnapshot(cameraRationale: Boolean) = PermissionSnapshot(
        accessibilityEnabled = permissionChecker.hasAccessibilityPermission(),
        cameraGranted = permissionChecker.hasCameraPermission(),
        cameraRationale = cameraRationale,
        notificationsEnabled = permissionChecker.areNotificationsEnabled(),
    )

    private data class PermissionSnapshot(
        val accessibilityEnabled: Boolean,
        val cameraGranted: Boolean,
        val cameraRationale: Boolean,
        val notificationsEnabled: Boolean,
    )

    private data class SessionState(
        val awaitingAccessibility: Boolean = false,
        val awaitingAppInfo: Boolean = false,
        val notificationHandled: Boolean = false,
        val message: SetupMessage? = null,
    )
}
