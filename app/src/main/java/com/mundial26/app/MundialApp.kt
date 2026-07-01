package com.mundial26.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mundial26.app.di.AppContainer
import com.mundial26.app.ui.home.HomeScreen
import com.mundial26.app.ui.home.HomeViewModel
import com.mundial26.app.ui.leaders.LeadersScreen
import com.mundial26.app.ui.leaders.LeadersViewModel
import com.mundial26.app.ui.matchdetail.MatchDetailScreen
import com.mundial26.app.ui.matchdetail.MatchDetailViewModel
import com.mundial26.app.ui.matches.MatchesScreen
import com.mundial26.app.ui.matches.MatchesViewModel
import com.mundial26.app.ui.navigation.BottomNav
import com.mundial26.app.ui.navigation.DetailRoutes
import com.mundial26.app.ui.navigation.TopDestination
import com.mundial26.app.ui.news.NewsScreen
import com.mundial26.app.ui.news.NewsViewModel
import com.mundial26.app.ui.newsdetail.NewsDetailScreen
import com.mundial26.app.ui.newsdetail.NewsDetailViewModel
import com.mundial26.app.ui.playerdetail.PlayerDetailScreen
import com.mundial26.app.ui.playerdetail.PlayerDetailViewModel
import com.mundial26.app.ui.teamdetail.TeamDetailScreen
import com.mundial26.app.ui.teamdetail.TeamDetailViewModel
import com.mundial26.app.ui.teams.TeamsScreen
import com.mundial26.app.ui.teams.TeamsViewModel

@Composable
fun MundialApp(container: AppContainer) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()
    val currentRouteStr = currentRoute?.destination?.route

    Scaffold(
        bottomBar = {
            if (!DetailRoutes.isDetail(currentRouteStr)) {
                BottomNav(
                    currentRoute = currentRouteStr,
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
                    },
                    onMatchClick = { id -> navController.navigate(DetailRoutes.matchDetail(id)) },
                    onNewsClick = { id -> navController.navigate(DetailRoutes.newsDetail(id)) },
                    onTeamClick = { id -> navController.navigate(DetailRoutes.teamDetail(id)) },
                    onLeadersClick = { navController.navigate(DetailRoutes.LEADERS) }
                )
            }
            composable(TopDestination.MATCHES.route) {
                val vm: MatchesViewModel = viewModel(factory = MatchesViewModel.Factory(container.matchRepository))
                MatchesScreen(
                    viewModel = vm,
                    onMatchClick = { id -> navController.navigate(DetailRoutes.matchDetail(id)) }
                )
            }
            composable(TopDestination.TEAMS.route) {
                val vm: TeamsViewModel = viewModel(factory = TeamsViewModel.Factory(container.teamRepository))
                TeamsScreen(
                    viewModel = vm,
                    onTeamClick = { id -> navController.navigate(DetailRoutes.teamDetail(id)) }
                )
            }
            composable(TopDestination.NEWS.route) {
                val vm: NewsViewModel = viewModel(factory = NewsViewModel.Factory(container.newsRepository))
                NewsScreen(
                    viewModel = vm,
                    onNewsClick = { id -> navController.navigate(DetailRoutes.newsDetail(id)) }
                )
            }
            composable(
                route = DetailRoutes.MATCH_DETAIL,
                arguments = listOf(navArgument(DetailRoutes.MATCH_ARG) { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString(DetailRoutes.MATCH_ARG).orEmpty()
                val vm: MatchDetailViewModel = viewModel(
                    factory = MatchDetailViewModel.Factory(container.matchRepository, id)
                )
                MatchDetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
            composable(
                route = DetailRoutes.NEWS_DETAIL,
                arguments = listOf(navArgument(DetailRoutes.NEWS_ARG) { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString(DetailRoutes.NEWS_ARG).orEmpty()
                val vm: NewsDetailViewModel = viewModel(
                    factory = NewsDetailViewModel.Factory(container.newsRepository, id)
                )
                NewsDetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
            composable(
                route = DetailRoutes.TEAM_DETAIL,
                arguments = listOf(navArgument(DetailRoutes.TEAM_ARG) { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString(DetailRoutes.TEAM_ARG).orEmpty()
                val vm: TeamDetailViewModel = viewModel(
                    factory = TeamDetailViewModel.Factory(container.teamRepository, id)
                )
                TeamDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onPlayerClick = { teamId -> navController.navigate(DetailRoutes.playerDetail(teamId)) },
                    onSquadPlayerClick = { playerId -> navController.navigate(DetailRoutes.playerById(playerId)) }
                )
            }
            composable(
                route = DetailRoutes.PLAYER_DETAIL,
                arguments = listOf(navArgument(DetailRoutes.PLAYER_ARG) { type = NavType.StringType })
            ) { entry ->
                val teamId = entry.arguments?.getString(DetailRoutes.PLAYER_ARG).orEmpty()
                val vm: PlayerDetailViewModel = viewModel(
                    factory = PlayerDetailViewModel.Factory(container.teamRepository, teamId)
                )
                PlayerDetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
            composable(
                route = DetailRoutes.PLAYER_BY_ID,
                arguments = listOf(navArgument(DetailRoutes.PLAYER_ID_ARG) { type = NavType.LongType })
            ) { entry ->
                val playerId = entry.arguments?.getLong(DetailRoutes.PLAYER_ID_ARG) ?: 0L
                val vm: PlayerDetailViewModel = viewModel(
                    factory = PlayerDetailViewModel.FactoryForPlayer(container.teamRepository, playerId)
                )
                PlayerDetailScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
            composable(DetailRoutes.LEADERS) {
                val vm: LeadersViewModel = viewModel(factory = LeadersViewModel.Factory(container.teamRepository))
                LeadersScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
        }
    }
}
