package bassamalim.hidaya.features.quran.reader

import android.app.Application
import android.os.Handler
import android.os.Looper
import bassamalim.hidaya.core.data.repositories.AnalyticsRepository
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.data.repositories.UserRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.AnalyticsEvent
import bassamalim.hidaya.core.di.ApplicationScope
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranReaderDomain @Inject constructor(
    private val app: Application,
    private val quranRepository: QuranRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val userRepository: UserRepository,
    private val analyticsRepository: AnalyticsRepository,
    @ApplicationScope private val appScope: CoroutineScope
) {

    private val handler = Handler(Looper.getMainLooper())
    private var lastRecordedPage = 0
    private lateinit var getPageNumCallback: () -> Int

    fun setPageNumCallback(callback: () -> Int) {
        getPageNumCallback = callback
    }

    private val runnable = Runnable {
        if (getPageNumCallback() == lastRecordedPage) {
            appScope.launch {
                updateRecords()
            }
        }
    }

    private suspend fun updateRecords() {
        val newRecord = getPagesRecord().first() + 1
        setPagesRecord(newRecord)

        val pageNum = getPageNumCallback()
        if (pageNum == getWerdPage().first())
            setWerdDone()

        trackQuranPageRead(pageNum)
    }

    fun handlePageChange(pageNum: Int) {
        if (pageNum != lastRecordedPage) {
            handler.removeCallbacks(runnable)
            checkPage()
        }
    }

    fun stopHandler() {
        handler.removeCallbacks(runnable)
    }

    private fun checkPage() {
        lastRecordedPage = getPageNumCallback()
        handler.postDelayed(runnable, 40000)
    }

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    suspend fun getSuraPageNum(suraId: Int) = quranRepository.getSuraPageNum(suraId)

    suspend fun getVersePageNum(verseId: Int) = quranRepository.getVersePageNum(verseId)

    suspend fun getAllVerses() = quranRepository.getAllVerses()

    suspend fun getSuraNames(language: Language) = quranRepository.getDecoratedSuraNames(language)

    fun getViewType() = quranRepository.getViewType()

    fun getFillPage() = quranRepository.getFillPage()

    suspend fun getShouldShowTutorial() = quranRepository.getShouldShowReaderTutorial().first()

    fun getTextSize() = quranRepository.getTextSize()

    fun getKeepScreenOn() = quranRepository.getKeepScreenOn()

    fun getBookmarks() = quranRepository.getBookmarks()

    private fun getPagesRecord() = userRepository.getLocalRecord().map {
        it.quranPages
    }

    private suspend fun setPagesRecord(record: Int) {
        userRepository.setLocalRecord(
            userRepository.getLocalRecord().first().copy(
                quranPages = record
            )
        )
    }

    private fun getWerdPage() = quranRepository.getWerdPageNum()

    private suspend fun setWerdDone() {
        quranRepository.setWerdDone(true)
    }

    suspend fun setDoNotShowTutorial() {
        quranRepository.setShouldShowReaderTutorial(false)
    }

    fun getScreenHeight() = app.resources.displayMetrics.heightPixels

    fun trackPageViewed(pageNum: Int) {
        analyticsRepository.trackEvent(AnalyticsEvent.QuranPageViewed(pageNum))
    }

    private fun trackQuranPageRead(pageNum: Int) {
        analyticsRepository.trackEvent(AnalyticsEvent.QuranPageRead(pageNum))
    }

}
