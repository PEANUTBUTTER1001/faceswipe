package com.peanutbutter1001.faceswipe.feature.faceswipe.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.peanutbutter1001.faceswipe.feature.faceswipe.screen.HomeRoute
import kotlinx.serialization.Serializable

/** Type-safe Navigation route for Home screen. */
@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen() {
    composable<HomeRoute> {
        HomeRoute()
    }
}

fun NavController.navigateToHome(navOptions: NavOptions? = null) {
    navigate(route = HomeRoute, navOptions = navOptions)
}
