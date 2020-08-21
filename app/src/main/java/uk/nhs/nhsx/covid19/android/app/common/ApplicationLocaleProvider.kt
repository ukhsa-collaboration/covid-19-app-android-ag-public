package uk.nhs.nhsx.covid19.android.app.common

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.util.Locale

class ApplicationLocaleProvider(
    sharedPreferences: SharedPreferences,
    languageCode: String? = null
) {
    private var languagePref = sharedPreferences.with<String>(APPLICATION_LANGUAGE_KEY)

    var language by languagePref

    init {
        language = languageCode
    }

    fun getLocale(): Locale =
        if (language.isNullOrEmpty()) Locale.getDefault() else Locale(language!!)

    companion object {
        const val APPLICATION_LANGUAGE_KEY = "APPLICATION_LANGUAGE_KEY"
    }
}
