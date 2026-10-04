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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import bassamalim.hidaya.core.ui.inFromBottom
import bassamalim.hidaya.core.ui.inFromTop
import bassamalim.hidaya.core.ui.outToBottom
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
import bassamalim.hidaya.features.verseGuess.VerseGuessScreen

@Composable
fun Navigation(navigator: Navigator, thenTo: Screen? = null, shouldOnboard: Boolean = false) {
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
        if (shouldOnboard) Screen.Onboarding
        else Screen.Main

    NavGraph(navController = navController, startDest = startDest)

    // Once per Activity, not per recomposition or recreation (back stack is restored then)
    var handledThenTo by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (thenTo != null && !handledThenTo) navController.navigate(thenTo)
        handledThenTo = true
    }
}

@Composable
fun NavGraph(navController: NavHostController, startDest: Screen) {
    NavHost(navController = navController, startDestination = startDest) {
        composable<Screen.About>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            AboutScreen(hiltViewModel())
        }

        composable<Screen.BookChaptersMenu>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            BookChaptersScreen(hiltViewModel())
        }

        composable<Screen.BookReader>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            BookReaderScreen(hiltViewModel())
        }

        composable<Screen.BookSearcher>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            BookSearcherScreen(hiltViewModel())
        }

        composable<Screen.BooksMenu>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            BooksMenuScreen(hiltViewModel())
        }

        dialog<Screen.BooksMenuFilter> {
            BooksMenuFilterDialog(hiltViewModel())
        }

        composable<Screen.DateConverter>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            DateConverterScreen(hiltViewModel())
        }

        dialog<Screen.DateEditor> {
            DateEditorDialog(hiltViewModel())
        }

        dialog<Screen.HijriDatePicker> {
            HijriDatePickerDialog(hiltViewModel())
        }

        composable<Screen.Leaderboard>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            LeaderboardScreen(hiltViewModel())
        }

        composable<Screen.LocationPicker>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            LocationPickerScreen(hiltViewModel())
        }

        composable<Screen.Locator>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            LocatorScreen(hiltViewModel())
        }

        composable<Screen.Main>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            MainScreen(hiltViewModel())
        }

        composable<Screen.Misbaha>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            MisbahaScreen(hiltViewModel())
        }

        composable<Screen.Onboarding>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            OnboardingScreen(hiltViewModel())
        }

        dialog<Screen.PrayerExtraReminderSettings> {
            PrayerExtraReminderSettingsDialog(hiltViewModel())
        }

        dialog<Screen.PrayerSettings> {
            PrayerNotificationSettingsDialog(hiltViewModel())
        }

        dialog<Screen.PrayerTimeCalculationSettings> {
            PrayerTimeCalculationSettingsDialog(hiltViewModel())
        }

        composable<Screen.Qibla>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            QiblaScreen(hiltViewModel())
        }

        composable<Screen.QuizLobby>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            QuizLobbyScreen(hiltViewModel())
        }

        composable<Screen.VerseGuess>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            VerseGuessScreen(hiltViewModel())
        }

        composable<Screen.QuizTest>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            QuizTestScreen(hiltViewModel())
        }

        composable<Screen.QuranReader>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            QuranReaderScreen(hiltViewModel())
        }

        dialog<Screen.QuranSettings> {
            QuranSettingsDialog(hiltViewModel())
        }

        composable<Screen.Radio>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                RadioClientScreen(hiltViewModel())
            }
        }

        composable<Screen.RecitationPlayer>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                RecitationPlayerScreen(hiltViewModel())
            }
        }

        dialog<Screen.RecitersMenuFilter> {
            RecitersMenuFilterDialog(hiltViewModel())
        }

        composable<Screen.RecitationsRecitersMenu>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                RecitationRecitersMenuScreen(hiltViewModel())
            }
        }

        composable<Screen.RecitationSurasMenu>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            RecitationSurasMenuScreen(hiltViewModel())
        }

        composable<Screen.RemembranceReader>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            RemembranceReaderScreen(hiltViewModel())
        }

        composable<Screen.RemembrancesMenu>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            RemembrancesMenuScreen(hiltViewModel())
        }

        composable<Screen.Settings>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            SettingsScreen(hiltViewModel())
        }

        composable<Screen.Tv>(
            enterTransition = inFromBottom,
            exitTransition = outToBottom,
            popEnterTransition = inFromTop,
            popExitTransition = outToTop
        ) {
            TvScreen(hiltViewModel())
        }

        dialog<Screen.VerseInfo> {
            VerseInfoDialog(hiltViewModel())
        }
    }
}