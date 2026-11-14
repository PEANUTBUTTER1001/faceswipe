package com.peanutbutter1001.faceswipe.feature.faceswipe.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peanutbutter1001.faceswipe.core.ui.component.SettingsDimens
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeTheme
import com.peanutbutter1001.faceswipe.feature.faceswipe.R

/** 홈 [정보] 섹션에서 여는 바텀시트 종류 */
enum class InfoSheet { Privacy, Licenses }

/**
 * 개인정보 처리방침 요약 / 오픈소스 라이선스 바텀시트.
 *
 * @param onOpenFullPolicy 처리방침 전문 열기. URL이 없으면 null → "전문 보기" 숨김 (명세 Q1)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(
    sheet: InfoSheet,
    onDismiss: () -> Unit,
    onOpenFullPolicy: (() -> Unit)?,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        when (sheet) {
            InfoSheet.Privacy -> PrivacyContent(onOpenFullPolicy)
            InfoSheet.Licenses -> LicensesContent()
        }
    }
}

@Composable
private fun PrivacyContent(onOpenFullPolicy: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        SheetTitle(stringResource(R.string.privacy_sheet_title))
        PrivacyItem(R.string.privacy_camera_title, R.string.privacy_camera_body)
        PrivacyItem(R.string.privacy_a11y_title, R.string.privacy_a11y_body)
        PrivacyItem(R.string.privacy_storage_title, R.string.privacy_storage_body)
        PrivacyItem(R.string.privacy_network_title, R.string.privacy_network_body)
        if (onOpenFullPolicy != null) {
            FilledTonalButton(
                onClick = onOpenFullPolicy,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SettingsDimens.ScreenInset, vertical = 12.dp)
                    .heightIn(min = 52.dp),
            ) { Text(stringResource(R.string.privacy_full)) }
        }
    }
}

@Composable
private fun PrivacyItem(titleRes: Int, bodyRes: Int) {
    HorizontalDivider()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsDimens.ScreenInset, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            stringResource(bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** 실제 의존성 기준 라이선스 목록 (gradle/libs.versions.toml) */
private val LICENSES = listOf(
    "Jetpack Compose" to R.string.license_apache2,
    "AndroidX (Activity, Lifecycle, Navigation, DataStore)" to R.string.license_apache2,
    "CameraX" to R.string.license_apache2,
    "Hilt / Dagger" to R.string.license_apache2,
    "Kotlin Coroutines" to R.string.license_apache2,
    "kotlinx.serialization" to R.string.license_apache2,
    "ML Kit Face Detection" to R.string.license_mlkit,
)

@Composable
private fun LicensesContent() {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        SheetTitle(stringResource(R.string.open_source_licenses))
        LICENSES.forEach { (name, licenseRes) ->
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = SettingsDimens.ScreenInset, vertical = 10.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    stringResource(licenseRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(horizontal = SettingsDimens.ScreenInset, vertical = 8.dp)
            .semantics { heading() },
    )
}

// 바텀시트 자체는 Preview에서 창으로 뜨지 않으므로 내용만 미리 본다.
@Preview(name = "처리방침 요약", showBackground = true, widthDp = 380)
@Composable
private fun PrivacyContentPreview() {
    FaceswipeTheme(dynamicColor = false) { PrivacyContent(onOpenFullPolicy = {}) }
}

@Preview(name = "라이선스 (다크)", showBackground = true, widthDp = 380)
@Composable
private fun LicensesContentPreview() {
    FaceswipeTheme(darkTheme = true, dynamicColor = false) { LicensesContent() }
}
