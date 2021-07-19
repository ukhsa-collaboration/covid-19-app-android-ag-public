package uk.nhs.nhsx.covid19.android.app.settings.languages

import android.annotation.SuppressLint
import android.content.res.Resources
import uk.nhs.nhsx.covid19.android.app.SupportedLanguage
import uk.nhs.nhsx.covid19.android.app.common.BuildVersionProvider
import javax.inject.Inject

class GetDefaultSystemLanguage @Inject constructor(
    private val versionProvider: BuildVersionProvider
) {
    @SuppressLint("NewApi")
    operator fun invoke(): SupportedLanguage {
        val preferredLocale =
            if (versionProvider.version() >= 24) {
                val supportedLocales = SupportedLanguage.values()
                    .map { it.code }
                    .toTypedArray()
                Resources.getSystem().configuration.locales.getFirstMatch(supportedLocales)
            } else {
                Resources.getSystem().configuration.locale
            }
        return preferredLocale?.language.let { preferredLanguage ->
            SupportedLanguage.values().find {
                it.code == preferredLanguage
            }
        } ?: SupportedLanguage.ENGLISH
    }
}
