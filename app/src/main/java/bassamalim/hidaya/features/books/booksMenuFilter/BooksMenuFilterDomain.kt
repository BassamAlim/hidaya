package bassamalim.hidaya.features.books.booksMenuFilter

import bassamalim.hidaya.core.data.repositories.BooksRepository
import bassamalim.hidaya.core.enums.Language
import bassamalim.hidaya.core.utils.LangUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BooksMenuFilterDomain @Inject constructor(
    private val booksRepository: BooksRepository
) {

    suspend fun getOptions(language: Language) = booksRepository.getSearchSelections().map {
        it.map { (id, isSelected) ->
            id to BooksMenuFilterItem(
                name = booksRepository.getBookTitle(id, language),
                isSelected = isSelected
            )
        }.toMap()
    }.first()

    suspend fun setOptions(options: Map<Int, Boolean>) {
        booksRepository.setSearchSelections(options)
    }

    fun getLanguage() = LangUtils.getAppLanguage()

}