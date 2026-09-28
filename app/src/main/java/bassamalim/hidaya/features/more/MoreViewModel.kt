package bassamalim.hidaya.features.more

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val navigator: Navigator
): ViewModel() {

    fun onRecitationsClick(snackBarHostState: SnackbarHostState, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            navigator.navigate(Screen.RecitationsRecitersMenu)
        else showUnsupported(snackBarHostState, message)
    }

    fun onQiblaClick() {
        navigator.navigate(Screen.Qibla)
    }

    fun onQuizClick() {
        navigator.navigate(Screen.QuizLobby)
    }

    fun onVerseGuessClick() {
        navigator.navigate(Screen.VerseGuess)
    }

    fun onBooksClick() {
        navigator.navigate(Screen.BooksMenu)
    }

    fun onTvClick() {
        navigator.navigate(Screen.Tv)
    }

    fun onRadioClick(snackBarHostState: SnackbarHostState, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            navigator.navigate(Screen.Radio)
        else showUnsupported(snackBarHostState, message)
    }

    fun onMisbahaClick() {
        navigator.navigate(Screen.Misbaha)
    }

    fun onDateConverterClick() {
        navigator.navigate(Screen.DateConverter)
    }

    fun onSettingsClick() {
        navigator.navigate(Screen.Settings)
    }

    fun onContactClick(context: Context) {
        val contactIntent = Intent(
            Intent.ACTION_SENDTO,
            Uri.fromParts("mailto", Globals.CONTACT_EMAIL, null)
        ).apply {
            putExtra(Intent.EXTRA_SUBJECT, "Hidaya")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(contactIntent, "Choose an Email client :"))
    }

    fun onShareClick(context: Context) {
        val sharingIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "App Share")
            putExtra(Intent.EXTRA_TEXT, Globals.PLAY_STORE_URL)
        }
        context.startActivity(Intent.createChooser(sharingIntent, "Share via"))
    }

    fun onAboutClick() {
        navigator.navigate(Screen.About)
    }

    private fun showUnsupported(snackBarHostState: SnackbarHostState, message: String) {
        viewModelScope.launch {
            snackBarHostState.showSnackbar(message)
        }
    }

}