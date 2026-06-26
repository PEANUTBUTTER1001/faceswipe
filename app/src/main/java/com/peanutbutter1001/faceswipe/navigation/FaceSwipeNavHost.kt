package com.peanutbutter1001.faceswipe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.HomeRoute
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.homeScreen
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.navigateToSettings
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.settingsScreen

@Composable
fun FaceSwipeNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        homeScreen(
            onSettingsClick = { navController.navigateToSettings() },
        )
        settingsScreen(
            onBackClick = { navController.popBackStack() },
        )
    }
}
