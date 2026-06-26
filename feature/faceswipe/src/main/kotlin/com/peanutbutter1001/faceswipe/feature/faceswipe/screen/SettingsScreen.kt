package com.peanutbutter1001.faceswipe.feature.faceswipe.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeTheme
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel.SettingsUiState
import com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel.SettingsViewModel
import com.peanutbutter1001.faceswipe.feature.faceswipe.R
import kotlinx.coroutines.launch

// ============================================================
// Route (상태 수집 + ViewModel 연결)
// ============================================================

@Composable
fun SettingsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onAppSelected = viewModel::selectApp,
        onActionSelected = viewModel::updateAction,
        onResetClick = viewModel::resetCurrentApp,
        modifier = modifier,
    )
}

// ============================================================
// Screen (Stateless Composable)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBackClick: () -> Unit,
    onAppSelected: (TargetApp) -> Unit,
    onActionSelected: (GestureTrigger, GestureAction?) -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
                actions = {
                    // 초기화 버튼
                    if (uiState is SettingsUiState.Success) {
                        IconButton(onClick = onResetClick) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.settings_reset),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is SettingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is SettingsUiState.Success -> {
                SettingsContent(
                    selectedApp = uiState.selectedApp,
                    mappings = uiState.mappings,
                    onAppSelected = onAppSelected,
                    onActionSelected = onActionSelected,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

// ============================================================
// 내부 Composable 구성 요소
// ============================================================

/**
 * 설정 화면 본문: 앱 탭 + 제스처 매핑 리스트.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    selectedApp: TargetApp,
    mappings: Map<GestureTrigger, GestureAction?>,
    onAppSelected: (TargetApp) -> Unit,
    onActionSelected: (GestureTrigger, GestureAction?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 바텀 시트에서 편집 중인 제스처
    var editingTrigger by remember { mutableStateOf<GestureTrigger?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        // -- 앱 선택 탭 --
        AppTabRow(
            selectedApp = selectedApp,
            onAppSelected = onAppSelected,
        )

        // -- 제스처 매핑 리스트 --
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = GestureTrigger.settingsEntries,
                key = { it::class.simpleName ?: "" },
            ) { trigger ->
                GestureCard(
                    trigger = trigger,
                    selectedAction = mappings[trigger],
                    onClick = { editingTrigger = trigger },
                )
            }
        }
    }

    // -- 액션 선택 바텀 시트 --
    editingTrigger?.let { trigger ->
        ModalBottomSheet(
            onDismissRequest = { editingTrigger = null },
            sheetState = sheetState,
        ) {
            ActionSelectionSheet(
                trigger = trigger,
                currentAction = mappings[trigger],
                onActionSelected = { action ->
                    onActionSelected(trigger, action)
                    scope.launch {
                        sheetState.hide()
                        editingTrigger = null
                    }
                },
            )
        }
    }
}

/**
 * 앱 선택 탭.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTabRow(
    selectedApp: TargetApp,
    onAppSelected: (TargetApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val apps = TargetApp.entries
    val selectedIndex = apps.indexOf(selectedApp)

    PrimaryTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        apps.forEach { app ->
            Tab(
                selected = app == selectedApp,
                onClick = { onAppSelected(app) },
                text = {
                    Text(
                        text = stringResource(app.displayNameRes()),
                        fontWeight = if (app == selectedApp) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                    )
                },
            )
        }
    }
}

/**
 * 개별 제스처 매핑 카드.
 * 터치하면 바텀 시트가 열린다.
 */
@Composable
private fun GestureCard(
    trigger: GestureTrigger,
    selectedAction: GestureAction?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 제스처 아이콘
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = trigger.iconText(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 제스처 이름 + 현재 매핑된 액션
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(trigger.displayNameRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedAction.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedAction == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 액션 선택 바텀 시트 내용.
 */
@Composable
private fun ActionSelectionSheet(
    trigger: GestureTrigger,
    currentAction: GestureAction?,
    onActionSelected: (GestureAction?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        // 시트 제목
        Text(
            text = stringResource(
                R.string.settings_sheet_title,
                stringResource(trigger.displayNameRes()),
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(8.dp))

        // 액션 옵션 리스트
        availableActions.forEach { action ->
            val isSelected = action == currentAction
            ActionOptionItem(
                action = action,
                isSelected = isSelected,
                onClick = { onActionSelected(action) },
            )
        }
    }
}

/**
 * 바텀 시트 내 개별 액션 옵션.
 */
@Composable
private fun ActionOptionItem(
    action: GestureAction?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 액션 아이콘
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = action.iconText(),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = action.displayName(),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

// ============================================================
// 헬퍼 함수 (표시 이름, 아이콘 매핑)
// ============================================================

/**
 * 설정 UI에서 선택 가능한 액션 목록.
 * null은 "(선택 안 함)"을 의미한다.
 */
private val availableActions: List<GestureAction?> = listOf(
    GestureAction.SwipeVertical(directionUp = true),
    GestureAction.SwipeVertical(directionUp = false),
    GestureAction.Tap,
    null,
)

/**
 * GestureAction의 표시 이름 반환.
 * strings.xml에서 가져온다.
 */
@Composable
private fun GestureAction?.displayName(): String = when (this) {
    is GestureAction.SwipeVertical -> if (directionUp) {
        stringResource(R.string.action_swipe_up)
    } else {
        stringResource(R.string.action_swipe_down)
    }
    is GestureAction.Tap -> stringResource(R.string.action_tap_bottom_quarter)
    else -> stringResource(R.string.action_none)
}

/**
 * GestureAction의 아이콘 텍스트 반환.
 */
private fun GestureAction?.iconText(): String = when (this) {
    is GestureAction.SwipeVertical -> if (directionUp) "⬆" else "⬇"
    is GestureAction.Tap -> "👇"
    else -> "✕"
}

/**
 * GestureTrigger의 표시 이름에 대응하는 문자열 리소스 ID.
 */
private fun GestureTrigger.displayNameRes(): Int = when (this) {
    GestureTrigger.HeadLeft -> R.string.trigger_head_left
    GestureTrigger.HeadRight -> R.string.trigger_head_right
    GestureTrigger.WinkLeft -> R.string.trigger_wink_left
    GestureTrigger.WinkRight -> R.string.trigger_wink_right
    GestureTrigger.MouthOpen -> R.string.trigger_mouth_open
}

/**
 * GestureTrigger의 아이콘 텍스트.
 */
private fun GestureTrigger.iconText(): String = when (this) {
    GestureTrigger.HeadLeft -> "👈"
    GestureTrigger.HeadRight -> "👉"
    GestureTrigger.WinkLeft -> "😉"
    GestureTrigger.WinkRight -> "😜"
    GestureTrigger.MouthOpen -> "😮"
}

/**
 * TargetApp의 표시 이름에 대응하는 문자열 리소스 ID.
 */
private fun TargetApp.displayNameRes(): Int = when (this) {
    TargetApp.YOUTUBE -> R.string.app_youtube
    TargetApp.MILLIE -> R.string.app_millie
}

// ============================================================
// Preview
// ============================================================

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun SettingsScreenLoadingPreview() {
    FaceswipeTheme {
        SettingsScreen(
            uiState = SettingsUiState.Loading,
            onBackClick = {},
            onAppSelected = {},
            onActionSelected = { _, _ -> },
            onResetClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun SettingsScreenSuccessPreview() {
    FaceswipeTheme {
        SettingsScreen(
            uiState = SettingsUiState.Success(
                selectedApp = TargetApp.YOUTUBE,
                mappings = mapOf(
                    GestureTrigger.HeadLeft to GestureAction.SwipeVertical(directionUp = true),
                    GestureTrigger.HeadRight to GestureAction.SwipeVertical(directionUp = false),
                    GestureTrigger.WinkLeft to GestureAction.Tap,
                    GestureTrigger.WinkRight to null,
                    GestureTrigger.MouthOpen to null,
                ),
            ),
            onBackClick = {},
            onAppSelected = {},
            onActionSelected = { _, _ -> },
            onResetClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun SettingsScreenAllMappedPreview() {
    FaceswipeTheme {
        SettingsScreen(
            uiState = SettingsUiState.Success(
                selectedApp = TargetApp.MILLIE,
                mappings = mapOf(
                    GestureTrigger.HeadLeft to GestureAction.SwipeVertical(directionUp = false),
                    GestureTrigger.HeadRight to GestureAction.SwipeVertical(directionUp = true),
                    GestureTrigger.WinkLeft to GestureAction.Tap,
                    GestureTrigger.WinkRight to GestureAction.SwipeVertical(directionUp = true),
                    GestureTrigger.MouthOpen to GestureAction.Tap,
                ),
            ),
            onBackClick = {},
            onAppSelected = {},
            onActionSelected = { _, _ -> },
            onResetClick = {},
        )
    }
}
