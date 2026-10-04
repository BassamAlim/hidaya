package bassamalim.hidaya.features.more

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.nav.Navigator
import bassamalim.hidaya.core.nav.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val navigator: Navigator
): ViewModel() {

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

    fun onRadioClick() {
        navigator.navigate(Screen.Radio)
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

}