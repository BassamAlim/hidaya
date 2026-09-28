package bassamalim.hidaya.features.quran.surasMenu


open class SearchMatch()

data class SuraMatch(
    val id: Int,
    val decoratedName: String,
    val plainName: String,
    val isFavorite: Boolean
) : SearchMatch()

data class PageMatch(
    val page: Int,
    val num: String,
    val suraName: String
) : SearchMatch()