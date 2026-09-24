package bassamalim.hidaya.features.books.bookReader

import bassamalim.hidaya.core.data.dataSources.preferences.objects.BookReadingPosition
import bassamalim.hidaya.core.data.repositories.BooksRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.utils.LangUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookReaderDomain @Inject constructor(
    private val booksRepository: BooksRepository
) {

    suspend fun getBookTitle(bookId: Int, language: Language) =
        booksRepository.getBookTitle(bookId, language)

    fun getDoors(bookId: Int, chapterId: Int) = booksRepository.getDoors(bookId, chapterId)

    fun getChapterTitles(bookId: Int) = booksRepository.getChapterTitles(bookId)

    fun getReadingPosition(bookId: Int) = booksRepository.getReadingPosition(bookId)

    suspend fun setReadingPosition(bookId: Int, position: BookReadingPosition) {
        booksRepository.setReadingPosition(bookId, position)
    }

    fun getTextSize() = booksRepository.getTextSize()

    suspend fun setTextSize(textSize: Float) {
        booksRepository.setTextSize(textSize)
    }

    fun getLanguage() = LangUtils.getAppLanguage()

}
