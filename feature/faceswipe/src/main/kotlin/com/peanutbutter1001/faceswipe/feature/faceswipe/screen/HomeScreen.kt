package com.peanutbutter1001.faceswipe.feature.faceswipe.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peanutbutter1001.faceswipe.feature.faceswipe.R
import com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel.HomeUiState

/**
 * Stateless HomeScreen.
 *
 * UiState에 따라 Loading / Success / Error 분기 렌더링.
 * Side Effect 없음 — 이벤트 콜백만 받는다.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is HomeUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is HomeUiState.Success -> {
            HomeContent(
                state = uiState,
                onStartService = onStartService,
                onStopService = onStopService,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onSettingsClick = onSettingsClick,
                modifier = modifier,
            )
        }

        is HomeUiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    // 제스처 설정 화면 진입 버튼
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = stringResource(R.string.app_title),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.app_subtitle),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            PermissionStatusCard(
                title = stringResource(R.string.camera_permission_title),
                subtitle = if (state.hasCameraPermission) {
                    stringResource(R.string.permission_granted)
                } else {
                    stringResource(R.string.camera_permission_desc)
                },
                isGranted = state.hasCameraPermission
            )

            PermissionStatusCard(
                title = stringResource(R.string.accessibility_service_title),
                subtitle = if (state.hasAccessibilityPermission) {
                    stringResource(R.string.permission_enabled)
                } else {
                    stringResource(R.string.accessibility_permission_desc)
                },
                isGranted = state.hasAccessibilityPermission,
                actionLabel = if (!state.hasAccessibilityPermission) {
                    stringResource(R.string.open_settings)
                } else null,
                onAction = onOpenAccessibilitySettings
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.isServiceRunning) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = if (state.isTargetAppActive)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.isTargetAppActive)
                            stringResource(R.string.target_app_detected)
                        else
                            stringResource(R.string.target_app_waiting),
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val canStart = state.hasCameraPermission && state.hasAccessibilityPermission

            Button(
                onClick = {
                    if (state.isServiceRunning) onStopService()
                    else onStartService()
                },
                enabled = canStart || state.isServiceRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isServiceRunning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (state.isServiceRunning) {
                        stringResource(R.string.stop_service)
                    } else {
                        stringResource(R.string.start_service)
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!canStart && !state.isServiceRunning) {
                Text(
                    text = stringResource(R.string.permissions_required),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}
