package com.mundial26.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mundial26.app.di.AppContainer
import com.mundial26.app.ui.home.HomeScreen
import com.mundial26.app.ui.home.HomeViewModel
import com.mundial26.app.ui.navigation.BottomNav
import com.mundial26.app.ui.navigation.TopDestination
import com.mundial26.app.ui.theme.MundialTheme

@Composable
fun MundialApp(container: AppContainer) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()

    Scaffold(
        bottomBar = {
            BottomNav(
                currentRoute = currentRoute?.destination?.route,
                onNavigate = { dest ->
                    navController.navigate(dest.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(TopDestination.HOME.route) {
                            saveState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopDestination.HOME.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(
                        container.matchRepository,
                        container.teamRepository,
                        container.newsRepository
                    )
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToTab = { route ->
                        navController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(TopDestination.HOME.route) { saveState = true }
                        }
                    }
                )
            }
            composable(TopDestination.MATCHES.route) { TabPlaceholder(TopDestination.MATCHES.label) }
            composable(TopDestination.TEAMS.route) { TabPlaceholder(TopDestination.TEAMS.label) }
            composable(TopDestination.NEWS.route) { TabPlaceholder(TopDestination.NEWS.label) }
        }
    }
}

@Composable
private fun TabPlaceholder(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MundialTheme.colors.appBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineSmall,
            color = MundialTheme.colors.brand
        )
    }
}
