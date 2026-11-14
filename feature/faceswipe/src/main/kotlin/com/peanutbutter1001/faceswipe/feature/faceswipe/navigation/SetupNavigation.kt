package com.peanutbutter1001.faceswipe.feature.faceswipe.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.peanutbutter1001.faceswipe.feature.faceswipe.setup.SetupRoute
import kotlinx.serialization.Serializable

/**
 * 설정 마법사 목적지.
 *
 * @param fromHome true면 홈에서 진입(SetupEntry.Home), false면 첫 실행(SetupEntry.First)
 * @param focusNotification 홈의 알림 행에서 진입. 이미 요청한 적이 있어도 알림 단계를 보여준다.
 */
@Serializable
data class SetupDestination(
    val fromHome: Boolean = false,
    val focusNotification: Boolean = false,
)

fun NavController.navigateToSetup(
    fromHome: Boolean,
    focusNotification: Boolean = false,
    navOptions: NavOptions? = null,
) {
    navigate(SetupDestination(fromHome, focusNotification), navOptions)
}

/**
 * @param onExit 마법사를 마쳤거나 닫았을 때. 인자는 홈에서 진입했는지 여부
 *   (홈 진입이면 popBackStack, 첫 실행이면 홈으로 이동하며 백스택 정리).
 */
fun NavGraphBuilder.setupScreen(
    onExit: (fromHome: Boolean) -> Unit,
) {
    composable<SetupDestination> { entry ->
        val destination = entry.toRoute<SetupDestination>()
        SetupRoute(onExit = { onExit(destination.fromHome) })
    }
}
