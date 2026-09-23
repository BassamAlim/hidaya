package bassamalim.hidaya.core.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import bassamalim.hidaya.core.enums.Language
import java.util.Locale

object LangUtils {

    private const val LOCALE_PREFS = "app_locale"
    private const val LOCALE_KEY = "language_tags"
    private val enNums = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private val arNums = arrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun getAppLanguage(): Language {
        val appLocale = AppCompatDelegate.getApplicationLocales()
        val languageTag = appLocale.toLanguageTags()
        return getTagLanguage(languageTag)
    }

    /**
     * Before API 33, AppCompat applies the app language to Activities only, so strings from an
     * Application or Service context come out in the device language. Use this for those.
     */
    fun Context.withAppLocale(): Context {
        val locale = AppCompatDelegate.getApplicationLocales()[0]
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || locale == null) return this

        val config = Configuration(resources.configuration).apply { setLocale(locale) }
        return createConfigurationContext(config)
    }

    /**
     * Before API 33, AppCompat only loads the saved app language when an Activity is created, so
     * a process started for an alarm or service (e.g. athan) sees no language at all. We keep a
     * copy of it, saved from the Activity and restored at process start.
     */
    fun saveAppLocale(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return

        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (tags.isNotEmpty()) localePrefs(context).edit { putString(LOCALE_KEY, tags) }
    }

    fun restoreAppLocale(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            || !AppCompatDelegate.getApplicationLocales().isEmpty) return

        val tags = localePrefs(context).getString(LOCALE_KEY, null) ?: return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
    }

    private fun localePrefs(context: Context) =
        context.getSharedPreferences(LOCALE_PREFS, Context.MODE_PRIVATE)

    fun setAppLanguage(language: Language) {
        val appLocale = languageToLocaleList(language)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun translateNums(string: String, numeralsLanguage: Language) : String {
        if (string.isEmpty()) return string

        return when (numeralsLanguage) {
            Language.ARABIC -> {
                val containsEnglish = string.any { enNums.contains(it) }
                if (containsEnglish) enToAr(string) else string
            }
            Language.ENGLISH -> {
                val containsArabic = string.any { arNums.contains(it) }
                if (containsArabic) arToEn(string) else string
            }
        }
    }

    fun translateTimeNums(
        string: String,
        language: Language,
        numeralsLanguage: Language,
        removeLeadingZeros: Boolean = true
    ) : String {
        if (string.isEmpty()) return string

        var str = string

        if (removeLeadingZeros)
            str = removeLeadingZeros(string)

        str = when (numeralsLanguage) {
            Language.ARABIC -> {
                val containsEnglish = str.any { enNums.contains(it) }
                if (containsEnglish) enToAr(str) else str
            }
            Language.ENGLISH -> {
                val containsArabic = str.any { arNums.contains(it) }
                if (containsArabic) arToEn(str) else str
            }
        }

        str = when (language) {
            Language.ARABIC -> suffixEnToAr(str)
            Language.ENGLISH -> suffixArToEn(str)
        }

        return str
    }

    private fun enToAr(english: String): String {
        val temp = StringBuilder()
        for (i in english.indices) {
            val index = enNums.indexOf(english[i])
            if (index == -1) temp.append(english[i])
            else temp.append(arNums[index])
        }
        return temp.toString()
    }

    private fun suffixEnToAr(english: String): String {
        return english
            .replace(Regex("am"), "ص")
            .replace(Regex("AM"), "ص")
            .replace(Regex("pm"), "م")
            .replace(Regex("PM"), "م")
    }

    private fun arToEn(arabic: String): String {
        val temp = StringBuilder()
        for (i in arabic.indices) {
            val index = arNums.indexOf(arabic[i])
            if (index == -1) temp.append(arabic[i])
            else temp.append(enNums[index])
        }
        return temp.toString()
    }

    private fun suffixArToEn(arabic: String): String {
        return arabic
            .replace(Regex("ص"), "am")
            .replace(Regex("م"), "pm")
    }
    
    private fun removeLeadingZeros(string: String) : String {
        val zeros = listOf("٠", "0")

        var str = string
        for (zero in zeros) {
            if (str.startsWith(zero)) {
                str = str.replaceFirst(zero, "")
                if (str.startsWith(zero)) {
                    str = str.replaceFirst("$zero:", "")
                    if (str.startsWith(zero) && !str.startsWith("$zero$zero"))
                        str = str.replaceFirst(zero, "")
                }
            }
        }
        return str
    }

    fun languageToLocale(language: Language): Locale {
        val languageTag = getLanguageTag(language)
        return Locale.forLanguageTag(languageTag)
    }

    fun languageToLocaleList(language: Language): LocaleListCompat {
        val languageTag = getLanguageTag(language)
        return LocaleListCompat.forLanguageTags(languageTag)
    }

    private fun getLanguageTag(language: Language) =
        when (language) {
            Language.ARABIC -> "ar"
            Language.ENGLISH -> "en"
        }

    private fun getTagLanguage(languageTag: String) =
        when (languageTag) {
            "ar" -> Language.ARABIC
            "en" -> Language.ENGLISH
            else -> Language.ARABIC
        }

}