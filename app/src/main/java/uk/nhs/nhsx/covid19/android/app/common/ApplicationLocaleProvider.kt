package uk.nhs.nhsx.covid19.android.app.common

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import uk.nhs.nhsx.covid19.android.app.settings.languages.GetDefaultSystemLanguage
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationLocaleProvider @Inject constructor(
    sharedPreferences: SharedPreferences,
    private val getDefaultSystemLanguage: GetDefaultSystemLanguage,
) {
    private var languagePref = sharedPreferences.with<String>(APPLICATION_LANGUAGE_KEY)

    var languageCode: String? by languagePref

    fun getLocale(): Locale {
        val code = languageCode
        return if (code.isNullOrEmpty()) Locale(getDefaultSystemLanguage().code) else Locale(code)
    }

    fun getUserSelectedLanguage(): SupportedLanguage? = SupportedLanguage.values().find {
        it.code == languageCode
    }

    fun getDefaultSystemLanguage(): SupportedLanguage = getDefaultSystemLanguage.invoke()

    companion object {
        const val APPLICATION_LANGUAGE_KEY = "APPLICATION_LANGUAGE_KEY"
    }
}
