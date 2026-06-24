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
 * Context 직접 사용을 제거하고, PermissionChecker / ServiceController를 통해
 * 간접 접근한다. 테스트 시 이들을 Mock으로 대체 가능.
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
        permissionRefreshTrigger
    ) { isTargetAppActive, _ ->
        HomeUiState.Success(
            isServiceRunning = serviceController.isRunning(),
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
        // 권한 결과를 받으면 전체 상태 갱신
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
