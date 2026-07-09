package com.mundial26.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mundial26.app.ui.components.WorldCupLoader
import com.mundial26.app.ui.home.HomeUiState
import com.mundial26.app.notifications.Notif
import com.mundial26.app.notifications.syncMatchAlarms
import com.mundial26.app.ui.notifications.NotificationsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
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
import com.mundial26.app.ui.teams.TeamsView
import com.mundial26.app.ui.teams.TeamsViewModel

@Composable
fun MundialApp(container: AppContainer) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()
    val currentRouteStr = currentRoute?.destination?.route
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext

    // Canales de notificación + reprogramar alarmas de partidos al abrir (única oportunidad de
    // reprogramar tras un reinicio del teléfono, ya que no hay receiver de BOOT).
    LaunchedEffect(Unit) {
        Notif.ensureChannels(appContext)
        runCatching { syncMatchAlarms(appContext) }
    }

    // Tap en una notificación (recordatorio o gol) → abrir el detalle de ese partido.
    val pendingMatch by MainActivity.pendingMatchId
    LaunchedEffect(pendingMatch) {
        pendingMatch?.let {
            navController.navigate(DetailRoutes.matchDetail(it))
            MainActivity.pendingMatchId.value = null
        }
    }

    // Las noticias reales del RSS (id = URL) abren directo en el navegador del teléfono;
    // las de muestra (sin URL) siguen yendo al detalle interno.
    val openNews: (String) -> Unit = { id ->
        if (id.startsWith("http")) runCatching { uriHandler.openUri(id) }
        else navController.navigate(DetailRoutes.newsDetail(id))
    }

    // ViewModel de Inicio compartido: se crea en el arranque para poder mantener el intro
    // hasta que Home tenga contenido real (evita el salto intro → spinner → contenido) y para
    // no recargar al cambiar de pestaña.
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            container.matchRepository,
            container.teamRepository,
            container.newsRepository
        )
    )

    // Intro a pantalla completa en el arranque en frío: se mantiene hasta que Home esté
    // completamente listo, incluido el carrusel de resúmenes (highlightsReady), para que todo
    // aparezca a la vez. Mínimo visible ~2,5 s y tope de 12 s por si la red va lenta.
    var booting by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        runCatching {
            withTimeoutOrNull(12000) {
                homeViewModel.uiState.first { it is HomeUiState.Success && it.highlightsReady }
            }
        }
        val elapsed = System.currentTimeMillis() - start
        if (elapsed < 2500) delay(2500 - elapsed)
        booting = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    onNewsClick = openNews,
                    onTeamClick = { id -> navController.navigate(DetailRoutes.teamDetail(id)) },
                    onLeadersClick = { navController.navigate(DetailRoutes.LEADERS) },
                    onSeeTable = {
                        TeamsViewModel.requestedView = TeamsView.STANDINGS
                        navController.navigate(TopDestination.TEAMS.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(TopDestination.HOME.route) { saveState = true }
                        }
                    },
                    onSeeBracket = {
                        TeamsViewModel.requestedView = TeamsView.BRACKET
                        navController.navigate(TopDestination.TEAMS.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(TopDestination.HOME.route) { saveState = true }
                        }
                    },
                    onNotifClick = { navController.navigate(DetailRoutes.NOTIFICATIONS) }
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
                val vm: TeamsViewModel = viewModel(factory = TeamsViewModel.Factory(container.teamRepository, container.matchRepository))
                TeamsScreen(
                    viewModel = vm,
                    onTeamClick = { id -> navController.navigate(DetailRoutes.teamDetail(id)) },
                    onMatchClick = { id -> navController.navigate(DetailRoutes.matchDetail(id)) }
                )
            }
            composable(TopDestination.NEWS.route) {
                val vm: NewsViewModel = viewModel(factory = NewsViewModel.Factory(container.newsRepository))
                NewsScreen(
                    viewModel = vm,
                    onNewsClick = openNews
                )
            }
            composable(
                route = DetailRoutes.MATCH_DETAIL,
                arguments = listOf(navArgument(DetailRoutes.MATCH_ARG) { type = NavType.StringType })
            ) { entry ->
                val id = entry.arguments?.getString(DetailRoutes.MATCH_ARG).orEmpty()
                val vm: MatchDetailViewModel = viewModel(
                    factory = MatchDetailViewModel.Factory(container.matchRepository, container.teamRepository, id)
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
            composable(DetailRoutes.NOTIFICATIONS) {
                NotificationsScreen(onBack = { navController.popBackStack() })
            }
        }
    }

        if (booting) {
            WorldCupLoader(modifier = Modifier.fillMaxSize())
        }
    }
}
