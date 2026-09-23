package bassamalim.hidaya.features.main

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTimeFilled
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import bassamalim.hidaya.R
import bassamalim.hidaya.core.ui.theme.tajwal

sealed class BottomNavItem(var route: String, var titleRId: Int, var icon: Any) {
    data object Home:
        BottomNavItem("home", R.string.title_home, Icons.Default.Home)
    data object PrayersBoard:
        BottomNavItem("prayers", R.string.title_prayers, Icons.Default.AccessTimeFilled)
    data object QuranSuras:
        BottomNavItem("quran", R.string.title_quran, R.drawable.ic_bar_quran)
    data object RemembranceCategories:
        BottomNavItem("remembrances", R.string.title_remembrances, R.drawable.ic_dua)
    data object More:
        BottomNavItem("more", R.string.title_more, Icons.Default.MoreHoriz)
}

@Composable
fun MyBottomNavigation(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.PrayersBoard,
        BottomNavItem.QuranSuras,
        BottomNavItem.RemembranceCategories,
        BottomNavItem.More
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val title = stringResource(item.titleRId)

            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { screenRoute ->
                            popUpTo(screenRoute) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    if (item.icon is ImageVector) {
                        Icon(
                            imageVector = item.icon as ImageVector,
                            contentDescription = title,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else if (item.icon is Int) {
                        Icon(
                            painter = painterResource(item.icon as Int),
                            contentDescription = title,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = { Text(text = title, fontFamily = tajwal) },
                alwaysShowLabel = true
            )
        }
    }
}