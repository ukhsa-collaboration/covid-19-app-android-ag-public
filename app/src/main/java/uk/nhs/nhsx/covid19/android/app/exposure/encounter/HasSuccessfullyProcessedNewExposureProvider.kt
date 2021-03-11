package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HasSuccessfullyProcessedNewExposureProvider @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<Boolean>(VALUE_KEY)

    var value: Boolean? by prefs

    companion object {
        const val VALUE_KEY = "HAS_FAILED_PROCESSING_NEW_EXPOSURE_KEY"
    }
}
