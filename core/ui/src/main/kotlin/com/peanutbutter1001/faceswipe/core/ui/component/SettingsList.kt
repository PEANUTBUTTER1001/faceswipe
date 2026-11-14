package com.peanutbutter1001.faceswipe.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peanutbutter1001.faceswipe.core.ui.theme.FaceswipeTheme

/**
 * android-settings.md 4절 시각 규격을 코드 토큰으로 고정한 값.
 *
 * - 좌우 인셋 20dp: 화면 제목, 섹션 소제목, 행 텍스트의 시작선을 하나로 맞춘다.
 * - 행 최소 높이 60dp: 보조 설명이나 글꼴 확대 시에는 heightIn으로 자연스럽게 늘어난다.
 * - 섹션 간격 24dp > 행 사이 간격 0dp: 그룹 경계를 구분선 대신 여백으로 만든다.
 */
object SettingsDimens {
    val ScreenInset = 20.dp
    val RowMinHeight = 60.dp
    val RowVerticalPadding = 12.dp
    val SectionTopSpacing = 24.dp
    val SectionHeaderBottomSpacing = 8.dp
    val TrailingSpacing = 16.dp
}

/** 비활성 행의 텍스트/컨트롤 투명도 (Material 비활성 기준값) */
private const val DISABLED_ALPHA = 0.38f

/**
 * 화면 상단 고정 제목 영역.
 *
 * 하위 화면이 아닌 루트 화면에서 상단 바 대신 사용한다.
 * heading 의미를 부여해 TalkBack에서 제목 단위 탐색이 가능하다.
 */
@Composable
fun SettingsScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsDimens.ScreenInset)
            .padding(top = 16.dp, bottom = 4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall, // 24sp
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium, // 14sp
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 화면 전체에 적용되는 짧은 안내문. 제목 아래 한 번만 사용한다.
 */
@Composable
fun SettingsDescription(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsDimens.ScreenInset, vertical = 8.dp),
    )
}

/**
 * 섹션 소제목. 짧은 명사형 사용을 전제로 한다.
 */
@Composable
fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium, // 12sp
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SettingsDimens.ScreenInset)
            .padding(
                top = SettingsDimens.SectionTopSpacing,
                bottom = SettingsDimens.SectionHeaderBottomSpacing,
            )
            .semantics { heading() },
    )
}

/**
 * 모든 설정 행의 공통 골격.
 *
 * 시작 쪽: 제목 + 보조 설명 / 끝쪽: trailing 슬롯(스위치, 현재 값, 화살표 등).
 * 클릭/토글 처리는 호출하는 쪽에서 [modifier]로 행 전체에 한 번만 부여한다.
 */
@Composable
private fun SettingsRowLayout(
    title: String,
    summary: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    summaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.RowMinHeight)
            .padding(
                horizontal = SettingsDimens.ScreenInset,
                vertical = SettingsDimens.RowVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .alpha(if (enabled) 1f else DISABLED_ALPHA),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge, // 16sp
                color = titleColor,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium, // 14sp
                    color = summaryColor,
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(SettingsDimens.TrailingSpacing))
            trailing()
        }
    }
}

/**
 * 즉시 변경되는 이진 설정 행.
 *
 * 행 전체를 toggleable로 만들고 Switch의 onCheckedChange는 null로 두어
 * 탭 한 번에 토글이 한 번만 일어나고, TalkBack에는 하나의 Switch 대상으로 노출된다.
 */
@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onDisabledClick: (() -> Unit)? = null,
) {
    // 비활성이어도 onDisabledClick이 있으면 탭을 받아 "왜 켤 수 없는지" 안내 흐름으로 보낸다.
    // 스위치 자체는 비활성 모양을 유지하고, 행은 버튼 역할로 노출한다.
    val rowModifier = if (!enabled && onDisabledClick != null) {
        modifier.clickable(role = Role.Button, onClick = onDisabledClick)
    } else {
        modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        )
    }
    SettingsRowLayout(
        title = title,
        summary = summary,
        enabled = enabled,
        modifier = rowModifier,
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

/**
 * 상세 화면으로 이동하는 행. 루트에서 상세 화면의 현재 상태를 요약해 보여준다.
 */
@Composable
fun SettingsNavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    SettingsRowLayout(
        title = title,
        summary = summary,
        enabled = enabled,
        modifier = modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        ),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null, // 행 제목이 이미 의미를 전달
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 단일 선택 행. 제목 아래에 현재 값을 표시하고, 탭하면 선택 대화상자를 연다.
 */
@Composable
fun SettingsChoiceRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueIsPlaceholder: Boolean = false,
) {
    SettingsRowLayout(
        title = title,
        summary = value,
        enabled = true,
        summaryColor = if (valueIsPlaceholder) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.primary
        },
        modifier = modifier.clickable(
            role = Role.Button,
            onClick = onClick,
        ),
    )
}

/**
 * 권한/시스템 기능 상태 행.
 *
 * 끝쪽에 상태 문구를 색과 함께 표시해 색만으로 상태를 전달하지 않는다.
 * [onClick]이 null이면 조작 불가능한 상태 표시 행으로 동작한다.
 */
@Composable
fun SettingsStatusRow(
    title: String,
    summary: String,
    statusText: String,
    statusColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = if (onClick != null) {
        modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        modifier
    }
    SettingsRowLayout(
        title = title,
        summary = summary,
        enabled = true,
        modifier = rowModifier,
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
        )
    }
}

/**
 * 읽기 전용 정보 행 (버전 등). 클릭 동작이 없다.
 */
@Composable
fun SettingsInfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    SettingsRowLayout(
        title = title,
        summary = null,
        enabled = true,
        modifier = modifier,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 데이터 삭제/초기화 행. 위험 색으로 제목을 표시하고, 확인 대화상자는 호출하는 쪽이 띄운다.
 */
@Composable
fun SettingsDestructiveRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
) {
    SettingsRowLayout(
        title = title,
        summary = summary,
        enabled = enabled,
        titleColor = MaterialTheme.colorScheme.error,
        modifier = modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick,
        ),
    )
}

// ============================================================
// Preview
// ============================================================

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun SettingsListPreview() {
    FaceswipeTheme(dynamicColor = false) {
        Column {
            SettingsScreenTitle(title = "faceswipe", description = "얼굴 움직임으로 화면 조작")
            SettingsSectionHeader(title = "실행")
            SettingsSwitchRow(title = "faceswipe 실행", summary = "꺼짐", checked = false, onCheckedChange = {})
            SettingsSwitchRow(title = "비활성 스위치", summary = "권한 필요", checked = false, enabled = false, onCheckedChange = {})
            SettingsSectionHeader(title = "권한")
            SettingsStatusRow(
                title = "카메라",
                summary = "얼굴 인식에 사용",
                statusText = "허용됨",
                statusColor = MaterialTheme.colorScheme.primary,
            )
            SettingsSectionHeader(title = "제스처")
            SettingsNavigationRow(title = "제스처 설정", summary = "유튜브 3개 연결", onClick = {})
            SettingsChoiceRow(title = "입 벌리기", value = "하단 1/4 영역 터치", onClick = {})
            SettingsDestructiveRow(title = "연결 모두 해제", onClick = {})
            SettingsSectionHeader(title = "정보")
            SettingsInfoRow(title = "앱 버전", value = "1.0")
        }
    }
}
