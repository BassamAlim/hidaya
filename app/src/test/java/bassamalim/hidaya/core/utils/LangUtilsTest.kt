package bassamalim.hidaya.core.utils

import bassamalim.hidaya.core.enums.Language
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** Which language the app reports, which must match the language its strings show in. */
class LangUtilsTest {

    private fun resolve(app: String?, vararg device: String) = LangUtils.resolveLanguage(
        appLocale = app?.let(Locale::forLanguageTag),
        deviceLocales = device.map(Locale::forLanguageTag)
    )

    @Test
    fun `a saved app language wins, region tags included`() {
        assertEquals(Language.ENGLISH, resolve("en", "ar"))
        assertEquals(Language.ENGLISH, resolve("en-US", "ar"))
        assertEquals(Language.ARABIC, resolve("ar-SA", "en"))
    }

    @Test
    fun `with none saved, an English device gets English`() {
        assertEquals(Language.ENGLISH, resolve(null, "en-GB"))
    }

    @Test
    fun `with none saved, the first device locale the app has strings for decides`() {
        assertEquals(Language.ENGLISH, resolve(null, "fr-FR", "en-US"))
        assertEquals(Language.ARABIC, resolve(null, "fr-FR", "ar-EG", "en-US"))
    }

    @Test
    fun `with none saved and no Arabic or English, the default strings are Arabic`() {
        assertEquals(Language.ARABIC, resolve(null, "fr-FR"))
        assertEquals(Language.ARABIC, resolve(null))
    }

    @Test
    fun `percentages take the numerals' digits and signs`() {
        assertEquals("24.3%", LangUtils.formatPercent(24.31, 1, Language.ENGLISH))
        assertEquals("٢٤٫٣٪", LangUtils.formatPercent(24.31, 1, Language.ARABIC))
        assertEquals("80%", LangUtils.formatPercent(80.0, 0, Language.ENGLISH))
    }

}
