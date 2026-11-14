package com.peanutbutter1001.faceswipe.feature.faceswipe.setup

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeTheme
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeThemeTokens
import com.peanutbutter1001.faceswipe.feature.faceswipe.R

/** 마법사 화면 이벤트 묶음 (파라미터 수를 줄이고 Preview를 단순하게 유지) */
data class SetupCallbacks(
    val onClose: () -> Unit = {},
    val onAgreeDisclosure: () -> Unit = {},
    val onRequestCamera: () -> Unit = {},
    val onOpenAppInfo: () -> Unit = {},
    val onRequestNotification: () -> Unit = {},
    val onSkipNotification: () -> Unit = {},
    val onAddTile: () -> Unit = {},
    val onTryIt: () -> Unit = {},
    val onGoHome: () -> Unit = {},
)

/** 본문 좌우 여백. 설정 목록(20dp)보다 넓게 두어 읽기 쉬운 안내 화면으로 구분 */
private val BodyInset = 24.dp

/**
 * 설정 마법사 화면 (Stateless).
 * 상단: 닫기/뒤로 + 단계 표시 / 본문: 단계별 안내 / 하단: 주 버튼과 보조 버튼.
 */
@Composable
fun SetupScreen(
    uiState: SetupUiState,
    snackbarHostState: SnackbarHostState,
    tileSupported: Boolean,
    callbacks: SetupCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val state = uiState as? SetupUiState.Success
        if (state == null || state.step == SetupStep.Finished) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            SetupTopBar(state, callbacks.onClose)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = BodyInset),
            ) {
                when (state.step) {
                    SetupStep.Disclose -> DiscloseBody()
                    SetupStep.Camera -> CameraBody(state.cameraState)
                    SetupStep.Notification -> NotificationBody()
                    SetupStep.Done -> DoneBody(tileSupported, state.tileAdded)
                    SetupStep.Finished -> Unit
                }
                Spacer(Modifier.height(16.dp))
            }
            SetupActions(state, tileSupported, callbacks)
        }
    }
}

@Composable
private fun SetupTopBar(state: SetupUiState.Success, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            // 첫 실행은 마법사를 닫는 의미(X), 홈 진입은 이전 화면으로 돌아가는 의미(←)
            if (state.entry == SetupEntry.First) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close))
            } else {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
        }
        Spacer(Modifier.weight(1f))
        if (state.stepNumber != null) {
            val cd = stringResource(R.string.setup_step_counter_cd, state.stepNumber, state.stepCount)
            Text(
                text = stringResource(R.string.setup_step_counter, state.stepNumber, state.stepCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp).semantics { contentDescription = cd },
            )
        }
    }
}

// ============================================================
// 단계별 본문
// ============================================================

@Composable
private fun DiscloseBody() {
    SetupTitle(stringResource(R.string.setup_disclose_title))
    SetupLead(stringResource(R.string.setup_disclose_lead))
    FactRow(Icons.Outlined.Apps, R.string.setup_fact_access_title, R.string.setup_fact_access_body)
    FactRow(Icons.Outlined.TouchApp, R.string.setup_fact_use_title, R.string.setup_fact_use_body)
    FactRow(Icons.Outlined.Lock, R.string.setup_fact_share_title, R.string.setup_fact_share_body)
    FactRow(Icons.Outlined.PanTool, R.string.setup_fact_not_title, R.string.setup_fact_not_body, positive = true)
    HorizontalDivider()
}

@Composable
private fun CameraBody(cameraState: CameraPermissionState) {
    SetupTitle(stringResource(R.string.setup_camera_title))
    SetupLead(stringResource(R.string.setup_camera_lead))
    // 상태 표시줄 카메라 사용 표시(초록 점)는 Android 12+에서만 제공된다.
    val whenBody = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        R.string.setup_camera_when_body
    } else {
        R.string.setup_camera_when_body_legacy
    }
    FactRow(Icons.Outlined.Videocam, R.string.setup_camera_when_title, whenBody)
    FactRow(Icons.Outlined.Face, R.string.setup_camera_what_title, R.string.setup_camera_what_body)
    FactRow(Icons.Outlined.Lock, R.string.setup_camera_store_title, R.string.setup_camera_store_body, positive = true)
    HorizontalDivider()
    when (cameraState) {
        CameraPermissionState.Denied -> Notice(stringResource(R.string.setup_camera_denied), warning = true)
        CameraPermissionState.Blocked -> Notice(stringResource(R.string.setup_camera_blocked), warning = true)
        else -> Unit
    }
}

@Composable
private fun NotificationBody() {
    SetupTitle(stringResource(R.string.setup_notif_title))
    SetupLead(stringResource(R.string.setup_notif_lead))
    NotificationPreview()
}

@Composable
private fun DoneBody(tileSupported: Boolean, tileAdded: Boolean) {
    val success = FaceswipeThemeTokens.statusColors.success
    Spacer(Modifier.height(8.dp))
    Box(
        modifier = Modifier.size(56.dp).clip(CircleShape).background(success.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = success, modifier = Modifier.size(30.dp))
    }
    Spacer(Modifier.height(16.dp))
    SetupTitle(stringResource(R.string.setup_done_title))
    StepItem(1, R.string.setup_done_step1_title, R.string.setup_done_step1_body)
    StepItem(2, R.string.setup_done_step2_title, R.string.setup_done_step2_body)
    StepItem(3, R.string.setup_done_step3_title, R.string.setup_done_step3_body)
    if (tileSupported) {
        Notice(
            title = stringResource(R.string.setup_done_tile_title),
            text = stringResource(if (tileAdded) R.string.setup_done_tile_added else R.string.setup_done_tile_missing),
        )
    }
}

// ============================================================
// 하단 버튼
// ============================================================

@Composable
private fun SetupActions(state: SetupUiState.Success, tileSupported: Boolean, callbacks: SetupCallbacks) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BodyInset, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when (state.step) {
            SetupStep.Disclose -> {
                Text(
                    text = stringResource(R.string.setup_disclose_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                PrimaryButton(R.string.setup_agree, callbacks.onAgreeDisclosure)
                SecondaryButton(R.string.common_later, callbacks.onClose)
            }
            SetupStep.Camera -> {
                when (state.cameraState) {
                    CameraPermissionState.Blocked -> PrimaryButton(R.string.setup_camera_open_app_info, callbacks.onOpenAppInfo)
                    CameraPermissionState.Denied -> PrimaryButton(R.string.setup_camera_retry, callbacks.onRequestCamera)
                    else -> PrimaryButton(R.string.setup_camera_allow, callbacks.onRequestCamera)
                }
                SecondaryButton(R.string.common_later, callbacks.onClose)
            }
            SetupStep.Notification -> {
                PrimaryButton(R.string.setup_notif_allow, callbacks.onRequestNotification)
                SecondaryButton(R.string.setup_notif_skip, callbacks.onSkipNotification)
            }
            SetupStep.Done -> {
                if (tileSupported && !state.tileAdded) {
                    FilledTonalButton(
                        onClick = callbacks.onAddTile,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) { Text(stringResource(R.string.setup_add_tile)) }
                }
                PrimaryButton(R.string.setup_try, callbacks.onTryIt)
                SecondaryButton(R.string.setup_go_home, callbacks.onGoHome)
            }
            SetupStep.Finished -> Unit
        }
    }
}

@Composable
private fun PrimaryButton(textRes: Int, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text(stringResource(textRes), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun SecondaryButton(textRes: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(textRes))
    }
}

// ============================================================
// 공통 구성 요소
// ============================================================

@Composable
internal fun SetupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall, // 24sp
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp).semantics { heading() },
    )
}

@Composable
internal fun SetupLead(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

/**
 * 고지 항목 한 줄. 아이콘은 장식이므로 설명을 두지 않고, 제목·본문을 하나의 TalkBack 대상으로 묶는다.
 * @param positive "하지 않는 일"처럼 안심 항목이면 성공 색 아이콘
 */
@Composable
internal fun FactRow(icon: ImageVector, titleRes: Int, bodyRes: Int, positive: Boolean = false) {
    HorizontalDivider()
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp).semantics(mergeDescendants = true) {}) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (positive) FaceswipeThemeTokens.statusColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(
                stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 번호가 붙은 안내 단계 */
@Composable
internal fun StepItem(number: Int, titleRes: Int, bodyRes: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics(mergeDescendants = true) {}) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Notice(text: String, warning: Boolean = false, title: String? = null) {
    Surface(
        color = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** 실행 중 알림 모양 미리보기 (실제 알림과 같은 문구) */
@Composable
private fun NotificationPreview() {
    val cd = stringResource(R.string.setup_notif_preview_cd)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = cd },
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                stringResource(R.string.setup_notif_preview_app),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.setup_notif_preview_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.setup_notif_preview_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.setup_notif_preview_action),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ============================================================
// Preview
// ============================================================

private fun previewState(step: SetupStep, camera: CameraPermissionState = CameraPermissionState.NotAsked) =
    SetupUiState.Success(
        entry = SetupEntry.First,
        step = step,
        stepNumber = when (step) {
            SetupStep.Disclose -> 1
            SetupStep.Camera -> 2
            SetupStep.Notification -> 3
            else -> null
        },
        stepCount = 3,
        cameraState = camera,
        tileAdded = false,
        message = null,
    )

@Composable
private fun SetupPreview(state: SetupUiState, dark: Boolean = false) {
    FaceswipeTheme(darkTheme = dark, dynamicColor = false) {
        SetupScreen(state, remember { SnackbarHostState() }, tileSupported = true, callbacks = SetupCallbacks())
    }
}

@Preview(name = "고지·동의", showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun DisclosePreview() = SetupPreview(previewState(SetupStep.Disclose))

@Preview(name = "카메라 · 차단", showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun CameraBlockedPreview() = SetupPreview(previewState(SetupStep.Camera, CameraPermissionState.Blocked))

@Preview(name = "알림 (다크)", showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun NotificationPreviewDark() = SetupPreview(previewState(SetupStep.Notification), dark = true)

@Preview(name = "완료", showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun DonePreview() = SetupPreview(previewState(SetupStep.Done))

@Preview(name = "로딩", showBackground = true, widthDp = 380, heightDp = 400)
@Composable
private fun LoadingPreview() = SetupPreview(SetupUiState.Loading)
