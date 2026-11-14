package com.peanutbutter1001.faceswipe.feature.faceswipe.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeTheme
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeThemeTokens
import com.peanutbutter1001.faceswipe.feature.faceswipe.R
import kotlinx.serialization.Serializable

/** 첫 실행 소개 목적지 */
@Serializable
data object WelcomeDestination

fun NavGraphBuilder.welcomeScreen(onStart: () -> Unit) {
    composable<WelcomeDestination> { WelcomeScreen(onStart = onStart) }
}

/**
 * 첫 실행 소개 화면 (Stateless).
 *
 * 권한은 여기서 요청하지 않는다. "시작하기"를 누르면 설정 마법사(첫 실행 모드)로 이동하고,
 * 마법사를 마치거나 건너뛸 때 onboardingDone이 저장된다.
 */
@Composable
fun WelcomeScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(20.dp))
            BrandRow()
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.welcome_lead),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            WayRow(Icons.Outlined.Lock, R.string.welcome_step1_title, R.string.welcome_step1_body, R.string.welcome_step1_tag)
            WayRow(Icons.Outlined.Face, R.string.welcome_step2_title, R.string.welcome_step2_body, R.string.welcome_step2_tag)
            WayRow(Icons.Outlined.Apps, R.string.welcome_step3_title, R.string.welcome_step3_body, R.string.welcome_step3_tag)
            HorizontalDivider()
        }
        // 하단 고정: 기기 안 처리 문장 + 시작하기
        Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.Top) {
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
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.welcome_start), style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BrandRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(9.dp),
            modifier = Modifier.size(30.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Face, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.app_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

/** 소개 3단계 한 줄. 아이콘은 장식, 텍스트는 하나의 TalkBack 대상으로 묶음 */
@Composable
private fun WayRow(icon: ImageVector, titleRes: Int, bodyRes: Int, tagRes: Int) {
    HorizontalDivider()
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp).semantics(mergeDescendants = true) {}) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(tagRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Preview(name = "소개", showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun WelcomePreview() = FaceswipeTheme(dynamicColor = false) { WelcomeScreen(onStart = {}) }

@Preview(name = "소개 (다크)", showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun WelcomeDarkPreview() = FaceswipeTheme(darkTheme = true, dynamicColor = false) { WelcomeScreen(onStart = {}) }
