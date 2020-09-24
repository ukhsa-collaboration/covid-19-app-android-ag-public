package uk.nhs.nhsx.covid19.android.app.onboarding

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingCompletedProvider @Inject constructor(sharedPreferences: SharedPreferences) {

    private val prefs = sharedPreferences.with<Boolean>(VALUE_KEY)

    var value: Boolean? by prefs

    companion object {
        private const val VALUE_KEY = "ONBOARDING_COMPLETED"
    }
}
