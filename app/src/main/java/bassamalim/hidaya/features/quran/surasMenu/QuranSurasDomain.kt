package bassamalim.hidaya.features.quran.surasMenu

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import bassamalim.hidaya.core.data.dataSources.room.entities.Verse
import bassamalim.hidaya.core.data.repositories.AnalyticsRepository
import bassamalim.hidaya.core.data.repositories.AppSettingsRepository
import bassamalim.hidaya.core.data.repositories.QuranRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.helpers.Searcher
import bassamalim.hidaya.core.models.AnalyticsEvent
import bassamalim.hidaya.core.models.Sura
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.flow.first
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranSurasDomain @Inject constructor(
    private val quranRepository: QuranRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val analyticsRepository: AnalyticsRepository
) {

    private val suraSearcher = Searcher<Sura>()
    private val verseSearcher = Searcher<Verse>()

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getNumeralsLanguage() = appSettingsRepository.getNumeralsLanguage().first()

    fun getBookmarks() = quranRepository.getBookmarks()

    suspend fun getShouldShowTutorial() =
        quranRepository.getShouldShowSurasMenuTutorial().first()

    suspend fun setTutorialSeen() {
        quranRepository.setShouldShowSurasMenuTutorial(false)
    }

    fun getAllSuras(language: Language) = quranRepository.observeAllSuras(language)

    suspend fun getAllVerses() = quranRepository.getAllVerses()

    suspend fun getSuraNames(language: Language) = quranRepository.getDecoratedSuraNames(language)

    suspend fun setFav(suraId: Int, fav: Boolean) {
        quranRepository.setSuraFavoriteStatus(suraId, fav)
    }

    fun searchSuras(query: String, items: List<Sura>, limit: Int): List<SearchMatch> {
        trackQuranSearchPerformed(query)

        return suraSearcher.containsSearch(
            items = items,
            query = query,
            keySelector = { it.plainName },
            limit = limit
        ).map { result ->
            SuraMatch(
                id = result.id,
                decoratedName = result.decoratedName,
                plainName = result.plainName,
                isFavorite = result.isFavorite
            )
        }
    }

    fun searchVerses(
        query: String,
        items: List<Verse>,
        suraNames: List<String>,
        numeralsLanguage: Language,
        highlightColor: Color
    ): List<VerseMatch> {
        val searchResults = verseSearcher.containsSearch(
            items = items,
            query = query,
            keySelector = { it.plainText },
            limit = 100
        )

        val matches = mutableListOf<VerseMatch>()
        val normalizedQueryPattern = Pattern.compile(verseSearcher.normalizeString(query))

        for (verse in searchResults) {
            val normalizedVerse = verseSearcher.normalizeString(verse.plainText, trim = false)
            val matcher = normalizedQueryPattern.matcher(normalizedVerse)

            if (!matcher.find()) continue

            val annotatedString = buildAnnotatedString {
                append(verse.plainText)

                do {
                    addStyle(
                        style = SpanStyle(highlightColor),
                        start = matcher.start(),
                        end = matcher.end()
                    )
                } while (matcher.find())
            }

            matches.add(
                VerseMatch(
                    id = verse.id,
                    verseNum = LangUtils.translateNums(
                        string = verse.num.toString(),
                        numeralsLanguage = numeralsLanguage
                    ),
                    suraName = suraNames[verse.suraNum - 1],
                    text = annotatedString
                )
            )
        }
        return matches
    }

    fun trackSuraViewed(suraName: String) {
        analyticsRepository.trackEvent(AnalyticsEvent.QuranSuraViewed(suraName))
    }

    private fun trackQuranSearchPerformed(query: String) {
        analyticsRepository.trackEvent(AnalyticsEvent.QuranSearchPerformed(query))
    }

}