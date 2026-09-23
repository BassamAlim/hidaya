package bassamalim.hidaya.features.books.bookChaptersMenu

import bassamalim.hidaya.core.data.repositories.BooksRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.Book
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BookChaptersDomain @Inject constructor(
    private val booksRepository: BooksRepository
) {

    suspend fun getBook(bookId: Int, language: Language): Flow<Book> =
        booksRepository.getFullBook(bookId, language)

    fun setFavoriteStatus(bookId: Int, chapterNum: Int, newValue: Boolean) {
        booksRepository.setChapterFavorite(bookId, chapterNum, newValue)
    }

    fun getReadingPosition(bookId: Int) = booksRepository.getReadingPosition(bookId)

    fun getLanguage() = LangUtils.getAppLanguage()

}