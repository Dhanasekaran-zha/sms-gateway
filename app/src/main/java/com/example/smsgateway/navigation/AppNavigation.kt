package com.example.smsgateway.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smsgateway.ui.dashboard.DashboardScreen
import com.example.smsgateway.ui.manual.ManualSmsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val MANUAL_SMS = "manual_sms"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToManualSms = {
                    navController.navigate(Routes.MANUAL_SMS)
                },
            )
        }
        composable(Routes.MANUAL_SMS) {
            ManualSmsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
