package bassamalim.hidaya.core.nav

import bassamalim.hidaya.core.enums.MenuType
import bassamalim.hidaya.core.enums.Prayer
import bassamalim.hidaya.features.quiz.test.QuizTestDomain
import bassamalim.hidaya.features.quran.reader.QuranTarget
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

// Serial names are the route names, so they stay readable and survive R8 renaming
@Serializable
sealed interface Screen {

    @Serializable @SerialName("about")
    data object About: Screen

    @Serializable @SerialName("book_chapters_menu")
    data class BookChaptersMenu(val bookId: Int): Screen

    @Serializable @SerialName("book_reader")
    data class BookReader(val bookId: Int, val chapterId: Int): Screen

    @Serializable @SerialName("book_searcher")
    data object BookSearcher: Screen

    @Serializable @SerialName("books_menu")
    data object BooksMenu: Screen

    @Serializable @SerialName("books_menu_filter")
    data object BooksMenuFilter: Screen

    @Serializable @SerialName("date_converter")
    data object DateConverter: Screen

    @Serializable @SerialName("date_editor")
    data object DateEditor: Screen

    @Serializable @SerialName("hijri_date_picker")
    data class HijriDatePicker(val initialDate: String): Screen

    @Serializable @SerialName("leaderboard")
    data object Leaderboard: Screen

    @Serializable @SerialName("location_picker")
    data object LocationPicker: Screen

    @Serializable @SerialName("locator")
    data class Locator(val isInitial: Boolean): Screen

    @Serializable @SerialName("main")
    data object Main: Screen

    @Serializable @SerialName("misbaha")
    data object Misbaha: Screen

    @Serializable @SerialName("onboarding")
    data object Onboarding: Screen

    @Serializable @SerialName("prayer_extra_reminder")
    data class PrayerExtraReminderSettings(val prayer: Prayer): Screen

    @Serializable @SerialName("prayer_settings")
    data class PrayerSettings(val prayer: Prayer): Screen

    @Serializable @SerialName("prayer_time_calculation_settings")
    data object PrayerTimeCalculationSettings: Screen

    @Serializable @SerialName("qibla")
    data object Qibla: Screen

    @Serializable @SerialName("quiz_lobby")
    data object QuizLobby: Screen

    @Serializable @SerialName("quiz_test")
    data class QuizTest(val category: String = QuizTestDomain.ALL_CATEGORIES): Screen

    @Serializable @SerialName("quran_reader")
    data class QuranReader(val targetType: QuranTarget, val targetValue: Int = -1): Screen

    @Serializable @SerialName("quran_settings")
    data object QuranSettings: Screen

    @Serializable @SerialName("radio")
    data object Radio: Screen

    @Serializable @SerialName("recitations_player")
    data class RecitationPlayer(val action: String, val mediaId: String): Screen

    @Serializable @SerialName("reciters_menu_filter")
    data object RecitersMenuFilter: Screen

    @Serializable @SerialName("recitations_reciters_menu")
    data object RecitationsRecitersMenu: Screen

    @Serializable @SerialName("recitation_suras_menu")
    data class RecitationSurasMenu(val reciterId: Int, val narrationId: Int): Screen

    @Serializable @SerialName("remembrance_reader")
    data class RemembranceReader(val id: Int): Screen

    @Serializable @SerialName("remembrances_menu")
    data class RemembrancesMenu(val type: MenuType, val categoryId: Int = 0): Screen

    @Serializable @SerialName("settings")
    data object Settings: Screen

    @Serializable @SerialName("tv")
    data object Tv: Screen

    @Serializable @SerialName("verse_guess")
    data object VerseGuess: Screen

    @Serializable @SerialName("verse_info")
    data class VerseInfo(val verseId: Int): Screen

}

/** For passing a destination through an Intent extra. */
fun Screen.toJson() = Json.encodeToString(serializer<Screen>(), this)

/** Null if [json] isn't a known destination (e.g. an extra from an older app version). */
fun screenFromJson(json: String): Screen? =
    try {
        Json.decodeFromString(serializer<Screen>(), json)
    } catch (_: IllegalArgumentException) {  // Includes SerializationException
        null
    }

// The default discriminator "type" clashes with RemembrancesMenu.type, and encoding a route
// with a property named like the discriminator throws. toJson() keeps the default so extras
// in already scheduled intents still decode; it just can't carry RemembrancesMenu.
private const val ROUTE_DISCRIMINATOR = "#route"
private val analyticsJson = Json { classDiscriminator = ROUTE_DISCRIMINATOR }

/** The route name, e.g. "book_reader", without argument values. */
val Screen.analyticsName: String
    get() = analyticsJson.encodeToJsonElement(serializer<Screen>(), this)
        .jsonObject[ROUTE_DISCRIMINATOR]!!.jsonPrimitive.content
