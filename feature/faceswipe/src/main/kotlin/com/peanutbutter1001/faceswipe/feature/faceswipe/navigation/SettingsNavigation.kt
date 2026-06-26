package com.peanutbutter1001.faceswipe.feature.faceswipe.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.peanutbutter1001.faceswipe.feature.faceswipe.screen.SettingsRoute
import kotlinx.serialization.Serializable

/**
 * 설정 화면의 type-safe Navigation route.
 */
@Serializable
data object SettingsRoute

/**
 * NavController 확장 함수: 설정 화면으로 이동.
 */
fun NavController.navigateToSettings() {
    navigate(SettingsRoute)
}

/**
 * NavGraphBuilder 확장 함수: 설정 화면 목적지 등록.
 *
 * @param onBackClick 뒤로가기 콜백 (NavController.popBackStack 등)
 */
fun NavGraphBuilder.settingsScreen(
    onBackClick: () -> Unit,
) {
    composable<SettingsRoute> {
        SettingsRoute(
            onBackClick = onBackClick,
        )
    }
}
