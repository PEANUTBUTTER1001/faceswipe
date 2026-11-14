package com.peanutbutter1001.faceswipe.feature.faceswipe.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.peanutbutter1001.faceswipe.data.faceswipe.service.TileAddResult
import com.peanutbutter1001.faceswipe.data.faceswipe.service.TileRequester
import com.peanutbutter1001.faceswipe.feature.faceswipe.tile.TileHelpDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import kotlinx.coroutines.launch

/**
 * 설정 마법사 Route: ViewModel 구독 + 권한 요청·시스템 설정 이동 등 Side Effect 처리.
 *
 * @param onExit 마법사를 마쳤거나 닫았을 때 (홈 진입이면 뒤로, 첫 실행이면 홈으로)
 */
@Composable
fun SetupRoute(
    onExit: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // 카메라 rationale은 Activity에서만 읽을 수 있어 Route에서 계산해 ViewModel에 전달
    val cameraRationale = {
        activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onCameraPermissionResult(granted, cameraRationale()) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.onNotificationPermissionResult(granted, cameraRationale()) }

    // 시스템 설정·권한 창에서 돌아올 때마다 OS 상태를 다시 읽는다.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh(cameraRationale())
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state = uiState as? SetupUiState.Success

    // 홈 진입에서 더 할 단계가 없으면 즉시 복귀
    LaunchedEffect(state?.step) {
        if (state?.step == SetupStep.Finished) onExit()
    }

    // 일회성 스낵바 메시지.
    // 메시지를 먼저 소비(null)하면 이 Effect가 취소되므로, 표시는 화면 범위 scope에서 실행한다.
    val snackbarScope = rememberCoroutineScope()
    val messageText = state?.message?.let { stringResource(it.textRes) }
    LaunchedEffect(state?.message) {
        if (messageText != null) {
            viewModel.onMessageShown()
            snackbarScope.launch { snackbarHostState.showSnackbar(messageText) }
        }
    }

    // 닫기·뒤로: 첫 실행이면 소개를 마친 것으로 기록하고 홈으로
    val close: () -> Unit = {
        if (state?.entry == SetupEntry.First) viewModel.completeFirstRun(startService = false)
        onExit()
    }
    BackHandler(onBack = close)

    // Android 12 이하 타일 수동 추가 안내
    var showTileHelp by rememberSaveable { mutableStateOf(false) }
    if (showTileHelp) TileHelpDialog(onDismiss = { showTileHelp = false })

    SetupScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        tileSupported = true,
        callbacks = SetupCallbacks(
            onClose = close,
            onAgreeDisclosure = {
                viewModel.onOpenAccessibilitySettings()
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            onRequestCamera = {
                viewModel.onCameraPermissionRequested()
                cameraLauncher.launch(Manifest.permission.CAMERA)
            },
            onOpenAppInfo = {
                viewModel.onOpenAppInfo()
                context.startActivity(context.appDetailsIntent())
            },
            onRequestNotification = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    viewModel.onNotificationPermissionRequested()
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.onSkipNotification()
                }
            },
            onSkipNotification = viewModel::onSkipNotification,
            onAddTile = {
                if (TileRequester.isSupported) {
                    TileRequester.request(context) { result ->
                        if (result != TileAddResult.NotAdded) {
                            viewModel.onTileAdded()
                            viewModel.showMessage(SetupMessage.TileAdded)
                        }
                    }
                } else {
                    showTileHelp = true
                }
            },
            onTryIt = {
                val launch = context.packageManager.getLaunchIntentForPackage(TargetApp.YOUTUBE.packageName)
                if (launch == null) {
                    viewModel.showMessage(SetupMessage.YoutubeMissing)
                } else {
                    viewModel.completeFirstRun(startService = true)
                    onExit()
                    context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            },
            onGoHome = {
                viewModel.completeFirstRun(startService = true)
                onExit()
            },
        ),
    )
}

/** 앱 정보(권한) 화면 인텐트 */
internal fun Context.appDetailsIntent(): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
