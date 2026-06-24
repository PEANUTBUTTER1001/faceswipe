package com.peanutbutter1001.faceswipe.feature.faceswipe.screen

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel.HomeViewModel

/**
 * Route 레이어: ViewModel 구독 + Side Effect 처리.
 *
 * SKILLS.md 4.3절: Route(ViewModel 구독) / Screen(Stateless 렌더링) 분리.
 * hiltViewModel(), collectAsState(), 권한 런처 등 Side Effect는 여기서 처리.
 */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 카메라 권한 런처
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }

    // 첫 진입 시 카메라 권한 요청
    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ON_RESUME마다 권한 재확인
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HomeScreen(
        uiState = uiState,
        onStartService = viewModel::startService,
        onStopService = viewModel::stopService,
        onOpenAccessibilitySettings = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        modifier = modifier
    )
}
