package bassamalim.hidaya.features.books.bookSearcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import bassamalim.hidaya.core.data.repositories.BooksRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.helpers.Searcher
import bassamalim.hidaya.core.models.BookContent
import bassamalim.hidaya.core.utils.LangUtils
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookSearcherDomain @Inject constructor(
    private val booksRepository: BooksRepository
) {

    private val searcher = Searcher<BookContent.Chapter.Door>()

    suspend fun search(
        query: String,
        bookSelections: Map<Int, Boolean>,
        maxMatches: Int,
        language: Language,
        highlightColor: Color
    ): List<BookSearcherMatch> {
        val matches = mutableListOf<BookSearcherMatch>()

        val normalizedQuery = searcher.normalizeString(query)
        val normalizedQueryPattern = Pattern.compile(normalizedQuery)

        val bookContents = booksRepository.getBookContents(language)
        for ((bookId, bookContent) in bookContents) {
            if (bookSelections[bookId] != true || !booksRepository.isDownloaded(bookId))
                continue

            for ((c, chapter) in bookContent.chapters.withIndex()) {
                val searchResults = searcher.containsSearch(
                    items = chapter.doors.toList(),
                    query = normalizedQuery,
                    keySelector = { it.text },
                    limit = maxMatches
                )

                for ((d, door) in searchResults.withIndex()) {
                    val normalizedText = searcher.normalizeString(door.text, trim = false)
                    val matcher = normalizedQueryPattern.matcher(normalizedText)

                    if (!matcher.find()) continue

                    val annotatedString = buildAnnotatedString {
                        append(door.text)

                        do {
                            addStyle(
                                style = SpanStyle(color = highlightColor),
                                start = matcher.start(),
                                end = matcher.end()
                            )
                        } while (matcher.find())
                    }

                    matches.add(
                        BookSearcherMatch(
                            bookId = bookId,
                            bookTitle = bookContent.info.title,
                            chapterId = c,
                            chapterTitle = chapter.title,
                            doorId = d,
                            doorTitle = door.title,
                            text = annotatedString
                        )
                    )

                    if (matches.size == maxMatches)
                        return matches
                }
            }
        }

        return matches
    }

    fun getLanguage() = LangUtils.getAppLanguage()

    fun getBookSelections() = booksRepository.getSearchSelections()

    fun getMaxMatches() = booksRepository.getSearchMaxMatches()

    suspend fun setMaxMatches(value: Int) {
        booksRepository.setSearchMaxMatches(value)
    }

    suspend fun getBookTitles(language: Language) = booksRepository.getBookTitles(language)

}