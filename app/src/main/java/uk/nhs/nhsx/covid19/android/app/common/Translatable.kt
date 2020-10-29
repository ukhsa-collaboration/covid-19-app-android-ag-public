package uk.nhs.nhsx.covid19.android.app.common

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import java.util.Locale

@Parcelize
@JsonClass(generateAdapter = true)
data class Translatable(val translations: Map<String, String>) : Parcelable {

    fun translate(): String {
        val languageAndRegion = Locale.getDefault().toLanguageTag()
        val languageCodeOnly = Locale.getDefault().language

        if (!isLanguageSupported(languageCodeOnly)) {
            return translations[fallbackLanguageAndRegion] ?: translations[fallbackLanguage] ?: ""
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
            ?: translations[fallbackLanguage] ?: ""
    }

    private fun getLanguageCode(languageAndRegion: String) =
        if (languageAndRegion.contains("-")) languageAndRegion.split("-")[0] else languageAndRegion

    private fun isLanguageSupported(languageCode: String) = SupportedLanguage.values()
        .mapNotNull { it.code }
        .any {
            it == languageCode
        }

    fun replace(oldValue: String, newValue: String): Map<String, String> =
        translations.mapValues { it.value.replace(oldValue, newValue) }

    companion object {
        private const val fallbackLanguageAndRegion = "en-GB"
        private const val fallbackLanguage = "en"
    }
}
