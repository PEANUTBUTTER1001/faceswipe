package com.peanutbutter1001.faceswipe.feature.faceswipe.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsDimens
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsInfoRow
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsNavigationRow
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsScreenTitle
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsSectionHeader
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsStatusRow
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsSwitchRow
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeTheme
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeThemeTokens
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import com.peanutbutter1001.faceswipe.feature.faceswipe.R
import com.peanutbutter1001.faceswipe.feature.faceswipe.info.InfoBottomSheet
import com.peanutbutter1001.faceswipe.feature.faceswipe.info.InfoSheet
import com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel.HomeUiState

/** 홈 화면 이벤트 묶음 */
data class HomeCallbacks(
    val onServiceToggle: (Boolean) -> Unit = {},
    /** 권한이 부족해 스위치가 비활성일 때 탭 → 마법사의 부족한 첫 단계 */
    val onFixPermissions: () -> Unit = {},
    val onAccessibilityClick: () -> Unit = {},
    val onCameraClick: () -> Unit = {},
    val onNotificationClick: () -> Unit = {},
    val onGestureSettingsClick: () -> Unit = {},
    val onOverlayToggle: (Boolean) -> Unit = {},
    val onHowToClick: () -> Unit = {},
    val onTileClick: () -> Unit = {},
    /** 처리방침 전문 열기. URL이 없으면 null (명세 Q1) */
    val onOpenFullPolicy: (() -> Unit)? = null,
)

/**
 * Stateless HomeScreen.
 *
 * android-settings.md 기준 단일 열 목록:
 *   제목·개인정보 한 줄 → [실행] → [권한] 접근성·카메라·알림 → [제스처] → [도움말] → [정보]
 * 하나의 배경 위에 소제목과 여백으로만 그룹을 구분한다(카드/구분선 없음).
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    versionName: String,
    callbacks: HomeCallbacks,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (uiState) {
            is HomeUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is HomeUiState.Success -> {
                HomeContent(
                    state = uiState,
                    versionName = versionName,
                    callbacks = callbacks,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = SettingsDimens.ScreenInset),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    versionName: String,
    callbacks: HomeCallbacks,
    modifier: Modifier = Modifier,
) {
    val successColor = FaceswipeThemeTokens.statusColors.success
    val errorColor = MaterialTheme.colorScheme.error
    // 정보 시트 표시 여부는 순수 UI 상태 → 화면 내부 보관 (회전 시 유지)
    var openSheet by rememberSaveable { mutableStateOf<InfoSheet?>(null) }

    // 설정 항목 수가 고정적이므로 LazyColumn 대신 스크롤 가능한 Column 사용 (android-settings.md 5절)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsScreenTitle(title = stringResource(R.string.app_title))
        PrivacyLine()

        // -- 실행 --
        SettingsSectionHeader(title = stringResource(R.string.section_run))
        SettingsSwitchRow(
            title = stringResource(R.string.service_switch_title),
            summary = stringResource(serviceSummaryRes(state)),
            checked = state.isServiceRunning,
            // 실행 중에는 권한이 빠져도 끌 수 있어야 하므로 isServiceRunning도 활성 조건에 포함
            enabled = state.canStartService || state.isServiceRunning,
            onCheckedChange = callbacks.onServiceToggle,
            onDisabledClick = callbacks.onFixPermissions,
        )

        // -- 권한: 꺼진 항목은 마법사 해당 단계로, 켜진 항목은 시스템 설정으로 --
        SettingsSectionHeader(title = stringResource(R.string.section_permissions))
        SettingsStatusRow(
            title = stringResource(R.string.accessibility_service_title),
            summary = stringResource(
                if (state.hasAccessibilityPermission) R.string.accessibility_on_desc else R.string.accessibility_off_desc
            ),
            statusText = stringResource(if (state.hasAccessibilityPermission) R.string.permission_on else R.string.permission_off),
            statusColor = if (state.hasAccessibilityPermission) successColor else errorColor,
            onClick = callbacks.onAccessibilityClick,
        )
        SettingsStatusRow(
            title = stringResource(R.string.camera_permission_title),
            summary = stringResource(if (state.hasCameraPermission) R.string.camera_on_desc else R.string.camera_off_desc),
            statusText = stringResource(if (state.hasCameraPermission) R.string.permission_granted else R.string.permission_denied),
            statusColor = if (state.hasCameraPermission) successColor else errorColor,
            onClick = callbacks.onCameraClick,
        )
        SettingsStatusRow(
            title = stringResource(R.string.notification_permission_title),
            summary = stringResource(
                if (state.hasNotificationPermission) R.string.notification_on_desc else R.string.notification_off_desc
            ),
            statusText = stringResource(
                if (state.hasNotificationPermission) R.string.permission_granted else R.string.permission_off
            ),
            statusColor = if (state.hasNotificationPermission) successColor else errorColor,
            onClick = callbacks.onNotificationClick,
        )

        // -- 제스처 --
        SettingsSectionHeader(title = stringResource(R.string.section_gestures))
        SettingsNavigationRow(
            title = stringResource(R.string.gesture_settings_entry),
            summary = gestureSummary(state.mappedGestureCount),
            onClick = callbacks.onGestureSettingsClick,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.overlay_switch_title),
            summary = stringResource(R.string.overlay_switch_desc),
            checked = state.showGestureOverlay,
            onCheckedChange = callbacks.onOverlayToggle,
        )

        // -- 도움말 --
        SettingsSectionHeader(title = stringResource(R.string.section_help))
        SettingsNavigationRow(
            title = stringResource(R.string.howto_entry),
            summary = stringResource(R.string.howto_entry_desc),
            onClick = callbacks.onHowToClick,
        )
        SettingsStatusRow(
            title = stringResource(R.string.tile_row_title),
            summary = stringResource(if (state.tileAdded) R.string.tile_row_added_desc else R.string.tile_row_missing_desc),
            statusText = stringResource(if (state.tileAdded) R.string.tile_row_added else R.string.tile_row_add),
            statusColor = if (state.tileAdded) successColor else MaterialTheme.colorScheme.primary,
            onClick = callbacks.onTileClick,
        )

        // -- 정보 --
        SettingsSectionHeader(title = stringResource(R.string.section_info))
        SettingsNavigationRow(
            title = stringResource(R.string.privacy_policy),
            onClick = { openSheet = InfoSheet.Privacy },
        )
        SettingsNavigationRow(
            title = stringResource(R.string.open_source_licenses),
            onClick = { openSheet = InfoSheet.Licenses },
        )
        SettingsInfoRow(
            title = stringResource(R.string.app_version),
            value = versionName,
        )

        // 마지막 행이 내비게이션 바에 붙지 않도록 하단 여백 (안전 영역은 Scaffold innerPadding이 처리)
        Spacer(modifier = Modifier.height(24.dp))
    }

    openSheet?.let { sheet ->
        InfoBottomSheet(
            sheet = sheet,
            onDismiss = { openSheet = null },
            onOpenFullPolicy = callbacks.onOpenFullPolicy,
        )
    }
}

/** 제목 아래 개인정보 한 줄 요약 */
@Composable
private fun PrivacyLine() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsDimens.ScreenInset)
            .padding(top = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = FaceswipeThemeTokens.statusColors.success,
            modifier = Modifier.padding(top = 2.dp).size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.privacy_line),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 서비스 스위치 보조 설명: 사용 불가 / 꺼짐 / 실행 중(감지·대기)을 문구로 구별 */
private fun serviceSummaryRes(state: HomeUiState.Success): Int = when {
    // 카메라 사용 불가는 다른 상태보다 우선 (인식이 실제로 멈춘 상태)
    state.isServiceRunning && state.cameraUnavailable -> R.string.service_status_camera_unavailable
    state.isServiceRunning && state.isTargetAppActive -> R.string.service_status_active
    state.isServiceRunning -> R.string.service_status_waiting
    !state.canStartService -> R.string.service_status_unavailable
    else -> R.string.service_status_off
}

/** "유튜브 3개 · 밀리의 서재 0개" 형태의 요약 문자열 */
@Composable
private fun gestureSummary(mappedCount: Map<TargetApp, Int>): String {
    val separator = stringResource(R.string.gesture_summary_separator)
    return TargetApp.entries
        .map { app ->
            stringResource(
                R.string.gesture_summary_item,
                stringResource(app.displayNameRes()),
                mappedCount[app] ?: 0,
            )
        }
        .joinToString(separator)
}

// ============================================================
// Preview
// ============================================================

private val previewReadyState = HomeUiState.Success(
    isServiceRunning = false,
    isTargetAppActive = false,
    hasCameraPermission = true,
    hasAccessibilityPermission = true,
    hasNotificationPermission = true,
    mappedGestureCount = mapOf(TargetApp.YOUTUBE to 3, TargetApp.MILLIE to 2),
)

@Composable
private fun HomeScreenPreviewContent(state: HomeUiState, dark: Boolean = false) {
    FaceswipeTheme(darkTheme = dark, dynamicColor = false) {
        HomeScreen(uiState = state, versionName = "1.0", callbacks = HomeCallbacks())
    }
}

@Preview(showBackground = true, widthDp = 380, name = "Loading")
@Composable
private fun HomeScreenLoadingPreview() = HomeScreenPreviewContent(HomeUiState.Loading)

@Preview(showBackground = true, widthDp = 380, heightDp = 1100, name = "Ready")
@Composable
private fun HomeScreenReadyPreview() = HomeScreenPreviewContent(previewReadyState)

@Preview(showBackground = true, widthDp = 380, heightDp = 1100, name = "Running (dark)")
@Composable
private fun HomeScreenRunningPreview() = HomeScreenPreviewContent(
    previewReadyState.copy(isServiceRunning = true, isTargetAppActive = true, tileAdded = true),
    dark = true,
)

@Preview(showBackground = true, widthDp = 380, heightDp = 1100, name = "Permission missing")
@Composable
private fun HomeScreenPermissionMissingPreview() = HomeScreenPreviewContent(
    previewReadyState.copy(
        hasCameraPermission = false,
        hasAccessibilityPermission = false,
        hasNotificationPermission = false,
    )
)

@Preview(showBackground = true, widthDp = 380, name = "Error")
@Composable
private fun HomeScreenErrorPreview() = HomeScreenPreviewContent(HomeUiState.Error("Unknown error"))
