package bassamalim.hidaya.core.data.repositories

import android.app.Application
import bassamalim.hidaya.core.data.dataSources.preferences.dataSources.BooksPreferencesDataSource
import bassamalim.hidaya.core.data.dataSources.preferences.objects.BookReadingPosition
import bassamalim.hidaya.core.data.dataSources.room.daos.BooksDao
import bassamalim.hidaya.core.di.ApplicationScope
import bassamalim.hidaya.core.di.DefaultDispatcher
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.models.Book
import bassamalim.hidaya.core.models.BookContent
import bassamalim.hidaya.core.models.BookInfo
import bassamalim.hidaya.core.utils.FileUtils
import bassamalim.hidaya.core.utils.report
import com.google.firebase.Firebase
import com.google.firebase.storage.FileDownloadTask
import com.google.firebase.storage.storage
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class BooksRepository @Inject constructor(
    private val app: Application,
    private val booksDao: BooksDao,
    private val booksPreferencesDataSource: BooksPreferencesDataSource,
    private val appSettingsRepository: AppSettingsRepository,
    private val gson: Gson,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
    @ApplicationScope private val scope: CoroutineScope
) {

    private val prefix = "/Books/"
    private val dir = "${app.getExternalFilesDir(null)}/Books/"

    suspend fun getBooksMenu(language: Language) = withContext(dispatcher) {
        booksDao.getAll().map {
            BookInfo(
                id = it.id,
                title = when (language) {
                    Language.ARABIC -> it.titleAr
                    Language.ENGLISH -> it.titleEn
                },
                author = when (language) {
                    Language.ARABIC -> it.authorAr
                    Language.ENGLISH -> it.authorEn
                },
                url = it.url,
                isFavorite = it.isFavorite == 1
            )
        }
    }

    suspend fun getBookInfo(id: Int, language: Language) = withContext(dispatcher) {
        booksDao.getById(id).let {
            BookInfo(
                id = it.id,
                title = when (language) {
                    Language.ARABIC -> it.titleAr
                    Language.ENGLISH -> it.titleEn
                },
                author = when (language) {
                    Language.ARABIC -> it.authorAr
                    Language.ENGLISH -> it.authorEn
                },
                url = it.url,
                isFavorite = it.isFavorite == 1
            )
        }
    }

    /**
     * Returns the downloaded content of the book, or null if it isn't downloaded yet or its file
     * is still incomplete or corrupt.
     */
    private fun getBookContent(bookId: Int): BookContent? {
        if (!isDownloaded(bookId)) return null
        val jsonStr = FileUtils.getJsonFromDownloads("$dir$bookId.json")
        return try {
            gson.fromJson(jsonStr, BookContent::class.java)
        } catch (e: JsonParseException) {
            e.report()
            null
        }
    }

    suspend fun getBookContents(language: Language): Map<Int, BookContent> {
        if (!File(dir).exists()) return emptyMap()

        return getBooksMenu(language).mapNotNull { bookInfo ->
            getBookContent(bookInfo.id)?.let { content -> bookInfo.id to content }
        }.toMap()
    }

    suspend fun getFullBook(bookId: Int, language: Language): Flow<Book> {
        val bookInfo = getBookInfo(bookId, language)
        val bookContent = getBookContent(bookId)
            ?: return flowOf(Book(id = bookInfo.id, title = bookInfo.title, chapters = emptyList()))
        val favorites = getChapterFavorites(bookId)

        return favorites.map {
            Book(
                id = bookInfo.id,
                title = bookInfo.title,
                chapters = bookContent.chapters.map { chapter ->
                    Book.Chapter(
                        id = chapter.id,
                        title = chapter.title,
                        doors = chapter.doors.map { door ->
                            Book.Chapter.Door(
                                id = door.id,
                                title = door.title,
                                text = door.text
                            )
                        },
                        isFavorite = it[chapter.id] ?: false
                    )
                }
            )
        }
    }

    suspend fun getBookTitles(language: Language) = withContext(dispatcher) {
        when (language) {
            Language.ARABIC -> booksDao.getTitlesAr()
            Language.ENGLISH -> booksDao.getTitlesEn()
        }
    }

    suspend fun getBookTitle(bookId: Int, language: Language) = withContext(dispatcher) {
        when (language) {
            Language.ARABIC -> booksDao.getTitleAr(bookId)
            Language.ENGLISH -> booksDao.getTitleEn(bookId)
        }
    }

    fun getDoors(bookId: Int, chapterId: Int) =
        getBookContent(bookId)?.chapters?.getOrNull(chapterId)?.doors?.toList() ?: emptyList()

    fun getChapterFavorites(bookId: Int): Flow<Map<Int, Boolean>> {
        val bookContent = getBookContent(bookId)

        return booksPreferencesDataSource.getChapterFavorites().map {
            if (it.containsKey(bookId)) it[bookId]!!.toMap()
            else if (bookContent == null) emptyMap()
            else {
                val favs = bookContent.chapters.associate { it.id to false }
                booksPreferencesDataSource.updateChapterFavorites(
                    it.mutate { oldMap -> oldMap[bookId] = favs.toPersistentMap() }
                )
                favs
            }
        }
    }

    fun setChapterFavorite(bookId: Int, chapterNum: Int, newValue: Boolean) {
        scope.launch {
            booksPreferencesDataSource.updateChapterFavorites(
                booksPreferencesDataSource.getChapterFavorites().first().mutate { oldMap ->
                    oldMap[bookId] = (oldMap[bookId] ?: persistentMapOf<Int, Boolean>())
                        .mutate { it[chapterNum] = newValue }
                }
            )
        }
    }

    fun getChapterTitles(bookId: Int) =
        getBookContent(bookId)?.chapters?.map { it.title } ?: emptyList()

    fun getReadingPosition(bookId: Int) =
        booksPreferencesDataSource.getReadingPositions().map { it[bookId] }

    suspend fun setReadingPosition(bookId: Int, position: BookReadingPosition) {
        booksPreferencesDataSource.updateReadingPosition(bookId, position)
    }

    fun getTextSize() = booksPreferencesDataSource.getTextSize()

    suspend fun setTextSize(textSize: Float) {
        booksPreferencesDataSource.updateTextSize(textSize)
    }

    fun getSearchSelections() =
        booksPreferencesDataSource.getSearchSelections().map {
            it.ifEmpty {
                val books = getBooksMenu(Language.ARABIC)
                it.mutate { oldMap ->
                    books.forEach { book -> oldMap[book.id] = true }
                }
            }.map { (id, isSelected) ->
                id to if (isDownloaded(id)) isSelected else false
            }.toMap()
        }

    suspend fun setSearchSelections(selections: Map<Int, Boolean>) {
        booksPreferencesDataSource.updateSearchSelections(selections.toPersistentMap())
    }

    fun getSearchMaxMatches() = booksPreferencesDataSource.getSearchMaxMatches()

    suspend fun setSearchMaxMatches(searchMaxMatches: Int) {
        booksPreferencesDataSource.updateSearchMaxMatches(searchMaxMatches)
    }

    fun getShouldShowTutorial() = booksPreferencesDataSource.getShouldShowTutorial()

    fun setShouldShowTutorial(shouldShowTutorial: Boolean) {
        scope.launch {
            booksPreferencesDataSource.updateShouldShowTutorial(shouldShowTutorial)
        }
    }

    fun download(bookId: Int): FileDownloadTask {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val fileRef = storageRef.child("${prefix.substring(1)}$bookId.json")

        FileUtils.createDir(app, prefix)
        val file = File("$dir${fileRef.name}")
        file.createNewFile()

        val task = fileRef.getFile(file)
        // The stub file is created before the download starts, so it has to be removed on failure
        // or cancellation, otherwise the book keeps showing up as still downloading.
        task.addOnFailureListener { file.delete() }

        return task
    }

    fun isDownloaded(id: Int): Boolean {
        val files = File(dir).listFiles() ?: return false
        return files.any { it.name == "$id.json" }
    }

    /**
     * The file is created as soon as the download starts, so a file that exists but can't be parsed
     * yet is still being downloaded (or was left behind by a failed download).
     */
    fun isDownloading(bookId: Int) = isDownloaded(bookId) && getBookContent(bookId) == null

    fun deleteBook(bookId: Int) {
        FileUtils.deleteFile(context = app, path = "${prefix}$bookId.json")
    }

}