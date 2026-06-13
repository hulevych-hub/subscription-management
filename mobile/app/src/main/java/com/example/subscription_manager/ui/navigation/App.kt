package com.example.subscription_manager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.subscription_manager.ui.screens.home.HomeScreen
import com.example.subscription_manager.ui.screens.settings.SettingsScreen
import com.example.subscription_manager.ui.screens.subscription.AddEditScreen
import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
    @Serializable
    data object Home : Route()
    @Serializable
    data object AddSubscription : Route()
    @Serializable
    data class EditSubscription(val id: Long) : Route() {
        fun asNavArgument(): String = "edit/$id"
    }
    @Serializable
    data object Settings : Route()
}

private const val SUBSCRIPTION_ID_ARGUMENT = "id"

@Composable
fun App(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        modifier = modifier,
        startDestination = Route.Home
    ) {
        composable<Route.Home> {
            HomeScreen(
                onAddSubscription = {
                    navController.navigate(Route.AddSubscription)
                },
                onEditSubscription = { id ->
                    navController.navigate("edit/$id")
                },
                onSettings = {
                    navController.navigate(Route.Settings)
                }
            )
        }
        composable<Route.AddSubscription> {
            AddEditScreen(
                subscriptionId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            route = "edit/{$SUBSCRIPTION_ID_ARGUMENT}",
            arguments = listOf(
                navArgument(SUBSCRIPTION_ID_ARGUMENT) {
                    type = NavType.LongType
                }
            )
        ) { entry ->
            val id = entry.arguments?.getLong(SUBSCRIPTION_ID_ARGUMENT)
            AddEditScreen(
                subscriptionId = id,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable<Route.Settings> {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
