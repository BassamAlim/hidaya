package bassamalim.hidaya.features.books.booksMenu

import android.util.Log
import bassamalim.hidaya.core.Globals
import bassamalim.hidaya.core.data.repositories.AnalyticsRepository
import bassamalim.hidaya.core.data.repositories.BooksRepository
import bassamalim.hidaya.core.enums.DownloadState
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.AnalyticsEvent
import bassamalim.hidaya.core.utils.LangUtils
import bassamalim.hidaya.core.utils.report
import javax.inject.Inject

class BooksMenuDomain @Inject constructor(
    private val booksRepository: BooksRepository,
    private val analyticsRepository: AnalyticsRepository
) {

    fun getLanguage() = LangUtils.getAppLanguage()

    suspend fun getBooks(language: Language) = booksRepository.getBooksMenu(language).associate {
        it.id to Book(
            title = it.title,
            author = it.author,
            url = it.url,
            isFavorite = it.isFavorite,
            downloadState = getDownloadState(it.id)
        )
    }

    private fun getDownloadState(bookId: Int) =
        if (booksRepository.isDownloaded(bookId)) {
            if (booksRepository.isDownloading(bookId)) DownloadState.DOWNLOADING
            else DownloadState.DOWNLOADED
        }
        else DownloadState.NOT_DOWNLOADED

    fun downloadBook(bookId: Int, onDownloadedCallback: () -> Unit, onFailedCallback: () -> Unit) {
        booksRepository.download(bookId)
            .addOnSuccessListener {
                Log.i(Globals.TAG, "File download succeeded")

                onDownloadedCallback()
            }
            .addOnFailureListener { e ->
                Log.e(Globals.TAG, "File download failed")
                e.report()

                onFailedCallback()
            }
    }

    fun deleteBook(bookId: Int) {
        booksRepository.deleteBook(bookId)
    }

    fun getShowTutorial() = booksRepository.getShouldShowTutorial()

    fun setTutorialSeen() {
        booksRepository.setShouldShowTutorial(false)
    }

    fun trackBookOpened(bookId: Int) {
        analyticsRepository.trackEvent(AnalyticsEvent.BookOpened(bookId))
    }

}