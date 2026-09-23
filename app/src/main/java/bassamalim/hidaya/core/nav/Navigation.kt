package bassamalim.hidaya.core.nav

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import bassamalim.hidaya.core.ui.inFromBottom
import bassamalim.hidaya.core.ui.inFromLeft
import bassamalim.hidaya.core.ui.inFromRight
import bassamalim.hidaya.core.ui.inFromTop
import bassamalim.hidaya.core.ui.outToBottom
import bassamalim.hidaya.core.ui.outToLeft
import bassamalim.hidaya.core.ui.outToTop
import bassamalim.hidaya.features.about.AboutScreen
import bassamalim.hidaya.features.books.bookChaptersMenu.BookChaptersScreen
import bassamalim.hidaya.features.books.bookReader.BookReaderScreen
import bassamalim.hidaya.features.books.bookSearcher.BookSearcherScreen
import bassamalim.hidaya.features.books.booksMenu.BooksMenuScreen
import bassamalim.hidaya.features.books.booksMenuFilter.BooksMenuFilterDialog
import bassamalim.hidaya.features.dateConverter.DateConverterScreen
import bassamalim.hidaya.features.dateEditor.DateEditorDialog
import bassamalim.hidaya.features.hijriDatePicker.HijriDatePickerDialog
import bassamalim.hidaya.features.leaderboard.LeaderboardScreen
import bassamalim.hidaya.features.locationPicker.LocationPickerScreen
import bassamalim.hidaya.features.locator.LocatorScreen
import bassamalim.hidaya.features.main.MainScreen
import bassamalim.hidaya.features.misbaha.MisbahaScreen
import bassamalim.hidaya.features.onboarding.OnboardingScreen
import bassamalim.hidaya.features.prayers.extraReminderSettings.PrayerExtraReminderSettingsDialog
import bassamalim.hidaya.features.prayers.notificationSettings.PrayerNotificationSettingsDialog
import bassamalim.hidaya.features.prayers.timeCalculationSettings.PrayerTimeCalculationSettingsDialog
import bassamalim.hidaya.features.qibla.QiblaScreen
import bassamalim.hidaya.features.quiz.lobby.QuizLobbyScreen
import bassamalim.hidaya.features.quiz.result.QuizResultScreen
import bassamalim.hidaya.features.quiz.test.QuizTestScreen
import bassamalim.hidaya.features.quran.reader.QuranReaderScreen
import bassamalim.hidaya.features.quran.settings.QuranSettingsDialog
import bassamalim.hidaya.features.quran.verseInfo.VerseInfoDialog
import bassamalim.hidaya.features.radio.RadioClientScreen
import bassamalim.hidaya.features.recitations.player.RecitationPlayerScreen
import bassamalim.hidaya.features.recitations.recitersMenu.RecitationRecitersMenuScreen
import bassamalim.hidaya.features.recitations.recitersMenuFilter.RecitersMenuFilterDialog
import bassamalim.hidaya.features.recitations.surasMenu.RecitationSurasMenuScreen
import bassamalim.hidaya.features.remembrances.reader.RemembranceReaderScreen
import bassamalim.hidaya.features.remembrances.remembrancesMenu.RemembrancesMenuScreen
import bassamalim.hidaya.features.settings.SettingsScreen
import bassamalim.hidaya.features.tv.TvScreen

@Composable
fun Navigation(navigator: Navigator, thenTo: String? = null, shouldOnboard: Boolean = false) {
    val navController = rememberNavController()

    // Rebind on every start, not just first composition: with several Activity instances
    // the singleton Navigator must follow the visible one or it navigates a destroyed controller
    LifecycleStartEffect(navController) {
        navigator.setController(navController)
        onStopOrDispose {}
    }
    DisposableEffect(navController) {
        onDispose { navigator.clearController(navController) }
    }

    val startDest =
        if (shouldOnboard) Screen.Onboarding.route
        else Screen.Main.route

    NavGraph(navController = navController, startDest = startDest)

    // Once per Activity, not per recomposition or recreation (back stack is restored then)
    var handledThenTo by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (thenTo != null && !handledThenTo) navController.navigate(thenTo)
        handledThenTo = true
    }
}

@Composable
fun NavGraph(navController: NavHostController, startDest: String) {
    NavHost(navController = navController, startDestination = startDest) {
        composable(
            route = Screen.About.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            AboutScreen(hiltViewModel())
        }

        composable(
            route = Screen.BookChaptersMenu("{book_id}").route,
            arguments = listOf(navArgument("book_id") { type = NavType.IntType }),
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            BookChaptersScreen(hiltViewModel())
        }

        composable(
            route = Screen.BookReader("{book_id}", "{chapter_id}").route,
            arguments = listOf(
                navArgument("book_id") { type = NavType.IntType },
                navArgument("chapter_id") { type = NavType.IntType }
            ),
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            BookReaderScreen(hiltViewModel())
        }

        composable(
            route = Screen.BookSearcher.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            BookSearcherScreen(hiltViewModel())
        }

        composable(
            route = Screen.BooksMenu.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            BooksMenuScreen(hiltViewModel())
        }

        dialog(route = Screen.BooksMenuFilter.route) {
            BooksMenuFilterDialog(hiltViewModel())
        }

        composable(
            route = Screen.DateConverter.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            DateConverterScreen(hiltViewModel())
        }

        dialog(route = Screen.DateEditor.route) {
            DateEditorDialog(hiltViewModel())
        }

        dialog(route = Screen.HijriDatePicker("{initial_date}").route) {
            HijriDatePickerDialog(hiltViewModel())
        }

        composable(
            route = Screen.Leaderboard.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            LeaderboardScreen(hiltViewModel())
        }

        composable(
            route = Screen.LocationPicker.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            LocationPickerScreen(hiltViewModel())
        }

        composable(
            route = Screen.Locator("{is_initial}").route,
            arguments = listOf(navArgument("is_initial") { type = NavType.BoolType }),
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            LocatorScreen(hiltViewModel())
        }

        composable(
            route = Screen.Main.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            MainScreen(hiltViewModel())
        }

        composable(
            route = Screen.Misbaha.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            MisbahaScreen(hiltViewModel())
        }

        composable(
            route = Screen.Onboarding.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            OnboardingScreen(hiltViewModel())
        }

        dialog(
            route = Screen.PrayerExtraReminderSettings("{prayer_name}").route,
            arguments = listOf(navArgument("prayer_name") { type = NavType.StringType })
        ) {
            PrayerExtraReminderSettingsDialog(hiltViewModel())
        }

        dialog(
            route = Screen.PrayerSettings("{prayer_name}").route,
            arguments = listOf(navArgument("prayer_name") { type = NavType.StringType })
        ) {
            PrayerNotificationSettingsDialog(hiltViewModel())
        }

        dialog(route = Screen.PrayerTimeCalculationSettings.route) {
            PrayerTimeCalculationSettingsDialog(hiltViewModel())
        }

        composable(
            route = Screen.Qibla.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            QiblaScreen(hiltViewModel())
        }

        composable(
            route = Screen.QuizLobby.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            QuizLobbyScreen(hiltViewModel())
        }

        composable(
            route = Screen.QuizResult.route,
            enterTransition = inFromLeft,
            exitTransition = outToLeft,
            popEnterTransition = inFromRight,
            popExitTransition = outToBottom
        ) {
            QuizResultScreen(hiltViewModel())
        }

        composable(
            route = Screen.QuizTest("{category}").route,
            arguments = listOf(navArgument("category") { type = NavType.StringType }),
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            QuizTestScreen(hiltViewModel())
        }

        composable(
            route = Screen.QuranReader("{target_type}", "{target_value}").route,
            arguments = listOf(
                navArgument("target_type") { type = NavType.StringType },
                navArgument("target_value") { type = NavType.IntType },
            ),
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            QuranReaderScreen(hiltViewModel())
        }

        dialog(route = Screen.QuranSettings.route) {
            QuranSettingsDialog(hiltViewModel())
        }

        composable(
            route = Screen.Radio.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                RadioClientScreen(hiltViewModel())
            }
        }

        composable(
            route = Screen.RecitationPlayer("{action}", "{media_id}").route,
            arguments = listOf(
                navArgument("action") { type = NavType.StringType },
                navArgument("media_id") { type = NavType.StringType }
            ),
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                RecitationPlayerScreen(hiltViewModel())
            }
        }

        dialog(route = Screen.RecitersMenuFilter.route) {
            RecitersMenuFilterDialog(hiltViewModel())
        }

        composable(
            route = Screen.RecitationsRecitersMenu.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                RecitationRecitersMenuScreen(hiltViewModel())
            }
        }

        composable(
            route = Screen.RecitationSurasMenu("{reciter_id}", "{narration_id}").route,
            arguments = listOf(
                navArgument("reciter_id") { type = NavType.IntType },
                navArgument("narration_id") { type = NavType.IntType }
            ),
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            RecitationSurasMenuScreen(hiltViewModel())
        }

        composable(
            route = Screen.RemembranceReader("{remembrance_id}").route,
            arguments = listOf(navArgument("remembrance_id") { type = NavType.IntType }),
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            RemembranceReaderScreen(hiltViewModel())
        }

        composable(
            route = Screen.RemembrancesMenu("{type}", "{category_id}").route,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("category_id") { type = NavType.IntType }
            ),
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            RemembrancesMenuScreen(hiltViewModel())
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            SettingsScreen(hiltViewModel())
        }

        composable(
            route = Screen.Tv.route,
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            TvScreen(hiltViewModel())
        }

        dialog(
            route = Screen.VerseInfo("{verse_id}").route,
            arguments = listOf(navArgument("verse_id") { type = NavType.IntType })
        ) {
            VerseInfoDialog(hiltViewModel())
        }
    }
}