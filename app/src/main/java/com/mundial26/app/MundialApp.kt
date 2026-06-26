package com.mundial26.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mundial26.app.di.AppContainer
import com.mundial26.app.ui.home.HomeScreen
import com.mundial26.app.ui.home.HomeViewModel
import com.mundial26.app.ui.matches.MatchesScreen
import com.mundial26.app.ui.matches.MatchesViewModel
import com.mundial26.app.ui.navigation.BottomNav
import com.mundial26.app.ui.navigation.TopDestination
import com.mundial26.app.ui.news.NewsScreen
import com.mundial26.app.ui.news.NewsViewModel
import com.mundial26.app.ui.teams.TeamsScreen
import com.mundial26.app.ui.teams.TeamsViewModel

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
            composable(TopDestination.MATCHES.route) {
                val vm: MatchesViewModel = viewModel(factory = MatchesViewModel.Factory(container.matchRepository))
                MatchesScreen(viewModel = vm)
            }
            composable(TopDestination.TEAMS.route) {
                val vm: TeamsViewModel = viewModel(factory = TeamsViewModel.Factory(container.teamRepository))
                TeamsScreen(viewModel = vm)
            }
            composable(TopDestination.NEWS.route) {
                val vm: NewsViewModel = viewModel(factory = NewsViewModel.Factory(container.newsRepository))
                NewsScreen(viewModel = vm)
            }
        }
    }
}
