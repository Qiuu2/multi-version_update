package com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.LoginScreen
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.SplashScreen

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    onSettingsClick: () -> Unit = {},
) {
    NavHost(navController = navController, startDestination = AppRoutes.SPLASH) {
        composable(AppRoutes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppRoutes.MAIN) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(AppRoutes.MAIN) {
            MainScaffold(onSettingsClick = onSettingsClick)
        }
    }
}
