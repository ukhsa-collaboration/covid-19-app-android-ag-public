package uk.nhs.nhsx.covid19.android.app.common

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Locale

@Parcelize
data class Translatable(val translations: Map<String, String>) : Parcelable {

    constructor() : this(mapOf())

    fun translate(): String {
        val languageAndRegion = Locale.getDefault().toLanguageTag()

        if (translations.containsKey(languageAndRegion)) {
            return translations[languageAndRegion] ?: ""
        }

        val languageCodeOnly = Locale.getDefault().language

        val firstMatchedLanguageCode = translations.keys
            .firstOrNull { translationsLanguageAndRegion ->
                getLanguageCode(
                    translationsLanguageAndRegion
                ) == languageCodeOnly
            }

        return translations[firstMatchedLanguageCode ?: fallbackLanguageAndRegion] ?: ""
    }

    private fun getLanguageCode(languageAndRegion: String) =
        if (languageAndRegion.contains("-")) languageAndRegion.split("-")[0] else languageAndRegion

    companion object {
        private const val fallbackLanguageAndRegion = "en-GB"
    }
}
