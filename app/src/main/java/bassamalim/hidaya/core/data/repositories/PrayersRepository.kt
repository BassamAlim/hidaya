package bassamalim.hidaya.core.data.repositories

import android.app.Application
import android.content.res.Resources
import bassamalim.hidaya.R
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.PrayersPreferencesDataSource
import bassamalim.hidaya.core.di.ApplicationScope
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.core.models.PrayerTimeCalculatorSettings
import bassamalim.hidaya.core.utils.LangUtils.withAppLocale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PrayersRepository @Inject constructor(
    private val app: Application,
    private val prayersPreferencesDataSource: PrayersPreferencesDataSource,
    @ApplicationScope private val scope: CoroutineScope
) {

    // Looked up on each use in the app language: Application resources follow the device
    // language before API 33, and a language change doesn't restart the process
    private val resources: Resources get() = app.withAppLocale().resources

    fun getContinuousPrayersNotificationEnabled() =
        prayersPreferencesDataSource.getContinuousPrayersNotificationEnabled()

    fun setContinuousPrayersNotificationEnabled(enabled: Boolean) {
        scope.launch {
            prayersPreferencesDataSource.updateContinuousPrayersNotificationEnabled(enabled)
        }
    }

    fun getPrayerTimesCalculatorSettings() =
        prayersPreferencesDataSource.getPrayerTimeCalculatorSettings()

    fun setPrayerTimesCalculatorSettings(settings: PrayerTimeCalculatorSettings) {
        scope.launch {
            prayersPreferencesDataSource.updatePrayerTimeCalculatorSettings(settings)
        }
    }

    fun getAthanAudioId() = prayersPreferencesDataSource.getAthanAudioId()

    fun setAthanAudioId(audioId: Int) {
        scope.launch {
            prayersPreferencesDataSource.updateAthanAudioId(audioId)
        }
    }

    fun getShouldShowBoardTutorial() =
        prayersPreferencesDataSource.getShouldShowBoardTutorial()

    fun setShouldShowBoardTutorial(shouldShow: Boolean) {
        scope.launch {
            prayersPreferencesDataSource.updateShouldShowBoardTutorial(shouldShow)
        }
    }

    fun getPrayerNames(): Map<Prayer, String> {
        val names = resources.getStringArray(R.array.prayer_names) as Array<String>
        return mapOf(
            Prayer.FAJR to names[0],
            Prayer.SUNRISE to names[1],
            Prayer.DHUHR to names[2],
            Prayer.ASR to names[3],
            Prayer.MAGHRIB to names[4],
            Prayer.ISHAA to names[5]
        )
    }

    fun getPrayerName(prayer: Prayer) =
        resources.getStringArray(R.array.prayer_names)[prayer.ordinal]!!

}