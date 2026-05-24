package com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.LoginScreen
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.SplashScreen

/**
 * Root NavGraph for the v4 UI.
 *
 *   splash → login → main
 *
 * No back stack from `main` to `login` — logout will clear+pop back to login.
 * The `main` destination embeds the 5-tab scaffold; tab switching lives inside
 * [MainScaffold] (local state for v0; can be promoted to a nested NavGraph
 * later if any tab needs deep navigation).
 */
@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppRoutes.Splash,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(AppRoutes.Splash) {
            SplashScreen(
                onFinish = {
                    // No real token check yet — always send to Login.
                    navController.navigate(AppRoutes.Login) {
                        popUpTo(AppRoutes.Splash) { inclusive = true }
                    }
                },
            )
        }

        composable(AppRoutes.Login) {
            LoginScreen(
                onLogin = { _, _, _, _ ->
                    navController.navigate(AppRoutes.Main) {
                        popUpTo(AppRoutes.Login) { inclusive = true }
                    }
                },
            )
        }

        composable(AppRoutes.Main) {
            MainScaffold()
        }
    }
}
