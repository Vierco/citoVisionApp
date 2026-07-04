package dev.lovelace.citovision.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.lovelace.citovision.presentation.navigation.routes.LoginRoute
import dev.lovelace.citovision.presentation.navigation.routes.MainRoute
import dev.lovelace.citovision.presentation.navigation.routes.SettingsRoute
import dev.lovelace.citovision.presentation.navigation.routes.SplashRoute
import dev.lovelace.citovision.presentation.screens.LoginScreen
import dev.lovelace.citovision.presentation.screens.MainScreen
import dev.lovelace.citovision.presentation.screens.SettingsScreen
import dev.lovelace.citovision.presentation.screens.SplashScreen
import dev.lovelace.citovision.presentation.viewmodels.LoginViewModel
import dev.lovelace.citovision.presentation.viewmodels.SplashViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = SplashRoute,
    ) {
        composable<SplashRoute> {
            val viewModel = koinViewModel<SplashViewModel>()
            LaunchedEffect(Unit) {
                viewModel.navigationEvents.collect { event ->
                    if (event == NavigationEvent.ToLogin) {
                        navController.navigate(LoginRoute) {
                            popUpTo<SplashRoute> { inclusive = true }
                        }
                    }
                }
            }
            SplashScreen()
        }

        composable<LoginRoute> {
            val viewModel = koinViewModel<LoginViewModel>()
            LaunchedEffect(Unit) {
                viewModel.navigationEvents.collect { event ->
                    if (event == NavigationEvent.ToMain) {
                        navController.navigate(MainRoute) {
                            popUpTo<LoginRoute> { inclusive = true }
                        }
                    }
                }
            }
            LoginScreen(
                onLoginClick = viewModel::onLoginClick,
                onGoogleLoginClick = viewModel::onGoogleLoginClick,
                onGuestClick = viewModel::onGuestClick,
            )
        }

        composable<MainRoute> {
            MainScreen(
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen()
        }
    }
}
