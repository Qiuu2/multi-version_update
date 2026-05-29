package com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.htgd.radiocontrol.aeroradiocontrol.ui.permissions.RequiredPermissionsGate
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.LoginRoute
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.SplashScreen

/**
 * Root NavGraph for the v4 UI.
 *
 *   splash → login → main
 *
 * No back stack from `main` to `login` — logout will clear+pop back to login.
 * The login destination is [LoginRoute] (ViewModel-driven, TASK-AR-005): it
 * navigates onward as soon as AuthStore reports a session, so a restored login
 * (process restart with a valid token) bounces straight through to main.
 * The `main` destination embeds the 5-tab scaffold; tab switching lives inside
 * [MainScaffold] (local state for v0; can be promoted to a nested NavGraph
 * later if any tab needs deep navigation).
 *
 * The `main` destination is wrapped in [RequiredPermissionsGate] (TASK-AR-010):
 * V4Activity is now the sole launcher and no longer routes through the legacy
 * permission screen, so runtime permissions are requested here, right before
 * paging/intercom become reachable.
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
            LoginRoute(
                onLoggedIn = {
                    navController.navigate(AppRoutes.Main) {
                        popUpTo(AppRoutes.Login) { inclusive = true }
                    }
                },
            )
        }

        composable(AppRoutes.Main) {
            RequiredPermissionsGate {
                MainScaffold()
            }
        }
    }
}
