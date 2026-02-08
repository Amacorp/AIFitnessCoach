package com.aicoach.fitness.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aicoach.fitness.ui.screens.*

sealed class Screen(val route: String) {
    data object ApiKey : Screen("api_key")
    data object Onboarding : Screen("onboarding")
    data object Dashboard : Screen("dashboard")
    data object Camera : Screen("camera/{dayNumber}/{exerciseId}") {
        fun createRoute(dayNumber: Int, exerciseId: String) = "camera/$dayNumber/$exerciseId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.ApiKey.route) {
            ApiKeyScreen(
                onApiKeySaved = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.ApiKey.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.ApiKey.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            MainDashboard(
                onStartWorkout = { dayNumber, exerciseId ->
                    navController.navigate(Screen.Camera.createRoute(dayNumber, exerciseId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Camera.route,
            arguments = listOf(
                navArgument("dayNumber") { type = NavType.IntType },
                navArgument("exerciseId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            // In a real app, you'd fetch the exercise name from the repository
            CameraScreen(
                exerciseName = "Exercise", // TODO: Get actual name
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
