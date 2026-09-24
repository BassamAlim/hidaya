package bassamalim.hidaya.features.about

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import bassamalim.hidaya.core.data.dataSources.room.AppDatabase
import bassamalim.hidaya.core.data.repositories.AppStateRepository
import bassamalim.hidaya.core.data.repositories.BooksRepository
import bassamalim.hidaya.core.data.repositories.PrayersRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.utils.ActivityUtils
import bassamalim.hidaya.core.utils.DbUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AboutDomain @Inject constructor(
    private val appStateRepository: AppStateRepository,
    private val booksRepository: BooksRepository,
    private val prayersRepository: PrayersRepository,
    private val quranRepository: QuranRepository,
    private val database: AppDatabase
) {

    private var counter by mutableIntStateOf(0)

    fun getLastUpdate() = appStateRepository.getLastDailyUpdateMillis()

    fun rebuildDatabase(activity: Activity) {
        DbUtils.resetDB(activity.applicationContext, database)

        ActivityUtils.restartApplication(activity)
    }

    suspend fun resetTutorials() {
        booksRepository.setShouldShowTutorial(true)
        quranRepository.setShouldShowReaderTutorial(true)
        quranRepository.setShouldShowSurasMenuTutorial(true)
        prayersRepository.setShouldShowBoardTutorial(true)
    }

    fun resetOnboarding() {
        appStateRepository.setOnboardingCompleted(false)
    }

    fun handleTitleClicks(setDevModeEnabled: () -> Unit) {
        if (++counter >= 5) setDevModeEnabled()
    }

    fun getSources() = appStateRepository.getSources()

}