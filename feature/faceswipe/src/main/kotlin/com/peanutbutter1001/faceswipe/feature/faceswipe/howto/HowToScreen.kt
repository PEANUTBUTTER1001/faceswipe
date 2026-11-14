package com.peanutbutter1001.faceswipe.feature.faceswipe.howto

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsDimens
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsSectionHeader
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeTheme
import com.peanutbutter1001.faceswipe.data.faceswipe.service.PermissionChecker
import com.peanutbutter1001.faceswipe.data.faceswipe.service.ServiceController
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp
import com.peanutbutter1001.faceswipe.feature.faceswipe.R
import com.peanutbutter1001.faceswipe.feature.faceswipe.screen.displayNameRes
import com.peanutbutter1001.faceswipe.feature.faceswipe.setup.StepItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

/** 사용 방법 목적지 */
@Serializable
data object HowToDestination

fun NavGraphBuilder.howToScreen(onBack: () -> Unit, onOpenSetup: () -> Unit) {
    composable<HowToDestination> { HowToRoute(onBack = onBack, onOpenSetup = onOpenSetup) }
}

/**
 * "시험해 보기" 버튼 활성 판단과 서비스 시작만 담당하는 작은 ViewModel.
 * 권한은 OS가 원천이므로 ON_RESUME마다 [refresh]로 다시 읽는다.
 */
@HiltViewModel
class HowToViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker,
    private val serviceController: ServiceController,
) : ViewModel() {
    private val _canStart = MutableStateFlow(readCanStart())
    val canStart: StateFlow<Boolean> = _canStart.asStateFlow()

    fun refresh() {
        _canStart.value = readCanStart()
    }

    fun startService() = serviceController.start()

    private fun readCanStart() =
        permissionChecker.hasAccessibilityPermission() && permissionChecker.hasCameraPermission()
}

@Composable
fun HowToRoute(
    onBack: () -> Unit,
    onOpenSetup: () -> Unit,
    viewModel: HowToViewModel = hiltViewModel(),
) {
    val canStart by viewModel.canStart.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val youtubeMissing = stringResource(R.string.setup_youtube_missing)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    HowToScreen(
        canStart = canStart,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTryIt = {
            val launch = context.packageManager.getLaunchIntentForPackage(TargetApp.YOUTUBE.packageName)
            if (launch == null) {
                scope.launch { snackbarHostState.showSnackbar(youtubeMissing) }
            } else {
                viewModel.startService()
                context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        },
        onOpenSetup = onOpenSetup,
    )
}

/** 제스처별 동작법 문구 */
private fun GestureTrigger.howToRes(): Int = when (this) {
    GestureTrigger.HeadLeft -> R.string.howto_head_left
    GestureTrigger.HeadRight -> R.string.howto_head_right
    GestureTrigger.MouthOpen -> R.string.howto_mouth_open
}

/**
 * 사용 방법 화면 (Stateless).
 * [사용 순서] 3단계 → [제스처별 동작법] 5종 → [잘 인식되게 하려면] 팁 3개 → 하단 시험해 보기
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToScreen(
    canStart: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTryIt: () -> Unit,
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.howto_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            // 권한이 부족하면 같은 자리에서 마법사로 안내
            Button(
                onClick = if (canStart) onTryIt else onOpenSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .heightIn(min = 52.dp),
            ) {
                Text(
                    stringResource(if (canStart) R.string.setup_try else R.string.howto_need_permission),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSectionHeader(stringResource(R.string.howto_section_order))
            Column(Modifier.padding(horizontal = SettingsDimens.ScreenInset)) {
                StepItem(1, R.string.howto_step1_title, R.string.howto_step1_body)
                StepItem(2, R.string.howto_step2_title, R.string.howto_step2_body)
                StepItem(3, R.string.howto_step3_title, R.string.howto_step3_body)
            }
            SettingsSectionHeader(stringResource(R.string.howto_section_gestures))
            GestureTrigger.settingsEntries.forEach { trigger ->
                HowToRow(stringResource(trigger.displayNameRes()), stringResource(trigger.howToRes()))
            }
            SettingsSectionHeader(stringResource(R.string.howto_section_tips))
            HowToRow(stringResource(R.string.howto_tip1_title), stringResource(R.string.howto_tip1_body))
            HowToRow(stringResource(R.string.howto_tip2_title), stringResource(R.string.howto_tip2_body))
            HowToRow(stringResource(R.string.howto_tip3_title), stringResource(R.string.howto_tip3_body))
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 읽기 전용 설명 행 (설정 행과 같은 시작선·여백) */
@Composable
private fun HowToRow(title: String, body: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.RowMinHeight)
            .padding(horizontal = SettingsDimens.ScreenInset, vertical = SettingsDimens.RowVerticalPadding),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "사용 방법", showBackground = true, widthDp = 380, heightDp = 1200)
@Composable
private fun HowToPreview() = FaceswipeTheme(dynamicColor = false) {
    HowToScreen(true, remember { SnackbarHostState() }, {}, {}, {})
}

@Preview(name = "사용 방법 · 권한 부족 (다크)", showBackground = true, widthDp = 380, heightDp = 800)
@Composable
private fun HowToNoPermissionPreview() = FaceswipeTheme(darkTheme = true, dynamicColor = false) {
    HowToScreen(false, remember { SnackbarHostState() }, {}, {}, {})
}
