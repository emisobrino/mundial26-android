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
 * Rutas de detalle para tareas futuras. No se registran todavía en el NavHost.
 */
object DetailRoutes {
    const val MATCH_DETAIL = "matchDetail/{id}"
    const val TEAM_DETAIL = "teamDetail/{id}"
    const val NEWS_DETAIL = "newsDetail/{id}"
}
