package com.peanutbutter1001.faceswipe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.HomeRoute
import com.peanutbutter1001.faceswipe.feature.faceswipe.navigation.homeScreen

@Composable
fun FaceSwipeNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        homeScreen()
    }
}
