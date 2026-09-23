package bassamalim.hidaya.features.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import bassamalim.hidaya.core.ui.TabEnter
import bassamalim.hidaya.core.ui.TabExit
import bassamalim.hidaya.core.ui.TabPopEnter
import bassamalim.hidaya.core.ui.TabPopExit
import bassamalim.hidaya.core.ui.theme.appTypography
import bassamalim.hidaya.core.ui.theme.dimensions
import bassamalim.hidaya.features.home.HomeScreen
import bassamalim.hidaya.features.more.MoreScreen
import bassamalim.hidaya.features.prayers.board.PrayersBoardScreen
import bassamalim.hidaya.features.quran.surasMenu.QuranSurasMenuScreen
import bassamalim.hidaya.features.remembrances.categoriesMenu.RemembranceCategoriesScreen

private val DateBarHeight = 44.dp

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomNavController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopBar(
                hijriDate = state.hijriDate,
                gregorianDate = state.gregorianDate,
                onDateClick = viewModel::onDateClick
            )
        },
        bottomBar = { MyBottomNavigation(bottomNavController) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        NavigationGraph(
            bottomNavController = bottomNavController,
            snackbarHostState = snackbarHostState,
            padding = padding
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(hijriDate: String, gregorianDate: String, onDateClick: () -> Unit) {
    val dims = MaterialTheme.dimensions

    CenterAlignedTopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(DateBarHeight),
        title = {
            // A pill around just the dates, so the press ripple reads as a button instead of
            // a thin full-width strip
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onDateClick)
                    .padding(horizontal = dims.spaceMd, vertical = dims.spaceXs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hijri first: it's the date the app is built around
                // Stands out by weight rather than size, which keeps the bar compact
                Text(
                    text = hijriDate,
                    style = MaterialTheme.appTypography.label.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )

                // Gives way first on narrow screens so the Hijri date always shows in full
                Text(
                    text = "  ·  $gregorianDate",
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.appTypography.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.width(dims.spaceSm))

                // Hints that tapping the dates opens the date adjustment
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = null,
                    modifier = Modifier.size(dims.iconSm),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun NavigationGraph(
    bottomNavController: NavHostController,
    snackbarHostState: SnackbarHostState,
    padding: PaddingValues
) {
    NavHost(
        navController = bottomNavController,
        startDestination = BottomNavItem.Home.route,
        modifier = Modifier.padding(padding)
    ) {
        composable(
            route = BottomNavItem.Home.route,
            enterTransition = TabEnter,
            exitTransition = TabExit,
            popEnterTransition = TabPopEnter,
            popExitTransition = TabPopExit
        ) {
            HomeScreen(
                viewModel = hiltViewModel(),
                bottomNavController = bottomNavController
            )
        }

        composable(
            route = BottomNavItem.PrayersBoard.route,
            enterTransition = TabEnter,
            exitTransition = TabExit,
            popEnterTransition = TabPopEnter,
            popExitTransition = TabPopExit
        ) {
            PrayersBoardScreen(
                hiltViewModel()
            )
        }

        composable(
            route = BottomNavItem.QuranSuras.route,
            enterTransition = TabEnter,
            exitTransition = TabExit,
            popEnterTransition = TabPopEnter,
            popExitTransition = TabPopExit
        ) {
            QuranSurasMenuScreen(hiltViewModel())
        }

        composable(
            route = BottomNavItem.RemembranceCategories.route,
            enterTransition = TabEnter,
            exitTransition = TabExit,
            popEnterTransition = TabPopEnter,
            popExitTransition = TabPopExit
        ) {
            RemembranceCategoriesScreen(
                hiltViewModel()
            )
        }

        composable(
            route = BottomNavItem.More.route,
            enterTransition = TabEnter,
            exitTransition = TabExit,
            popEnterTransition = TabPopEnter,
            popExitTransition = TabPopExit
        ) {
            MoreScreen(
                viewModel = hiltViewModel(),
                snackBarHostState = snackbarHostState
            )
        }
    }
}