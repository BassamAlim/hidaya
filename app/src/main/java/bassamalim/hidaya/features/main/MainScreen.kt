package bassamalim.hidaya.features.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import bassamalim.hidaya.core.ui.components.MyText
import bassamalim.hidaya.core.ui.theme.nsp
import bassamalim.hidaya.features.home.HomeScreen
import bassamalim.hidaya.features.more.MoreScreen
import bassamalim.hidaya.features.prayers.board.PrayersBoardScreen
import bassamalim.hidaya.features.quran.surasMenu.QuranSurasMenuScreen
import bassamalim.hidaya.features.remembrances.categoriesMenu.RemembranceCategoriesScreen

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
    CenterAlignedTopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDateClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MyText(text = hijriDate, fontSize = 16.nsp, fontWeight = FontWeight.Bold)

                MyText(text = gregorianDate, fontSize = 16.nsp, fontWeight = FontWeight.Bold)
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