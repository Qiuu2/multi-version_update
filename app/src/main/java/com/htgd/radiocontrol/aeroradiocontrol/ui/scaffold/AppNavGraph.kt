package com.htgd.radiocontrol.aeroradiocontrol.ui.scaffold

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.LoginScreen
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.auth.SplashScreen
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task.ExecutionLogScreen
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task.SchemeDetailScreen
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task.SchemeEditScreen
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.task.TempFileBroadcastScreen
import com.htgd.radiocontrol.aeroradiocontrol.ui.screens.terminal.ZoneDetailScreen

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
            MainScaffold(
                onSettingsClick = onSettingsClick,
                onOpenZone = { zoneId -> navController.navigate(AppRoutes.zoneDetail(zoneId)) },
                onOpenSchemeDetail = { id -> navController.navigate(AppRoutes.schemeDetail(id)) },
                onOpenSchemeEdit = { id -> navController.navigate(AppRoutes.schemeEdit(id)) },
                onOpenLog = { navController.navigate(AppRoutes.EXEC_LOG) },
                onOpenTempBroadcast = { navController.navigate(AppRoutes.TEMP_BROADCAST) },
            )
        }
        composable(
            route = AppRoutes.ZONE_DETAIL,
            arguments = listOf(navArgument(AppRoutes.ZONE_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val zoneId = backStackEntry.arguments?.getString(AppRoutes.ZONE_ARG).orEmpty()
            ZoneDetailScreen(zoneId = zoneId, onBack = { navController.popBackStack() })
        }
        composable(
            route = AppRoutes.SCHEME_DETAIL,
            arguments = listOf(navArgument(AppRoutes.SCHEME_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val schemeId = backStackEntry.arguments?.getString(AppRoutes.SCHEME_ARG).orEmpty()
            SchemeDetailScreen(
                schemeId = schemeId,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(AppRoutes.schemeEdit(id)) },
            )
        }
        composable(
            route = AppRoutes.SCHEME_EDIT,
            arguments = listOf(navArgument(AppRoutes.SCHEME_ARG) { type = NavType.StringType }),
        ) { backStackEntry ->
            val schemeId = backStackEntry.arguments?.getString(AppRoutes.SCHEME_ARG).orEmpty()
            SchemeEditScreen(schemeId = schemeId, onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.EXEC_LOG) {
            ExecutionLogScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.TEMP_BROADCAST) {
            TempFileBroadcastScreen(onBack = { navController.popBackStack() })
        }
    }
}
