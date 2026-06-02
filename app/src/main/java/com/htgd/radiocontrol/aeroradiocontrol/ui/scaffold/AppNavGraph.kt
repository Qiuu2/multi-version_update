package com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.htgd.radiocontrol.aeroradiocontrol.ui.permissions.RequiredPermissionsGate
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.LoginRoute

/**
 * Root NavGraph for the v4 UI.
 *
 *   login → main   (entry route picked by V4Activity via StartupAuthDecider)
 *
 * ★ NEXT-2 (2026-06-01) — the Splash composable + its `// No real token check
 * yet — always send to Login` hack are REMOVED. V4Activity.onCreate now calls
 * [com.htgd.radiocontrol.aeroradiocontrol.data.auth.StartupAuthDecider]
 * synchronously before `setContent` to decide [AppRoutes.Main] vs
 * [AppRoutes.Login], and passes the verdict as [startDestination]. The
 * [AppRoutes.Splash] string constant is retained for backward compatibility;
 * no composable is mapped to it under v4.
 *
 * No back stack from `main` to `login` — logout will clear+pop back to login.
 * The login destination is [LoginRoute] (ViewModel-driven, TASK-AR-005): on a
 * fresh login submission it navigates to Main via the [LoginRoute.onLoggedIn]
 * callback. Pre-NEXT-2, LoginRoute's internal `LaunchedEffect(loggedIn)` also
 * fired on a RESTORED session (process restart with a valid token) — causing
 * the 2026-06-01 kill-app BLOCKER because it bypassed
 * `V3LoginAuthenticator.authenticate()` and so `Constant.serveraddress` stayed
 * null. With the startup decider in place, V4Activity routes a restored session
 * STRAIGHT to Main; on Login bounce `isLoggedIn` is false so the LaunchedEffect
 * is dormant.
 *
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
    startDestination: String = AppRoutes.Login,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
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
