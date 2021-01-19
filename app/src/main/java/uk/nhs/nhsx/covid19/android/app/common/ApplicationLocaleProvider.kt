package uk.nhs.nhsx.covid19.android.app.common

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build.VERSION_CODES
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationLocaleProvider @Inject constructor(
    sharedPreferences: SharedPreferences,
    private val versionProvider: BuildVersionProvider
) {
    private var languagePref = sharedPreferences.with<String>(APPLICATION_LANGUAGE_KEY)

    var languageCode: String? by languagePref

    fun getLocale(): Locale =
        if (languageCode.isNullOrEmpty()) Locale.getDefault() else Locale(languageCode!!)

    fun getUserSelectedLanguage(): SupportedLanguage? = SupportedLanguage.values().find {
        it.code != null && it.code == languageCode
    }

    @SuppressLint("NewApi")
    fun getSystemLanguage(): SupportedLanguage {
        val preferredLocale =
            if (versionProvider.version() >= VERSION_CODES.N) {
                val supportedLocales = SupportedLanguage.values()
                    .filter { it.code != null }
                    .map { it.code!! }
                    .toTypedArray()
                Resources.getSystem().configuration.locales.getFirstMatch(supportedLocales)
            } else {
                Resources.getSystem().configuration.locale
            }
        return preferredLocale?.language.let { preferredLanguage ->
            SupportedLanguage.values().find {
                it.code != null && it.code == preferredLanguage
            }
        } ?: SupportedLanguage.ENGLISH
    }

    companion object {
        const val APPLICATION_LANGUAGE_KEY = "APPLICATION_LANGUAGE_KEY"
    }
}
