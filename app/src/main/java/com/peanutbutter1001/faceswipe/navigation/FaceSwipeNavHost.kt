package com.peanutbutter1001.faceswipe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.peanutbutter1001.faceswipe.feature.faceswipe.howto.HowToDestination
import com.peanutbutter1001.faceswipe.feature.faceswipe.howto.howToScreen
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.HomeRoute
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.homeScreen
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.navigateToSettings
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.navigateToSetup
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.settingsScreen
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.setupScreen
import com.peanutbutter1001.faceswipe.feature.faceswipe.onboarding.WelcomeDestination
import com.peanutbutter1001.faceswipe.feature.faceswipe.onboarding.welcomeScreen

/**
 * 앱 내비게이션 그래프.
 *
 * 첫 실행: Welcome → Setup(First) → Home (Welcome·Setup은 백스택에서 제거)
 * 이후: Home 시작. 권한 행·비활성 스위치·타일 요청 → Setup(Home) → 뒤로
 *
 * @param startWithWelcome onboardingDone=false면 소개 화면부터
 * @param openSetupRequest 빠른 설정 타일이 "설정 필요" 상태에서 눌려 앱이 열린 경우
 */
@Composable
fun FaceSwipeNavHost(
    startWithWelcome: Boolean,
    openSetupRequest: Boolean,
    onOpenSetupConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    // 외부 요청으로 마법사 열기 (첫 실행 중이면 첫 실행 모드 유지)
    LaunchedEffect(openSetupRequest) {
        if (openSetupRequest) {
            navController.navigateToSetup(fromHome = !startWithWelcome, navOptions = navOptions { launchSingleTop = true })
            onOpenSetupConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (startWithWelcome) WelcomeDestination else HomeRoute,
        modifier = modifier,
    ) {
        welcomeScreen(
            onStart = { navController.navigateToSetup(fromHome = false) },
        )
        setupScreen(
            onExit = { fromHome ->
                if (fromHome) {
                    navController.popBackStack()
                } else {
                    // 첫 실행 완료: 소개·마법사를 백스택에서 지우고 홈을 루트로
                    navController.navigate(HomeRoute) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            },
        )
        homeScreen(
            onSettingsClick = { navController.navigateToSettings() },
            onHowToClick = { navController.navigate(HowToDestination) },
            onOpenSetup = { focusNotification ->
                navController.navigateToSetup(fromHome = true, focusNotification = focusNotification)
            },
        )
        settingsScreen(
            onBackClick = { navController.popBackStack() },
        )
        howToScreen(
            onBack = { navController.popBackStack() },
            onOpenSetup = { navController.navigateToSetup(fromHome = true) },
        )
    }
}
