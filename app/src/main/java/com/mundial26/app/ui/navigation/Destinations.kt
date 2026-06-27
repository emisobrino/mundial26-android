package com.mundial26.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Pestañas raíz de la bottom navigation.
 */
enum class TopDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Inicio", Icons.Outlined.Home),
    MATCHES("matches", "Partidos", Icons.Outlined.SportsSoccer),
    TEAMS("teams", "Equipos", Icons.Outlined.Shield),
    NEWS("news", "Noticias", Icons.AutoMirrored.Outlined.Article)
}

/**
 * Rutas de detalle. La parte `{...}` es el nombre del argumento de navegación.
 */
object DetailRoutes {
    const val MATCH_ARG = "id"
    const val TEAM_ARG = "id"
    const val PLAYER_ARG = "teamId"
    const val NEWS_ARG = "id"

    const val MATCH_DETAIL = "matchDetail/{id}"
    const val TEAM_DETAIL = "teamDetail/{id}"
    const val PLAYER_DETAIL = "playerDetail/{teamId}"
    const val NEWS_DETAIL = "newsDetail/{id}"

    fun matchDetail(id: String) = "matchDetail/$id"
    fun teamDetail(id: String) = "teamDetail/$id"
    fun playerDetail(teamId: String) = "playerDetail/$teamId"
    fun newsDetail(id: String) = "newsDetail/$id"

    /** ¿La ruta actual es una pantalla de detalle? (para ocultar la bottom nav) */
    fun isDetail(route: String?): Boolean =
        route != null && (route.startsWith("matchDetail/") ||
            route.startsWith("teamDetail/") ||
            route.startsWith("playerDetail/") ||
            route.startsWith("newsDetail/"))
}
