package com.mundial26.app.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mundial26.app.ui.theme.MundialTheme

@Composable
fun BottomNav(currentRoute: String?, onNavigate: (TopDestination) -> Unit) {
    NavigationBar(containerColor = MundialTheme.colors.surface) {
        TopDestination.entries.forEach { dest ->
            val selected = currentRoute == dest.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest) },
                icon = { Icon(imageVector = dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MundialTheme.colors.brand,
                    selectedTextColor = MundialTheme.colors.brand,
                    unselectedIconColor = MundialTheme.colors.navInactive,
                    unselectedTextColor = MundialTheme.colors.navInactive,
                    indicatorColor = MundialTheme.colors.surface
                )
            )
        }
    }
}
