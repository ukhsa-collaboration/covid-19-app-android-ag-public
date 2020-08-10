package uk.nhs.nhsx.covid19.android.app.analytics

import android.content.SharedPreferences
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject

class AnalyticsLastSubmittedDate @Inject constructor(sharedPreferences: SharedPreferences) {

    private val lastSubmittedDatePrefs = sharedPreferences.with<String>(LAST_SUBMITTED_DATE_KEY)

    var lastSubmittedDate by lastSubmittedDatePrefs

    companion object {
        const val LAST_SUBMITTED_DATE_KEY = "LAST_SUBMITTED_DATE_KEY"
    }
}
