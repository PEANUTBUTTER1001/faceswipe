package com.peanutbutter1001.faceswipe.feature.faceswipe.screen

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.peanutbutter1001.faceswipe.data.faceswipe.label.GestureLabels
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureAction
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.GestureTrigger
import com.peanutbutter1001.faceswipe.domain.faceswipe.model.TargetApp

/**
 * 도메인 모델 → 화면 표시 문자열.
 *
 * 레이블 원본은 data 모듈의 [GestureLabels] (서비스 오버레이와 공유, 명세 Q2 = A).
 */

/** 설정 UI에서 선택 가능한 액션 목록. null은 "선택 안 함". */
internal val availableActions: List<GestureAction?> = listOf(
    GestureAction.SwipeVertical(directionUp = true),
    GestureAction.SwipeVertical(directionUp = false),
    GestureAction.Tap,
    null,
)

@Composable
internal fun GestureAction?.displayName(): String = stringResource(GestureLabels.action(this))

@StringRes
internal fun GestureTrigger.displayNameRes(): Int = GestureLabels.trigger(this)

@StringRes
internal fun TargetApp.displayNameRes(): Int = GestureLabels.app(this)
