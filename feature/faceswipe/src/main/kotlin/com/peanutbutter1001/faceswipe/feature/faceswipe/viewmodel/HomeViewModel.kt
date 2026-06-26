package com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peanutbutter1001.faceswipe.data.faceswipe.service.AppStateManager
import com.peanutbutter1001.faceswipe.data.faceswipe.service.PermissionChecker
import com.peanutbutter1001.faceswipe.data.faceswipe.service.ServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * HomeScreen을 위한 ViewModel.
 *
 * 서비스 실행 상태는 AppStateManager.isServiceRunning(StateFlow)을 직접 관찰하여
 * 실제 서비스 상태와 UI 버튼 상태가 항상 일치하도록 한다.
 * -> 모든 앱 종료 후 재실행 시에도 실제로 멈춰 있으면 시작 버튼이 초기 상태로 표시됨.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appStateManager: AppStateManager,
    private val permissionChecker: PermissionChecker,
    private val serviceController: ServiceController
) : ViewModel() {

    /** 권한 상태 수동 갱신 트리거 (ON_RESUME 등) */
    private val permissionRefreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<HomeUiState> = combine(
        appStateManager.isTargetAppActive,
        appStateManager.isServiceRunning,
        permissionRefreshTrigger
    ) { isTargetAppActive, isServiceRunning, _ ->
        HomeUiState.Success(
            isServiceRunning = isServiceRunning,
            isTargetAppActive = isTargetAppActive,
            hasCameraPermission = permissionChecker.hasCameraPermission(),
            hasAccessibilityPermission = permissionChecker.hasAccessibilityPermission()
        ) as HomeUiState
    }
        .catch { emit(HomeUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading
        )

    fun onCameraPermissionResult(granted: Boolean) {
        permissionRefreshTrigger.value++
    }

    fun refreshPermissions() {
        permissionRefreshTrigger.value++
    }

    fun startService() {
        serviceController.start()
        permissionRefreshTrigger.value++
    }

    fun stopService() {
        serviceController.stop()
        permissionRefreshTrigger.value++
    }
}
