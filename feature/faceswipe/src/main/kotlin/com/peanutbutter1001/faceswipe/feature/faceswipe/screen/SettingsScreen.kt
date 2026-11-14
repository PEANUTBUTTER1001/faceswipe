package com.peanutbutter1001.faceswipe.feature.faceswipe.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsChoiceRow
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsDescription
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsDestructiveRow
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsDimens
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsSectionHeader
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeTheme
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import com.peanutbutter1001.faceswipe.feature.faceswipe.R
import com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel.SettingsUiState
import com.peanutbutter1001.faceswipe.feature.faceswipe.viewmodel.SettingsViewModel
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
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    // 전체 해제 직후 "실행 취소" 스낵바. 시간 초과·닫힘이면 복원 불가로 처리한다.
    val pendingUndoApp = (uiState as? SettingsUiState.Success)?.pendingUndoApp
    val doneText = pendingUndoApp?.let { stringResource(R.string.settings_reset_done, stringResource(it.displayNameRes())) }
    val undoLabel = stringResource(R.string.settings_reset_undo)
    val undoneText = stringResource(R.string.settings_reset_undone)
    LaunchedEffect(pendingUndoApp) {
        if (doneText == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = doneText,
            actionLabel = undoLabel,
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoReset()
            // undoReset으로 키가 바뀌어 이 Effect가 취소되므로 별도 scope에서 안내
            snackbarScope.launch { snackbarHostState.showSnackbar(undoneText) }
        } else {
            viewModel.onUndoDismissed()
        }
    }

    SettingsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onAppSelected = viewModel::selectApp,
        onActionSelected = viewModel::updateAction,
        onResetConfirmed = viewModel::resetCurrentApp,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

// ============================================================
// Screen (Stateless Composable)
// ============================================================

/**
 * 제스처 설정 상세 화면.
 *
 * android-settings.md 기준 구성:
 *   안내문 → [대상 앱] 2분할 선택 → [제스처와 동작] 단일 선택 행 5개 → [초기화] 위험 행(확인 대화상자)
 * 기존 상단 바의 즉시 초기화 아이콘은 확인 없이 데이터를 지워 제거하고, 섹션 끝 위험 행으로 옮겼다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBackClick: () -> Unit,
    onAppSelected: (TargetApp) -> Unit,
    onActionSelected: (GestureTrigger, GestureAction?) -> Unit,
    onResetConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                // 본문과 같은 배경 한 장을 쓰도록 상단 바도 background 색 사용
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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
                    onResetConfirmed = onResetConfirmed,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

// ============================================================
// 내부 Composable 구성 요소
// ============================================================

@Composable
private fun SettingsContent(
    selectedApp: TargetApp,
    mappings: Map<GestureTrigger, GestureAction?>,
    onAppSelected: (TargetApp) -> Unit,
    onActionSelected: (GestureTrigger, GestureAction?) -> Unit,
    onResetConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 대화상자 표시 여부는 순수 UI 상태이므로 화면 내부에 둔다. 회전 시 유지를 위해 Saveable 사용.
    // GestureTrigger는 Saveable이 아니므로 settingsEntries의 인덱스로 저장한다.
    var editingTriggerIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    val appName = stringResource(selectedApp.displayNameRes())
    val mappedCount = mappings.values.count { it != null }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsDescription(text = stringResource(R.string.settings_description))

        // -- 대상 앱: 짧고 자명한 2개 선택지 → 분할 선택 컨트롤 --
        SettingsSectionHeader(title = stringResource(R.string.settings_section_target_app))
        TargetAppSelector(
            selectedApp = selectedApp,
            onAppSelected = onAppSelected,
            modifier = Modifier.padding(horizontal = SettingsDimens.ScreenInset),
        )

        // -- 제스처와 동작: 단일 선택 행, 탭하면 라디오 대화상자 --
        SettingsSectionHeader(title = stringResource(R.string.settings_section_gestures))
        GestureTrigger.settingsEntries.forEachIndexed { index, trigger ->
            val action = mappings[trigger]
            SettingsChoiceRow(
                title = stringResource(trigger.displayNameRes()),
                value = action.displayName(),
                valueIsPlaceholder = action == null,
                onClick = { editingTriggerIndex = index },
            )
        }

        // -- 초기화: 데이터 삭제 행은 관련 섹션 끝에 배치 --
        SettingsSectionHeader(title = stringResource(R.string.settings_section_reset))
        SettingsDestructiveRow(
            title = stringResource(R.string.settings_reset_title, appName),
            summary = if (mappedCount > 0) {
                stringResource(R.string.settings_reset_summary, mappedCount)
            } else {
                stringResource(R.string.settings_reset_summary_empty)
            },
            // 해제할 연결이 없으면 실행할 의미가 없으므로 비활성
            enabled = mappedCount > 0,
            onClick = { showResetDialog = true },
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    // -- 동작 선택 대화상자 --
    editingTriggerIndex
        ?.let { GestureTrigger.settingsEntries.getOrNull(it) }
        ?.let { trigger ->
            ActionChoiceDialog(
                trigger = trigger,
                currentAction = mappings[trigger],
                onActionSelected = { action ->
                    onActionSelected(trigger, action)
                    editingTriggerIndex = null
                },
                onDismiss = { editingTriggerIndex = null },
            )
        }

    // -- 초기화 확인 대화상자: 대상(앱)과 범위(개수)를 명시, 취소 시 데이터 유지 --
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = stringResource(R.string.settings_reset_dialog_title, appName)) },
            text = { Text(text = stringResource(R.string.settings_reset_dialog_message, mappedCount)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 대화상자를 먼저 닫아 확인 버튼 중복 탭으로 인한 재실행 방지
                        showResetDialog = false
                        onResetConfirmed()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_reset_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

/**
 * 대상 앱 2분할 선택 컨트롤.
 */
@Composable
private fun TargetAppSelector(
    selectedApp: TargetApp,
    onAppSelected: (TargetApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val apps = TargetApp.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        apps.forEachIndexed { index, app ->
            SegmentedButton(
                selected = app == selectedApp,
                onClick = { onAppSelected(app) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = apps.size),
            ) {
                Text(text = stringResource(app.displayNameRes()))
            }
        }
    }
}

/**
 * 단일 선택 라디오 대화상자.
 *
 * 선택지를 누르면 즉시 저장하고 닫는다(즉시 적용형). 취소는 값 변경 없이 닫는다.
 * 행 전체를 selectable(Role.RadioButton)로 만들어 TalkBack에 하나의 라디오 대상으로 노출한다.
 */
@Composable
private fun ActionChoiceDialog(
    trigger: GestureTrigger,
    currentAction: GestureAction?,
    onActionSelected: (GestureAction?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    R.string.settings_choice_dialog_title,
                    stringResource(trigger.displayNameRes()),
                ),
            )
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                availableActions.forEach { action ->
                    val selected = action == currentAction
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { onActionSelected(action) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = action.displayName(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_cancel))
            }
        },
    )
}

// ============================================================
// Preview
// ============================================================

@Composable
private fun SettingsScreenPreviewContent(state: SettingsUiState) {
    FaceswipeTheme(dynamicColor = false) {
        SettingsScreen(
            uiState = state,
            onBackClick = {},
            onAppSelected = {},
            onActionSelected = { _, _ -> },
            onResetConfirmed = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 380, name = "Loading")
@Composable
private fun SettingsScreenLoadingPreview() {
    SettingsScreenPreviewContent(SettingsUiState.Loading)
}

@Preview(showBackground = true, widthDp = 380, name = "Partially mapped")
@Composable
private fun SettingsScreenSuccessPreview() {
    SettingsScreenPreviewContent(
        SettingsUiState.Success(
            selectedApp = TargetApp.YOUTUBE,
            mappings = mapOf(
                GestureTrigger.HeadLeft to GestureAction.SwipeVertical(directionUp = true),
                GestureTrigger.HeadRight to GestureAction.SwipeVertical(directionUp = false),
                GestureTrigger.MouthOpen to null,
            ),
        )
    )
}

@Preview(showBackground = true, widthDp = 380, name = "Nothing mapped")
@Composable
private fun SettingsScreenEmptyPreview() {
    SettingsScreenPreviewContent(
        SettingsUiState.Success(
            selectedApp = TargetApp.MILLIE,
            mappings = GestureTrigger.settingsEntries.associateWith { null },
        )
    )
}
