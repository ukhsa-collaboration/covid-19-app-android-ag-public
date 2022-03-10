package uk.nhs.nhsx.covid19.android.app.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import java.util.Locale
import javax.inject.Inject

interface Translatable<T> : Parcelable {
    val translations: Map<String, T>

    @Deprecated(
        message = "Utilises static Locale call, making testing difficult.  Please pass in the Locale to use",
        replaceWith = ReplaceWith("translateOrNull(locale)")
    )
    fun translateOrNull(): T? = translateOrNull(Locale.getDefault())

    fun translateOrNull(locale: Locale): T? {
        val languageAndRegion = locale.toLanguageTag()
        val languageCodeOnly = locale.language

        if (!isLanguageSupported(languageCodeOnly)) {
            return translations[fallbackLanguageAndRegion] ?: translations[fallbackLanguage]
        }

        val exactMatch = translations[languageAndRegion]
        if (exactMatch != null) {
            return exactMatch
        }

        val firstMatchedLanguageCode = translations.keys
            .firstOrNull { translationsLanguageAndRegion ->
                getLanguageCode(
                    translationsLanguageAndRegion
                ) == languageCodeOnly
            }

        return translations[firstMatchedLanguageCode ?: fallbackLanguageAndRegion]
            ?: translations[fallbackLanguage]
    }

    private fun getLanguageCode(languageAndRegion: String) =
        if (languageAndRegion.contains("-")) languageAndRegion.split("-")[0] else languageAndRegion

    private fun isLanguageSupported(languageCode: String) = SupportedLanguage.values()
        .map { it.code }
        .any { it == languageCode }

    companion object {
        private const val fallbackLanguageAndRegion = "en-GB"
        private const val fallbackLanguage = "en"
    }
}

@Parcelize
data class TranslatableString(override val translations: Map<String, String>) : Translatable<String> {
    fun replace(oldValue: String, newValue: String): TranslatableString =
        TranslatableString(translations.mapValues { it.value.replace(oldValue, newValue) })

    @Deprecated(
        message = "Utilises static Locale call, making testing difficult.  Please pass in the Locale to use",
        replaceWith = ReplaceWith("translate(locale)")
    )
    fun translate(): String = translateOrNull() ?: ""
    fun translate(locale: Locale): String = translateOrNull(locale) ?: ""
}

class LocaleProvider @Inject constructor() {
    fun default() = Locale.getDefault()
}
