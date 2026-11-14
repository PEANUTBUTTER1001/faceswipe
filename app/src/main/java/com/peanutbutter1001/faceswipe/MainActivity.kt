package com.peanutbutter1001.faceswipe

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeTheme
import com.peanutbutter1001.faceswipe.data.faceswipe.service.FaceSwipeIntents
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.AppPreferences
import com.peanutbutter1001.faceswipe.domain.faceswipe.usecase.ObserveAppPreferencesUseCase
import com.peanutbutter1001.faceswipe.navigation.FaceSwipeNavHost
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 앱 시작 상태: 환경설정을 읽기 전(null)에는 시작 화면을 정할 수 없다.
 * 외부(빠른 설정 타일)에서 설정 마법사를 열어 달라는 요청도 보관한다.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeAppPreferences: ObserveAppPreferencesUseCase,
) : ViewModel() {
    val preferences: StateFlow<AppPreferences?> =
        observeAppPreferences().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _openSetupRequest = MutableStateFlow(false)
    val openSetupRequest: StateFlow<Boolean> = _openSetupRequest.asStateFlow()

    fun handleIntent(intent: Intent?) {
        if (intent?.action == FaceSwipeIntents.ACTION_OPEN_SETUP) _openSetupRequest.value = true
    }

    fun consumeOpenSetupRequest() {
        _openSetupRequest.value = false
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) viewModel.handleIntent(intent)
        setContent {
            FaceswipeTheme {
                val prefs by viewModel.preferences.collectAsStateWithLifecycle()
                val openSetup by viewModel.openSetupRequest.collectAsStateWithLifecycle()
                val loaded = prefs
                if (loaded == null) {
                    // 환경설정 로딩 중: 시작 화면이 바뀌어 깜빡이지 않도록 빈 배경만 표시
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                } else {
                    // 시작 화면은 처음 읽은 값으로 한 번만 결정 (이후 onboardingDone 변경으로 그래프를 다시 만들지 않음)
                    val startWithWelcome = remember { !loaded.onboardingDone }
                    FaceSwipeNavHost(
                        startWithWelcome = startWithWelcome,
                        openSetupRequest = openSetup,
                        onOpenSetupConsumed = viewModel::consumeOpenSetupRequest,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.handleIntent(intent)
    }
}
