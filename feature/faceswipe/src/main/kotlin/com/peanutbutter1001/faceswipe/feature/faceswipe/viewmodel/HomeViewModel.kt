package com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peanutbutter1001.faceswipe.data.faceswipe.service.AppStateManager
import com.peanutbutter1001.faceswipe.data.faceswipe.service.PermissionChecker
import com.peanutbutter1001.faceswipe.data.faceswipe.service.ServiceController
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import com.peanutbutter1001.faceswipe.domain.faceswipe.usecase.GetGestureMappingUseCase
import com.peanutbutter1001.faceswipe.domain.faceswipe.usecase.ObserveAppPreferencesUseCase
import com.peanutbutter1001.faceswipe.domain.faceswipe.usecase.UpdateAppPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HomeScreen을 위한 ViewModel.
 *
 * 서비스 실행 상태는 AppStateManager.isServiceRunning(StateFlow)을 직접 관찰하여
 * 실제 서비스 상태와 UI 스위치 상태가 항상 일치하도록 한다.
 * 권한 상태는 OS가 원천이므로 ON_RESUME마다 [refreshPermissions]로 다시 읽는다.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appStateManager: AppStateManager,
    private val permissionChecker: PermissionChecker,
    private val serviceController: ServiceController,
    getGestureMapping: GetGestureMappingUseCase,
    observeAppPreferences: ObserveAppPreferencesUseCase,
    private val updateAppPreferences: UpdateAppPreferencesUseCase,
) : ViewModel() {

    /** 권한 상태 수동 갱신 트리거 (ON_RESUME 등) */
    private val permissionRefreshTrigger = MutableStateFlow(0)

    /** 일회성 스낵바 메시지 */
    private val message = MutableStateFlow<HomeMessage?>(null)

    /**
     * 대상 앱별 "동작이 연결된 제스처 수".
     * 설정 화면에서 매핑을 바꾸면 DataStore Flow가 다시 emit되어 홈 요약도 자동 갱신된다.
     */
    private val mappedGestureCount: Flow<Map<TargetApp, Int>> = combine(
        TargetApp.entries.map { app ->
            getGestureMapping(app).map { mappings ->
                app to mappings.values.count { it != null }
            }
        }
    ) { pairs -> pairs.toMap() }

    /** 권한 갱신 트리거와 메시지를 하나로 묶어 combine 인자 수(최대 5)를 맞춘다 */
    private val localState = combine(permissionRefreshTrigger, message) { _, msg -> msg }

    /** 서비스 상태 3종을 묶어 combine 인자 수(최대 5)를 맞춘다 */
    private val serviceState = combine(
        appStateManager.isTargetAppActive,
        appStateManager.isServiceRunning,
        appStateManager.cameraUnavailable,
    ) { active, running, unavailable -> Triple(active, running, unavailable) }

    val uiState: StateFlow<HomeUiState> = combine(
        serviceState,
        localState,
        mappedGestureCount,
        observeAppPreferences(),
    ) { (isTargetAppActive, isServiceRunning, cameraUnavailable), msg, mappedCount, prefs ->
        HomeUiState.Success(
            isServiceRunning = isServiceRunning,
            isTargetAppActive = isTargetAppActive,
            cameraUnavailable = cameraUnavailable,
            hasCameraPermission = permissionChecker.hasCameraPermission(),
            hasAccessibilityPermission = permissionChecker.hasAccessibilityPermission(),
            hasNotificationPermission = permissionChecker.areNotificationsEnabled(),
            notificationRequested = prefs.notificationPermissionRequested,
            showGestureOverlay = prefs.showGestureOverlay,
            tileAdded = prefs.tileAdded,
            mappedGestureCount = mappedCount,
            message = msg,
        ) as HomeUiState
    }
        .catch { emit(HomeUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    fun refreshPermissions() {
        permissionRefreshTrigger.value++
    }

    fun setServiceEnabled(enable: Boolean) {
        if (enable) serviceController.start() else serviceController.stop()
        message.value = if (enable) HomeMessage.ServiceStarted else HomeMessage.ServiceStopped
        permissionRefreshTrigger.value++
    }

    fun setGestureOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch { updateAppPreferences { it.copy(showGestureOverlay = enabled) } }
    }

    fun onTileAddResult(added: Boolean, alreadyAdded: Boolean) {
        if (added || alreadyAdded) {
            viewModelScope.launch { updateAppPreferences { it.copy(tileAdded = true) } }
        }
        message.value = when {
            alreadyAdded -> HomeMessage.TileAlreadyAdded
            added -> HomeMessage.TileAdded
            else -> null
        }
    }

    fun showMessage(value: HomeMessage) {
        message.value = value
    }

    fun onMessageShown() {
        message.value = null
    }
}
