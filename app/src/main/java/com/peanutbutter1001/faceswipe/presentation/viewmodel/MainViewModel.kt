package com.peanutbutter1001.faceswipe.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peanutbutter1001.faceswipe.service.AppStateManager
import com.peanutbutter1001.faceswipe.service.faceswipeForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val isServiceRunning: Boolean = false,
    val isYouTubeActive: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appStateManager: AppStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        appStateManager.isYouTubeActive
            .onEach { active -> _uiState.update { it.copy(isYouTubeActive = active) } }
            .launchIn(viewModelScope)
    }

    /** 카메라 권한 결과를 외부(런처 콜백)에서 받아 갱신 */
    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                hasCameraPermission = granted,
                hasAccessibilityPermission = isAccessibilityServiceEnabled()
            )
        }
    }

    /**
     * ON_RESUME마다 호출 — 카메라 권한을 직접 조회하고 접근성 서비스도 재확인.
     * 접근성 설정에서 돌아올 때 버튼 활성화를 위해 반드시 필요.
     */
    fun refreshPermissions() {
        val hasCam = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        _uiState.update {
            it.copy(
                hasCameraPermission = hasCam,
                hasAccessibilityPermission = isAccessibilityServiceEnabled()
            )
        }
    }

    fun startService() {
        Intent(context, faceswipeForegroundService::class.java).also { intent ->
            intent.action = faceswipeForegroundService.ACTION_START
            context.startForegroundService(intent)
        }
        _uiState.update { it.copy(isServiceRunning = true) }
    }

    fun stopService() {
        Intent(context, faceswipeForegroundService::class.java).also { intent ->
            intent.action = faceswipeForegroundService.ACTION_STOP
            context.startService(intent)
        }
        _uiState.update { it.copy(isServiceRunning = false) }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        // Android는 "packageName/fully.qualified.ClassName" 형태로 저장.
        // 상대경로(.service.XXX)가 아닌 전체 클래스명으로 비교해야 함.
        val expected = "${context.packageName}/${context.packageName}.service.GestureAccessibilityService"

        return enabledServices
            .split(":")
            .any { it.trim().equals(expected, ignoreCase = true) }
    }
}
