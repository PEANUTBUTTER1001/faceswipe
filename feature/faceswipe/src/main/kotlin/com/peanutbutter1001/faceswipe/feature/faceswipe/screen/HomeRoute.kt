package com.peanutbutter1001.faceswipe.feature.faceswipe.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peanutbutter1001.faceswipe.feature.faceswipe.R
import com.peanutbutter1001.faceswipe.feature.faceswipe.setup.appDetailsIntent
import com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel.HomeMessage
import com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel.HomeUiState
import com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

/**
 * Route 레이어: ViewModel 구독 + Side Effect 처리.
 *
 * SKILLS.md 4.3절: Route(ViewModel 구독) / Screen(Stateless 렌더링) 분리.
 * 권한 요청은 설정 마법사가 담당하므로 홈은 진입 시 권한을 요청하지 않는다.
 *
 * @param onOpenSetup 마법사 열기. focusNotification=true면 알림 단계를 바로 보여준다.
 */
@Composable
fun HomeRoute(
    onSettingsClick: () -> Unit,
    onHowToClick: () -> Unit,
    onOpenSetup: (focusNotification: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val versionName = remember(context) { context.appVersionName() }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    // Android 12 이하 타일 수동 추가 안내 대화상자
    var showTileHelp by rememberSaveable { mutableStateOf(false) }
    if (showTileHelp) TileHelpDialog(onDismiss = { showTileHelp = false })

    // ON_RESUME마다 권한 재확인 (시스템 설정에서 복귀 시 상태 재조회)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 일회성 스낵바 (메시지를 먼저 소비해도 표시가 취소되지 않도록 별도 scope)
    val state = uiState as? HomeUiState.Success
    val messageText = state?.message?.let { stringResource(it.textRes()) }
    LaunchedEffect(state?.message) {
        if (messageText != null) {
            viewModel.onMessageShown()
            snackbarScope.launch { snackbarHostState.showSnackbar(messageText) }
        }
    }

    HomeScreen(
        uiState = uiState,
        versionName = versionName,
        snackbarHostState = snackbarHostState,
        callbacks = HomeCallbacks(
            onServiceToggle = viewModel::setServiceEnabled,
            // 화면이 바로 바뀌므로 홈 스낵바 대신 마법사의 단계 안내로 이유를 전달
            onFixPermissions = { onOpenSetup(false) },
            onAccessibilityClick = {
                // 켜져 있으면 끄기·관리를 위해 시스템 설정, 꺼져 있으면 고지 화면부터
                if (state?.hasAccessibilityPermission == true) {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } else {
                    onOpenSetup(false)
                }
            },
            onCameraClick = {
                if (state?.hasCameraPermission == true) context.startActivity(context.appDetailsIntent())
                else onOpenSetup(false)
            },
            onNotificationClick = {
                // 이미 요청했는데 꺼져 있으면 시스템이 다시 묻지 않을 수 있어 앱 알림 설정으로 직접 이동
                if (state?.hasNotificationPermission == true || state?.notificationRequested == true) {
                    context.startActivity(context.appNotificationSettingsIntent())
                } else {
                    onOpenSetup(true)
                }
            },
            onGestureSettingsClick = onSettingsClick,
            onOverlayToggle = viewModel::setGestureOverlayEnabled,
            onHowToClick = onHowToClick,
            onTileClick = {
                when {
                    // 추가돼 있으면 열 수 있는 공개 API가 없으므로 위치만 안내
                    state?.tileAdded == true -> viewModel.showMessage(HomeMessage.TileAlreadyAdded)
                    TileRequester.isSupported -> TileRequester.request(context) { result ->
                        viewModel.onTileAddResult(
                            added = result == TileAddResult.Added,
                            alreadyAdded = result == TileAddResult.AlreadyAdded,
                        )
                    }
                    else -> showTileHelp = true
                }
            },
            onOpenFullPolicy = if (privacyUrl.isBlank()) null else {
                { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))) }
            },
        ),
        modifier = modifier,
    )
}

private fun HomeMessage.textRes(): Int = when (this) {
    HomeMessage.ServiceStarted -> R.string.service_started
    HomeMessage.ServiceStopped -> R.string.service_stopped
    HomeMessage.TileAdded -> R.string.tile_added_message
    HomeMessage.TileAlreadyAdded -> R.string.tile_already_message
}

/** 앱 알림 설정 화면 (API 26+) */
private fun Context.appNotificationSettingsIntent(): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

/** 설치된 앱의 versionName. 조회 실패 시 빈 문자열. */
private fun Context.appVersionName(): String = runCatching {
    packageManager.getPackageInfo(packageName, 0).versionName
}.getOrNull().orEmpty()
