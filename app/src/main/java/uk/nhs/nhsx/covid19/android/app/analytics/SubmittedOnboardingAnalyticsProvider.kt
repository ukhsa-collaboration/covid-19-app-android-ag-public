package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class SubmittedOnboardingAnalyticsProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<Boolean>(VALUE_KEY)

    var value: Boolean? by prefs

    companion object {
        private const val VALUE_KEY = "SUBMITTED_ONBOARDING"
    }
}
